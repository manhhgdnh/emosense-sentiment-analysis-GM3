package api;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.*;

/**
 * Optional external sentiment analyzer backed by the Gemini API.
 *
 * The API key is read from the GEMINI_API_KEY environment variable and is
 * intentionally never stored in source code.
 */
public class ApiAnalyzer {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final int TIMEOUT_MS = 8000;

    private static final String SYSTEM_PROMPT =
        "Tu es un moteur d'analyse de sentiment. Analyse le texte suivant et réponds UNIQUEMENT avec un objet JSON " +
        "(sans Markdown, sans ```json) respectant strictement cette structure : " +
        "{ " +
        "  \"rawS\": (double, score brut entre -5 et 5), " +
        "  \"score20\": (double, note sur 20 entre 0.0 et 20.0), " +
        "  \"positivePercent\": (double, 0-100), " +
        "  \"negativePercent\": (double, 0-100), " +
        "  \"dominantBucket\": (String, un parmi: JOY_POS, JOY_NEG, SADNESS_NEG, ANGER_NEG, FEAR_NEG, DISGUST_NEG, SURPRISE_POS, NEUTRAL), " +
        "  \"bucketPercents\": { \"JOY_POS\": 0.0, \"ANGER_NEG\": 0.0 } " +
        "} Texte à analyser : ";

    public boolean isConfigured() {
        String key = System.getenv("GEMINI_API_KEY");
        return key != null && !key.isBlank();
    }

    public AnalysisResult analyze(String text) {
        if (!isConfigured()) {
            return fallbackNeutral();
        }

        String jsonResponse = callApi(text);
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return fallbackNeutral();
        }

        String cleanJson = extractInnerJson(jsonResponse);

        double rawS = getNum(cleanJson, "rawS", 0.0);
        double score20 = clamp(getNum(cleanJson, "score20", 10.0), 0.0, 20.0);
        double posPct = clamp(getNum(cleanJson, "positivePercent", 0.0), 0.0, 100.0);
        double negPct = clamp(getNum(cleanJson, "negativePercent", 0.0), 0.0, 100.0);

        Map<EmotionBucket, Double> bucketPct = new EnumMap<>(EmotionBucket.class);
        for (EmotionBucket b : EmotionBucket.values()) bucketPct.put(b, 0.0);

        Map<String, Double> bucketMap = getObjectNumMap(cleanJson, "bucketPercents");
        if (bucketMap != null) {
            for (Map.Entry<String, Double> kv : bucketMap.entrySet()) {
                EmotionBucket b = tryParseBucket(kv.getKey());
                if (b != null) bucketPct.put(b, kv.getValue());
            }
        }

        EmotionBucket dominant = EmotionBucket.NEUTRAL;
        String domStr = getString(cleanJson, "dominantBucket");
        EmotionBucket domParsed = tryParseBucket(domStr);
        if (domParsed != null) dominant = domParsed;
        else dominant = inferDominant(score20, bucketPct, 0.5);

        return new AnalysisResult(
                rawS, score20, posPct, negPct, bucketPct, dominant,
                SourceType.API_EXTERNAL, new ArrayList<>()
        );
    }

    private String callApi(String text) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) return null;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(TIMEOUT_MS))
                    .build();

            String promptComplet = SYSTEM_PROMPT + text;
            String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":" + quoteJson(promptComplet) + "}]}]}";
            String urlWithKey = API_URL + "?key=" + apiKey;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlWithKey))
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 200 && res.statusCode() < 300) return res.body();

            System.err.println("API request failed with HTTP status " + res.statusCode());
            return null;
        } catch (Exception e) {
            System.err.println("API request failed: " + e.getMessage());
            return null;
        }
    }

    private String extractInnerJson(String geminiResponse) {
        Pattern p = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(geminiResponse);
        if (m.find()) {
            String inner = m.group(1);
            inner = inner.replace("\\\"", "\"").replace("\\n", " ").replace("\\r", "");
            inner = inner.replaceAll("```json", "").replaceAll("```", "");
            return inner;
        }
        return geminiResponse;
    }

    private AnalysisResult fallbackNeutral() {
        Map<EmotionBucket, Double> bucketPct = new EnumMap<>(EmotionBucket.class);
        for (EmotionBucket b : EmotionBucket.values()) bucketPct.put(b, 0.0);
        bucketPct.put(EmotionBucket.NEUTRAL, 100.0);
        return new AnalysisResult(
                0.0, 10.0, 0.0, 0.0, bucketPct, EmotionBucket.NEUTRAL,
                SourceType.API_EXTERNAL, new ArrayList<>()
        );
    }

    private EmotionBucket inferDominant(double score20, Map<EmotionBucket, Double> pct, double neutralBand) {
        if (Math.abs(score20 - 10.0) <= neutralBand) return EmotionBucket.NEUTRAL;
        EmotionBucket best = EmotionBucket.NEUTRAL;
        double bestV = -1.0;
        for (Map.Entry<EmotionBucket, Double> e : pct.entrySet()) {
            if (e.getValue() > bestV) {
                bestV = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private static double getNum(String json, String key, double def) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : def;
    }

    private static String getString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Map<String, Double> getObjectNumMap(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        String inside = m.group(1);
        Map<String, Double> out = new HashMap<>();
        Pattern kv = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher mkv = kv.matcher(inside);
        while (mkv.find()) out.put(mkv.group(1).trim(), Double.parseDouble(mkv.group(2)));
        return out;
    }

    private static EmotionBucket tryParseBucket(String s) {
        if (s == null) return null;
        try {
            return EmotionBucket.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static String quoteJson(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }
}

package io;

import core.params.SystemParams;

import model.AnalysisResult;
import model.Contribution;
import model.EmotionBucket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JsonExporter {

    public void save(AnalysisResult result, String filePath) {
        save(result, null, filePath);
    }

    public void save(AnalysisResult result, String inputText, String filePath) {
        StringBuilder json = new StringBuilder();

        json.append("{\n");

        // Export original input sentence
        json.append("  \"inputText\": \"").append(escapeJson(inputText)).append("\",\n");

        json.append("  \"source\": \"").append(result.getSource()).append("\",\n");
        json.append("  \"rawS\": ").append(format4(result.getRawS())).append(",\n");
        json.append("  \"score20\": ").append(format2(result.getScore20())).append(",\n");
        json.append("  \"positivePercent\": ").append(format1(result.getPositivePercent())).append(",\n");
        json.append("  \"negativePercent\": ").append(format1(result.getNegativePercent())).append(",\n");
        json.append("  \"dominantBucket\": \"").append(result.getDominantBucket()).append("\",\n");

        // Bucket distribution
        json.append("  \"bucketPercents\": {\n");
        Map<EmotionBucket, Double> pct = result.getBucketPercents();
        if (pct != null) {
            int k = 0;
            for (EmotionBucket b : EmotionBucket.values()) {
                Double v = pct.get(b);
                if (v == null) v = 0.0;
                json.append("    \"").append(b.name()).append("\": ").append(format3(v));
                k++;
                if (k < EmotionBucket.values().length) json.append(",");
                json.append("\n");
            }
        }
        json.append("  },\n");

        // Explanation fields replicated from ExplanationPrinter
        json.append("  \"explanation\": {\n");
        List<Contribution> cs = result.getContributions();
        int m = (cs == null) ? 0 : cs.size();
        int mm = Math.max(1, m);

        SystemParams params = new SystemParams();
        double sMax = mm * params.wMax * params.intensityClampA;

        double rawS = result.getRawS();
        double z = rawS / (sMax + 1e-10);
        z = clamp(z, -1.0, 1.0);

        double kExp = 1.50;
        double bExp = 0.10;
        double y = Math.tanh(kExp * z + bExp);

        double score20Mapped = 10.0 + 10.0 * y;
        score20Mapped = clamp(score20Mapped, 0.0, 20.0);

        json.append("    \"m\": ").append(mm).append(",\n");
        json.append("    \"sMax\": ").append(format4(sMax)).append(",\n");
        json.append("    \"z\": ").append(format4(z)).append(",\n");
        json.append("    \"k\": ").append(format2(kExp)).append(",\n");
        json.append("    \"b\": ").append(format2(bExp)).append(",\n");
        json.append("    \"y\": ").append(format4(y)).append(",\n");
        json.append("    \"score20Mapped\": ").append(format2(score20Mapped)).append("\n");
        json.append("  },\n");

        // Contributions list
        json.append("  \"contributions\": [\n");
        if (cs != null) {
            for (int i = 0; i < cs.size(); i++) {
                Contribution c = cs.get(i);
                json.append("    {\n");
                json.append("      \"cue\": \"").append(escapeJson(c.getCueText())).append("\",\n");
                json.append("      \"label\": \"").append(c.getLabel()).append("\",\n");
                json.append("      \"startIndex\": ").append(c.getStartIndex()).append(",\n");
                json.append("      \"endIndex\": ").append(c.getEndIndex()).append(",\n");
                json.append("      \"nGramLen\": ").append(c.getnGramLen()).append(",\n");
                json.append("      \"v\": ").append(format3(c.getValenceV())).append(",\n");
                json.append("      \"a\": ").append(format3(c.getIntensityA())).append(",\n");
                json.append("      \"w\": ").append(format3(c.getSegmentWeightW())).append(",\n");
                json.append("      \"c\": ").append(format4(c.getContributionC())).append(",\n");

                json.append("      \"notes\": [");
                List<String> notes = c.getNotes();
                if (notes != null && !notes.isEmpty()) {
                    for (int j = 0; j < notes.size(); j++) {
                        if (j > 0) json.append(", ");
                        json.append("\"").append(escapeJson(notes.get(j))).append("\"");
                    }
                }
                json.append("]\n");

                json.append("    }");
                if (i < cs.size() - 1) json.append(",");
                json.append("\n");
            }
        }
        json.append("  ]\n");

        json.append("}\n");

        File out = new File(filePath);
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();

        try (FileWriter writer = new FileWriter(out)) {
            writer.write(json.toString());
            System.out.println("Export JSON file with success !! " + filePath);
        } catch (IOException e) {
            System.err.println("Error when write file: " + e.getMessage());
        }
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String format1(double x) {
        return String.format(Locale.ROOT, "%.1f", x);
    }

    private static String format2(double x) {
        return String.format(Locale.ROOT, "%.2f", x);
    }

    private static String format3(double x) {
        return String.format(Locale.ROOT, "%.3f", x);
    }

    private static String format4(double x) {
        return String.format(Locale.ROOT, "%.4f", x);
    }
}

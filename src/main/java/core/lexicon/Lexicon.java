package core.lexicon;

import model.EmotionLabel;
import model.MarkerHit;
import model.MarkerType;
import model.CueMatch;
import model.Token;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Lexicon {

    // map with nGramLen -> map phraseKey -> entry
    private final Map<Integer, Map<String, LexiconEntry>> emotionByLen = new HashMap<>();
    private final Map<Integer, Map<String, LexiconEntry>> markerByLen = new HashMap<>();

    private int maxEmotionLen = 1;
    private int maxMarkerLen = 1;

    public int getMaxEmotionLen() {
        return maxEmotionLen;
    }

    public int getMaxMarkerLen() {
        return maxMarkerLen;
    }

    // LOADERS : 

    // emotion format: phrase;EMO;valence;baseIntensity;label
     public void loadEmotionsFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split(";");
                if (p.length < 5) continue;

                String phrase = normalizePhrase(p[0]); // keep spaces inside
                String type = p[1].trim().toUpperCase();
                if (!type.equals("EMO")) continue;

                double v = Double.parseDouble(p[2].trim());
                double a = Double.parseDouble(p[3].trim());
                EmotionLabel label = EmotionLabel.valueOf(p[4].trim().toUpperCase());

                LexiconEntry entry = LexiconEntry.emotion(v, a, label);
                putEmotion(phrase, entry);
            }
        } catch (Exception e) {
            System.err.println("Error when loading emotion from file : " + e.getMessage());
        }
    }

    // marker format: phrase;TYPE;factor where TYPE in NEG, INT, DIM, CON, ESC
     public void loadMarkersFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split(";");
                if (p.length < 3) continue;

                String phrase = normalizePhrase(p[0]);
                String type = p[1].trim().toUpperCase();
                double factor = Double.parseDouble(p[2].trim());

                MarkerType mt = mapMarkerType(type);
                if (mt == null) continue;

                LexiconEntry entry = LexiconEntry.marker(mt, factor);
                putMarker(phrase, entry);
            }
        } catch (Exception e) {
            System.err.println("Error when loading marker from file : " + e.getMessage());
        }
    }

    // Right here, we try to organize the data of the dictionary that we put the phrase having the same length into the same bucket
    private void putEmotion(String phraseKey, LexiconEntry entry) {
        int len = phraseLen(phraseKey); // by spaces

        // If there isnt the bucket for this length we will create it
        if (!emotionByLen.containsKey(len)) {
            emotionByLen.put(len, new HashMap<String, LexiconEntry>());
        }

        // Using the bucket related to the length of len
        Map<String, LexiconEntry> bucket = emotionByLen.get(len);

        // Add phrase into bucket
        bucket.put(phraseKey, entry);

        if (len > maxEmotionLen) maxEmotionLen = len;
    }

    // Right here, we try to organize the data of the dictionary that we put the phrase having the same length into the same bucket
    private void putMarker(String phraseKey, LexiconEntry entry) {
        int len = phraseLen(phraseKey);

        // If there isnt the bucket for this length we will create it
        if (!markerByLen.containsKey(len)) {
            markerByLen.put(len, new HashMap<String, LexiconEntry>());
        }

        // Using the bucket related to the length of len
        Map<String, LexiconEntry> bucket = markerByLen.get(len);

        // Add phrase into bucket
        bucket.put(phraseKey, entry);

        if (len > maxMarkerLen) maxMarkerLen = len;
    }

    // MATCH EMOTION (LONGEST-FIRST)
    public CueMatch matchEmotion(List<Token> tokens, int startIndex) {
        int maxLen = Math.min(maxEmotionLen, tokens.size() - startIndex);

        for (int len = maxLen; len >= 1; len--) {
            int endIndex = startIndex + len - 1;

            String key = join(tokens, startIndex, endIndex); // join by space
            Map<String, LexiconEntry> map = emotionByLen.get(len);
            if (map == null) continue;

            LexiconEntry entry = map.get(key);
            if (entry != null) {
                return new CueMatch(key, startIndex, endIndex, len, entry);
            }
        }
        return null;
    }

    // MATCH MARKER (LONGEST-FIRST)
    public MarkerHit matchMarker(List<Token> tokens, int startIndex) {
        int maxLen = Math.min(maxMarkerLen, tokens.size() - startIndex);

        for (int len = maxLen; len >= 1; len--) {
            int endIndex = startIndex + len - 1;

            String key = join(tokens, startIndex, endIndex);
            Map<String, LexiconEntry> map = markerByLen.get(len);
            if (map == null) continue;

            LexiconEntry entry = map.get(key);
            if (entry != null) {
                return new MarkerHit(entry.getMarkerType(), entry.getFactor(), key,
                        startIndex, endIndex, len);
            }
        }
        return null;
    }

    // HELPERS 

    private MarkerType mapMarkerType(String type) {
        switch (type) {
            case "NEG": return MarkerType.NEGATION;
            case "INT": return MarkerType.INTENSIFIER;
            case "DIM": return MarkerType.DIMINISH;
            case "CON": return MarkerType.CONTRAST;
            case "ESC": return MarkerType.ESCALATE;
            default: return null;
        }
    }

    // normalize phrase: lower-case, keep single spaces
    private String normalizePhrase(String raw) {
        String s = raw.trim().toLowerCase();
        s = s.replaceAll("[^\\p{L}\\p{N}_\\p{So}\\s]+", "");    // Keep letters, digits, spaces, emojis/symbols
        s = s.replaceAll("\\s+", " ");    // Collapse multiple spaces
        return s;
    }

    private int phraseLen(String phraseKey) {
        if (phraseKey.isEmpty()) return 0;
        return phraseKey.split(" ").length;
    }

    private String join(List<Token> tokens, int i, int j) {
        StringBuilder sb = new StringBuilder();
        for (int k = i; k <= j; k++) {
            if (k > i) sb.append(" ");
            sb.append(tokens.get(k).getNormalized());
        }
        return sb.toString();
    }
}

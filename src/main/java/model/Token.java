package model;

public class Token {
    private final String text;
    private final String normalized; // lowercase don gian cho v1
    private final int index;
    private final boolean isCapsLock;

    public Token(String text, int index) {
        this.text = text;
        this.index = index;

        // Normalized to lookup lexicon stably
        this.normalized = normalize(text);

        // CapsLock boost for emphasizing (depends on SystemParams)
        this.isCapsLock = text.matches(".*[a-zA-Z].*") && text.equals(text.toUpperCase());
    }

    private String normalize(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(java.util.Locale.ROOT);

        // Keep letters, numbers, emojis, and punctuation markers ! ? .
        // This allows tokens like "!!", "!?", "..." to be recognized as markers
        t = t.replaceAll("[^\\p{L}\\p{N}\\p{So}!?\\.]+", "");
        return t;
    }

    public String getText() {
        return text;
    }

    public String getNormalized() {
        return normalized;
    }

    public int getIndex() {
        return index;
    }

    public boolean isCapsLock() {
        return isCapsLock;
    }
}


package model;

public class MarkerHit {
    private final MarkerType type;
    private final double factor;
    private final String phraseKey;   // Normalized marker phrase (e.g., "a little bit", "however")
    private final int startIndex;
    private final int endIndex; // inclusive
    private final int nGramLen;

    public MarkerHit(MarkerType type, double factor, String phraseKey, int startIndex, int endIndex, int nGramLen) {
        this.type = type;
        this.factor = factor;
        this.phraseKey = phraseKey;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.nGramLen = nGramLen;
    }

    public MarkerType getType() {
        return type;
    }

    public double getFactor() {
        return factor;
    }

    public String getPhraseKey() {
        return phraseKey;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getnGramLen() {
        return nGramLen;
    }
}

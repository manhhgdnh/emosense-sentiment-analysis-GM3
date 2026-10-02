package model;

import core.lexicon.LexiconEntry;

public class CueMatch {
    private final String phraseKey;   
    private final int startIndex;     // token start
    private final int endIndex;       // token end (inclusive)
    private final int nGramLen;
    private final LexiconEntry entry; // EMO entry

    public CueMatch(String phraseKey, int startIndex, int endIndex, int nGramLen, LexiconEntry entry) {
        this.phraseKey = phraseKey;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.nGramLen = nGramLen;
        this.entry = entry;
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

    public LexiconEntry getEntry() {
        return entry;
    }
}

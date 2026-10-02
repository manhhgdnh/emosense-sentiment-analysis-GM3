package model;

import java.util.ArrayList;
import java.util.List;

public class Contribution {

    private final String cueText;         // phraseKey
    private final EmotionLabel label;     // label to calculate Emotion%
    private final int startIndex;         // 
    private final int endIndex;           // 
    private final int nGramLen;           // 

    // v: direction (+/-), a: intensity, w: weight coefficient, c: final contribution
    private final double valenceV;
    private final double intensityA;
    private final double segmentWeightW;
    private final double contributionC;

    // short explanation for the result
    private final List<String> explanationsNotes;

    public Contribution(String cueText,
                        EmotionLabel label,
                        int startIndex,
                        int endIndex,
                        int nGramLen,
                        double valenceV,
                        double intensityA,
                        double segmentWeightW) {

        this.cueText = cueText;

        this.label = label;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.nGramLen = nGramLen;

        this.valenceV = valenceV;
        this.intensityA = intensityA;
        this.segmentWeightW = segmentWeightW;

        // NOTE: c = w*a*v (raw, need to map to /20)
        this.contributionC = segmentWeightW * intensityA * valenceV;

        this.explanationsNotes = new ArrayList<>();
    }

    public void addNote(String note) {
        this.explanationsNotes.add(note);
    }

    public String getCueText() {
        return cueText;
    }

    public EmotionLabel getLabel() {
        return label;
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

    public double getValenceV() {
        return valenceV;
    }

    public double getIntensityA() {
        return intensityA;
    }

    public double getSegmentWeightW() {
        return segmentWeightW;
    }

    public double getContributionC() {
        return contributionC;
    }

    public List<String> getNotes() {
        return explanationsNotes;
    }
}


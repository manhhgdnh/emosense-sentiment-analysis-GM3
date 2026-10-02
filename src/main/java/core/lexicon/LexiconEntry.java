package core.lexicon;


import model.EmotionLabel;
import model.MarkerType;

public class LexiconEntry {

    public enum Kind {
        EMOTION,
        MARKER
    }

    private final Kind kind;

    // Emotion fields
    private final double valence;       // [-1..1]
    private final double baseIntensity; // [0..A] before modifiers
    private final EmotionLabel label;   //

    // Marker fields
    private final MarkerType markerType;
    private final double factor; // Example : very=1.2, slightly=0.7

    // For future online adaptation (not used yet)
    private final double confidence; // 0..1
    private final int count;         // seen count

    // Constructor for emotion word
    public static LexiconEntry emotion(double valence, double baseIntensity, EmotionLabel label) {
        return new LexiconEntry(Kind.EMOTION, valence, baseIntensity, label, null, 1.0, 1.0, 0);
    }

    // Constructor for marker
    public static LexiconEntry marker(MarkerType type, double factor) {
        return new LexiconEntry(Kind.MARKER, 0.0, 0.0, null, type, factor, 1.0, 0);
    }

    private LexiconEntry(Kind kind,
                         double valence,
                         double baseIntensity,
                         EmotionLabel label,
                         MarkerType markerType,
                         double factor,
                         double confidence,
                         int count) {

        this.kind = kind;
        this.valence = valence;
        this.baseIntensity = baseIntensity;
        this.label = label;
        this.markerType = markerType;
        this.factor = factor;
        this.confidence = confidence;
        this.count = count;
    }

    public Kind getKind() {
        return kind;
    }

    public double getValence() {
        return valence;
    }

    public double getBaseIntensity() {
        return baseIntensity;
    }

    public EmotionLabel getLabel() {
        return label;
    }

    public MarkerType getMarkerType() {
        return markerType;
    }

    public double getFactor() {
        return factor;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getCount() {
        return count;
    }
}

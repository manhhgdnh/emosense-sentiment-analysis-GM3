package core.params;

public class SystemParams {

    // NOTE: Contrast segmentation : but/however split
    public final double contrastPreWeight = 0.7;
    public final double contrastPostWeight = 1.1;

    // NOTE: Escalation 
    public final double escalateFactor = 1.15;
    // NOTE: Escalation markers window (count token before cue)
    public final int escalateWindow = 4;

    // NOTE: Clamp weight to prevent explosion
    public final double wMin = 0.5;
    public final double wMax = 1.5;

    // NOTE: Negation
    public final int negationWindow = 4;
    public final double negationIntensityFactor = 0.8;
    public final double negationFlipValence = 0.75;

    // NOTE: Intensifier/Diminish window (count token before cue)
    public final int intensifierWindow = 2;

    // NOTE: Clamp intensity
    public final double intensityClampA = 2.0;

    // NOTE: Top M contributions to avoid long-sentence bias
    public final int topM = 10;

    public final double smoothingLambda = 0.02;
    public final double epsilon = 1e-10;

    // If ALL CAPS then boost 
    public final double capsBoost = 1.15;

    // A cue is considered "neutral-ish" if |valence| is small
    public final double neutralValenceThreshold = 0.05;

    // Interpolation: valence magnitude that counts as "strong" (maps to mostly POS/NEG bucket)
    public final double strongValence = 0.85;
    // When score is close to 10, dominant emotion should be NEUTRAL.
    public final double neutralBand = 0.8; // +/- band around 10.0

    // NOTE: Punctuation window after cue (count token after cue)
    public final int punctuationWindowAfter = 1;

}

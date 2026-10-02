package model;

import java.util.List;
import java.util.Map;

public class AnalysisResult {

    // NOTE: rawS is the sum S before mapping 
    private final double rawS;

    // NOTE: score out of 20
    private final double score20;

    private final double positivePercent;
    private final double negativePercent;

    // bucket distribution (18 buckets + global NEUTRAL)
    private final Map<EmotionBucket, Double> bucketPercents;

    // dominant bucket according to app rules
    private final EmotionBucket dominantBucket;

    private final SourceType source;
    private final List<Contribution> contributions;

    public AnalysisResult(double rawS,
                          double score20,
                          double positivePercent,
                          double negativePercent,
                          Map<EmotionBucket, Double> bucketPercents,
                          EmotionBucket dominantBucket,
                          SourceType source,
                          List<Contribution> contributions) {

        this.rawS = rawS;
        this.score20 = score20;
        this.positivePercent = positivePercent;
        this.negativePercent = negativePercent;
        this.bucketPercents = bucketPercents;
        this.dominantBucket = dominantBucket;
        this.source = source;
        this.contributions = contributions;
    }

    public double getRawS() {
        return rawS;
    }

    public double getScore20() {
        return score20;
    }

    public double getPositivePercent() {
        return positivePercent;
    }

    public double getNegativePercent() {
        return negativePercent;
    }

    public Map<EmotionBucket, Double> getBucketPercents() {
        return bucketPercents;
    }

    public EmotionBucket getDominantBucket() {
        return dominantBucket;
    }

    public SourceType getSource() {
        return source;
    }

    public List<Contribution> getContributions() {
        return contributions;
    }

    @Override
    public String toString() {
        return String.format("[%s] Score: %.2f/20 (rawS = %.4f, +%%=%.1f, -%%=%.1f, dominant=%s)",
                source, score20, rawS, positivePercent, negativePercent, dominantBucket);
    }
}


package model;

/*
 * 18 buckets = 6 emotions x {POS, NEG, NEU} + a global NEUTRAL bucket.
 
 * POS/NEG/NEU buckets represent how the cue mass is attributed for explanation.
 * The global NEUTRAL bucket is used when the whole sentence is close to neutral
 * (score near 10) or when there is insufficient signal.
 */
public enum EmotionBucket {
    JOY_POS, JOY_NEG, JOY_NEU,
    SADNESS_POS, SADNESS_NEG, SADNESS_NEU,
    ANGER_POS, ANGER_NEG, ANGER_NEU,
    FEAR_POS, FEAR_NEG, FEAR_NEU,
    DISGUST_POS, DISGUST_NEG, DISGUST_NEU,
    SURPRISE_POS, SURPRISE_NEG, SURPRISE_NEU,

    NEUTRAL
}
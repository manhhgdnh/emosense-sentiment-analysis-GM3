package core.rule;

import core.params.SystemParams;
import core.lexicon.Lexicon;
import model.*;

import java.util.*;
import java.util.regex.Pattern;

public class RuleEngine {

    private static final Pattern PUNCT_INT = Pattern.compile("^[!?]+$");
    private static final Pattern PUNCT_ELLIPSIS = Pattern.compile("^\\.\\.\\.$");

    public AnalysisResult evaluate(List<Token> tokens, Lexicon lexicon, SystemParams params) {

        // 1) Detect markers hits (longest-first scan)
        List<MarkerHit> markerHits = detectMarkers(tokens, lexicon);

        // 2) Find first contrast to segment weights (v1)
        MarkerHit firstContrast = findFirstContrast(markerHits);
        int contrastStart = (firstContrast == null) ? -1 : firstContrast.getStartIndex();

        // 3) Extract cues + compute contributions (longest-first for emotions)
        List<Contribution> contributions = new ArrayList<>();

        int i = 0;
        while (i < tokens.size()) {
            CueMatch cue = lexicon.matchEmotion(tokens, i);
            if (cue == null) {
                i++;
                continue;
            }

            // Segment weight
            double w = 1.0;
            if (contrastStart != -1) {
                if (cue.getStartIndex() < contrastStart) w = params.contrastPreWeight;
                else w = params.contrastPostWeight;
            }
            w = clamp(w, params.wMin, params.wMax);

            // Base
            double v = cue.getEntry().getValence();
            double a = cue.getEntry().getBaseIntensity();
            EmotionLabel label = cue.getEntry().getLabel();

            // Modifiers in window before cue start (INT / DIM)
            WindowMarkers mods = modifierFactor(markerHits, cue.getStartIndex(), params.intensifierWindow);
            a *= mods.factor;

            // Escalation in window before cue start (ESC)
            WindowMarkers esc = escalateFactor(markerHits, cue.getStartIndex(), params.escalateWindow);
            if (esc.applied != null && !esc.applied.isEmpty()) {
                w *= params.escalateFactor;
            }
            w *= esc.factor;
            w = clamp(w, params.wMin, params.wMax);

            // Negation in window before cue start
            WindowMarkers negs = negationMarkers(markerHits, cue.getStartIndex(), params.negationWindow);
            int negCount = (negs.applied == null) ? 0 : negs.applied.size();
            boolean neg = (negCount % 2 == 1);

            // Caps boost: If capslock -> boost 
            boolean caps = anyCaps(tokens, cue.getStartIndex(), cue.getEndIndex());
            boolean canCapsBoost = (negCount == 0);
            if (caps && canCapsBoost) a *= params.capsBoost;

            String negNote = null;
            if (negCount > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Negation markers: ");
                for (int k = 0; k < negCount; k++) {
                    MarkerHit nh = negs.applied.get(k);
                    if (k > 0) sb.append(", ");
                    sb.append("\"").append(nh.getPhraseKey()).append("\" (NEG)");
                }

                if (!neg) {
                    sb.append(" => Double negation cancelled");
                    negNote = sb.toString();
                } else {
                    double oldV = v;    // Store BEFORE flipping
                    v = -params.negationFlipValence * v;    // Damp the flip to avoid "not sad" being as positive as "happy"
                    a *= params.negationIntensityFactor;

                    sb.append(" => v: ").append(format2(oldV)).append(" -> ").append(format2(v))
                      .append(", a x").append(format2(params.negationIntensityFactor));
                    negNote = sb.toString();
                }
            }

            // Punctuation right after cue end can intensify or diminish
            WindowMarkers postP = postfixPunctuationFactor(markerHits, cue.getEndIndex(), params.punctuationWindowAfter);
            a *= postP.factor;

            // Clamp intensity
            a = clamp(a, 0.0, params.intensityClampA);

            Contribution c = new Contribution(
                    cue.getPhraseKey(), label,
                    cue.getStartIndex(), cue.getEndIndex(), cue.getnGramLen(),
                    v, a, w
            );

            if (negNote != null) c.addNote(negNote);

            if (contrastStart != -1) {
                String side = cue.getStartIndex() < contrastStart ? "pre-contrast" : "post-contrast";
                c.addNote(side + " weight due to contrast marker \"" + firstContrast.getPhraseKey() + "\"");
            }

            if (esc.factor != 1.0 && esc.applied != null && !esc.applied.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Escalate: ");
                for (int k = 0; k < esc.applied.size(); k++) {
                    MarkerHit mh = esc.applied.get(k);
                    if (k > 0) sb.append(", ");
                    sb.append("\"").append(mh.getPhraseKey()).append("\"")
                            .append(" (ESC x").append(format2(mh.getFactor())).append(")");
                }
                sb.append(" => total x").append(format2(esc.factor));
                c.addNote(sb.toString());
            }

            if (mods.factor != 1.0 && mods.applied != null && !mods.applied.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Modifiers: ");
                for (int k = 0; k < mods.applied.size(); k++) {
                    MarkerHit mh = mods.applied.get(k);
                    if (k > 0) sb.append(", ");
                    sb.append("\"").append(mh.getPhraseKey()).append("\"")
                    .append(" (").append(mh.getType()).append(" x").append(format2(mh.getFactor())).append(")");
                }
                sb.append(" => total x").append(format2(mods.factor));
                c.addNote(sb.toString());
            }

            if (caps && canCapsBoost) c.addNote("Caps boost x" + format2(params.capsBoost));

            if (postP.factor != 1.0 && postP.applied != null && !postP.applied.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Post punctuation: ");
                for (int k = 0; k < postP.applied.size(); k++) {
                    MarkerHit mh = postP.applied.get(k);
                    if (k > 0) sb.append(", ");
                    sb.append("\"").append(mh.getPhraseKey()).append("\"")
                            .append(" (PUNC x").append(format2(mh.getFactor())).append(")");
                }
                sb.append(" => total x").append(format2(postP.factor));
                c.addNote(sb.toString());
            }

            contributions.add(c);

            // Skip ahead by nGramLen 
            i = cue.getEndIndex() + 1;
        }

        // 4) TopM contributions
        contributions.sort(Comparator.comparingDouble(x -> -Math.abs(x.getContributionC())));
        if (contributions.size() > params.topM) {
            contributions = new ArrayList<>(contributions.subList(0, params.topM));
        }

        // 5) RawS
        double rawS = 0.0;
        double P = 0.0;
        double N = 0.0;

        // For bucket% (18 buckets + global NEUTRAL)
        Map<EmotionBucket, Double> bucketMass = new EnumMap<>(EmotionBucket.class);
        for (EmotionBucket b : EmotionBucket.values()) bucketMass.put(b, 0.0);

        for (Contribution c : contributions) {
            double ci = c.getContributionC();
            rawS += ci;

            if (ci > 0) P += ci;
            if (ci < 0) N += Math.abs(ci);

            // Bucket interpolation (POS/NEG/NEU within each emotion)
            WindowMarkers negs = negationMarkers(markerHits, c.getStartIndex(), params.negationWindow);
            int negCount = (negs.applied == null) ? 0 : negs.applied.size();
            boolean neg = (negCount % 2 == 1);
            accumulateBuckets(bucketMass, c, ci, neg, params);
        }

        // 6) Mapping score20
        // Because if sMax depends directly on topM (7) then if there is a short sentence like "I am happy", the score gonna be compressed harshly
        int m = Math.max(1, contributions.size());
        double sMax = m * params.wMax * params.intensityClampA;
        double score20 = mapTo20(rawS, sMax);

        // 7) Pos/Neg %
        double posPct = (P / (P + N + params.epsilon)) * 100.0;
        double negPct = (N / (P + N + params.epsilon)) * 100.0;

        // 8) Bucket% (with "100%" rule for single-bucket sentences)
        Map<EmotionBucket, Double> bucketPct = bucketPercents(bucketMass, params.smoothingLambda);

        // 9) Dominant bucket rule
        EmotionBucket dominant = dominantBucket(score20, bucketPct, params.neutralBand);

        return new AnalysisResult(rawS, score20, posPct, negPct, bucketPct, dominant, SourceType.RULE_BASED, contributions);
    }

    private static final class WindowMarkers {
        final double factor;
        final List<MarkerHit> applied;

        WindowMarkers(double factor, List<MarkerHit> applied) {
            this.factor = factor;
            this.applied = applied;
        }
    }

    private MarkerHit findMarker(List<MarkerHit> hits, String phraseKey) {
        for (MarkerHit h : hits) {
            if (phraseKey.equalsIgnoreCase(h.getPhraseKey())) return h;
        }
        return null;
    }

    // Marker detection (longest-first) 
    private List<MarkerHit> detectMarkers(List<Token> tokens, Lexicon lexicon) {
        List<MarkerHit> hits = new ArrayList<>();
        int i = 0;
        while (i < tokens.size()) {

            MarkerHit p = matchPunctuationMarker(tokens, i);
            if (p != null) {
                hits.add(p);
                i = p.getEndIndex() + 1;
                continue;
            }

            MarkerHit hit = lexicon.matchMarker(tokens, i);
            if (hit == null) {
                i++;
                continue;
            }
            hits.add(hit);
            i = hit.getEndIndex() + 1;
        }
        return hits;
    }

    private MarkerHit matchPunctuationMarker(List<Token> tokens, int i) {
        String t = tokens.get(i).getText();
        if (t == null) return null;

        if (PUNCT_ELLIPSIS.matcher(t).matches()) {
            double f = 1.1;
            return new MarkerHit(MarkerType.DIMINISH, f, t, i, i, 1);
        }

        if (PUNCT_INT.matcher(t).matches()) {
            int n = t.length();    // "!!" -> 2, "???" -> 3
            char ch = t.charAt(0);

            double f;

            if (ch == '!') {
                f = 1.0 + Math.min(0.05 * n, 0.25);
            } else if (ch == '?') {
                f = 1.0 - Math.min(0.03 * n, 0.15);
            } else {
                return null; 
            }

            return new MarkerHit(MarkerType.INTENSIFIER, f, t, i, i, 1);
        }

        return null;
    }

    MarkerHit findFirstContrast(List<MarkerHit> hits) {
        for (MarkerHit h : hits) {
            if (h.getType() == MarkerType.CONTRAST) return h;
        }
        return null;
    }

    // Rules Helpers 

    WindowMarkers negationMarkers(List<MarkerHit> hits, int cueStartIndex, int window) {
        int start = Math.max(0, cueStartIndex - window);
        List<MarkerHit> applied = new ArrayList<>();

        for (MarkerHit h : hits) {
            if (h.getType() != MarkerType.NEGATION) continue;
            if (h.getEndIndex() < cueStartIndex && h.getEndIndex() >= start) {
                applied.add(h);
            }
        }
        // factor is unused for NEG, keep 1.0
        return new WindowMarkers(1.0, applied);
    }

    WindowMarkers modifierFactor(List<MarkerHit> hits, int cueStartIndex, int window) {
        int start = Math.max(0, cueStartIndex - window);
        double factor = 1.0;
        List<MarkerHit> applied = new ArrayList<>();

        for (MarkerHit h : hits) {
            if (h.getEndIndex() >= cueStartIndex) continue;
            if (h.getEndIndex() < start) continue;

            if (h.getType() == MarkerType.INTENSIFIER || h.getType() == MarkerType.DIMINISH) {
                factor *= h.getFactor();
                applied.add(h);
            }
        }
        return new WindowMarkers(factor, applied);
    }

    WindowMarkers escalateFactor(List<MarkerHit> hits, int cueStartIndex, int window) {
        int start = Math.max(0, cueStartIndex - window);
        double factor = 1.0;
        List<MarkerHit> applied = new ArrayList<>();

        // Detect paired pattern not only ... but also
        MarkerHit notOnly = null;
        MarkerHit butAlso = null;

        for (MarkerHit h : hits) {
            if (h.getType() != MarkerType.ESCALATE) continue;

            if ("not only".equalsIgnoreCase(h.getPhraseKey())) {
                if (notOnly == null || h.getStartIndex() < notOnly.getStartIndex()) notOnly = h;
            }

            if ("but also".equalsIgnoreCase(h.getPhraseKey())) {
                if (butAlso == null || h.getStartIndex() < butAlso.getStartIndex()) butAlso = h;
            }
        }

        boolean hasPair = (notOnly != null && butAlso != null && notOnly.getStartIndex() < butAlso.getStartIndex());
        int split = hasPair ? butAlso.getStartIndex() : Integer.MAX_VALUE;

        for (MarkerHit h : hits) {
            if (h.getEndIndex() >= cueStartIndex) continue;
            if (h.getEndIndex() < start) continue;

            if (h.getType() == MarkerType.ESCALATE) {

                // Scope paired construction
                if (hasPair) {
                    // not only applies only to left side
                    if ("not only".equalsIgnoreCase(h.getPhraseKey()) && cueStartIndex > split) continue;

                    // but also applies only to right side
                    if ("but also".equalsIgnoreCase(h.getPhraseKey()) && cueStartIndex < split) continue;
                }

                factor *= h.getFactor();
                applied.add(h);
            }
        }

        return new WindowMarkers(factor, applied);
    }

    WindowMarkers postfixPunctuationFactor(List<MarkerHit> hits, int cueEndIndex, int window) {
        int end = cueEndIndex + window;
        double factor = 1.0;
        List<MarkerHit> applied = new ArrayList<>();

        for (MarkerHit h : hits) {
            if (h.getStartIndex() <= cueEndIndex) continue;
            if (h.getStartIndex() > end) continue;

            boolean isPunc = PUNCT_INT.matcher(h.getPhraseKey()).matches() || PUNCT_ELLIPSIS.matcher(h.getPhraseKey()).matches();
            if (!isPunc) continue;

            if (h.getType() == MarkerType.INTENSIFIER || h.getType() == MarkerType.DIMINISH) {
                factor *= h.getFactor();
                applied.add(h);
            }
        }
        return new WindowMarkers(factor, applied);
    }

    private boolean anyCaps(List<Token> tokens, int i, int j) {
        for (int k = i; k <= j; k++) {
            if (tokens.get(k).isCapsLock()) return true;
        }
        return false;
    }

    // The logic of calculating when using Bucket
    private void addMass(Map<EmotionBucket, Double> m, EmotionBucket b, double x) {
        m.put(b, m.get(b) + x);
    }

    private EmotionBucket bucketOf(EmotionLabel label, EmotionPolarity pol) {
        return EmotionBucket.valueOf(label.name() + "_" + pol.name());
    }

    /*
     * Interpolation inside the same emotion: distribute cue mass into POS/NEG/NEU buckets
     * Strong valence -> mostly POS or NEG
     * Weak valence -> more NEU
     * Negation increases NEU share (uncertainty) and shifts polarity via the signed ci */
    
    // Attribute mass into the buckets
    private void accumulateBuckets(Map<EmotionBucket, Double> bucketMass,
                                   Contribution c,
                                   double ci,
                                   boolean neg,
                                   SystemParams params) {

        EmotionLabel label = c.getLabel();
        double mass = Math.abs(ci);

        // Neutral-ish lexicon entries (valence close to 0) should live in _NEU
        if (Math.abs(c.getValenceV()) <= params.neutralValenceThreshold) {
            // If valence is close to 0, ci can be 0, we still keep a non-zero "explanatory" mass which means mass =  |W * A|
            mass = Math.abs(c.getSegmentWeightW() * c.getIntensityA());
            addMass(bucketMass, bucketOf(label, EmotionPolarity.NEU), mass);
            return;
        }

        // Base neutral share from valence strength
        double strength = Math.abs(c.getValenceV()) / params.strongValence;
        strength = clamp(strength, 0.0, 1.0);
        double neuShare = 1.0 - strength; // valence is opposite to neuShare in terms of strength

        // High intensity -> less neutrality (more categorical)
        double aNorm = c.getIntensityA() / (params.intensityClampA + 1e-10); // 0..1
        aNorm = clamp(aNorm, 0.0, 1.0);

        double gamma = 0.48; // higher -> NEUTRAL drops more
        neuShare *= (1.0 - gamma * aNorm);
        neuShare = clamp(neuShare, 0.0, 0.75);

        // Negation makes the meaning less categorical -> push more to NEU
        if (neg) {
            neuShare = clamp(neuShare + 0.20, 0.0, 0.70);
        }

        double polShare = 1.0 - neuShare;
        EmotionPolarity pol = (ci >= 0) ? EmotionPolarity.POS : EmotionPolarity.NEG;

        addMass(bucketMass, bucketOf(label, EmotionPolarity.NEU), mass * neuShare);
        addMass(bucketMass, bucketOf(label, pol), mass * polShare);
    }

    // Convert mass to percentage without leaking to inactive buckets
    private Map<EmotionBucket, Double> bucketPercents(Map<EmotionBucket, Double> mass, double lambda) {
        Map<EmotionBucket, Double> out = new EnumMap<>(EmotionBucket.class);

        double total = 0.0;
        List<EmotionBucket> active = new ArrayList<>();

        for (EmotionBucket b : EmotionBucket.values()) {
            double v = mass.getOrDefault(b, 0.0);
            total += v;
            if (v > 0.0) active.add(b);
        }

        // If no signal -> 100% NEUTRAL only
        if (total <= 1e-12) {
            for (EmotionBucket b : EmotionBucket.values()) out.put(b, 0.0);
            out.put(EmotionBucket.NEUTRAL, 100.0);
            return out;
        }

        // If exactly one active bucket -> 100% that bucket
        if (active.size() == 1) {
            for (EmotionBucket b : EmotionBucket.values()) out.put(b, 0.0);
            out.put(active.get(0), 100.0);
            return out;
        }

        // Initialize all to 0 so inactive never gets any leakage
        for (EmotionBucket b : EmotionBucket.values()) out.put(b, 0.0);

        // Smooth ONLY among active buckets
        int K = active.size();
        double smoothTotal = total + lambda * K;

        for (EmotionBucket b : active) {
            double v = mass.getOrDefault(b, 0.0);
            double pct = (v + lambda) / smoothTotal * 100.0;
            out.put(b, pct);
        }

        return out;
    }

    private EmotionBucket dominantBucket(double score20, Map<EmotionBucket, Double> pct, double neutralBand) {
        if (Math.abs(score20 - 10.0) <= neutralBand) {
            return EmotionBucket.NEUTRAL;
        }

        EmotionBucket best = null;
        double bestV = -1.0;

        for (Map.Entry<EmotionBucket, Double> e : pct.entrySet()) {
            EmotionBucket b = e.getKey();
            if (b == EmotionBucket.NEUTRAL) continue;
            double v = e.getValue();
            if (v > bestV) {
                bestV = v;
                best = b;
            }
        }

        if (best == null) return EmotionBucket.NEUTRAL;
        return best;
    }

    private double mapTo20(double rawS, double sMax) {
        double z = rawS / (sMax + 1e-10);
        z = clamp(z, -1.0, 1.0);

        double k = 1.50;
        double b = 0.10;
        double y = Math.tanh(k * z + b);

        double score20 = 10.0 + 10.0 * y;
        return clamp(score20, 0.0, 20.0);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static String format2(double x) {
        return String.format(Locale.ROOT, "%.2f", x);
    }
}

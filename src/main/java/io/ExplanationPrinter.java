package io;

import core.params.SystemParams;

import model.AnalysisResult;
import model.Contribution;

import java.util.List;
import java.util.Locale;

public final class ExplanationPrinter {

    private ExplanationPrinter() {}

    public static void printScoreExplanation(AnalysisResult r, SystemParams params) {
        System.out.println("\n SCORE EXPLANATION (Rule-Based)");

        List<Contribution> cs = r.getContributions();
        if (cs == null || cs.isEmpty()) {
            System.out.println("No contributions (no matched cues) || rawS = 0 -> score is neutral-ish");
            System.out.println("#### END EXPLANATION #### \n");
            return;
        }

        double sum = 0.0;

        for (int i = 0; i < cs.size(); i++) {
            Contribution c = cs.get(i);

            double v = c.getValenceV();
            double a = c.getIntensityA();
            double w = c.getSegmentWeightW();
            double ci = c.getContributionC();
            sum += ci;

            System.out.printf(Locale.ROOT,
                    "%02d) cue = \"%s\" label = %s  v=%.3f  a=%.3f  w=%.3f  c=%.4f%n",
                    (i + 1),
                    c.getCueText(),
                    c.getLabel(),
                    v, a, w, ci
            );

            List<String> notes = c.getNotes();
            if (notes != null && !notes.isEmpty()) {
                System.out.println("    notes: " + notes);
            }
        }

        int m = Math.max(1, cs.size());
        double sMax = m * params.wMax * params.intensityClampA;

        double z = sum / (sMax + 1e-10);
        z = clamp(z, -1.0, 1.0);

        double k = 1.50;
        double b = 0.10;
        double y = Math.tanh(k * z + b);

        double score20 = 10.0 + 10.0 * y;
        score20 = clamp(score20, 0.0, 20.0);

        System.out.printf(Locale.ROOT, "rawS =  SUM OF c = %.4f%n", sum);
        System.out.printf(Locale.ROOT, "Total cue = %d, sMax = m * wMax * intensityClampA = %.4f%n", m, sMax);
        System.out.printf(Locale.ROOT, "z = rawS/sMax = %.4f (clamped)%n", z);
        System.out.printf(Locale.ROOT, "y = tanh(k*z + b), k=%.2f, b=%.2f => y=%.4f%n", k, b, y);
        System.out.printf(Locale.ROOT, "Score20 = 10 + 10*y = %.2f%n", score20);

        System.out.println("#### END EXPLANATION #### \n");
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }
}

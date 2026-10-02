package io;

import model.EmotionBucket;

import javax.imageio.ImageIO;              // Built-in PNG writer

import java.awt.Color;
import java.awt.Font;                      // For title/legend text
import java.awt.BasicStroke;               // For border thickness
import java.awt.Graphics2D;                // For drawing
import java.awt.RenderingHints;            // Anti-aliasing for nicer chart
import java.awt.geom.Arc2D;                // Pie slices
import java.awt.image.BufferedImage;       // Canvas image

import java.io.File;
import java.io.IOException;

import java.util.*;

public class EmotionChartExporter {

    public static void exportPiePNG(Map<EmotionBucket, Double> bucketPercents,
                                    EmotionBucket dominant, // To deal with case of NEUTRAL
                                    String title,
                                    String outputPath) throws IOException {

        if (bucketPercents == null || bucketPercents.isEmpty()) {
            throw new IllegalArgumentException("bucketPercents is empty ");
        }

        final double scale = 1.0; // I just want to be sure for the bucketPercents

        // Build filtered list
        List<Map.Entry<EmotionBucket, Double>> items = new ArrayList<>();

        // If the entire sentence is neutral -> the chart will only have 1 slice
        if (dominant == EmotionBucket.NEUTRAL) {
            items.add(Map.entry(EmotionBucket.NEUTRAL, 100.0));
        } else {
            for (Map.Entry<EmotionBucket, Double> e : bucketPercents.entrySet()) {
                EmotionBucket b = e.getKey();
                Double v = e.getValue();
                if (b == null || v == null) continue;
                if (b == EmotionBucket.NEUTRAL) continue;       // Hide neutral in normal cases
                double prop = v * scale;
                if (prop <= 1e-10) continue;    // Throwing away buckets is almost 0%
                items.add(Map.entry(b, prop));  // Add an active bucket to the list
            }
            // Sort descending
            items.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            if (items.isEmpty()) {    // Fallback in case of not having active bucket
                items.add(Map.entry(EmotionBucket.NEUTRAL, 100.0));
            }
        }

        // Draw pie chart with Java2D 
        int W = 900, H = 600; // Image size
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); 

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        // Title
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(title, 40, 40);

        // Pie area
        int pieX = 30, pieY = 60, pieSize = 360;

        // Draw slices
        double start = 0.0;
        for (Map.Entry<EmotionBucket, Double> it : items) {
            EmotionBucket bucket = it.getKey();
            double pct = it.getValue(); // 0..100
            double extent = 360.0 * (pct / 100.0);

            g.setColor(colorFamily(bucket)); // ColorFamily
            g.fill(new Arc2D.Double(pieX, pieY, pieSize, pieSize, start, extent, Arc2D.PIE));

            start += extent;
        }

        // Border
        g.setColor(Color.GRAY);
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(pieX, pieY, pieSize, pieSize);

        // Legend
        int lx = 440, ly = 95;
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(Color.BLACK);
        g.drawString("Legend", lx, ly - 25);

        int box = 12, line = 22;
        for (int i = 0; i < items.size(); i++) {
            EmotionBucket bucket = items.get(i).getKey();
            double pct = items.get(i).getValue();

            int y = ly + i * line;

            g.setColor(colorFamily(bucket));
            g.fillRect(lx, y - box + 2, box, box);

            g.setColor(Color.GRAY);
            g.drawRect(lx, y - box + 2, box, box);

            g.setColor(Color.BLACK);
            String text = bucket.name() + " (" + String.format(Locale.ROOT, "%.1f", pct) + "%)";
            g.drawString(text, lx + 20, y + 2);
        }

        g.dispose(); // Release graphics context

        // Ensure output folder exists
        File out = new File(outputPath);
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();

        ImageIO.write(img, "png", out);
    }

    // One color per emotion bucket
    private static Color colorFamily(EmotionBucket bucket) {
        switch (bucket) {
            case JOY_POS:      return new Color( 80, 170, 120);
            case JOY_NEG:      return new Color( 40, 120,  85);
            case JOY_NEU:      return new Color(150, 200, 175);

            case SADNESS_POS:  return new Color( 95, 140, 190);
            case SADNESS_NEG:  return new Color( 55,  95, 155);
            case SADNESS_NEU:  return new Color(165, 190, 220);

            case ANGER_POS:    return new Color(200,  90,  90);
            case ANGER_NEG:    return new Color(150,  50,  55);
            case ANGER_NEU:    return new Color(225, 170, 170);

            case FEAR_POS:     return new Color(140, 120, 190);
            case FEAR_NEG:     return new Color( 95,  80, 150);
            case FEAR_NEU:     return new Color(195, 185, 220);

            case DISGUST_POS:  return new Color(120, 150, 110);
            case DISGUST_NEG:  return new Color( 80, 110,  75);
            case DISGUST_NEU:  return new Color(185, 200, 175);

            case SURPRISE_POS: return new Color(210, 165,  75);
            case SURPRISE_NEG: return new Color(170, 120,  45);
            case SURPRISE_NEU: return new Color(235, 205, 150);

            case NEUTRAL:      return new Color(190, 190, 190);

            default:           return new Color(160, 160, 160);
        }
    }
}

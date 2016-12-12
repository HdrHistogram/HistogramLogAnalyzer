/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

class ColorHelper {

    private static ArrayList<Color> colors = new ArrayList<Color>();

    static {
        colors.add(Color.RED);
        colors.add(Color.BLUE);
        colors.add(Color.GREEN);
    }

    private static void ensireSize(int size) {
        colors.ensureCapacity(size);
        while (colors.size() < size) {
            colors.add(null);
        }
    }

    static Color getColor(int i) {
        ensireSize(i + 1);
        Color c = colors.get(i);
        if (c == null) {
            Random r = new Random();
            c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
            colors.add(c);
        }
        return c;
    }

    static Color getSLAColor() {
        return new Color(255, 204, 102);
    }

    static Color getHPLColor(String percentileString) {
        switch (percentileString) {
            case "99%":
            case "99.0%":
                return Color.GREEN;
            case "99.9%":
                return Color.BLUE;
            case "99.99%":
                return new Color(128, 0, 128); // purple
            case "Max":
                return Color.RED;
        }
        throw new RuntimeException("unexpected HPL: "+percentileString);
    }
}

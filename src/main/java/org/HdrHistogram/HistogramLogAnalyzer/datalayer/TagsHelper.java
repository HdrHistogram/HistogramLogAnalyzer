/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.HistogramLogProcessor;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

public class TagsHelper {

    /*
     * returns list of tags or null if no tags found (default tag)
     */
    public static Set<String> listTags(String inputFileName)
            throws FileNotFoundException
    {
        TagsExtractor te = new TagsExtractor();
        PrintStream origOut = System.out;
        System.setOut(te);

        String[] args = new String[]{"-i", inputFileName, "-listtags"};
        HistogramLogProcessor hlp = new HistogramLogProcessor(args);
        hlp.run();

        System.setOut(origOut);
        return te.getTags();
    }

    private static class TagsExtractor extends PrintStream {
        Set<String> tags = null;
        boolean inited = false;

        private final static String NO_TAG_STRING = "[NO TAG (default)]";

        TagsExtractor() { super(new ByteArrayOutputStream());}

        Set<String> getTags() {
            if (!inited) {
                String content = new String(((ByteArrayOutputStream)out).toByteArray());
                String[] lines = content.split(System.getProperty("line.separator"));
                if (lines.length == 2 && lines[1].equals(NO_TAG_STRING)) {
                    lines[1] = null; // treat default tag as null
                }
                tags = new LinkedHashSet<String>();
                // ignore first line
                tags.addAll(Arrays.asList(lines).subList(1, lines.length));
                inited = true;
            }
            return tags;
        }
    }
}

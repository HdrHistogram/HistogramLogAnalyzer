/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogProcessor;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class TagsHelper {

    /*
     * returns list of tags or null if no tags found (default tag)
     */
    static List<String> listTags(String hlogFileName)
            throws FileNotFoundException
    {
        TagsExtractor te = new TagsExtractor();
        PrintStream origOut = System.out;
        System.setOut(te);

        String[] args = new String[]{"-i", hlogFileName, "-listtags"};
        HistogramLogProcessor hlp = new HistogramLogProcessor(args);
        hlp.run();

        System.setOut(origOut);
        return te.getTags();
    }

    private static class TagsExtractor extends PrintStream {
        List<String> tags = null;
        boolean inited = false;

        private final static String NO_TAG_STRING = "[NO TAG (default)]";

        TagsExtractor() { super(new ByteArrayOutputStream());}

        List<String> getTags() {
            if (!inited) {
                String content = new String(((ByteArrayOutputStream)out).toByteArray());
                tags = new LinkedList<String>(Arrays.asList(content.split(System.getProperty("line.separator"))));
                tags.remove(0);
                if (tags.size() == 1 && NO_TAG_STRING.equals(tags.get(0))) {
                    tags = null;
                }
                inited = true;
            }
            return tags;
        }
    }

    static void processFile(String inputFileName, String outputFileName, String tag)
            throws FileNotFoundException
    {
        String[] args;
        if (tag != null) {
            args = new String[]{"-i", inputFileName, "-o", outputFileName, "-tag", tag};
        } else {
            args = new String[]{"-i", inputFileName, "-o", outputFileName};
        }
        HistogramLogProcessor hlp = new HistogramLogProcessor(args);
        hlp.run();
    }
}

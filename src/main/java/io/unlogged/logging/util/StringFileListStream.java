package io.unlogged.logging.util;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class to write strings to files.
 */
public class StringFileListStream {

    private final FileNameGenerator filenames;
    private final long itemPerFile;

    private long itemCount;
    private final boolean compress;

    private PrintWriter writer;

    /**
     * @param filenames   specifies a file name generator for files to be written.
     * @param itemPerFile specifies the number of strings stored in a single file.
     * @param compress    option enables to compress the output file in GZip.
     */
    public StringFileListStream(FileNameGenerator filenames, long itemPerFile, boolean compress) {
        this.filenames = filenames;
        this.itemPerFile = itemPerFile;
        this.compress = compress;

        this.itemCount = 0;

        prepareNextFile();
    }

    private void prepareNextFile() {
        if (writer != null) {
            writer.close();
        }
        File f = filenames.getNextFile();
        try {
            if (compress) {
                GZIPOutputStream w = new GZIPOutputStream(new FileOutputStream(f));
                writer = new PrintWriter(new OutputStreamWriter(w));
            } else {
                BufferedOutputStream w = new BufferedOutputStream(new FileOutputStream(f), 64 * 1024);
                writer = new PrintWriter(new OutputStreamWriter(w));
            }
        } catch (IOException e) {
            writer = null;
        }
    }

    /**
     * Write a string.
     *
     * @param s is a String.
     */
    public synchronized void write(String s) {
        if (writer != null) {
            writer.print(s);
            itemCount++;
            if (itemCount >= itemPerFile) {
                prepareNextFile();
                itemCount = 0;
            }
        }
    }

    /**
     * Output strings in the internal buffer to a file,
     * and then close the stream.
     */
    public synchronized void close() {
        writer.close();
        writer = null;
    }

}

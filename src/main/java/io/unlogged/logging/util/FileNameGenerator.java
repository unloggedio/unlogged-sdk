package io.unlogged.logging.util;

import java.io.File;
import java.util.Date;

/**
 * This object generates file names with sequence numbers,
 * e.g. log-00001.slg, log-00002.slg, and so on.
 */
public class FileNameGenerator {

    private final File dir;
    private final String prefix;
    private final String suffix;
    private int fileCount = 0;

    /**
     * Create a generator instance.
     * A generated file is prefix + sequence number (e.g. "00001") + suffix in a specified directory.
     * For example, log-00001.slg is the first file name when the prefix "log-" and the suffix is ".slg".
     *
     * @param dir    specifies the base directory for generated files.
     * @param prefix specifies prefix for a file name.
     * @param suffix specifies suffix for a file name.
     */
    public FileNameGenerator(File dir, String prefix, String suffix) {
        this.dir = dir;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * @return a file object representing a new file name.
     */
    public File getNextFile() {
        return new File(dir, prefix + String.format("%05d", ++fileCount) + "-" + new Date().getTime() + suffix);
    }

    public File getNextFile(String threadId) {
        return new File(dir, prefix + String.format("%06d", ++fileCount) + "-" + threadId + suffix);
    }

    public String getOutputDir() {
        return dir.getAbsolutePath();
    }
}

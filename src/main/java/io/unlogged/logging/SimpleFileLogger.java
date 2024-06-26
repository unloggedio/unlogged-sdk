package io.unlogged.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class SimpleFileLogger implements IErrorLogger {

	private File logFile;
	public static final String ERROR_LOG_FILE = "log.txt";
	private PrintStream logger;

    public SimpleFileLogger(File outputDir) {
        try {
			this.logFile = new File(outputDir, ERROR_LOG_FILE);
            logger = new PrintStream(this.logFile);
        } catch (FileNotFoundException e) {
            logger = System.out;
            logger.println("[unlogged] failed to open " + ERROR_LOG_FILE + " in " + outputDir.getAbsolutePath());
            logger.println("[unlogged] using System.out instead.");
        }
    }

    @Override
    public void log(Throwable throwable) {
        logger.println(throwable.getMessage());
        throwable.printStackTrace(logger);
    }

    @Override
    public void log(String message) {
        logger.println(message);
    }

    @Override
    public void close() {
        if (logger != System.out) {
            logger.close();
        }
    }

	@Override
	public String getPath() {
		return this.logFile.getAbsolutePath();
	}
}

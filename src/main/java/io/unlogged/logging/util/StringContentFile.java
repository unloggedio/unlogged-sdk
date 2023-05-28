package io.unlogged.logging.util;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

import java.io.File;
import java.io.IOException;


/**
 * This class is to record the contents of String objects.
 */
public class StringContentFile {
	
	private StringFileListStream stringList;

	/**
	 * Create an instance.
	 * @param outputDir specifies a directory for storing output files.
	 * @throws IOException failed to save string contents to the file at outputDir
	 */
	public StringContentFile(File outputDir) throws IOException {
		FileNameGenerator filenames = new FileNameGenerator(outputDir, "LOG$String", ".txt");
		stringList = new StringFileListStream(filenames, 100000, false);
	}

	/**
	 * Record a String. 
	 * @param objectId specifies the object ID of the content object.
	 * @param content specifies the string to be recorded.
	 * TODO Improve the file format 
	 */
	public void write(long objectId, String content) {
		StringBuilder builder = new StringBuilder(content.length() + 32);
		builder.append(Long.toString(objectId));
		builder.append(",");
		builder.append(Integer.toString(content.length()));
		builder.append(",");
		builder.append("\"");
		JsonStringEncoder.getInstance().quoteAsString(content, builder);
		builder.append("\"");
		builder.append("\n");
		stringList.write(builder.toString());
	}

	/**
	 * Close the stream.
	 */
	public void close() {
		stringList.close();
	}
	
}

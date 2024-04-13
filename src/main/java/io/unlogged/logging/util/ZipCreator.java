package io.unlogged.logging.util;

import java.io.*;
import java.util.zip.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipCreator {

	private static final Logger logger = LoggerFactory.getLogger(ZipCreator.class);

    public static void writeZip(String weaveClassPath, String pathZip) {
        try {
			// open streams for reading and writing
            ZipOutputStream classWeaveOutputStream = new ZipOutputStream(new FileOutputStream(new File(pathZip)));
            FileInputStream classWeaveInputStream = new FileInputStream(weaveClassPath);
			
			// zip entry 
			ZipEntry classWeaveZipEntry = new ZipEntry(new File(weaveClassPath).getName());
            classWeaveOutputStream.putNextEntry(classWeaveZipEntry);
            
            // data from input to output files
            byte[] buffer = new byte[1024];
            int length;
            while ((length = classWeaveInputStream.read(buffer)) > 0) {
                classWeaveOutputStream.write(buffer, 0, length);
            }
            
            // Close the input and output stream
            classWeaveOutputStream.closeEntry();
            classWeaveInputStream.close();
            classWeaveOutputStream.close();
            
			logger.info("zip created successfully: " + pathZip);
        } catch (IOException e) {
			logger.info("zip write failed: " + pathZip);
            e.printStackTrace();
        }
    }
}

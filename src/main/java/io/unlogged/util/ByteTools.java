package io.unlogged.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ByteTools {

    public static String compressBase64String(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length)) {
            Deflater compressor = new Deflater();
            compressor.setLevel(Deflater.BEST_COMPRESSION);
            compressor.setInput(data);
            compressor.finish();
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                baos.write(buf, 0, count);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            // does not happen
        }
        return "";
    }

    public static byte[] decompressBase64String(String base64String) {
        byte[] data = Base64.getDecoder().decode(base64String);
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            // does not happen
        }
        return new byte[0];
    }

}

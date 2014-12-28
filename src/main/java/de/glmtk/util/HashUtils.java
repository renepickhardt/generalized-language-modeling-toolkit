package de.glmtk.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    /**
     * @see <a href=
     *      "http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java"
     *      >Stack Overflow: Getting a File's MD5 Checksum in Java</a>.
     */
    public static String generateMd5Hash(Path file) throws IOException {
        try {
            InputStream input = Files.newInputStream(file);

            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");

            int numRead;
            do {
                numRead = input.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            input.close();

            byte[] resultByte = digest.digest();
            String result = "";
            for (int i = 0; i != resultByte.length; ++i) {
                result +=
                        Integer.toString((resultByte[i] & 0xff) + 0x100, 16)
                        .substring(1);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}

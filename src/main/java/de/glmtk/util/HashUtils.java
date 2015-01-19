/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    /**
     * <a href=
     * "http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java"
     * >Stack Overflow: Getting a File's MD5 Checksum in Java</a>.
     */
    public static String generateMd5Hash(Path file) throws IOException {
        try {
            MessageDigest digest = null;
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[1024];
                digest = MessageDigest.getInstance("MD5");

                int numRead;
                do {
                    numRead = input.read(buffer);
                    if (numRead > 0)
                        digest.update(buffer, 0, numRead);
                } while (numRead != -1);
            }

            byte[] resultByte = digest.digest();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i != resultByte.length; ++i)
                result.append(Integer.toString((resultByte[i] & 0xff) + 0x100,
                        16).substring(1));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

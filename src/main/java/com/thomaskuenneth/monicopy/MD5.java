/*
 * Copyright 2017 - 2026 Thomas Kuenneth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thomaskuenneth.monicopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class calculates md5 hashes of files.
 *
 * @author Thomas Kuenneth
 */
public class MD5 {

    private static final Logger LOGGER = Logger.getGlobal();

    private final int buflen;
    private final byte[] buffer;
    private final MessageDigest md;
    private final StringBuilder sb;

    private boolean readFromBuffer;
    private long lengthOfFile;

    public MD5() {
        this(IoBuffers.DEFAULT_LENGTH);
    }

    public MD5(int len) {
        buffer = new byte[len];
        buflen = len;
        readFromBuffer = false;
        lengthOfFile = 0;
        MessageDigest _md = null;
        try {
            _md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage());
        }
        md = _md;
        sb = new StringBuilder();
    }

    /**
     * Returns the length of the last hashed file.
     *
     * @return length of the last hashed file
     */
    public long getLengthOfFile() {
        return lengthOfFile;
    }

    /**
     * Returns true if the file can be read completely from the buffer.
     *
     * @return true if the file can be read completely from the buffer
     */
    public boolean canReadFromBuffer() {
        return readFromBuffer;
    }

    /**
     * Returns the buffer the file was read into. Should be used only if
     * canReadFromBuffer() returns true.
     *
     * @return the buffer the file was read into
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Reset all values.
     */
    public void reset() {
        readFromBuffer = false;
        lengthOfFile = 0;
        if (md != null) {
            md.reset();
        }
    }

    /**
     * Returns the md5 hash of a file.
     *
     * @param file file to be hashed
     * @return the md5 hash of a file
     */
    public synchronized String getChecksum(File file) {
        String result = null;
        reset();
        if (file.exists() && file.isFile()) {
            lengthOfFile = file.length();
            long alreadyRead = 0;
            try (FileInputStream fis = new FileInputStream(file)) {
                while (alreadyRead < lengthOfFile) {
                    int bytesToRead = (int) Math.min(buflen, lengthOfFile - alreadyRead);
                    int n = fis.read(buffer, 0, bytesToRead);
                    if (n < 0) {
                        LOGGER.log(Level.SEVERE,
                                "unexpected EOF after {0} of {1} bytes in {2}",
                                new Object[]{alreadyRead, lengthOfFile, file.getAbsolutePath()});
                        break;
                    }
                    if (alreadyRead == 0) {
                        readFromBuffer = (n == lengthOfFile) && (lengthOfFile <= buflen);
                    }
                    md.update(buffer, 0, n);
                    alreadyRead += n;
                }
                result = convertDigestToString(md.digest());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "getChecksum()", e);
            }
        } else {
            lengthOfFile = 0;
        }
        return result;
    }

    private String convertDigestToString(byte[] digest) {
        sb.setLength(0);
        for (int i = 0; i < digest.length; i++) {
            long l = digest[i] & 0xff;
            if (l < 16) {
                sb.append('0');
            }
            sb.append(Long.toHexString(l));
        }
        return sb.toString();
    }
}

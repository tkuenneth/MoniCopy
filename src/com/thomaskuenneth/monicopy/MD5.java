/*
 * Copyright 2017 Thomas Kuenneth
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

    private static final Logger LOGGER
            = Logger.getLogger(MD5.class.getName());

    private final int buflen;
    private final byte[] buffer;
    private final MessageDigest md;
    private final StringBuilder sb;

    private boolean atomic;
    private int lengthOfFile;

    public MD5() {
        this(64 * 1024 * 1024);
    }

    public MD5(int len) {
        buffer = new byte[len];
        buflen = len;
        atomic = false;
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
    public int getLengthOfFile() {
        return lengthOfFile;
    }

    /**
     * Returns true if the file could be read completely into the buffer.
     *
     * @return true if the file could be read completely into the buffer
     */
    public boolean isAtomic() {
        return atomic;
    }

    /**
     * Returns the buffer the file was read into. Should be used only if
     * isAtomic() returns true.
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
        atomic = false;
        lengthOfFile = 0;
        md.reset();
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
        if (file.isFile()) {
            lengthOfFile = (int) file.length();
            int alreadyRead = 0;
            int bytesToRead;
            boolean first = true;
            try (FileInputStream fis = new FileInputStream(file)) {
                while ((bytesToRead = (lengthOfFile - alreadyRead)) > 0) {
                    if (bytesToRead > buflen) {
                        bytesToRead = buflen;
                    }
                    bytesToRead = fis.read(buffer, 0, bytesToRead);
                    if (first) {
                        first = false;
                        atomic = (bytesToRead == lengthOfFile) && (lengthOfFile <= buflen);
                    }
                    md.update(buffer, 0, bytesToRead);
                    alreadyRead += bytesToRead;
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

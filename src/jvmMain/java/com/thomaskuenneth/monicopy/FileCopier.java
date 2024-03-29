/*
 * Copyright 2017 - 2023 Thomas Kuenneth
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCopier {

    private static final Logger LOGGER = Logger.getGlobal();

    private final byte[] buffer;

    private String lastLocalizedMessage = null;

    public FileCopier() {
        this(64 * 1024 * 1024);
    }

    public FileCopier(int bufsize) {
        buffer = new byte[bufsize];
    }

    public String getLastLocalizedMessage() {
        return lastLocalizedMessage;
    }

    public synchronized boolean copy(File from, File to) {
        long lenFrom = from.length();
        long read = 0;
        long num;
        long buflen = buffer.length;
        File parent = to.getParentFile();
        var created = parent.mkdirs();
        LOGGER.log(Level.INFO, String.format("%s created: %b",
                parent.getAbsolutePath(), created));
        try (FileInputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to)) {
            while ((num = (lenFrom - read)) > 0) {
                if (num > buflen) {
                    num = buflen;
                }
                num = in.read(buffer, 0, (int) num);
                out.write(buffer, 0, (int) num);
                read += num;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error while copying", e);
            lastLocalizedMessage = e.getLocalizedMessage();
            return false;
        }
        return lenFrom == to.length();
    }

    public synchronized boolean copy(byte[] from, int lenFrom, File to) {
        File parent = to.getParentFile();
        var created = parent.mkdirs();
        LOGGER.log(Level.INFO, String.format("%s created: %b",
                parent.getAbsolutePath(), created));
        try (FileOutputStream out = new FileOutputStream(to)) {
            out.write(from, 0, lenFrom);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error while copying", e);
            lastLocalizedMessage = e.getLocalizedMessage();
            return false;
        }
        return lenFrom == to.length();
    }
}

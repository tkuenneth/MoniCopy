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
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCopier {

    private final byte[] buffer;

    public FileCopier() {
        this(IoBuffers.DEFAULT_LENGTH);
    }

    public FileCopier(int bufsize) {
        buffer = new byte[bufsize];
    }

    public int getBufferLength() {
        return buffer.length;
    }

    public synchronized boolean copy(File from, File to) throws IOException {
        long lenFrom = from.length();
        long read = 0;
        long num;
        long buflen = buffer.length;
        //noinspection ResultOfMethodCallIgnored
        to.getParentFile().mkdirs();
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
        }
        return lenFrom == to.length();
    }

    public synchronized boolean copy(byte[] from, long lenFrom, File to) throws IOException {
        if (lenFrom < 0 || lenFrom > from.length) {
            throw new IOException(String.format(
                    "invalid length %d for buffer of size %d", lenFrom, from.length));
        }
        //noinspection ResultOfMethodCallIgnored
        to.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(to)) {
            out.write(from, 0, (int) lenFrom);
        }
        return lenFrom == to.length();
    }
}

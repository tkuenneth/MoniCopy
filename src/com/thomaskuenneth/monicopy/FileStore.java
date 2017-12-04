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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a queue with the files to be copied.
 *
 * @author Thomas Kuenneth
 */
public class FileStore {

    private static final Logger LOGGER
            = Logger.getLogger(FileStore.class.getName());

    private final ArrayBlockingQueue<File> queue;

    private Thread filler;

    public FileStore() {
        this(1000);
    }

    public FileStore(int size) {
        queue = new ArrayBlockingQueue<>(size);
        filler = null;
    }

    public boolean isFilling() {
        return filler != null;
    }

    public File poll() {
        return queue.poll();
    }

    public synchronized void fill(File baseDir) {
        filler = new Thread(() -> {
            _fill(baseDir);
            filler = null;
        });
        filler.start();
    }

    private void _fill(File file) {
        if (file == null) {
            LOGGER.log(Level.SEVERE, "called _fill with null file");
            return;
        }
        if (file.isDirectory()) {
            LOGGER.log(Level.INFO, String.format("filling from %s",
                    file.getAbsolutePath()));
            File[] files = file.listFiles();
            if (files == null) {
                LOGGER.log(Level.SEVERE, "listFiles() returned null");
            } else {
                for (File child : files) {
                    _fill(child);
                }
            }
        } else if (file.isFile()) {
            try {
                queue.put(file);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "interruption while waiting for put()",
                        ex);
            }
        }
    }
}

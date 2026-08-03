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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a queue with the children to be copied.
 *
 * @author Thomas Kuenneth
 */
public class FileStore {
    
    private static final Logger LOGGER = Logger.getGlobal();
    public static final int SYMBOLIC_LINKS_INITIAL_CAPACITY = 1000;
    
    private final Pausable callback;
    private final List<File> files;
    private final List<File> symbolicLinks;
    
    private long numberOfDirectories;
    
    public FileStore(Pausable callback) {
        this.callback = callback;
        files = new ArrayList<>(200000);
        symbolicLinks = new ArrayList<>(SYMBOLIC_LINKS_INITIAL_CAPACITY);
        numberOfDirectories = 0;
    }

    /**
     * Get the number of children.
     *
     * @return Get the number of children
     */
    public long getNumberOfFiles() {
        return files.size();
    }

    public List<File> getSymbolicLinks() {
        return symbolicLinks;
    }

    /**
     * Get the number of directories. The base directory is included.
     *
     * @return number of directories
     */
    public long getNumberOfDirectories() {
        return numberOfDirectories;
    }
    
    public synchronized List<File> fill(File file, List<String> ignores) {
        if (ignores == null) {
            ignores = new ArrayList<>();
        }
        if (file == null) {
            LOGGER.log(Level.SEVERE, "called fill() with null file");
            return null;
        }
        if (callback != null) {
            callback.checkForPause();
        }
        if (!Files.isSymbolicLink(file.toPath())) {
            if (file.isDirectory()) {
                String absolutePath = file.getAbsolutePath();
                if (!ignores.contains(absolutePath)) {
                    numberOfDirectories += 1;
                    LOGGER.log(Level.FINE, String.format("filling from %s",
                            absolutePath));
                    File[] children = file.listFiles();
                    if (children == null) {
                        LOGGER.log(Level.SEVERE,
                                String.format("listFiles(%s) returned null", absolutePath));
                    } else {
                        for (File child : children) {
                            fill(child, ignores);
                        }
                    }
                }
            } else if (file.isFile()) {
                files.add(file);
            }
        } else {
            String absolutePath = file.getAbsolutePath();
            if (!ignores.contains(absolutePath)) {
                symbolicLinks.add(file);
            }
        }
        return files;
    }
}

/*
 * Copyright 2018 Thomas Kuenneth
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
import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class stores absolute path names of folders.
 *
 * @author Thomas Kuenneth
 */
public class FolderMap {

    private final TreeSet<File> parents = new TreeSet<>();

    public void fill(File base) {
        parents.clear();
        _fill(base);
    }

    public Iterator<File> getIterator() {
        return parents.descendingIterator();
    }

    private void _fill(File dir) {
        if (dir.isDirectory()) {
            parents.add(dir);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    _fill(file);
                }
            }
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thomaskuenneth.monicopy;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tkuen
 */
public class FileStore {

    private static final Logger LOGGER = Logger.getLogger(FileStore.class.getName());

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
            Main.message("called _fill with null file");
            return;
        }
        if (file.isDirectory()) {
            Main.message(String.format("filling from %s", file.getAbsolutePath()));
            File[] files = file.listFiles();
            if (files == null) {
                Main.message("listFiles() returned null");
            } else {
                for (File child : files) {
                    _fill(child);
                }
            }
        } else if (file.isFile()) {
            try {
                queue.put(file);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "interruption while waiting for put()", ex);
            }
        }
    }
}

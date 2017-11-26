/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thomaskuenneth.monicopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tkuen
 */
public class FileCopier {

    private static final Logger LOGGER = Logger.getLogger(FileCopier.class.getName());

    private final byte[] buffer;

    public FileCopier() {
        this(32 * 1024);
    }

    public FileCopier(int bufsize) {
        buffer = new byte[bufsize];
    }

    public synchronized boolean copy(File from, File to) {
        int lenFrom = (int) from.length();
        int read = 0;
        int num;
        int buflen = buffer.length;
        File parent = to.getParentFile();
        parent.mkdirs();
        try (FileInputStream in = new FileInputStream(from);
                FileOutputStream out = new FileOutputStream(to)) {
            while ((num = (lenFrom - read)) > 0) {
                if (num > buflen) {
                    num = buflen;
                }
                num = in.read(buffer, 0, num);
                out.write(buffer, 0, num);
                read += num;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error while copying", e);
        }
        return lenFrom == to.length();
    }
}

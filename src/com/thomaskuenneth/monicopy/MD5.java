/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thomaskuenneth.monicopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author tkuen
 */
public class MD5 {

    private static final String MD5 = "MD5";

    private final int buflen;
    private final byte[] buffer;
    private final MessageDigest md;
    private final StringBuilder sb;

    public MD5() {
        this(32 * 1024);
    }

    public MD5(int len) {
        buffer = new byte[len];
        buflen = len;
        MessageDigest _md = null;
        try {
            _md = MessageDigest.getInstance(MD5);
        } catch (NoSuchAlgorithmException e) {
            // FIXME: error handling
        }
        md = _md;
        sb = new StringBuilder();
    }

    public synchronized String getChecksum(File file) {
        String result = null;
        if (file.isFile()) {
            int filelen = (int) file.length();
            int read = 0;
            int num;
            try (FileInputStream fis = new FileInputStream(file)) {
                while ((num = (filelen - read)) > 0) {
                    if (num > buflen) {
                        num = buflen;
                    }
                    num = fis.read(buffer, 0, num);
                    md.update(buffer, 0, num);
                    read += num;
                }
                result = convertDigestToString(md.digest());
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                md.reset();
            }
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

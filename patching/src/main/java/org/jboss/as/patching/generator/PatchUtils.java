/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.patching.generator;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utilities related to patch file generation.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PatchUtils {

    private PatchUtils() {
        // no instantiation
    }

    public static final byte[] NO_CONTENT = new byte[0];

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        if (sourceFile.isDirectory()) {
            copyDir(sourceFile, targetFile);
        } else {
            File parent = targetFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IllegalStateException("Cannot create directory " + parent);
                }
            }
            final InputStream is = new FileInputStream(sourceFile);
            try {
                final OutputStream os = new FileOutputStream(targetFile);
                try {
                    copyStreamAndClose(is, os, DEFAULT_BUFFER_SIZE);
                } finally {
                    safeClose(os);
                }
            } finally {
                safeClose(is);
            }
        }
    }

    private static void copyDir(File sourceDir, File targetDir) throws IOException {
        if (targetDir.exists()) {
            if (!targetDir.isDirectory()) {
                throw new IllegalStateException(targetDir + " is not a directory");
            }
        } else if (!targetDir.mkdirs()) {
            throw new IllegalStateException("Cannot create directory " + targetDir);
        }

        File[] children = sourceDir.listFiles();
        if (children != null) {
            for (File child : children) {
                copyFile(child, new File(targetDir, child.getName()));
            }
        }
    }

    /**
     * Copy input stream to output stream and close them both
     *
     * @param is input stream
     * @param os output stream
     * @param bufferSize the buffer size to use
     *
     * @throws IOException for any error
     */
    private static void copyStreamAndClose(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        try {
            copyStream(is, os, bufferSize);
            // throw an exception if the close fails since some data might be lost
            is.close();
            os.close();
        }
        finally {
            // ...but still guarantee that they're both closed
            safeClose(is);
            safeClose(os);
        }
    }

    private static void copyStream(InputStream is, OutputStream os, int bufferSize)
            throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("input stream is null");
        }
        if (os == null) {
            throw new IllegalArgumentException("output stream is null");
        }
        byte[] buff = new byte[bufferSize];
        int rc;
        while ((rc = is.read(buff)) != -1) os.write(buff, 0, rc);
        os.flush();
    }

    public static void safeClose(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }
}

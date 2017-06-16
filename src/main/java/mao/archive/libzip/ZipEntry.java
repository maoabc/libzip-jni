/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package mao.archive.libzip;


import java.util.Arrays;

/**
 * 从java.util.zip.ZipEntry修改来的
 * This class is used to represent a ZIP file entry.
 *
 * @author David Connelly
 */
public class ZipEntry {

    protected long index = -1;                 /* index within archive */

    private byte[] rawName;        // entry name
    private String name;
    private long time;     // last modification time
    private long crc = -1;      // crc-32 of entry data
    private long size = -1;     // uncompressed size of entry data
    private long csize = -1;    // compressed size of entry data
    private int method = -1;    // compression method
    private int emethod = -1;     // encryption method
    private int flag = 0;       // general purpose flag
    private byte[] extra;       // optional extra field data for entry
    private String comment;     // optional comment string for entry

    private ZipCoder coder;

    /**
     * Compression method for uncompressed entries.
     */
    public static final int STORED = 0;

    /**
     * Compression method for compressed (deflated) entries.
     */
    public static final int DEFLATED = 8;


    /**
     * Creates a new un-initialized zip entry
     */
    public ZipEntry() {
    }

    /**
     * Creates a new zip entry with the specified name.
     *
     * @param name The entry name
     * @throws NullPointerException     if the entry name is null
     * @throws IllegalArgumentException if the entry name is longer than
     *                                  0xFFFF bytes
     */
    public ZipEntry(String name) {
        this.rawName = ZipCoder.UTF8.getBytes(name);
        this.name = name;
    }


    /**
     * Creates a new zip entry with fields taken from the specified
     * zip entry.
     *
     * @param e A zip Entry object
     * @throws NullPointerException if the entry object is null
     */
    public ZipEntry(ZipEntry e) {
        index = e.index;
        rawName = e.rawName;
        name = e.name;
        time = e.time;
        crc = e.crc;
        size = e.size;
        csize = e.csize;
        method = e.method;
        flag = e.flag;
        extra = e.extra;
        comment = e.comment;
    }

    public int getEmethod() {
        return emethod;
    }


    /**
     * Returns the name of the entry.
     *
     * @return the name of the entry
     */
    public String getName(ZipCoder coder) {
        if (name == null) {
            name = coder.toString(rawName);
            this.coder = coder;
        }
        return name;
    }

    /**
     * Returns the name of the entry.
     *
     * @return the name of the entry
     */
    public String getName() {
        if (name == null) {
            name = (coder == null ? ZipCoder.UTF8 : coder).toString(rawName);
        }
        return name;
    }


    /**
     * Sets the last modification time of the entry.
     * <p>
     * <p> If the entry is output to a ZIP file or ZIP file formatted
     * output stream the last modification time set by this method will
     * be stored into the {@code date and time fields} of the zip file
     * entry and encoded in standard {@code MS-DOS date and time format}.
     * The {@link java.util.TimeZone#getDefault() default TimeZone} is
     * used to convert the epoch time to the MS-DOS data and time.
     *
     * @param time The last modification time of the entry in milliseconds
     *             since the epoch
     * @see #getTime()
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * Returns the last modification time of the entry.
     * <p>
     * <p> If the entry is read from a ZIP file or ZIP file formatted
     * input stream, this is the last modification time from the {@code
     * date and time fields} of the zip file entry. The
     * {@link java.util.TimeZone#getDefault() default TimeZone} is used
     * to convert the standard MS-DOS formatted date and time to the
     * epoch time.
     *
     * @return The last modification time of the entry in milliseconds
     * since the epoch, or -1 if not specified
     * @see #setTime(long)
     */
    public long getTime() {
        return time * 1000;
    }


    /**
     * Sets the uncompressed size of the entry data.
     *
     * @param size the uncompressed size in bytes
     * @throws IllegalArgumentException if the specified size is less
     *                                  than 0, is greater than 0xFFFFFFFF when
     *                                  <a href="package-summary.html#zip64">ZIP64 format</a> is not supported,
     *                                  or is less than 0 when ZIP64 is supported
     * @see #getSize()
     */
    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
    }

    /**
     * Returns the uncompressed size of the entry data.
     *
     * @return the uncompressed size of the entry data, or -1 if not known
     * @see #setSize(long)
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the size of the compressed entry data.
     * <p>
     * <p> In the case of a stored entry, the compressed size will be the same
     * as the uncompressed size of the entry.
     *
     * @return the size of the compressed entry data, or -1 if not known
     * @see #setCompressedSize(long)
     */
    public long getCompressedSize() {
        return csize;
    }

    /**
     * Sets the size of the compressed entry data.
     *
     * @param csize the compressed size to set to
     * @see #getCompressedSize()
     */
    public void setCompressedSize(long csize) {
        this.csize = csize;
    }

    /**
     * Sets the CRC-32 checksum of the uncompressed entry data.
     *
     * @param crc the CRC-32 value
     * @throws IllegalArgumentException if the specified CRC-32 value is
     *                                  less than 0 or greater than 0xFFFFFFFF
     * @see #getCrc()
     */
    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = crc;
    }

    /**
     * Returns the CRC-32 checksum of the uncompressed entry data.
     *
     * @return the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known
     * @see #setCrc(long)
     */
    public long getCrc() {
        return crc;
    }

    /**
     * Sets the compression method for the entry.
     *
     * @param method the compression method, either STORED or DEFLATED
     * @throws IllegalArgumentException if the specified compression
     *                                  method is invalid
     * @see #getMethod()
     */
    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }

    /**
     * Returns the compression method of the entry.
     *
     * @return the compression method of the entry, or -1 if not specified
     * @see #setMethod(int)
     */
    public int getMethod() {
        return method;
    }


    /**
     * Returns the extra field data for the entry.
     *
     * @return the extra field data for the entry, or null if none
     */
    public byte[] getExtra() {
        return extra;
    }

    /**
     * Sets the optional comment string for the entry.
     * <p>
     * <p>ZIP entry comments have maximum length of 0xffff. If the length of the
     * specified comment string is greater than 0xFFFF bytes after encoding, only
     * the first 0xFFFF bytes are output to the ZIP file entry.
     *
     * @param comment the comment string
     * @see #getComment()
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the comment string for the entry.
     *
     * @return the comment string for the entry, or null if none
     * @see #setComment(String)
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns true if this is a directory entry. A directory entry is
     * defined to be one whose name ends with a '/'.
     *
     * @return true if this is a directory entry
     */
    public boolean isDirectory() {
        byte[] rawName = this.rawName;
        return rawName[rawName.length - 1] == '/';
    }

    /**
     * Returns a string representation of the ZIP entry.
     */
    public String toString() {
        return getName(ZipCoder.UTF8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZipEntry)) {
            return false;
        }
        ZipEntry entry = (ZipEntry) o;
        return Arrays.equals(this.rawName, entry.rawName);
    }

    /**
     * Returns the hash code value for this entry.
     */
    public int hashCode() {
        return Arrays.hashCode(rawName);
    }

}

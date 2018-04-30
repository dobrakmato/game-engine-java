/**
 * lpsim - 
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.lpsim.content.bf;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.*;
import java.util.Arrays;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class BFInputStream implements DataInput {

    private InputStream original;
    private DataInputStream in;

    /**
     * Creates a BFInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public BFInputStream(InputStream in) {
        this.original = in;
        this.in = new DataInputStream(in);
    }

    public void verifyMagic(byte[] magic) throws IOException {
        byte[] actualMagic = new byte[3];
        in.readFully(actualMagic);

        if (!Arrays.equals(actualMagic, magic)) {
            throw new RuntimeException("Invalid file! Trying to open " + BFConstants.getTypeName(actualMagic) + " as Image!");
        }
    }

    public void verifyVersion() throws IOException {
        byte actualVersion = in.readByte();

        if (actualVersion > BFConstants.VERSION) {
            throw new RuntimeException("Reading file with greater version " + actualVersion + " then supported " + BFConstants.VERSION + "!");
        }
    }

    /**
     * Automatically decompresses remaining bytes in stream with specified compression type.
     *
     * @param compressionType compression type
     * @throws IOException
     */
    public void decompress(int compressionType) throws IOException {
        switch (compressionType) {
            case BFConstants.COMPRESSION_NONE:
                break;
            case BFConstants.COMPRESSION_DEFLATE:
                decompressDeflate();
                break;
            case BFConstants.COMPRESSION_LZ4:
                decompressLZ4();
                break;
            default:
                throw new RuntimeException("Invalid compression type " + compressionType + " !");
        }
    }

    /**
     * Decompresses remaining bytes in stream using deflate.
     */
    public void decompressDeflate() {
        in = new DataInputStream(new InflaterInputStream(original, new Inflater(), 512));
    }

    /**
     * Decompresses remaining bytes in stream using LZ4.
     *
     * @throws IOException
     */
    public void decompressLZ4() throws IOException {
        int uncompressedLength = in.readInt();
        int compressedLength = in.readInt();

        byte[] compressed = new byte[compressedLength];
        byte[] uncompressed = new byte[uncompressedLength];

        in.readFully(compressed);
        // Whole file should be read. Close the stream.
        original.close();

        LZ4Factory factory = BFConstants.getLZ4Factory();
        LZ4FastDecompressor decompressor = factory.fastDecompressor();
        decompressor.decompress(compressed, uncompressed);

        // Fake input stream with ByteArrayInputStream.
        in = new DataInputStream(new ByteArrayInputStream(uncompressed));
    }

    public void close() throws IOException {
        in.close();
        this.original = null;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        in.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        in.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return in.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return in.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return in.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return in.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return in.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return in.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return in.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return in.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return in.readUTF();
    }
}

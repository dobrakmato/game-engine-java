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

import eu.matejkormuth.lpsim.content.StreamUtils;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class BFOutputStream implements DataOutput {

    private OutputStream original;
    private DataOutputStream out;

    // For LZ4 compression.
    private ByteArrayOutputStream lz4FakeStream = null;

    /**
     * Creates a new BFOutputStream to write data to the specified
     * underlying output stream. The counter <code>written</code> is
     * set to zero.
     *
     * @param out the underlying output stream, to be saved for later
     *            use.
     * @see FilterOutputStream#out
     */
    public BFOutputStream(OutputStream out) {
        this.original = out;
        this.out = new DataOutputStream(out);
    }

    public void writeVersion() throws IOException {
        out.writeByte(BFConstants.VERSION);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        if (lz4FakeStream != null) {
            throw new RuntimeException("Forget to call compressFinish()?");
        }

        out.close();
        original = null;
    }

    /**
     * Automatically starts compression in specified format. Uncompressed size is used for
     * optimizing memory usage of compression with reflection hack.
     *
     * @param compressionType  type of compression
     * @param uncompressedSize amount of bytes that will be written before calling compressFinish()
     * @throws IOException
     */
    public void compressStart(int compressionType, int uncompressedSize) throws IOException {
        switch (compressionType) {
            case BFConstants.COMPRESSION_NONE:
                break;
            case BFConstants.COMPRESSION_DEFLATE:
                this.compressStartDeflate();
                break;
            case BFConstants.COMPRESSION_LZ4:
                this.compressStartLZ4(uncompressedSize);
                break;
            default:
                throw new RuntimeException("Invalid compression type " + compressionType + " !");
        }
    }

    public void compressStartDeflate() throws IOException {
        // For deflate, we just replace DataOutputStream with deflated one.
        out.flush();
        out = new DataOutputStream(new DeflaterOutputStream(original, new Deflater(9), 512)); // 9 is deflate highest compression level
    }

    public void compressStartLZ4(int uncompressedSize) throws IOException {
        out.flush();
        // First we create fake output stream.
        lz4FakeStream = ByteArrayStreamUtil
                .createByteArrayOutputStream(uncompressedSize);
        out = new DataOutputStream(lz4FakeStream);
    }

    public void compressFinish() throws IOException {
        // If we were compressing LZ4 finish compression.
        if (lz4FakeStream != null) {
            out.flush();

            LZ4Factory factory = BFConstants.getLZ4Factory();
            LZ4Compressor compressor = factory.highCompressor(17); // 17 is the highest compression level for lz4

            byte[] uncompressed = ByteArrayStreamUtil.getByteArray(lz4FakeStream);
            byte[] compressed = compressor.compress(uncompressed);

            StreamUtils.write(original, uncompressed.length);
            StreamUtils.write(original, compressed.length);

            // Write data to *real* stream and close it.
            original.write(compressed);
            original.close();
            lz4FakeStream = null;
        }
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        out.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        out.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        out.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        out.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        out.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        out.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        out.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        out.writeUTF(s);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }
}

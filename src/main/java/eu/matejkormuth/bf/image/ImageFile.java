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
package eu.matejkormuth.bf.image;

import eu.matejkormuth.bf.BFUtils;
import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.compression.BFOutputStream;
import eu.matejkormuth.lpsim.*;
import eu.matejkormuth.lpsim.gl.FilterMode;
import eu.matejkormuth.lpsim.gl.Texture2D;
import eu.matejkormuth.lpsim.gl.TextureCube;
import eu.matejkormuth.lpsim.gl.WrapMode;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * BF Image file.
 */
@Slf4j
public class ImageFile {

    public static final byte BF_TYPE = 'I';

    /*
     * Version history:
     * 1 - initial version with LZ4 compression
     */
    public static final byte VERSION = 1;

    private static int verifyCanReadVersion(BFInputStream in) throws IOException {
        int version = in.readByte();
        if (version > VERSION) {
            throw new RuntimeException("Can't read this file! (Version mismatch)");
        }
        return version;
    }

    private static int readHeader(BFInputStream in) throws IOException {
        BFUtils.readHeader(in);

        if (in.readByte() != BF_TYPE) {
            throw new RuntimeException("Not a BF Image file! (File Type mismatch)");
        }

        return verifyCanReadVersion(in);
    }

    private static void writeHeader(BFOutputStream out) throws IOException {
        BFUtils.writeHeader(out);
        out.writeByte(BF_TYPE);
        out.writeByte(VERSION);
    }


    // used mainly during development
    public static Image loadToImage(BFInputStream in) throws IOException {
        int version = readHeader(in);

        in.decompress();

        int width = in.readShort();
        int height = in.readShort();

        Image image = new Image(width, height);

        int layers = in.readUnsignedByte();
        for (int i = 0; i < layers; i++) {
            String name = in.readString();
            Format format = in.readImageFormat();

            Layer layer = image.addLayer(name, format);
            in.readFully(layer.getRaster());
        }

        in.close();
        return image;
    }

    // used mainly in engine
    public static ByteBuffer loadSingleLayer(BFInputStream in) throws IOException {
        int version = readHeader(in);

        in.decompress();

        int width = in.readShort();
        int height = in.readShort();

        int layers = in.readUnsignedByte();

        if (layers > 1) {
            throw new RuntimeException("Too many layers!");
        }

        in.readString(); // skip layer name
        Format format = in.readImageFormat();

        int length = width * height * (format.getBitsPerPixel() / Byte.SIZE);
        byte[] buffer = new byte[length];

        in.readFully(buffer);
        in.close();

        return (ByteBuffer) BufferUtils.createByteBuffer(buffer.length).put(buffer).flip();
    }

    public static void loadIntoTexture(BFInputStream in, Texture2D to) throws IOException {
        Application.P.texturesIO.end();
        Application.P.texturesProcessing.start();
        Application.P.texturesParse.start();
        int version = readHeader(in);

        Application.P.texturesParse.end();
        Application.P.texturesDecompress.start();
        in.decompress();
        Application.P.texturesDecompress.end();

        Application.P.texturesParse.start();
        int width = in.readShort();
        int height = in.readShort();

        int layers = in.readUnsignedByte();

        if (layers > 1) {
            throw new RuntimeException("Too many layers!");
        }

        in.readString(); // skip layer name
        Format format = in.readImageFormat();

        int length = width * height * (format.getChannels() * format.getBitsPerPixel() / Byte.SIZE);
        byte[] buffer = new byte[length];

        in.readFully(buffer);
        in.close();
        Application.P.texturesParse.end();

        Application.P.texturesDirectBuffer.start();
        ByteBuffer buff = DirectBufferPool.acquire(buffer.length);
        buff.put(buffer, 0, buffer.length);
        buff.flip();
        Application.P.texturesDirectBuffer.end();

        Application.P.texturesProcessing.end();

        Application.P.texturesSetup.start();
        to.bind();
        Application.P.texturesSetup.end();

        Application.P.texturesUpload.start();
        boolean compressTextures = false;
        int internalFormat = compressTextures ?
                Format.getGlCompressedInternalFormat(format) : Format.getGlInternalFormat(format);
        to.setImageData(buff, Format.getGlFormat(format), internalFormat, width, height);
        DirectBufferPool.release(buff);
        Application.P.texturesUpload.end();

        Application.P.texturesSetup.start();
        to.generateMipmaps();
        to.setFilters(FilterMode.LINEAR_MIPMAP_LINEAR, FilterMode.LINEAR);
        to.setWraps(WrapMode.REPEAT, WrapMode.REPEAT);
        to.enableMaxAF();
        Application.P.texturesSetup.end();

        //Util.checkGLError();
    }

    public static void loadIntoTexture(BFInputStream in, TextureCube to) throws IOException {
        int version = readHeader(in);

        in.decompress();

        int width = in.readShort();
        int height = in.readShort();

        int layers = in.readUnsignedByte();

        if (layers < 6) {
            throw new RuntimeException("Need at least 6 layers: posx, posy, posz, negx, negy, negz");
        }

        to.bind();
        to.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        to.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);

        int foundLayers = 0;
        for (int i = 0; i < 6; i++) {
            String layerName = in.readString();
            Format format = in.readImageFormat();

            int length = width * height * (format.getChannels() * format.getBitsPerPixel() / Byte.SIZE);
            byte[] buffer = new byte[length];

            in.readFully(buffer);

            ByteBuffer buff = (ByteBuffer) DirectBufferPool.acquire(buffer.length).put(buffer).flip();

            switch (layerName.toLowerCase()) {
                case "posx":
                    foundLayers++;
                    to.setPositiveXImageData(width, height, Format.getGlFormat(format), Format.getGlInternalFormat(format), buff);
                    break;
                case "posy":
                    foundLayers++;
                    to.setPositiveYImageData(width, height, Format.getGlFormat(format), Format.getGlInternalFormat(format), buff);
                    break;
                case "posz":
                    foundLayers++;
                    to.setPositiveZImageData(width, height, Format.getGlFormat(format), Format.getGlInternalFormat(format), buff);
                    break;
                case "negx":
                    foundLayers++;
                    to.setNegativeXImageData(width, height, Format.getGlFormat(format), Format.getGlInternalFormat(format), buff);
                    break;
                case "negy":
                    foundLayers++;
                    to.setNegativeYImageData(width, height, Format.getGlFormat(format), Format.getGlInternalFormat(format), buff);
                    break;
                case "negz":
                    foundLayers++;
                    to.setNegativeZImageData(width, height, Format.getGlFormat(format), Format.getGlInternalFormat(format), buff);
                    break;
            }

            DirectBufferPool.release(buff);
        }

        if (foundLayers != 6) {
            throw new RuntimeException("Not a valid TextureCube BIF file: not enough cube sides!");
        }

        to.generateMipmaps();
        in.close();

    }

    public static void save(Image image, BFOutputStream out) throws IOException {
        writeHeader(out);

        // compute size
        int size = 0;
        for (Layer layer : image.getLayers()) {
            size += Integer.BYTES; // name is string which is prefixed by its length
            size += layer.getName().getBytes("UTF-8").length; //name
            size += 3 * Byte.BYTES; // format
            size += layer.getRaster().length; // raster
        }

        System.out.println("comptued size: " + size);

        out.compressStart((2 * Short.BYTES) + Byte.BYTES + size);

        out.writeShort(image.getWidth());
        out.writeShort(image.getHeight());

        out.writeByte(image.getLayers().size());
        for (Layer layer : image.getLayers()) {
            out.writeString(layer.getName());
            out.writeImageFormat(layer.getFormat());

            out.write(layer.getRaster());
        }

        out.compressFinish();
        out.flush();
        out.close();
    }

    public static void save(TextureCube image, BFOutputStream out) throws IOException {

    }


}

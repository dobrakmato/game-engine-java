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
package eu.matejkormuth.bf.tools;

import eu.matejkormuth.bf.compression.BFOutputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.ColorSpace;
import eu.matejkormuth.lpsim.Format;
import eu.matejkormuth.lpsim.Image;
import eu.matejkormuth.lpsim.Layer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class Skybox2Bif extends AbstractTool {

    public static void main(String[] args) {
        new Skybox2Bif().start(args);
    }

    @Override
    public void setupOptions(Options options) {
        options.addOption("posx", "positivex", true, "Image for positive X side of cubemap");
        options.addOption("posy", "positivey", true, "Image for positive Y side of cubemap");
        options.addOption("posz", "positivez", true, "Image for positive Z side of cubemap");

        options.addOption("negx", "negativex", true, "Image for negative X side of cubemap");
        options.addOption("negy", "negativey", true, "Image for negative Y side of cubemap");
        options.addOption("negz", "negativez", true, "Image for negative Z side of cubemap");

        options.addOption("o", "output", true, "Output bif file");

        options.addOption("gm", "gamma", false, "Set gamma color space for images (default)");
        options.addOption("ln", "linear", false, "Set linear color space for images");

        options.addOption("rgba", "rgba", false, "Allow alpha in texture");
    }

    @Override
    public void execute(CommandLine cmd) throws Exception {
        if (cmd.hasOption("gm") && cmd.hasOption("ln")) {
            print("Both gamma and linear color spaces are specified in arguments!");
            return;
        }

        if (!(cmd.hasOption("posx") &&
                cmd.hasOption("posy") &&
                cmd.hasOption("posz") &&
                cmd.hasOption("negx") &&
                cmd.hasOption("negy") &&
                cmd.hasOption("negz"))) {
            help();
            return;
        }

        if (!cmd.hasOption("o")) {
            print("No output file specified!");
            help();
            return;
        }

        boolean hasAlpha = cmd.hasOption("rgba");
        boolean gamma = !cmd.hasOption("ln");

        String pathPosX = cmd.getOptionValue("posx");
        String pathPosY = cmd.getOptionValue("posy");
        String pathPosZ = cmd.getOptionValue("posz");

        String pathNegX = cmd.getOptionValue("negx");
        String pathNegY = cmd.getOptionValue("negy");
        String pathNegZ = cmd.getOptionValue("negz");

        Image temp = awtLoad(pathPosX, gamma, hasAlpha, "Positive X");
        Format format = temp.getLayer().getFormat();

        Image cubemap = new Image(temp.getWidth(), temp.getHeight());

        print("------------------");
        print("Copying posX layer to new cubemap image.");
        temp.getLayer().copyTo(cubemap.addLayer("posx", format)); // copy layer too avoid second reading of file

        print("Copying other images.");
        awtLoadToLayer(cubemap.addLayer("posy", format), pathPosY, gamma, hasAlpha, "Positive Y");
        awtLoadToLayer(cubemap.addLayer("posz", format), pathPosZ, gamma, hasAlpha, "Positive Z");

        awtLoadToLayer(cubemap.addLayer("negx", format), pathNegX, gamma, hasAlpha, "Negative X");
        awtLoadToLayer(cubemap.addLayer("negy", format), pathNegY, gamma, hasAlpha, "Negative Y");
        awtLoadToLayer(cubemap.addLayer("negz", format), pathNegZ, gamma, hasAlpha, "Negative Z");

        ImageFile.save(cubemap, new BFOutputStream(new BufferedOutputStream(new FileOutputStream(cmd.getOptionValue("o")))));
    }


    private static Image awtLoad(String in, boolean gamma, boolean hasAlpha, String side) throws IOException {
        print("Loading input file (" + side + ")...");
        BufferedImage img = ImageIO.read(Paths.get(in).toAbsolutePath().toFile());
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());

        print("[" + side + "] Width: " + img.getWidth());
        print("[" + side + "] Height: " + img.getHeight());

        print("[" + side + "] Has alpha (set by arguments! (even implicitly)): " + hasAlpha);
        print("[" + side + "] Pixels " + img.getHeight() * img.getWidth());
        print("[" + side + "] Bytes: " + (hasAlpha ?
                (4 * img.getHeight() * img.getWidth()) : (3 * img.getWidth() * img.getHeight())));

        print("[" + side + "]Creating Image...");
        Image image = new Image(img.getWidth(), img.getHeight(),
                new Format(hasAlpha ? 4 : 3, 8, gamma ? ColorSpace.GAMMA : ColorSpace.LINEAR));
        Layer l = image.getLayer();

        print("[" + side + "] Created raster size: " + l.getRaster().length);
        print("[" + side + "] Created layer pixels: " + l.getPixels());
        print("[" + side + "] Created layer bytes: " + l.getLength());
        print("[" + side + "] Created layer format: " + l.getFormat());
        print("[" + side + "] Created layer channels: " + l.getChannelCount());

        int rasterIndex = 0;
        int currentPixel;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {

                //pixel   = rgbArray[offset + (y-startY)*scansize + (x-startX)];
                currentPixel = pixels[y * img.getWidth() + x];

                int r = ((currentPixel >> 16) & 0xFF);
                int g = ((currentPixel >> 8) & 0xFF);
                int b = ((currentPixel) & 0xFF);

                l.getRaster()[rasterIndex++] = (byte) r;
                l.getRaster()[rasterIndex++] = (byte) g;
                l.getRaster()[rasterIndex++] = (byte) b;

                if (hasAlpha) {
                    int a = ((currentPixel >> 24) & 0xFF);
                    l.getRaster()[rasterIndex++] = (byte) a;
                }

            }
        }

        return image;
    }

    private static void awtLoadToLayer(Layer l, String in, boolean gamma, boolean hasAlpha, String side) throws IOException {
        print("Loading input file (" + side + ")...");
        BufferedImage img = ImageIO.read(Paths.get(in).toAbsolutePath().toFile());
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());

        print("[" + side + "] Width: " + img.getWidth());
        print("[" + side + "] Height: " + img.getHeight());

        print("[" + side + "] Has alpha (set by arguments! (even implicitly)): " + hasAlpha);
        print("[" + side + "] Pixels " + img.getHeight() * img.getWidth());
        print("[" + side + "] Bytes: " + (hasAlpha ?
                (4 * img.getHeight() * img.getWidth()) : (3 * img.getWidth() * img.getHeight())));

        int rasterIndex = 0;
        int currentPixel;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {

                currentPixel = pixels[y * img.getWidth() + x];

                int r = ((currentPixel >> 16) & 0xFF);
                int g = ((currentPixel >> 8) & 0xFF);
                int b = ((currentPixel) & 0xFF);

                l.getRaster()[rasterIndex++] = (byte) r;
                l.getRaster()[rasterIndex++] = (byte) g;
                l.getRaster()[rasterIndex++] = (byte) b;

                if (hasAlpha) {
                    int a = ((currentPixel >> 24) & 0xFF);
                    l.getRaster()[rasterIndex++] = (byte) a;
                }

            }
        }
    }
}

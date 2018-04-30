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

import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.ChannelUtils;
import eu.matejkormuth.lpsim.Image;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

@Slf4j
public class Bif2Png {

    public static void main(String[] args) throws IOException {
        String input = args[0];
        String output = input.replace(".bif", "_backconv.png");

        print("Reading...");
        Image original = ImageFile.loadToImage(new BFInputStream(new FileInputStream(Paths.get(input).toAbsolutePath().toFile())));
        print("Writing...");
        toAwt(output, original);
        print("Done!");
    }

    private static void print(String line) {
        System.out.println(line);
    }

    private static void toAwt(String output, Image o) throws IOException {
        BufferedImage img = new BufferedImage(o.getWidth(), o.getHeight(), o.getLayer().getChannelCount() == 3 ? TYPE_INT_RGB : TYPE_INT_ARGB);
        for (int y = 0; y < o.getWidth(); y++) {
            for (int x = 0; x < o.getHeight(); x++) {
                int r = ChannelUtils.red(o).get(x, y) & 0xFF;
                int g = ChannelUtils.green(o).get(x, y) & 0xFF;
                int b = ChannelUtils.blue(o).get(x, y) & 0xFF;

                int rgb;
                if (o.getLayer().getChannelCount() == 3) {
                    rgb = (r << 16) | (g << 8) | b;
                } else {
                    int a = ChannelUtils.alpha(o).get(x, y) & 0xFF;
                    rgb = (a << 24) | (r << 16) | (g << 8) | b;
                }

                img.setRGB(x, y, rgb);
            }
        }
        ImageIO.write(img, "png", Paths.get(output).toAbsolutePath().toFile());
    }
}

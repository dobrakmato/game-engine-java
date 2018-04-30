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
import eu.matejkormuth.bf.compression.CompressionUtil;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
public class Png2Bif {

    public static void main(String[] args) throws IOException {
        Options options = new Options();
        options.addOption("i", "input", true, "input file to read");
        options.addOption("o", "output", true, "output file to write");

        options.addOption("r", "red", true, "specifies mapping of input red channel");
        options.addOption("g", "green", true, "specifies mapping of input green channel");
        options.addOption("b", "blue", true, "specifies mapping of input blue channel");
        options.addOption("a", "alpha", true, "specifies mapping of input alpha channel");

        options.addOption("fast", "fast-compression", false, "use fast but inefficient compression");

        options.addOption("gm", "gamma", false, "specifies gamma color space");
        options.addOption("ln", "linear", false, "specifies linear color space");

        options.addOption("v", "verbose", false, "enables logging more information");

        options.getOption("i").setRequired(true);
        options.getOption("r").setValueSeparator('=');
        options.getOption("g").setValueSeparator('=');
        options.getOption("b").setValueSeparator('=');
        options.getOption("a").setValueSeparator('=');

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            verbose = cmd.hasOption("v");

            if (cmd.hasOption("gm") && cmd.hasOption("ln")) {
                print("Both gamma and linear color spaces are specified in arguments!");
                return;
            }

            if (cmd.hasOption("i")) {
                String input = cmd.getOptionValue("i");
                String output = cmd.hasOption("o") ? cmd.getOptionValue("o") : createOutputName(input);
                boolean gamma = cmd.hasOption("gm");

                if (cmd.hasOption("fast")) {
                    CompressionUtil.setFastCompressor(true);
                }

                Image original = awtLoad(input, gamma);
                boolean hasAlpha = original.getLayer().getChannelCount() == 4;

                if (cmd.hasOption("r") || cmd.hasOption("g") || cmd.hasOption("b") || cmd.hasOption("a")) {
                    print("Remapping channels...");

                    print(" Red -> " + color(cmd.getOptionValue("r", "r")));
                    print(" Green -> " + color(cmd.getOptionValue("g", "g")));
                    print(" Blue -> " + color(cmd.getOptionValue("b", "b")));
                    if (hasAlpha)
                        print(" Alpha -> " + color(cmd.getOptionValue("a", "a")));

                    int discarded = 0;

                    if (cmd.getOptionValue("r", "r").equalsIgnoreCase("x"))
                        discarded++;
                    if (cmd.getOptionValue("g", "g").equalsIgnoreCase("x"))
                        discarded++;
                    if (cmd.getOptionValue("b", "b").equalsIgnoreCase("x"))
                        discarded++;
                    if (hasAlpha)
                        if (cmd.getOptionValue("a", "a").equalsIgnoreCase("x"))
                            discarded++;

                    print("Copying channels...");
                    if (discarded == 4) {
                        print("All channels discarded! No data left! Exiting.");
                        return;
                    } else {
                        print(" Total " + discarded + " channel(s) discarded");
                    }

                    Image mapped = new Image(original.getWidth(), original.getHeight(),
                            new Format(original.getLayer().getChannelCount() - discarded, 8,
                                    gamma ? ColorSpace.GAMMA : ColorSpace.LINEAR));

                    print(" Copying Red channel...");
                    ChannelUtils.red(original).copyTo(mapped.getLayer().getChannel(channel(cmd.getOptionValue("r", "r"))));

                    if (!cmd.getOptionValue("g", "g").equalsIgnoreCase("x")) { // if not discarded
                        print(" Copying Green channel...");
                        ChannelUtils.green(original).copyTo(mapped.getLayer().getChannel(channel(cmd.getOptionValue("g", "g"))));
                    }

                    if (!cmd.getOptionValue("b", "b").equalsIgnoreCase("x")) { // if not discarded
                        print(" Copying Blue channel...");
                        ChannelUtils.blue(original).copyTo(mapped.getLayer().getChannel(channel(cmd.getOptionValue("b", "b"))));
                    }

                    if (hasAlpha)
                        if (!cmd.getOptionValue("a", "a").equalsIgnoreCase("x")) { // if not discarded
                            print(" Copying Alpha channel...");
                            ChannelUtils.alpha(original).copyTo(mapped.getLayer().getChannel(channel(cmd.getOptionValue("a", "a"))));
                        }

                    print("Saving BIF file...");
                    ImageFile.save(mapped, new BFOutputStream(new FileOutputStream(Paths.get(output).toAbsolutePath().toFile())));
                } else {
                    print("Saving BIF file...");
                    ImageFile.save(original, new BFOutputStream(new FileOutputStream(Paths.get(output).toAbsolutePath().toFile())));
                }
                print("Done!");

            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("png2bif", options);
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("png2bif", options);
        }
    }

    private static int channel(String c) {
        c = c.toLowerCase();
        switch (c) {
            case "r":
                return 0;
            case "g":
                return 1;
            case "b":
                return 2;
            case "a":
                return 3;
            case "x":
                return -1;
            default:
                throw new RuntimeException();
        }
    }

    private static String color(String c) {
        c = c.toLowerCase();
        switch (c) {
            case "r":
                return "Red";
            case "g":
                return "Green";
            case "b":
                return "Blue";
            case "a":
                return "Alpha";
            case "x":
                return "Discard";
            default:
                return c;
        }
    }

    private static String createOutputName(String input) {
        return input.substring(0, input.lastIndexOf('.')) + ".bif";
    }

    private static boolean verbose = false;

    private void verbose(String line) {
        if (verbose)
            System.out.println(line);
    }

    private static void print(String line) {
        System.out.println(line);
    }

    private static Image awtLoad(String in, boolean gamma) throws IOException {
        print("Loading input file...");
        BufferedImage img = ImageIO.read(Paths.get(in).toAbsolutePath().toFile());
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());

        print(" Width: " + img.getWidth());
        print(" Height: " + img.getHeight());

        boolean hasAlpha = img.getColorModel().hasAlpha();

        print(" Has alpha: " + hasAlpha);
        print(" Pixels " + img.getHeight() * img.getWidth());
        print(" Bytes: " + (hasAlpha ?
                (4 * img.getHeight() * img.getWidth()) : (3 * img.getWidth() * img.getHeight())));

        print("Creating Image...");
        Image image = new Image(img.getWidth(), img.getHeight(),
                new Format(hasAlpha ? 4 : 3, 8, gamma ? ColorSpace.GAMMA : ColorSpace.LINEAR));
        Layer l = image.getLayer();

        print(" Created raster size: " + l.getRaster().length);
        print(" Created layer pixels: " + l.getPixels());
        print(" Created layer bytes: " + l.getLength());
        print(" Created layer format: " + l.getFormat());
        print(" Created layer channels: " + l.getChannelCount());

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
}

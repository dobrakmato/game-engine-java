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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.bf.tools.AbstractTool;
import eu.matejkormuth.lpsim.Image;
import eu.matejkormuth.lpsim.Layer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class BifInfo extends AbstractTool {

    public static void main(String[] args) throws IOException {
        new eu.matejkormuth.bf.tools.BifInfo().start(args);
    }

    private static void bifInfo(String file, boolean json) throws IOException {
        File f = Paths.get(file).toAbsolutePath().toFile();
        print(json, "Reading...");
        Image image = ImageFile.loadToImage(new BFInputStream(new FileInputStream(f)));

        print(json, "File: " + f.toString());
        print(json, " Width: " + image.getWidth());
        print(json, " Height: " + image.getHeight());
        print(json, " Layers: " + image.getLayers().size());
        for (Layer layer : image.getLayers()) {
            print(json, "  - Layer");
            print(json, "     Name: " + layer.getName());
            print(json, "     Format: " + layer.getFormat());
            print(json, "     Channels: " + layer.getChannelCount());
            print(json, "     Pixels: " + layer.getPixels());
            print(json, "     Bytes: " + layer.getLength());

        }

        if (json) {
            JsonObject obj = new JsonObject();
            obj.add("file", f.toString());
            obj.add("width", image.getWidth());
            obj.add("height", image.getHeight());
            JsonArray layers = new JsonArray();
            for (Layer layer : image.getLayers()) {
                JsonObject l = new JsonObject();
                l.add("name", layer.getName());
                l.add("format", layer.getFormat().toString());
                l.add("channels", layer.getChannelCount());
                l.add("pixels", layer.getPixels());
                l.add("bytes", layer.getLength());
                layers.add(l);
            }
            obj.add("layers", layers);
            System.out.println(obj.toString(WriterConfig.PRETTY_PRINT));
        }
    }

    private static void print(boolean disabled, String line) {
        if (!disabled)
            System.out.println(line);
    }

    @Override
    public void setupOptions(Options options) {
        options.addOption("f", "file", true, "input file");
        options.addOption("json", false, "return json output instead");
    }

    @Override
    public void execute(CommandLine cmd) throws Exception {
        if (cmd.hasOption("file")) {
            bifInfo(cmd.getOptionValue("file"), cmd.hasOption("json"));
        } else {
            help();
        }
    }
}

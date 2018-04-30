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
import eu.matejkormuth.bf.geometry.GeometryFile;
import eu.matejkormuth.lpsim.Geometry;
import eu.matejkormuth.lpsim.content.OBJImporter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Obj2Bgf extends AbstractTool {

    public static void main(String[] args) {
        new eu.matejkormuth.bf.tools.Obj2Bgf().start(args);
    }

    @Override
    public void setupOptions(Options options) {
        options.addOption("i", "input", true, "input file to read");
        options.addOption("o", "output", true, "output file to write");

        options.addOption("n", "normals", false, "whether to compute normals");
        options.addOption("t", "tangents", false, "whether to compute tangents");
        options.addOption("b", "bitangents", false, "whether to compute bitangents");

        options.getOption("i").setRequired(true);
    }

    @Override
    public void execute(CommandLine cmd) throws IOException {
        if (cmd.hasOption('i')) {
            String output = cmd.hasOption("o") ? cmd.getOptionValue("o") : createOutputName(cmd.getOptionValue('i'));

            Geometry geometry = OBJImporter.loadInternal(cmd.getOptionValue('i'));

            print("Original geometry: ");
            print(" Has faces: " + (geometry.hasFaces() ? "yes" : "no"));
            print(" Has normals: " + (geometry.hasNormals() ? "yes" : "no"));
            print(" Has texcoords: " + (geometry.hasTexCoords() ? "yes" : "no"));
            print(" Has tangents: " + (geometry.hasTangents() ? "yes" : "no"));
            print(" Has bitangents: " + (geometry.hasBitangets() ? "yes" : "no"));

            print(" Vertices: " + geometry.getPositions().length);
            print(" Faces: " + geometry.getIndices().length / 3);

            if (cmd.hasOption('n')) {
                print("Computing normals...");
                geometry.computeNormals();
            }

            if (cmd.hasOption('t')) {
                print("Computing tangents...");
                geometry.computeTangents(cmd.hasOption('b'));
            }

            print("New geometry: ");
            print(" Has faces: " + (geometry.hasFaces() ? "yes" : "no"));
            print(" Has normals: " + (geometry.hasNormals() ? "yes" : "no"));
            print(" Has texcoords: " + (geometry.hasTexCoords() ? "yes" : "no"));
            print(" Has tangents: " + (geometry.hasTangents() ? "yes" : "no"));
            print(" Has bitangents: " + (geometry.hasBitangets() ? "yes" : "no"));

            print(" Vertices: " + geometry.getPositions().length);
            print(" Faces: " + geometry.getIndices().length / 3);

            print("Saving BGF...");
            GeometryFile.save(geometry, new BFOutputStream(new BufferedOutputStream(new FileOutputStream(new File(output)))));

        } else {
            help();
        }
    }

    private static String createOutputName(String input) {
        return input.substring(0, input.lastIndexOf('.')) + ".bgf";
    }
}

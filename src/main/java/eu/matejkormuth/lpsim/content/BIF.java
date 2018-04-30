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
package eu.matejkormuth.lpsim.content;

import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.Application;
import eu.matejkormuth.lpsim.gl.Texture2D;
import eu.matejkormuth.lpsim.gl.TextureCube;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Deprecated
public class BIF {
    public static Texture2D load(String file) {
        Application.P.texturesIO.start();
        log.info("Loading " + file + ".bif");
        return load(Content.getContent().openRead("textures", file + ".bif"),
                Content.getContent().resolve("textures", file + ".bif").toAbsolutePath().toString());
    }

    public static Texture2D loadRawPath(String file) {
        Application.P.texturesIO.start();
        log.info("Loading " + file + ".bif");
        return load(Content.getContent().openRead(file + ".bif"),
                Content.getContent().resolve(file + ".bif").toAbsolutePath().toString());
    }

    private static Texture2D load(InputStream is, String tag) {
        Texture2D tex = Texture2D.create();
        try {
            ImageFile.loadIntoTexture(new BFInputStream(is), tex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String label = tag.substring(tag.lastIndexOf('\\')+1);
        tex.setTag(tag, label);
        return tex;
    }

    public static TextureCube loadSkybox(String file) {
        log.info("Loading " + file + ".bif");
        return loadSkybox(Content.getContent().openRead("skyboxes", file + ".bif"));
    }

    private static TextureCube loadSkybox(InputStream is) {
        TextureCube tex = TextureCube.create();
        try {
            ImageFile.loadIntoTexture(new BFInputStream(is), tex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tex;
    }
}

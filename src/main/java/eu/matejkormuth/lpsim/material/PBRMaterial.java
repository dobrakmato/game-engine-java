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
package eu.matejkormuth.lpsim.material;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import eu.matejkormuth.bf.compression.BFInputStream;
import eu.matejkormuth.bf.image.ImageFile;
import eu.matejkormuth.lpsim.Material;
import eu.matejkormuth.lpsim.InterleavedVertexLayout;
import eu.matejkormuth.lpsim.content.Content;
import eu.matejkormuth.lpsim.gl.Program;
import eu.matejkormuth.lpsim.gl.Texture2D;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.Util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PBRMaterial extends Material {

    private Texture2D albedo; // (0) [RGB] ALBEDO
    private Texture2D roughness; // (1) [R] MICROSURFACE (gloss/smoothness)
    private Texture2D metallic; // (2) [R/BINARY] METALNESS (metal or not metal - binary / really?)
    private Texture2D normal; // (3) [RGB] NORMALS
    private Texture2D ambientOcclusion; // (4) [R] AMBIENT OCCLUSION (ao, occlusion)

    //private Texture2D displacement; // (5) [R] DISPLACEMENT (only for displacement pbr shader)
    private Texture2D height; // (6) [R] HEIGHT (used for POM)
    private Texture2D emissive; // [R]

    @Getter
    private boolean parallaxOcclusionMapping = false;
    @Getter
    private boolean emissiveEnabled = false;

    private static List<PBRMaterial> materials = new ArrayList<>();

    @Getter
    @Setter
    private boolean anisotropic;

    public static PBRMaterial random() {
        int i = (int) (Math.random() * materials.size());
        return materials.get(i);
    }

    public static PBRMaterial fromJSON(String first, String... more) {
        return fromJSON(Content.getContent().resolve(first, more));
    }

    public static PBRMaterial fromJSON(String folderName) {
        return fromJSON(Content.getContent().resolve(folderName, "material.json"));
    }

    public static PBRMaterial fromJSON(Path file) {
        Path folder = file.getParent();

        Texture2D albedo, roughness, metallic, normal, ambientOcclusion, height, emissive;

        String json;
        try {
            json = new String(Files.readAllBytes(file), Charset.forName("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("Can't read material file!", e);
        }
        JsonObject config = Json.parse(json).asObject();
        Util.checkGLError();
        // Albedo and diffuse are the same.
        if (config.getString("TYPE_ALBEDO", null) != null) { // Prefer ALBEDO over DIFFUSE!
            albedo = resolveTex(config.getString("TYPE_ALBEDO", null), folder, Texture2D.Util.ERROR);
        } else {
            albedo = resolveTex(config.getString("TYPE_DIFFUSE", null), folder, Texture2D.Util.ERROR);
        }

        Util.checkGLError();
        roughness = resolveTex(config.getString("TYPE_ROUGHNESS", null), folder, Texture2D.Util.ERROR);
        metallic = resolveTex(config.getString("TYPE_METAL", null), folder, Texture2D.Util.BLACK);
        normal = resolveTex(config.getString("TYPE_NORMAL", null), folder, Texture2D.Util.FLAT_NORMAL);
        ambientOcclusion = resolveTex(config.getString("TYPE_AMBIENT_OCCLUSION", null), folder, Texture2D.Util.WHITE);
        height = resolveTex(config.getString("TYPE_HEIGHT", null), folder, null);
        emissive = resolveTex(config.getString("TYPE_EMISSIVE", null), folder, null);

        Util.checkGLError();
        if (emissive != null) {
            return PBRMaterial.emissiveMaterial(albedo, roughness, metallic, normal, ambientOcclusion, emissive);
        }

        if (height == null) { //height == null || true
            return new PBRMaterial(albedo, roughness, metallic, normal, ambientOcclusion);
        } else {
            return PBRMaterial.pomMaterial(albedo, roughness, metallic, normal, ambientOcclusion, height);
        }
    }

    private static Texture2D resolveTex(String file, Path folder, Texture2D def) {
        final String BLACK_ONLY = "$BLACK$";
        final String WHITE_ONLY = "$WHITE$";
        final String FLAT_NORMAL = "$FLAT_NORMAL$";

        // Check special cases.
        if (file == null)
            return def;
        if (file.equals(BLACK_ONLY))
            return Texture2D.Util.BLACK;
        if (file.equals(WHITE_ONLY))
            return Texture2D.Util.WHITE;
        if (file.equals(FLAT_NORMAL))
            return Texture2D.Util.FLAT_NORMAL;

        // Fix file name.
        file = file.replace("png", "bif").replace("jpg", "bif")
                .replace("jpeg", "bif").replace("tga", "bif").replace("bmp", "bif");

        Path p = folder.resolve(Paths.get(file)).toAbsolutePath();
        String tag = p.toString();
        String label = tag.substring(tag.lastIndexOf('\\') + 1);

        // Check already loaded.
        if (!Texture2D.find(tag).isEmpty()) {
            return Texture2D.find(tag).get(0);
        }

        // Load it.
        Texture2D tex = Texture2D.create();
        try {
            log.info("Loading " + file + ".bif");
            ImageFile.loadIntoTexture(new BFInputStream(new BufferedInputStream(new FileInputStream(p.toFile()))), tex);
            tex.setTag(tag, label);
            return tex;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PBRMaterial(Texture2D albedo, Texture2D roughness,
                       Texture2D metallic, Texture2D normal, Texture2D ambientOcclusion) {
        this.albedo = albedo;
        this.roughness = roughness;
        this.metallic = metallic;
        this.normal = normal;
        this.ambientOcclusion = ambientOcclusion;

        vertexLayout = InterleavedVertexLayout.STANDARD;
        materials.add(this);
    }

    public PBRMaterial(Texture2D albedo, Texture2D roughness,
                       Texture2D metallic, Texture2D normal, Texture2D ambientOcclusion, Texture2D height, Texture2D emissive) {
        this.albedo = albedo;
        this.roughness = roughness;
        this.metallic = metallic;
        this.normal = normal;
        this.ambientOcclusion = ambientOcclusion;
        this.height = height;
        this.emissive = emissive;

        vertexLayout = InterleavedVertexLayout.STANDARD;
        materials.add(this);
    }

    public static PBRMaterial pomMaterial(Texture2D albedo, Texture2D roughness,
                                          Texture2D metallic, Texture2D normal, Texture2D ambientOcclusion, Texture2D height) {
        PBRMaterial material = new PBRMaterial(albedo, roughness, metallic, normal, ambientOcclusion, height, null);
        material.parallaxOcclusionMapping = true;
        return material;
    }

    public static PBRMaterial emissiveMaterial(Texture2D albedo, Texture2D roughness,
                                               Texture2D metallic, Texture2D normal, Texture2D ambientOcclusion, Texture2D emissive) {
        PBRMaterial material = new PBRMaterial(albedo, roughness, metallic, normal, ambientOcclusion, null, emissive);
        material.emissiveEnabled = true;
        return material;
    }


    @Override
    public void setUniforms(Program program) {
        // Sampler uniforms should be set at initialization of shader.

        program.setUniform("G_POM", parallaxOcclusionMapping);
        program.setUniform("G_EMISSIVE", emissiveEnabled);
        program.setUniform("G_OCCLUSION", ambientOcclusion != null);
        program.setUniform("G_ANISOTROPIC", anisotropic);

        if (albedo != null) {
            Texture2D.activeSampler(0);
            albedo.bind();
        }
        if (roughness != null) {
            Texture2D.activeSampler(1);
            roughness.bind();
        }
        if (metallic != null) {
            Texture2D.activeSampler(2);
            metallic.bind();
        }
        if (normal != null) {
            Texture2D.activeSampler(3);
            normal.bind();
        }
        if (ambientOcclusion != null) {
            Texture2D.activeSampler(4);
            ambientOcclusion.bind();
        }
        if (height != null) {
            Texture2D.activeSampler(5);
            height.bind();
        }
        if (emissive != null) {
            Texture2D.activeSampler(6);
            emissive.bind();
        }

    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException("not yet implemented!");
    }
}

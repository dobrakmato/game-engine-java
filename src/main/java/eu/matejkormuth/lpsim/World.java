/**
 * lpsim -
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * <p>
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
package eu.matejkormuth.lpsim;

import eu.matejkormuth.lpsim.components.Rotation;
import eu.matejkormuth.lpsim.content.BGF;
import eu.matejkormuth.lpsim.content.BIF;
import eu.matejkormuth.lpsim.content.OBJImporter;
import eu.matejkormuth.lpsim.gl.*;
import eu.matejkormuth.lpsim.material.PBRMaterial;
import eu.matejkormuth.lpsim.material.PhongTextureMaterial;
import eu.matejkormuth.lpsim.math.Matrix4f;
import eu.matejkormuth.lpsim.timing.GLProfiler;
import eu.matejkormuth.math.vectors.Vector3f;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static eu.matejkormuth.lpsim.Syntax.vec3;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

@Slf4j
public class World {

    @Getter
    private Camera camera;

    private DirectionalLight sun = new DirectionalLight();
    private List<WorldObject> objects = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();

    /**
     * Lifetime of the world.
     */
    @Getter
    private double lifetime = 0;

    public static final float RENDER_QUALITY = 1f;

    public static final int CANVAS_WIDTH = (int) (Display.getWidth() * RENDER_QUALITY);
    public static final int CANVAS_HEIGHT = (int) (Display.getHeight() * RENDER_QUALITY);

    // todo: this is slow.
    private TextureCube skyboxBlur;
    private TextureCube skybox;
    private ReflectionProbe globalProbe;

    int pointLights;
    int visibleObjects;

    private VSM sunDepth;

    public World() {
        //camera = new Camera(new Vector3f(-0, 128, 256), new Vector3f(-0, -0.3f, -0.9f), new Vector3f(0, 1, 0));
        camera = new Camera(new Vector3f(54.153694f, 4.8976555f, 137.51971f), new Vector3f(0.97030014f, -0.24082103f, -0.022866523f), new Vector3f(0, 1, 0));
        sun.setColor(new Vector3f(214 / 255f, 235 / 255f, 255 / 255f)); // sun -> new Vector3f(237 / 255f, 215 / 255f, 150 / 255f), moon -> new Vector3f(214 / 255f, 235 / 255f, 255 / 255f)
        sun.setDirection(new Vector3f(0, 1, 2).normalize());
        sun.setCastingShadows(true);
        sun.setIntensity(0.2f); // 0.2f moon

        sunDepth = sun.getVsm();

        camera.setPosition(new Vector3f(43.636654f, 30.58645f, -22.965435f));
        camera.setForward(new Vector3f(0.074211895f, -0.41556677f, 0.90653014f));
        camera.setExposure(0.1f);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(new Vector3f(255 / 255f, 0 / 255f, 0 / 255f));
        sun2.setDirection(new Vector3f(2, 2, -2).normalize());
        sun2.setIntensity(1.0f);
        sun2.setCastingShadows(true);

        spotLight.setCutoff(0.5f);
        spotLight.setAttenuation(new Attenuation(1.0f, 0.0007f, 0.000014f));
        spotLight.setColor(new Vector3f(214 / 255f, 235 / 255f, 255 / 255f));
        spotLight.setIntensity(8f);
        spotLight.setPosition(new Vector3f(-18.049149f, 18.517641f, 70.88632f));
        spotLight.setDirection(new Vector3f(0.25980803f, -0.13908601f, -0.95559144f).normalize());
        spotLight.setCastingShadows(true);

        SpotLight spotLight2 = new SpotLight();
        spotLight2.setCutoff(0.66f);
        spotLight2.setAttenuation(new Attenuation(1.0f, 0.022f, 0.0019f));
        spotLight2.setColor(new Vector3f(1, 1, 0.7f));
        spotLight2.setPosition(new Vector3f(-69.59351f, 9.149203f, -42.050236f));
        spotLight2.setDirection(new Vector3f(-0.014806181f, -0.33405316f, 0.942438f).normalize());
        //spotLight2.setCastingShadows(true);

        SpotLight spotLight3 = new SpotLight();
        spotLight3.setCutoff(0.66f);
        spotLight3.setAttenuation(new Attenuation(1.0f, 0.022f, 0.0019f));
        spotLight3.setColor(new Vector3f(1, 1, 0.7f));
        spotLight3.setPosition(new Vector3f(-58.14333f, 9.066237f, -42.278793f));
        spotLight3.setDirection(new Vector3f(-0.014806181f, -0.33405316f, 0.942438f).normalize());
        //spotLight3.setCastingShadows(true);

        SpotLight spotLight4 = new SpotLight();
        spotLight4.setCutoff(0.66f);
        spotLight4.setAttenuation(new Attenuation(1.0f, 0.007f, 0.00014f));
        spotLight4.setColor(new Vector3f(0, 0, 1));
        spotLight4.setPosition(new Vector3f(32.29608f, 25.178493f, 92.17323f));
        spotLight4.setDirection(new Vector3f(-0.45636812f, -0.17785656f, -0.87183446f).normalize());
        //spotLight4.setCastingShadows(true);

        testLight = new PointLight();
        testLight.setAttenuation(new Attenuation(1.0f, 0.0f, 0.017f));
        testLight.setColor(new Vector3f(0, 1, 0));
        testLight.setPosition(new Vector3f(95.68084f, 21.984394f, 181.12003f));

        int lightCount = Integer.valueOf(System.getProperty("lights", "0")); // 256
        int maxPlace = 256;

        Random random = new Random(13L);

        for (int i = 0; i < lightCount; i++) {
            PointLight pl = new PointLight();
            pl.setAttenuation(new Attenuation(1.0f, 0.14f, 0.07f));
            pl.setColor(new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat()).normalize());
            pl.setPosition(new Vector3f(r(random, -maxPlace, maxPlace), 5, r(random, -maxPlace, maxPlace)));
            lights.add(pl);
        }

        // lights.add(sun);
        lights.add(spotLight);
        lights.add(testLight);
        //lights.add(spotLight2);
        //lights.add(spotLight3);
        //lights.add(spotLight4);
        //lights.add(sun2);

        this.sky = new PreethamSky(new Mesh(BGF.load("shpere10_20"), InterleavedVertexLayout.POSITION_ONLY), camera, sun);
        this.globalProbe = new ReflectionProbe((PreethamSky) sky);
        this.globalProbe.setPosition(new Vector3f());
        this.initializeWorld();
    }

    private float r(Random random, float min, float max) {
        return (float) (min + random.nextDouble() * (max - min));
    }

    PointLight testLight = new PointLight();

    SpotLight spotLight = new SpotLight();

    private void initializeWorld() {
        //Terrain terrain = new Terrain();
        //terrain.setPosition(new Vector3f(100, 0, 0));
        //terrain.setScale(new Vector3f(2, 2, 2));
        //Water water = new Water();
        //water.setScale(new Vector3f(1, 0.25f, 1));
        //water.setPosition(new Vector3f(0, -64, 0));

        log.info("-----------------------");
        Application.P.load.start();

        Display.setTitle("Loading skyboxes...");
        Application.P.skyboxes.start();
        skyboxBlur = BIF.loadSkybox("bluecloud_blur"); // AWTDecoder.loadSkybox("bluecloud_blur", "jpg");
        skybox = BIF.loadSkybox("bluecloud_blur"); //AWTDecoder.loadSkybox("bluecloud", "jpg");
        Application.P.skyboxes.end();

        Display.setTitle("Loading materials...");
        long start = System.nanoTime();

        Application.P.textures.start();

        PBRMaterial caveFloor1 = PBRMaterial.fromJSON("Cavefloor1 B");
        PBRMaterial carvedlimestoneground1 = PBRMaterial.fromJSON("Carvedlimestoneground1 B");
        PBRMaterial scuffedPlastic = PBRMaterial.fromJSON("Scuffed Plastic 1");
        PBRMaterial bambooWoodSemigloss = PBRMaterial.fromJSON("Bamboo Wood Semigloss 1");
        PBRMaterial goldScuffed = PBRMaterial.fromJSON("Gold Scuffed B");
        PBRMaterial copperRock = PBRMaterial.fromJSON("Copper Rock1 N");
        PBRMaterial concrete = PBRMaterial.fromJSON("Concrete Shimizu");
        PBRMaterial ironRusted4 = PBRMaterial.fromJSON("Iron Rusted4");
        PBRMaterial mahogFloor = PBRMaterial.fromJSON("Mahogfloor B");
        PBRMaterial sculptedFloorBoards = PBRMaterial.fromJSON("Sculptedfloorboards2b B3");
        PBRMaterial mossyGround1 = PBRMaterial.fromJSON("Mossy Ground1 N");
        PBRMaterial blocksRough = PBRMaterial.fromJSON("Blocksrough B");
        PBRMaterial spacedTiles1 = PBRMaterial.fromJSON("Spaced Tiles1 N");
        PBRMaterial tiles1 = PBRMaterial.fromJSON("Tiles");
        PBRMaterial roughnessTest = PBRMaterial.fromJSON("Roughness Test");
        PBRMaterial soil = PBRMaterial.fromJSON("Soil Mud PjDto20 2K Surface Ms");
        PBRMaterial oldTexturedFabric = PBRMaterial.fromJSON("Old Textured Fabric N");
        PBRMaterial greasyMetal = PBRMaterial.fromJSON("Greasy Metal Pan1 A");
        PBRMaterial grass1 = PBRMaterial.fromJSON("Grass1 N");
        PBRMaterial oakFloor = PBRMaterial.fromJSON("Oakfloor Fb1 N");
        PBRMaterial oakFloor2 = PBRMaterial.fromJSON("Oakfloor2 B");
        PBRMaterial pockedConcrete1 = PBRMaterial.fromJSON("Pockedconcrete1 N");
        PBRMaterial waterwornstone1 = PBRMaterial.fromJSON("Waterwornstone1 C");
        PBRMaterial wornpaintedcement = PBRMaterial.fromJSON("Wornpaintedcement");
        PBRMaterial bluePlastic = PBRMaterial.fromJSON("Corkboard3b");
        PBRMaterial wallLightcyan = PBRMaterial.fromJSON("Wall Lightcyan");
        PBRMaterial wallTorquise = PBRMaterial.fromJSON("Wall Torquise");
        PBRMaterial oldTiles = PBRMaterial.fromJSON("Old Tiles");
        PBRMaterial aluminium = PBRMaterial.fromJSON("Aluminum Scuffed 4");
        PBRMaterial ice = PBRMaterial.fromJSON("Marble Speckled Unreal Engine");
        PBRMaterial flakingLimestone = PBRMaterial.fromJSON("Flaking Limestone1 Unreal Engine");

        PBRMaterial floorYellow = PBRMaterial.fromJSON("Floor Yellow", "floorYellow.json");
        PBRMaterial floorWhite = PBRMaterial.fromJSON("Floor Yellow", "floorWhite.json");
        PBRMaterial floorAqua = PBRMaterial.fromJSON("Floor Yellow", "floorAqua.json");
        PBRMaterial floorDarkblue = PBRMaterial.fromJSON("Floor Yellow", "floorDarkblue.json");

        PBRMaterial blackRubber = PBRMaterial.fromJSON("Synth Rubber Unreal Engine");
        PBRMaterial blueRubber = PBRMaterial.fromJSON("T Paint Black");

        PBRMaterial bloomTest = PBRMaterial.emissiveMaterial(
                BIF.loadRawPath("Bloom Test/albedo"),
                BIF.loadRawPath("Bloom Test/roughness"),
                Texture2D.Util.BLACK,
                BIF.loadRawPath("Bloom Test/normal"),
                Texture2D.Util.BLACK,
                BIF.loadRawPath("Bloom Test/emissive")
        );

        //PBRMaterial terrainMaterial = new PBRMaterial(
        //        BIF.loadRawPath("textures/terrain_color"),
        //        Texture2D.Util.WHITE,
        //        Texture2D.Util.BLACK,
        //        Texture2D.Util.FLAT_NORMAL,
        //        Texture2D.Util.BLACK
        //);

        PBRMaterial granitesmooth1 = PBRMaterial.fromJSON("Granitesmooth1 Unreal Engine");
        PBRMaterial granitesmooth2 = PBRMaterial.fromJSON("Granitesmooth1 Unreal Engine", "material2.json");
        PBRMaterial granitesmooth3 = PBRMaterial.fromJSON("Granitesmooth1 Unreal Engine", "material3.json");
        PBRMaterial granitesmooth4 = PBRMaterial.fromJSON("Granitesmooth1 Unreal Engine", "material4.json");

        PBRMaterial crateredRock = PBRMaterial.fromJSON("Cratered Rock Unreal Engine");
        PBRMaterial pineneedlesGround = PBRMaterial.fromJSON("Pineneedles Ground");
        PBRMaterial concreteTadao = PBRMaterial.fromJSON("Concrete Tadao");
        PBRMaterial paintPeeling = PBRMaterial.fromJSON("Paint Peeling");
        PBRMaterial bark1 = PBRMaterial.fromJSON("Bark1 Unreal Engine");
        PBRMaterial brickCinder = PBRMaterial.fromJSON("OldPlaster 01");
        PBRMaterial ines = PBRMaterial.fromJSON("Ines");
        PBRMaterial rock_sandstoneMaterial = PBRMaterial.fromJSON("textures/rock_sandstone");
        PBRMaterial logmat = PBRMaterial.fromJSON("textures/logs");
        PBRMaterial gunmat = PBRMaterial.fromJSON("textures/Cerberus_LP");
        PBRMaterial sofamat = PBRMaterial.fromJSON("textures/sofa");
        PBRMaterial Combat_00mat = PBRMaterial.fromJSON("textures/Combat_00");
        PBRMaterial frostmournemat = PBRMaterial.fromJSON("textures/frostmourne");
        PBRMaterial lampamat = PBRMaterial.fromJSON("textures/lampa");
        PBRMaterial wolfmat = PBRMaterial.fromJSON("textures/wolf");
        PBRMaterial bambusmat = PBRMaterial.fromJSON("textures/bambus");

        PBRMaterial blackRock = PBRMaterial.fromJSON("Blackrock");
        PBRMaterial dryDirt1 = PBRMaterial.fromJSON("Dry Dirt1");
        PBRMaterial dryDirt2 = PBRMaterial.fromJSON("Dry Dirt2");
        PBRMaterial rustedmetalmix = PBRMaterial.fromJSON("Rustedmetalmix");
        PBRMaterial stonewall = PBRMaterial.fromJSON("Stonewall");
        PBRMaterial wornredishrockface = PBRMaterial.fromJSON("Wornredishroughrockface C");
        PBRMaterial myBricks2b = PBRMaterial.fromJSON("Mybricks2 B");
        PBRMaterial myBricks4b = PBRMaterial.fromJSON("Mybricks4 B");


        // awt loads in 10 - 12 seconds, bif in 1.5 seconds
        long materialsEnd = System.nanoTime();
        Application.P.textures.end();


        Application.P.models.start();
        long geometryStart = System.nanoTime();
        Display.setTitle("Loading geometry...");
        Mesh floor = new Mesh(BGF.load("floor"), InterleavedVertexLayout.STANDARD); //OBJImporter.load("floor").computeTangents(true);
        Mesh cube = new Mesh(BGF.load("cube"), InterleavedVertexLayout.STANDARD); //OBJImporter.load("cube").computeTangents(true);
        Mesh sphere = new Mesh(BGF.load("sphere"), InterleavedVertexLayout.STANDARD); //OBJImporter.load("sphere").computeTangents(true);
        Mesh smoothSphere = new Mesh(BGF.load("smoothsphere"), InterleavedVertexLayout.STANDARD); //OBJImporter.load("smoothsphere").computeTangents(true);
        Mesh smoothCube = new Mesh(BGF.load("smoothcube"), InterleavedVertexLayout.STANDARD); //OBJImporter.load("smoothcube").computeTangents(true);
        Mesh barrel = new Mesh(BGF.load("barrel"), InterleavedVertexLayout.STANDARD);
        Mesh sofa = new Mesh(BGF.load("sofa_blender"), InterleavedVertexLayout.STANDARD);
        Mesh sofa2 = new Mesh(BGF.load("sofa2_blender"), InterleavedVertexLayout.STANDARD);
        Mesh frostmourne = new Mesh(BGF.load("frostmourne_blender"), InterleavedVertexLayout.STANDARD);
        Mesh Combat_00 = new Mesh(BGF.load("Combat_00"), InterleavedVertexLayout.STANDARD);
        Mesh lampa = new Mesh(BGF.load("lampa"), InterleavedVertexLayout.STANDARD);
        Mesh wolf = new Mesh(BGF.load("wolf-obj"), InterleavedVertexLayout.STANDARD);
        Mesh bambus = new Mesh(BGF.load("bambus2"), InterleavedVertexLayout.STANDARD);
        //Mesh terrain = new Mesh(BGF.load("terrain_blender"), VertexLayout.STANDARD);
        Mesh rock_sandstone = new Mesh(BGF.load("rock_sandstone"), InterleavedVertexLayout.STANDARD);
        Mesh logs = new Mesh(BGF.load("woods"), InterleavedVertexLayout.STANDARD);
        Mesh Cerberus_LP = new Mesh(BGF.load("Cerberus_LP"), InterleavedVertexLayout.STANDARD);
        long geometryEnd = System.nanoTime();
        Application.P.models.end();


        Display.setTitle("Loading...");
        //Geometry round = OBJImporter.load("triss_body.obj").computeNormals().computeTangents(true);
        addTestObject(caveFloor1);
        addTestObject(bambooWoodSemigloss);
        addTestObject(goldScuffed);
        addTestObject(copperRock);
        addTestObject(mahogFloor);
        addTestObject(scuffedPlastic);
        addTestObject(sculptedFloorBoards);
        addTestObject(mossyGround1);
        addTestObject(blocksRough);
        addTestObject(spacedTiles1);
        addTestObject(tiles1);
        addTestObject(roughnessTest);
        addTestObject(ironRusted4);
        addTestObject(carvedlimestoneground1);
        addTestObject(floorYellow);
        addTestObject(wallLightcyan);
        addTestObject(wallTorquise);
        addTestObject(oldTiles);
        addTestObject(soil);
        addTestObject(oldTexturedFabric);
        addTestObject(greasyMetal);
        addTestObject(grass1);
        addTestObject(oakFloor);
        addTestObject(oakFloor2);
        addTestObject(pockedConcrete1);
        addTestObject(waterwornstone1);
        addTestObject(wornpaintedcement);
        addTestObject(floorWhite);
        addTestObject(blueRubber);
        addTestObject(blackRubber);
        addTestObject(aluminium);
        addTestObject(floorAqua);
        addTestObject(floorDarkblue);
        addTestObject(flakingLimestone);
        addTestObject(ice);
        addTestObject(bluePlastic);
        addTestObject(granitesmooth1);
        addTestObject(granitesmooth2);
        addTestObject(granitesmooth3);
        addTestObject(granitesmooth4);
        addTestObject(crateredRock);
        addTestObject(pineneedlesGround);
        addTestObject(concreteTadao);
        addTestObject(paintPeeling);
        addTestObject(bark1);
        addTestObject(brickCinder);
        addTestObject(blackRock);
        addTestObject(dryDirt1);
        addTestObject(dryDirt2);
        addTestObject(rustedmetalmix);
        addTestObject(stonewall);
        addTestObject(wornredishrockface);
        addTestObject(myBricks2b);
        addTestObject(myBricks4b);

        testMaterial("Floor Walnut");
        testMaterial("Asphalt New");
        testMaterial("Bark 1");
        testMaterial("Bark 2");
        testMaterial("Bark 3");
        testMaterial("Cement 1");
        testMaterial("Cement 2");
        testMaterial("Cloth 1");
        //testMaterial("Fabric 1");
        //testMaterial("Fabric 2");
        //testMaterial("Fabric 3");
        //testMaterial("Fabric 4");
        //testMaterial("Fabric 5");
        //testMaterial("Fabric 6");
        //testMaterial("Jeans 1");
        //testMaterial("Jeans 2");
        //testMaterial("Jeans 3");
        //testMaterial("Leather 1");
        //testMaterial("Soil 1");
        //testMaterial("Stones 1");
        //testMaterial("Stones 2");
        //testMaterial("Tiles 1");
        //testMaterial("Tiles 2");
        //testMaterial("Tiles 3");
        //testMaterial("Wall 1");
        //testMaterial("Wall 2");
        //testMaterial("Wall 3");
        //testMaterial("Wall 4");
        //testMaterial("Wall 5");

        Model cube_mod44 = new Model(barrel, ines);
        cube_mod44.setPosition(new Vector3f(-50, 10, 98.678478f));
        cube_mod44.setScale(new Vector3f(4, 4, 4));
        cube_mod44.addComponent(new Rotation(new Vector3f(0, 1f, 0), 0.001f));

        Model bloom_test_mod = new Model(smoothCube, bloomTest);
        bloom_test_mod.setPosition(new Vector3f(-90, 10, 90));
        bloom_test_mod.setScale(new Vector3f(8, 8, 8));

        Model sphere_mod = new Model(smoothSphere, goldScuffed);
        sphere_mod.setPosition(new Vector3f(0, 50, 0));
        sphere_mod.setScale(new Vector3f(3, 3, 3));

        Model rocksandstone_mod = new Model(rock_sandstone, rock_sandstoneMaterial);
        rocksandstone_mod.setPosition(new Vector3f(-40, 0, -10));
        rocksandstone_mod.setScale(new Vector3f(.3f, .3f, .3f));

        Model logs_mod = new Model(logs, logmat);
        logs_mod.setPosition(new Vector3f(-60, 0, -10));

        Model gun_mod = new Model(Cerberus_LP, gunmat);
        gun_mod.setPosition(new Vector3f(-80, 25, 0));
        gun_mod.setScale(new Vector3f(5, 5, 5));

        Model sofa_mod = new Model(sofa, sofamat);
        sofa_mod.setPosition(new Vector3f(-100, 0, 0));
        sofa_mod.setScale(new Vector3f(5, 5, 5));

        Model sofa2_mod = new Model(sofa2, sofamat);
        sofa2_mod.setPosition(new Vector3f(-165, 0, 60));
        sofa2_mod.setScale(new Vector3f(5, 5, 5));

        Model frostmourne_mod = new Model(frostmourne, frostmournemat);
        frostmourne_mod.setPosition(new Vector3f(-180, 10, 0));
        frostmourne_mod.setScale(new Vector3f(1, 1, 1));

        Model Combat_00_mod = new Model(Combat_00, Combat_00mat);
        Combat_00_mod.setPosition(new Vector3f(-210, 20, 0));
        Combat_00_mod.setScale(new Vector3f(1, 1, 1));

        Model lampa_mod = new Model(lampa, lampamat);
        lampa_mod.setPosition(new Vector3f(-180, 0, 60));
        lampa_mod.setBackfaceCullingEnabled(false);
        lampa_mod.setScale(new Vector3f(1, 1, 1));

        PointLight lampaL = new PointLight();
        lampaL.setPosition(new Vector3f(-179.92f, 8.64f, 59.98f));
        lampaL.setAttenuation(new Attenuation(1, 0.014f, 0.007f));
        lampaL.setIntensity(1.0f);
        lampaL.setColor(new Vector3f(1, 215 / 255f, 215 / 255f));
        lights.add(lampaL);

        Model wall = new Model(floor, myBricks4b);
        wall.setScale(new Vector3f(1024, 1, 1024));
        wall.setPosition(new Vector3f(-1100, -950, 75f));
        wall.setRotation(new Vector3f((float) Math.toRadians(-90), 0, 0));

        Model wolf_mod = new Model(wolf, wolfmat);
        wolf_mod.setScale(new Vector3f(1, 1, 1));
        wolf_mod.setPosition(new Vector3f(-163, 0, 50));
        wolf_mod.setRotation(new Vector3f(0, (float) Math.toRadians(-120), 0));

        Model bambus_mod = new Model(bambus, bambusmat);
        bambus_mod.setScale(new Vector3f(3, 3, 3));
        bambus_mod.setPosition(new Vector3f(-145, 0, 65));
        //bambus_mod.setRotation(new Vector3f(0, (float) Math.toRadians(-120), 0));

        Model probecubetest = new Model(smoothSphere, PBRMaterial.fromJSON("Mirror"));
        probecubetest.setScale(new Vector3f(3, 3, 3));
        probecubetest.setPosition(new Vector3f(-20, 20, 20));

        Model sexShopSign = new Model(new Mesh(BGF.load("sex_shop"), InterleavedVertexLayout.STANDARD), PBRMaterial.fromJSON("textures/neon"));
        sexShopSign.setPosition(new Vector3f(-165, 22, 73));
        sexShopSign.setScale(new Vector3f(1, 1, 1));
        sexShopSign.setRotation(new Vector3f((float) Math.toRadians(90), 0, 0));

        Mesh fanStill = new Mesh(BGF.load("fan_still"), InterleavedVertexLayout.STANDARD);
        Mesh fanBlades = new Mesh(BGF.load("fan_blades"), InterleavedVertexLayout.STANDARD);

        floor_mod = new Model(floor, mahogFloor); //dryDirt1
        //floor_mod.setCastingShadows(false);
        floor_mod.setScale(new Vector3f(1024, 1, 1024));
        floor_mod.setPosition(new Vector3f(0, 0, 0));

        //Model terrain_mod = new Model(terrain, terrainMaterial);
        //terrain_mod.setPosition(new Vector3f(0, 0, 0));
        //terrain_mod.setScale(new Vector3f(1024, 1024, 1024));

        //terrain.setPosition(new Vector3f(-160, 0, -60));

        Vector3f fanPos = new Vector3f(-127.22668f, 31.1858f, 36.976162f);
        Vector3f fanDir = new Vector3f((float) Math.toRadians(-120), -30, 0);

        Model fanStill_mod = new Model(fanStill, scuffedPlastic);
        Model fanBlades_mod = new Model(fanBlades, scuffedPlastic);
        fanStill_mod.setPosition(fanPos);
        fanStill_mod.setScale(new Vector3f(2, 2, 2));
        fanStill_mod.setRotation(fanDir);

        fanBlades_mod.setScale(new Vector3f(2, 2, 2));
        fanBlades_mod.setPosition(fanPos);
        fanBlades_mod.setRotation(fanDir);

        objects.add(fanStill_mod);
        objects.add(fanBlades_mod);
        //objects.add(bus);
        //objects.add(terrain_mod);
        objects.add(sphere_mod);
        objects.add(floor_mod);
        objects.add(wall);
        objects.add(rocksandstone_mod);
        objects.add(probecubetest);
        objects.add(logs_mod);
        objects.add(gun_mod);
        objects.add(sofa_mod);
        objects.add(sofa2_mod);
        objects.add(frostmourne_mod);
        objects.add(Combat_00_mod);
        objects.add(lampa_mod);
        //objects.add(wolf_mod);
        objects.add(bambus_mod);
        //objects.add(cube_mod);
        objects.add(cube_mod44);
        objects.add(bloom_test_mod);
        // objects.add(sexShopSign);
        //objects.add(terrain);

        System.gc();

        log.error("-----------------------");
        log.error("Materials loaded in {} ms!", (materialsEnd - start) / 1_000_000f);
        log.error("Geometry loaded in {} ms!", (geometryEnd - geometryStart) / 1_000_000f);
        log.error("Total: {} ms!", (System.nanoTime() - start) / 1_000_000f);
        log.error("-----------------------");
        Application.P.load.end();

        System.out.println(Application.P.root.toString(true));
        log.error("-----------------------");

        //Runtime.getRuntime().exit(2);
    }


    public void testMaterial(String nameOrPath) {
        PBRMaterial mat = PBRMaterial.fromJSON(nameOrPath);
        addTestObject(mat);
    }

    // region Test Objects
    private Mesh smoothSphere = new Mesh(BGF.load("smoothsphere"), InterleavedVertexLayout.STANDARD);
    private Mesh smoothCube = new Mesh(BGF.load("smoothcube"), InterleavedVertexLayout.STANDARD);
    private Mesh skydome = new Mesh(BGF.load("skydome2"), InterleavedVertexLayout.STANDARD);

    private int col = 0;
    private int row = -1;

    public void addTestObject(Material mat) {
        final int SPACING_COL = 30;
        final int SPACING_ROW = 20;
        final int MAX_IN_ROW = 19;

        row++;
        if (row >= MAX_IN_ROW) {
            col++;
            row = 0;
        }
        Model mod = new Model(smoothCube, mat);
        mod.setPosition(new Vector3f(SPACING_ROW * row, 10, SPACING_COL * col));
        mod.setScale(new Vector3f(4, 4, 4));

        objects.add(mod);
    }
    // endregion


    private Model cube_mod;
    private Model floor_mod;
    private boolean renderShadowMap = false;
    private boolean draggingLight = false;
    private boolean sunMoving = false;
    //Terrain terrain = new Terrain();
    private long frames;

    public void update(float deltaTime) {
        P.update.start();
        lifetime += deltaTime;
        frames++;

        if (sunMoving) {
            sun.setDirection(new Vector3f((float) Math.sin(lifetime / 1000f), 0.5f, (float) Math.cos(lifetime / 1000f)).normalize());
        }
        //sun.setDirection(new Vector3f(0.1f + (float) Math.abs(Math.sin(lifetime / 1000f) * 3f), 1f, 0).normalize());

        if (sunMoving) {
            spotLight.setPosition(new Vector3f((float) (95.68084f + (Math.sin(lifetime / 1000f) * 100f)), 20, -30));
        }

        //spotLight.setCutoff((float) ((Math.sin(lifetime / 1000f) + 1) / 2));
        //cube_mod.setPosition(new Vector3f(cube_mod.getPosition().getX(), (float) (Math.sin(lifetime / 1000f) * 20 + 20), cube_mod.getPosition().getZ()));

        // benchmark start
        if (false) {
            camera.setPosition(new Vector3f(43.636654f, 30.58645f, -22.965435f));
            camera.setForward(new Vector3f(0.074211895f, -0.41556677f, 0.90653014f));
            if (frames > 600) {
                System.exit(0);
            }
        }
        // benchmark end

        //camera.setPosition(new Vector3f(0, 50 + (float) Math.abs(Math.sin(lifetime / 1000f) * 30f), 0f));
        camera.update(deltaTime);
        sun.recalculateLightSpaceMatrix();

        for (WorldObject object : objects) {
            object.update(deltaTime);
        }

        for (Light light : lights) {
            if (light instanceof PointLight) {
                ((PointLight) light).update(deltaTime);
            }
        }

        // Debug only
        if (Application.getInput().wasPressed(Keyboard.KEY_K)) {
            renderShadowMap = !renderShadowMap;
        }

        if (Application.getInput().wasPressed(Keyboard.KEY_I)) {
            floor_mod.setMaterial(PBRMaterial.random());
        }

        if (Application.getInput().wasPressed(Keyboard.KEY_F)) {
            draggingLight = !draggingLight;
        }

        if (Application.getInput().wasPressed(Keyboard.KEY_L)) {
            sunMoving = !sunMoving;
        }

        if (Application.getInput().wasPressed(Keyboard.KEY_I)) {
            drawSpecular = !drawSpecular;
        }

        if (Application.getInput().wasPressed(Keyboard.KEY_P)) {
            log.info("\n" + World.P.root.toString(true));
        }

        if (draggingLight) {
            spotLight.setPosition(camera.getPosition());
            spotLight.setDirection(camera.getForward());
        }
        P.update.end();
    }

    private Quad quad = new Quad();

    public void render2() {
        // https://www.cryengine.com/assets/images/showcase/fullsize/renderdoc1.png

        // 1. SHADOW MAPS
        // 2. DEPTH PRE-PASS
        // 3. SSAO
        // 4. DEFFERED
        // 5. OPAQUE
        // 6. TRANSPARENT - FOG
        // 7. TRANSPARENT - WATER
        // 8. TRANSPARENT...
        // 9. HDR
        // 10. POST PROCESS
    }

    public void render() {
        this.renderDeffered();
    }

    /**
     * ## [-- R8 --] [-- G8 --] [-- B8 --] [-- A8 --]
     * C0  Normal.x   Normal.y   Normal.z   Roughness
     * C1  Albedo.r   Albedo.g   Albedo.b   Metalness
     * C2  Emissive.r Emissive.g Emissive.b Shinyness
     * D  [------- DEPTH 24 BITS --------]  Stencil
     */
    private static IntBuffer DRAW_BUFFERS = (IntBuffer) BufferUtils.createIntBuffer(4).put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).put(GL30.GL_COLOR_ATTACHMENT2).flip();

    private FrameBuffer DEPTH_FBO;
    private Texture2D depthTexture;

    {
        DEPTH_FBO = new FrameBuffer();
        DEPTH_FBO.bind();
        DEPTH_FBO.setTag("Depth Copy FBO");

        depthTexture = new Texture2D();
        depthTexture.bind();
        depthTexture.setTag("Depth Copy Texture");
        depthTexture.setImageDataFloat(GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, CANVAS_WIDTH, CANVAS_HEIGHT);
        depthTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        depthTexture.setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);

        DEPTH_FBO.attach(FrameBufferTarget.FRAMEBUFFER, depthTexture, GL30.GL_DEPTH_ATTACHMENT);
        DEPTH_FBO.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);
    }

    class GBuffer extends FrameBuffer {
        private Texture2D[] colorAttachments = new Texture2D[3];
        private Texture2D depthAttachment;


        public GBuffer() {
            this.init();
        }

        private void init() {
            this.bind();

            int width = CANVAS_WIDTH;
            int height = CANVAS_HEIGHT;

            colorAttachments[0] = Texture2D.create();
            colorAttachments[0].bind();
            colorAttachments[0].setImageDataUnsignedByte(GL_RGBA8, GL_RGBA, width, height);
            colorAttachments[0].setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
            this.attach(FrameBufferTarget.FRAMEBUFFER, colorAttachments[0], GL30.GL_COLOR_ATTACHMENT0);
            colorAttachments[0].setTag("GBuffer #1");

            colorAttachments[1] = Texture2D.create();
            colorAttachments[1].bind();
            colorAttachments[1].setImageDataFloat(GL_RGB10_A2, GL_RGB, width, height);
            colorAttachments[1].setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
            colorAttachments[1].setTag("GBuffer Normal");
            this.attach(FrameBufferTarget.FRAMEBUFFER, colorAttachments[1], GL30.GL_COLOR_ATTACHMENT1);

            colorAttachments[2] = Texture2D.create();
            colorAttachments[2].bind();
            colorAttachments[2].setImageDataUnsignedByte(GL_RGB8, GL_RGB, width, height);
            colorAttachments[2].setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
            colorAttachments[2].setTag("GBuffer #3");
            this.attach(FrameBufferTarget.FRAMEBUFFER, colorAttachments[2], GL30.GL_COLOR_ATTACHMENT2);

            //colorAttachments[3] = Texture2D.create();
            //colorAttachments[3].bind();
            //colorAttachments[3].setTag("GBuffer Position");
            //colorAttachments[3].setImageDataFloat(GL_RGBA32F, GL_RGBA, width, height);
            //colorAttachments[3].setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
            //colorAttachments[3].setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
            //this.attach(FrameBufferTarget.FRAMEBUFFER, colorAttachments[3], GL30.GL_COLOR_ATTACHMENT3);

            // Create depth attachment.
            depthAttachment = Texture2D.create();
            depthAttachment.bind();
            depthAttachment.setImageDataUnsignedByte(GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL, width, height, GL_UNSIGNED_INT_24_8);
            depthAttachment.setTag("GBuffer Depth");
            depthAttachment.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
            depthAttachment.setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
            this.attach(FrameBufferTarget.FRAMEBUFFER, depthAttachment, GL30.GL_DEPTH_STENCIL_ATTACHMENT);

            glDrawBuffers(DRAW_BUFFERS);

            this.setTag("GBuffer");

            this.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);
        }
    }


    // Deffered rendering stuff.
    private GBuffer gBuffer = new GBuffer();
    private Program geometryPass = ShaderCollection.provideProgram("defferedGeometry");
    private Program grassPass = ShaderCollection.provideProgram("grassGeometry");
    private Program skyPass = ShaderCollection.provideProgram("defferedSkybox");
    private Program lightPass = ShaderCollection.provideProgram("defferedDirectional");
    private Program pointLightPass = ShaderCollection.provideProgram("defferedPointLight");
    private Program pointLightStencil = ShaderCollection.provideProgram("defferedPointStencil");
    private Program fxaa = ShaderCollection.provideProgram("fxaa");

    private Program spotLightPass = ShaderCollection.provideProgram("defferedSpotLight");
    private Program spotLightStencil = ShaderCollection.provideProgram("defferedPointStencil");

    private Program bloomLight = ShaderCollection.provideProgram("bloomLight");

    private Program vsmBlurFilter = ShaderCollection.provideProgram("gaussianBlur"); // Use ONLY to blur VSM shadow maps!
    private Program blur9x9 = ShaderCollection.provideProgram("blur9x9");

    private Program tonemap = ShaderCollection.provideProgram("tonemap");

    private Mesh quadMesh = new Mesh(Geometry.quad(), InterleavedVertexLayout.POSITION_TEXCOORD_ONLY);
    private Mesh sphereMesh = new Mesh(OBJImporter.load("int_uvsphere"), InterleavedVertexLayout.POSITION_ONLY); // for point lights
    private Mesh coneMesh = new Mesh(OBJImporter.load("int_cone"), InterleavedVertexLayout.POSITION_ONLY); // for spot lights

    // Skybox stuff
    //private Program skyboxPass = ShaderCollection.provideProgram("Skybox");
    private Mesh cubeMesh = new Mesh(OBJImporter.load("cube"), InterleavedVertexLayout.POSITION_ONLY);

    private boolean drawSpecular = true;

    Texture2D MAIN_FB_COLOR_ATTACHMENT;
    Texture2D MAIN_LDR_FB_COLOR_ATTACHMENT;

    private final FrameBuffer MAIN_FB = new FrameBuffer();
    private final FrameBuffer MAIN_LDR_FB = new FrameBuffer();

    private final Bloom bloom = new Bloom(6, quadMesh); // 3 blurs
    //private final Bloom2 bloom2 = new Bloom2(quadMesh);

    {
        MAIN_FB.bind();
        MAIN_FB.setTag("Main HDR FBO");

        MAIN_FB_COLOR_ATTACHMENT = Texture2D.create();
        MAIN_FB_COLOR_ATTACHMENT.bind();
        MAIN_FB_COLOR_ATTACHMENT.setTag("Main HDR ColorBuff");
        // todo: rgb32f to allow hrd
        MAIN_FB_COLOR_ATTACHMENT.setImageDataUnsignedByte(GL_RGB16F, GL_RGB, CANVAS_WIDTH, CANVAS_HEIGHT);
        MAIN_FB_COLOR_ATTACHMENT.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        MAIN_FB.attach(FrameBufferTarget.FRAMEBUFFER, MAIN_FB_COLOR_ATTACHMENT, GL30.GL_COLOR_ATTACHMENT0);

        //Texture2D depthAttachment = Texture2D.create();
        //depthAttachment.bind();
        //depthAttachment.setImageDataFloat(GL_DEPTH24_STENCIL8, GL_DEPTH_COMPONENT, Display.getWidth(), Display.getHeight());
        MAIN_FB.attach(FrameBufferTarget.FRAMEBUFFER, gBuffer.depthAttachment, GL30.GL_DEPTH_STENCIL_ATTACHMENT);
        MAIN_FB.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);

        MAIN_LDR_FB.bind();
        MAIN_LDR_FB.setTag("Main LDR FBO");

        MAIN_LDR_FB_COLOR_ATTACHMENT = Texture2D.create();
        MAIN_LDR_FB_COLOR_ATTACHMENT.bind();
        MAIN_LDR_FB_COLOR_ATTACHMENT.setTag("Main LDR ColorBuff");
        MAIN_LDR_FB_COLOR_ATTACHMENT.setImageDataUnsignedByte(GL_RGB16F, GL_RGB, CANVAS_WIDTH, CANVAS_HEIGHT);
        MAIN_LDR_FB_COLOR_ATTACHMENT.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);

        MAIN_LDR_FB.attach(FrameBufferTarget.FRAMEBUFFER, MAIN_LDR_FB_COLOR_ATTACHMENT, GL30.GL_COLOR_ATTACHMENT0);
        MAIN_LDR_FB.checkFramebuffer(FrameBufferTarget.FRAMEBUFFER);

        FrameBuffer.SCREEN.bind();
    }

    {
        geometryPass.use()
                .setUniform("map_albedo", 0)
                .setUniform("map_roughness", 1)
                .setUniform("map_metalic", 2)
                .setUniform("map_normal", 3)
                .setUniform("map_ambientOcclusion", 4)
                .setUniform("map_height", 5)
                .setUniform("map_emissive", 6);

        //skyboxPass.use()
        //.setUniform("skybox", 8);

        lightPass.use()
                .setUniform("c0", 0)
                .setUniform("c1", 1)
                .setUniform("c2", 2)
                .setUniform("shadowMap", 8)
                .setUniform("depth", 9);

        vsmBlurFilter.use().setUniform("filterTexture", 8);
        blur9x9.use().setUniform("filterTexture", 8);

        skyPass.use()
                .setUniform("c0", 0)
                .setUniform("c1", 1)
                .setUniform("c2", 2)
                //.setUniform("c3", 3)
                .setUniform("skybox", 8);

        //.setUniform("shadowMap", 8);

        pointLightPass.use()
                .setUniform("c0", 0)
                .setUniform("c1", 1)
                .setUniform("c2", 2)
                .setUniform("depth", 9);
        //.setUniform("shadowMap", 7);

        spotLightPass.use()
                .setUniform("c0", 0)
                .setUniform("c1", 1)
                .setUniform("c2", 2)
                .setUniform("shadowMap", 7)
                .setUniform("depth", 9);

        fxaa.use()
                .setUniform("tex", 0);

        tonemap.use()
                .setUniform("tex", 0);

        bloomLight.use()
                .setUniform("c2", 2)
                .setUniform("tex", 0);
    }

    static class P {
        static GLProfiler root = GLProfiler.createRoot();
        static GLProfiler update = root.createChild("Update");

        static GLProfiler render = root.createChild("Render");
        static GLProfiler shadowMaps = render.createChild("Shadow Maps");
        static GLProfiler vsm = shadowMaps.createChild("VSM");
        static GLProfiler vsmShadowMap = vsm.createChild("Shadow Map");
        static GLProfiler vsmBlur = vsm.createChild("Blur");
        static GLProfiler spotLightMaps = shadowMaps.createChild("Spot Light Maps");
        static GLProfiler reflectionProbes = render.createChild("Reflection Probes");


        static GLProfiler geometryPass = render.createChild("Geometry Pass");
        static GLProfiler geometryPassClear = geometryPass.createChild("Clear FBO");
        static GLProfiler geometryPassCull = geometryPass.createChild("Cull Objects");
        static GLProfiler geometryPassSort = geometryPass.createChild("Sort Objects");
        static GLProfiler geometryPassRender = geometryPass.createChild("Render Objects");
        static GLProfiler geometryPassRenderFoliage = geometryPassRender.createChild("Foliage");

        static GLProfiler geometryPassRenderStateChange = geometryPassRender.createChild("State Changes");
        static GLProfiler geometryPassRenderDraw = geometryPassRender.createChild("Draw");


        static GLProfiler decals = render.createChild("Decals");
        static GLProfiler copyDepth = render.createChild("Copy Depth");


        static GLProfiler lightPass = render.createChild("Light Pass");
        static GLProfiler lightCulling = lightPass.createChild("Cull Lights");
        static GLProfiler lightSort = lightPass.createChild("Sort Lights");

        static GLProfiler pointLights = lightPass.createChild("Point Lights");
        static GLProfiler pointStencilPass = pointLights.createChild("Stencil Pass");
        static GLProfiler pointStencilPassDraw = pointStencilPass.createChild("Draw");

        static GLProfiler pointAdditivePass = pointLights.createChild("Additive Pass");
        static GLProfiler pointAdditivePassDraw = pointAdditivePass.createChild("Draw");

        static GLProfiler spotLights = lightPass.createChild("Spot Lights");
        static GLProfiler spotStencilPass = spotLights.createChild("Stencil Pass");
        static GLProfiler spotStencilPassDraw = spotStencilPass.createChild("Draw");

        static GLProfiler spotAdditivePass = spotLights.createChild("Additive Pass");
        static GLProfiler spotAdditivePassDraw = spotAdditivePass.createChild("Draw");

        static GLProfiler directionalLights = lightPass.createChild("Directional Lights");
        static GLProfiler skyLight = lightPass.createChild("Sky Light");

        static GLProfiler skybox = render.createChild("Skybox");
        static GLProfiler finalPass = render.createChild("Final Pass");

        static GLProfiler postEffects = finalPass.createChild("Post Effects");
        static GLProfiler postEffectFxaa = postEffects.createChild("FXAA");
        static GLProfiler postEffectBloom = postEffects.createChild("Bloom");
        static GLProfiler postEffectToneMapping = postEffects.createChild("Tone Mapping");
        static GLProfiler postEffectToneDOF = postEffects.createChild("Depth Of Field");
        static GLProfiler postEffectToneColorCorrecction = postEffects.createChild("Color Correction");
        static GLProfiler postEffectMotionBlur = postEffects.createChild("Motion Blur");


        static GLProfiler debugGBuffer = render.createChild("Debug Gbuffer");
    }

    private Sky sky;

    private Mesh grassMesh = new Mesh(BGF.load("grass_model2"), InterleavedVertexLayout.STANDARD);
    private Mesh fernMesh = new Mesh(BGF.load("fern"), InterleavedVertexLayout.STANDARD);

    private Texture2D grassNormal = BIF.load("grass_blades_normal");
    private Texture2D grassAlbedo = BIF.load("grass_blades_alpha");

    private Texture2D fernNormal = BIF.load("randomweed9_normal");
    private Texture2D fernAlbedo = BIF.load("randomweed9");

    private void renderDeffered() {
        P.render.start();

        glDisable(GL_BLEND);

        // region Shadow map rendering
        P.shadowMaps.start();
        glEnable(GL11.GL_DEPTH_TEST);
        glDepthMask(true);
        //glDepthFunc(GL_LESS);
        glCullFace(GL_BACK);

        List<DirectionalLight> vsmCasters = new ArrayList<>();

        for (Light light : lights) {
            if (light.isCastingShadows()) {

                if (light instanceof DirectionalLight) {
                    vsmCasters.add((DirectionalLight) light);
                }
            }
        }

        // Directional Lights -> VSM
        // Directional Lights (global) -> ESM
        // Spot Lights -> VSM
        // Point Lights -> Cubemap VSM

        P.vsm.start();
        glEnable(GL_DEPTH_CLAMP);
        for (DirectionalLight light : vsmCasters) {

            P.vsmShadowMap.start();
            VSM vsm = light.getVsm();
            vsm.bindForWriting();
            vsm.getProgram().setUniform("lightSpaceMatrix", light.getLightSpaceMatrix());

            for (WorldObject object : objects) {
                // todo: sort objects from front to back
                // todo: cull objects that can't be shadow casters
                if (object.isCastingShadows()) {
                    vsm.getProgram().setUniform("model", object.getTransformMatrix());
                    object.render(camera);
                }
            }

            //renderGrass(light.getLightSpaceMatrix(), new Matrix4f().initIdentity());

            P.vsmShadowMap.end();

            P.vsmBlur.start();
            // Blur the shadow map.
            Texture2D.activeSampler(8);
            vsm.getShadowMap().bind(); // source texture
            vsm.bindTempForWriting();  // target

            // blur horizontally
            vsmBlurFilter.use().setUniform("blurScale", (float) (1.0 / vsm.getSize()) * 1f, 0);
            quadMesh.drawElements();

            // blur vertically
            vsm.getTempMap().bind(); // source texture
            vsm.bindForWriting();    // target
            vsmBlurFilter.use().setUniform("blurScale", 0, (float) (1.0 / vsm.getSize()) * 1f);
            quadMesh.drawElements();
            P.vsmBlur.end();
        }
        glDisable(GL_DEPTH_CLAMP);
        P.vsm.end();

        P.spotLightMaps.start();
        glCullFace(GL_FRONT);

        // filter lights which volumes might be visible
        List<Light> sLights = lights.stream().filter(light -> light instanceof SpotLight).collect(Collectors.toList());

        for (Light light : sLights) {
            // find objects inside the lighht volume
            // filter out small objects
            // order front to back
            ShadowMap shadowMap = light.getShadowMap();
            shadowMap.bindForWriting();
            shadowMap.getProgram().use().setUniform("lightSpaceMatrix", light.getLightSpaceMatrix());

            for (WorldObject object : objects) {
                if (object.isCastingShadows()) {
                    shadowMap.getProgram().setUniform("model", object.getTransformMatrix());
                    object.render(camera);
                }
            }

            //renderGrass(light.getLightSpaceMatrix(), new Matrix4f().initIdentity());
        }


        P.spotLightMaps.end();

        // render only visible and important! shadows
        // maybe batch all geometry, to render everything in ONE drawcall.

        //for (Light light : lights) {
        //    if (light.isCastingShadows()) {
        //        // Todo: frustum culling & occlusion culling.

        //        ShadowMap shadowMap = light.getShadowMap();
        //        shadowMap.bindForWriting();
        //        shadowMap.getProgram().use().setUniform("lightSpaceMatrix", light.getLightSpaceMatrix());

        //        for (WorldObject object : objects) {
        //            if (object.isCastingShadows()) {
        //                shadowMap.getProgram().setUniform("model", object.getTransformMatrix());
        //                object.render();
        //            }
        //        }
        //    }
        //}

        // Reset viewport size.
        Application.get().viewport(CANVAS_WIDTH, CANVAS_HEIGHT);
        GL11.glClearColor(0, 0, 0, 1);

        glCullFace(GL_BACK);
        glDisable(GL_STENCIL_TEST);
        P.shadowMaps.end();
        // endregion

        // region Reflection Probes
        P.reflectionProbes.start();
        globalProbe.capture();
        P.reflectionProbes.end();
        // endregion

        // region Geometry pass
        P.geometryPass.start();
        Matrix4f projection = camera.getProjectionMatrix();
        Matrix4f view = camera.getViewMatrix();

        gBuffer.bindForWriting();
        glDrawBuffers(DRAW_BUFFERS);

        // Only the geometry pass updates the depth buffer
        glDepthMask(true);
        P.geometryPassClear.start();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        P.geometryPassClear.end();

        //glEnable(GL_DEPTH_TEST); // already done in shadow map pass
        //glDisable(GL_BLEND); // already done in shadow map pass


        // Todo: Cull invsilible objects.
        P.geometryPassCull.start();
        List<WorldObject> visibleObjects = new ArrayList<>(objects);
        this.visibleObjects = visibleObjects.size();
        P.geometryPassCull.end();

        // Sort front to back.
        P.geometryPassSort.start();
        visibleObjects.sort((o1, o2) -> {
            float o1Dist = camera.getPosition().distanceSquared(o1.getPosition());
            float o2Dist = camera.getPosition().distanceSquared(o2.getPosition());
            return o2Dist < o1Dist ? 1 : -1;
        });
        P.geometryPassSort.end();

        P.geometryPassRender.start();
        geometryPass.use().setUniform("projection", projection).setUniform("view", view).setUniform("cameraPos", camera.getPosition());

        boolean bfcull = true;
        for (WorldObject object : visibleObjects) {
            P.geometryPassRenderStateChange.start();
            geometryPass.setUniform("model", object.getTransformMatrix());
            object.getMaterial().setUniforms(geometryPass);

            if (object.isBackfaceCullingEnabled() != bfcull) {
                bfcull = object.isBackfaceCullingEnabled();
                if (bfcull) glEnable(GL_CULL_FACE);
                else glDisable(GL_CULL_FACE);
            }

            P.geometryPassRenderStateChange.end();

            P.geometryPassRenderDraw.start();
            object.render(camera);
            P.geometryPassRenderDraw.end();
        }
        P.geometryPassRender.end();


        P.geometryPass.end();
        // endregion

        // region Grass Rendering
        P.geometryPassRenderFoliage.start();
        renderGrass();
        P.geometryPassRenderFoliage.end();
        // endregion

        glDepthMask(false); // disable writing to depth buffer

        // region Render decals
        P.decals.start();
        P.decals.end();
        // endregion

        // region Copy Depth
        //P.copyDepth.start();
        //gBuffer.bindForReading();
        //DEPTH_FBO.bindForWriting();
        //glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(), 0, 0, Display.getWidth(), Display.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        //P.copyDepth.end();
        // endregion

        glDrawBuffers(DRAW_BUFFERS);

        // region Light pass
        P.lightPass.start();

        glDisable(GL_DEPTH_TEST);
        gBuffer.bindForReading();
        //FrameBuffer.SCREEN.bindForWriting();
        MAIN_FB.bindForWriting();

        //Util.checkGLError();
        // Copy depth buffer from gbuffer.
        //glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(), 0, 0, Display.getWidth(), Display.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        //Util.checkGLError();


        skyPass.use().setUniform("view", view).setUniform("projection", projection);
        glClear(GL_COLOR_BUFFER_BIT);
        //glEnable(GL_BLEND);
        //glBlendEquation(GL_FUNC_ADD);
        //glBlendFunc(GL_ONE, GL_ONE);

        // Init GBuffer textures as samplers in shaders.
        Texture2D.activeSampler(0);
        gBuffer.colorAttachments[0].bind();
        Texture2D.activeSampler(1);
        gBuffer.colorAttachments[1].bind();
        Texture2D.activeSampler(2);
        gBuffer.colorAttachments[2].bind();
        Texture2D.activeSampler(9);
        gBuffer.depthAttachment.bind();
        //depthTexture.bind();


        P.lightCulling.start();
        // Lists of visible lights.
        List<DirectionalLight> directional = new ArrayList<>();
        List<PointLight> point = new ArrayList<>();
        List<SpotLight> spot = new ArrayList<>();

        // Todo: Light culling.
        Frustum cameraFrustum = camera.getFrustum();

        for (Light light : lights) {
            if (light instanceof DirectionalLight) {
                directional.add((DirectionalLight) light);
            } else if (light instanceof SpotLight) {
                spot.add((SpotLight) light);
                //todo: culling
            } else {
                PointLight pointLight = (PointLight) light;

                if (cameraFrustum.testSphere(pointLight.getPosition(), pointLight.getRange()) != Intersection.OUTSIDE) {
                    point.add(pointLight);
                }
            }
        }
        this.pointLights = point.size() + spot.size();
        P.lightCulling.end();

        // Light ordering.
        P.lightSort.start();
        point.sort((o1, o2) -> {
            float o1Dist = camera.getPosition().distanceSquared(o1.getPosition());
            float o2Dist = camera.getPosition().distanceSquared(o2.getPosition());
            return o2Dist < o1Dist ? 1 : -1;
        });
        spot.sort((o1, o2) -> {
            float o1Dist = camera.getPosition().distanceSquared(o1.getPosition());
            float o2Dist = camera.getPosition().distanceSquared(o2.getPosition());
            return o2Dist < o1Dist ? 1 : -1;
        });
        P.lightSort.end();

        P.pointLights.start();
        pointLightPass.use().setUniform("gScreenWidth", CANVAS_WIDTH).setUniform("gScreenHeight", CANVAS_HEIGHT);
        pointLightPass.setUniform("cameraPos", camera.getPosition());

        pointLightPass.setUniform("view", view).setUniform("projection", projection).setUniform("view", view).setUniform("projection", projection);
        pointLightStencil.use().setUniform("view", view).setUniform("projection", projection);

        glEnable(GL_STENCIL_TEST);

        for (PointLight light : point) {
            // region Stencil Pass
            P.pointStencilPass.start();
            gBuffer.bindForReading(); // bind for stencil pass
            MAIN_FB.bindForWriting();
            glDrawBuffer(GL_NONE);

            glEnable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            glClear(GL_STENCIL_BUFFER_BIT);

            glStencilFunc(GL_ALWAYS, 0, 0);

            GL20.glStencilOpSeparate(GL_BACK, GL_KEEP, GL_INCR_WRAP, GL_KEEP);
            GL20.glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_DECR_WRAP, GL_KEEP);

            pointLightStencil.use().setUniform("model", light.getTransformMatrix());
            P.pointStencilPassDraw.start();
            sphereMesh.drawElements();
            P.pointStencilPassDraw.end();

            P.pointStencilPass.end();
            // endregion

            // region Point Light Pass
            P.pointAdditivePass.start();
            gBuffer.bindForReading();
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            MAIN_FB.bindForWriting();

            pointLightPass.use().setUniform("model", light.getTransformMatrix());
            light.setUniforms(pointLightPass);

            glStencilFunc(GL_NOTEQUAL, 0, 0xFF);
            glDisable(GL_DEPTH_TEST);

            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_ONE, GL_ONE);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_FRONT);

            P.pointAdditivePassDraw.start();
            sphereMesh.drawElements();
            P.pointAdditivePassDraw.end();

            glDisable(GL_BLEND);
            P.pointAdditivePass.end();
            // endregion


        }
        P.pointLights.end();


        P.spotLights.start(); // start spotlights
        spotLightPass.use().setUniform("gScreenWidth", CANVAS_WIDTH).setUniform("gScreenHeight", CANVAS_HEIGHT);
        spotLightPass.setUniform("cameraPos", camera.getPosition());

        spotLightPass.setUniform("view", view).setUniform("projection", projection).setUniform("view", view).setUniform("projection", projection);
        spotLightStencil.use().setUniform("view", view).setUniform("projection", projection);

        for (SpotLight light : spot) {
            // region Stencil Pass
            P.spotStencilPass.start();
            gBuffer.bindForReading(); // bind for stencil pass
            MAIN_FB.bindForWriting();
            glDrawBuffer(GL_NONE);

            glEnable(GL_DEPTH_TEST);
            glDisable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            glClear(GL_STENCIL_BUFFER_BIT);

            glStencilFunc(GL_ALWAYS, 0, 0);

            GL20.glStencilOpSeparate(GL_BACK, GL_KEEP, GL_INCR_WRAP, GL_KEEP);
            GL20.glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_DECR_WRAP, GL_KEEP);

            spotLightStencil.use().setUniform("model", light.getTransformMatrix());
            P.spotStencilPassDraw.start();
            sphereMesh.drawElements();
            P.spotStencilPassDraw.end();

            P.spotStencilPass.end();
            // endregion

            // region Point Light Pass
            P.spotAdditivePass.start();
            gBuffer.bindForReading();
            glDrawBuffers(DRAW_BUFFERS);
            MAIN_FB.bindForWriting();

            spotLightPass.use().setUniform("model", light.getTransformMatrix()).setUniform("castingShadows", light.isCastingShadows());
            if (light.isCastingShadows()) {
                Texture2D.activeSampler(7);
                light.getShadowMap().getTexture().bind();
                spotLightPass.setUniform("lightSpaceMatrix", light.getLightSpaceMatrix());
            }

            light.setUniforms(spotLightPass);

            glStencilFunc(GL_NOTEQUAL, 0, 0xFF);
            glDisable(GL_DEPTH_TEST);

            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_ONE, GL_ONE);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_FRONT);

            P.spotAdditivePassDraw.start();
            sphereMesh.drawElements();
            P.spotAdditivePassDraw.end();

            glDisable(GL_BLEND);
            P.spotAdditivePass.end();
            // endregion
        }

        glDisable(GL_STENCIL_TEST);
        glDisable(GL_DEPTH_TEST);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        P.spotLights.end(); // end spotlights

        P.directionalLights.start();
        lightPass.use().setUniform("view", view).setUniform("projection", projection);
        lightPass.setUniform("cameraPos", camera.getPosition());

        for (DirectionalLight light : directional) {
            //lightPass.setUniform("specularMix", drawSpecular ? 1.0f : 0.0f);

            light.setUniforms(lightPass);

            Texture2D.activeSampler(8);
            light.getVsm().getShadowMap().bind();
            quadMesh.drawElements();
        }

        P.directionalLights.end(); // end directional lights


        P.skyLight.start();  // start sky lights (ambient)
        skyPass.use();
        skyPass.setUniform("cameraPos", camera.getPosition());
        Texture2D.activeSampler(8);
        globalProbe.getTexture().bind();
        //quadMesh.drawElements();
        P.skyLight.end();  // end sky light (ambient)


        P.lightPass.end();
        // endregion

        // region Forward passes
        // endregion

        // region Skybox
        P.skybox.start();
        glCullFace(GL_FRONT);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        sky.render();

        glEnable(GL_CULL_FACE);
        glDepthFunc(GL_LESS);
        glCullFace(GL_BACK);
        P.skybox.end();
        // endregion

        P.finalPass.start();
        MAIN_FB.bindForReading();

        P.postEffects.start();

        // region Bloom
        P.postEffectBloom.start();

        //// Pass 1: Filter LIGHT parts of image to all bloom buffers.
        Texture2D.activeSampler(2);
        gBuffer.colorAttachments[2].bind();
        Texture2D.activeSampler(0);
        MAIN_FB_COLOR_ATTACHMENT.bind(); // hdr buffer
        bloom.passOneLightFilter(camera.getExposure());

        // Pass 2: Blur all fbos.
        Texture2D.activeSampler(8);
        bloom.passTwoBlurBuffers();

        // Pass 3: Combine blurred buffer.
        MAIN_FB.bindForWriting();
        bloom.passThreeCombine();

        P.postEffectBloom.end();
        // endregion

        // region Tone Mapping
        P.postEffectToneMapping.start();
        MAIN_LDR_FB.bindForWriting();
        Texture2D.activeSampler(0);
        MAIN_FB_COLOR_ATTACHMENT.bind();

        tonemap.use().setUniform("exposure", camera.getExposure());
        quadMesh.drawElements();

        P.postEffectToneMapping.end();
        // endregion

        // fxaa
        P.postEffectFxaa.start();

        glDisable(GL_DEPTH_TEST);

        Application.get().viewport(Display.getWidth(), Display.getHeight());
        FrameBuffer.SCREEN.bindForWriting();
        Texture2D.activeSampler(0);
        MAIN_LDR_FB_COLOR_ATTACHMENT.bind();
        fxaa.use().setUniform("gScreenWidth", (int) (CANVAS_WIDTH / RENDER_QUALITY)).setUniform("gScreenHeight", (int) (CANVAS_HEIGHT / RENDER_QUALITY));
        quadMesh.drawElements();

        glEnable(GL_DEPTH_TEST);
        P.postEffectFxaa.end();


        //glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(), 0, 0, Display.getWidth(), Display.getHeight(), GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT, GL_NEAREST);
        P.postEffects.stop();
        P.finalPass.end();

        P.debugGBuffer.start();
        this.drawGBufferComponents();
        P.debugGBuffer.end();


        glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        sunDepth.bindForReading();

        //GL30.glBlitFramebuffer(0, 0, sunDepth.getSize(), sunDepth.getSize(), 0, Display.getHeight(), 256, Display.getHeight() - 256, GL_COLOR_BUFFER_BIT, GL_LINEAR);

        //if (renderShadowMap) {
        //    glDisable(GL11.GL_DEPTH_TEST);
        //    quad.render(lights.acquire(0).getShadowMap().getTexture());
        //}
        P.render.end();
    }

    private final int GRASS_SIZE = 128;
    private final int FERN_SIZE = 16;
    private InstancedFoliage grassInstances = new InstancedFoliage(grassMesh, new InstancedFoliage.FoliageMaterial(
            grassAlbedo,
            grassNormal
    ), GRASS_SIZE * GRASS_SIZE);
    private InstancedFoliage fernInstances = new InstancedFoliage(fernMesh, new InstancedFoliage.FoliageMaterial(
            fernAlbedo,
            fernNormal
    ), FERN_SIZE * FERN_SIZE);

    private final float GRASS_COLOR_VARIATION = 0.3f;

    {
        // Generate grass patches.
        for (int i = 0; i < GRASS_SIZE; i++) {
            for (int j = 0; j < GRASS_SIZE; j++) {
                float scale = (float) (Math.random() + 2);

                Vector3f position = vec3(
                        (float) (2.4 * i + Math.random() * Math.sin(i * 0.5) * 2),
                        0.5f,
                        (float) (2.4 * j + Math.random() * Math.sin(j * 0.5 + 0.8314) * 2));

                Vector3f color = vec3(
                        (float) (1 - ((Math.random() + 1) * GRASS_COLOR_VARIATION)),
                        (float) (1 - ((Math.random() + 1) * GRASS_COLOR_VARIATION)),
                        (float) (1 - ((Math.random() + 1) * GRASS_COLOR_VARIATION))
                );

                Matrix4f mat = new Matrix4f().initTranslation(position.getX(), position.getY() + 1.4f * scale, position.getZ())
                        .multiply(new Matrix4f().initRotation((float) Math.toRadians(180), (float) (Math.random() * Math.PI), 0)
                                .multiply(new Matrix4f().initScale(scale, scale, scale)));

                grassInstances.addInstance(mat, color);
            }
        }

        // Generate fern patches.
        for (int i = 0; i < FERN_SIZE; i++) {
            for (int j = 0; j < FERN_SIZE; j++) {
                float scale = (float) (Math.random() + 4);

                Vector3f position = vec3(
                        (float) (16.8 * i + Math.random() * Math.sin(i * 0.5) * 16),
                        2f,
                        (float) (16.8 * j + Math.random() * Math.sin(j * 0.5 + 0.8314) * 16));

                Vector3f color = vec3(
                        (float) (1 - ((Math.random() + 1) * GRASS_COLOR_VARIATION)),
                        (float) (1 - ((Math.random() + 1) * GRASS_COLOR_VARIATION)),
                        (float) (1 - ((Math.random() + 1) * GRASS_COLOR_VARIATION))
                );

                Matrix4f mat = new Matrix4f().initTranslation(position.getX(), position.getY() + 1.4f * scale, position.getZ())
                        .multiply(new Matrix4f().initRotation((float) Math.toRadians(180), (float) (Math.random() * Math.PI), 0)
                                .multiply(new Matrix4f().initScale(scale, scale, scale)));

                fernInstances.addInstance(mat, color);
            }
        }
    }

    private void renderGrass() {
        //glDisable(GL_CULL_FACE);
        grassInstances.render(camera);
        fernInstances.render(camera);
        //glEnable(GL_CULL_FACE);
    }

    private void drawGBufferComponents() {
        gBuffer.bindForReading();

        int picWidth = Display.getWidth() / 6;
        int picHeight = Display.getHeight() / 6;

        glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(),
                0, 0, picWidth, picHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);

        bloom.drawDebugBuffers(picWidth * 4, picWidth * 5, picHeight);

        gBuffer.bindForReading();

        glReadBuffer(GL30.GL_COLOR_ATTACHMENT1);
        GL30.glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(),
                picWidth, 0, picWidth * 2, picHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);

        glReadBuffer(GL30.GL_COLOR_ATTACHMENT2);
        GL30.glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(),
                picWidth * 2, 0, picWidth * 3, picHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);

        glReadBuffer(GL30.GL_COLOR_ATTACHMENT3);
        GL30.glBlitFramebuffer(0, 0, Display.getWidth(), Display.getHeight(),
                picWidth * 3, 0, picWidth * 4, picHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
    }

    private void renderForward() {
        // FIRST PASS - SHADOW MAP RENDER
        glEnable(GL11.GL_DEPTH_TEST);
        glCullFace(GL_FRONT);

        //for (Light light : lights) {
        //    if (light.isCastingShadows()) {
        //        // Todo: frustum culling & occlusion culling.
//
        //        ShadowMap shadowMap = light.getShadowMap();
//
        //        shadowMap.bindForWriting();
        //        shadowMap.getProgram().use().setUniform("lightSpaceMatrix", light.getLightSpaceMatrix());
//
        //        for (WorldObject object : objects) {
        //            shadowMap.getProgram().setUniform("model", object.getTransformMatrix());
        //            object.render();
        //        }
        //    }
        //}

        glCullFace(GL_BACK);

        // SECOND PASS - WORLD RENDER
        Matrix4f projection = camera.getProjectionMatrix();
        Matrix4f view = camera.getViewMatrix();

        FrameBuffer.SCREEN.bind();
        //glViewport(0, 0, Display.getWidth(), Display.getHeight());
        Application.get().viewport(CANVAS_WIDTH, CANVAS_HEIGHT);
        glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);

        // Multipass forward rendering:
        // 1. Disable blending.
        // 2. Render objects with ambient (indirect) light.
        // 3. Enable blending.
        // 4. Render Directional/Spot/Point/Area lights **with object + light culling**. THIZ IZ SLOWWW

        // Todo: Cull invsilible objects.
        List<WorldObject> visibleObjects = new ArrayList<>(objects);
        // Sort front to back.
        visibleObjects.sort((o1, o2) -> {
            float o1Dist = camera.getPosition().distanceSquared(o1.getPosition());
            float o2Dist = camera.getPosition().distanceSquared(o2.getPosition());
            return o2Dist < o1Dist ? 1 : -1;
        });

        for (WorldObject object : visibleObjects) {
            Program program = object.getLightPrograms().getAmbientProgram();
            program.use()
                    .setUniform("projection", projection)
                    .setUniform("view", view)
                    .setUniform("model", object.getTransformMatrix());
            //.setUniform("cameraPos", camera.getPosition());

            if (object.getMaterial() != null) {
                //object.getMaterial().setUniforms(program);

                Texture2D.activeSampler(0);
                if (object.getMaterial() instanceof PhongTextureMaterial) {
                    ((PhongTextureMaterial) object.getMaterial()).getDiffuseTexture().bind();
                }

                // use only diffuse/albedo map
                program.setUniform("diffuseMap", 0);
            }

            object.render(camera);
        }


        glEnable(GL_BLEND);
        glDepthMask(false);
        glDepthFunc(GL_EQUAL);

        // Lists of visible lights.
        List<DirectionalLight> directional = new ArrayList<>();
        List<PointLight> point = new ArrayList<>();
        List<SpotLight> spot = new ArrayList<>();

        // Todo: Light culling.
        for (Light light : lights) {
            if (light instanceof DirectionalLight) {
                directional.add((DirectionalLight) light);
            } else if (light instanceof SpotLight) {
                spot.add((SpotLight) light);
            } else {
                point.add((PointLight) light);
            }
        }

        for (DirectionalLight light : directional) {
            Texture2D.activeSampler(2);
            light.getShadowMap().getTexture().bind();

            for (WorldObject object : visibleObjects) {
                // Setup rendering program.
                Program program = object.getLightPrograms().getDirectionalLightProgram();
                program.use()
                        .setUniform("projection", projection)
                        .setUniform("view", view)
                        .setUniform("model", object.getTransformMatrix())
                        .setUniform("cameraPos", camera.getPosition())
                        .setUniform("shadowMap", 2);

                light.setUniforms(object.getLightPrograms().getDirectionalLightProgram());

                if (object.getMaterial() != null) {
                    object.getMaterial().setUniforms(program);
                }

                object.render(camera);
            }
        }

        for (SpotLight light : spot) {
            if (light.isCastingShadows()) {
                Texture2D.activeSampler(2);
                light.getShadowMap().getTexture().bind();
            }

            for (WorldObject object : visibleObjects) {
                // Setup rendering program.
                Program program = object.getLightPrograms().getSpotLightProgram();
                program.use()
                        .setUniform("projection", projection)
                        .setUniform("view", view)
                        .setUniform("model", object.getTransformMatrix())
                        .setUniform("cameraPos", camera.getPosition())
                        .setUniform("shadowMap", 2);

                light.setUniforms(object.getLightPrograms().getSpotLightProgram());

                if (object.getMaterial() != null) {
                    object.getMaterial().setUniforms(program);
                }

                object.render(camera);
            }
        }

        for (PointLight light : point) {
            Texture2D.activeSampler(2);
            light.getShadowMap().getTexture().bind();

            for (WorldObject object : visibleObjects) {
                // Setup rendering program.
                Program program = object.getLightPrograms().getPointLightProgram();
                program.use()
                        .setUniform("projection", projection)
                        .setUniform("view", view)
                        .setUniform("model", object.getTransformMatrix())
                        .setUniform("cameraPos", camera.getPosition())
                        .setUniform("shadowMap", 2);

                light.setUniforms(program);

                if (object.getMaterial() != null) {
                    object.getMaterial().setUniforms(program);
                }

                object.render(camera);
            }
        }

        glDepthFunc(GL_LESS);
        glDepthMask(true);
        glDisable(GL_BLEND);


        if (renderShadowMap) {
            glDisable(GL11.GL_DEPTH_TEST);
            quad.render(lights.get(0).getShadowMap().getTexture());
        }

        // terrain render
        //Texture2D.activeSampler(0);
        //terrain.grassInstances.bind();
        //Texture2D.activeSampler(7);
        //terrain.cliff.bind();
        //Texture2D.activeSampler(5);
        //terrain.getHeightmapTexture().bind();
        ////Texture2D.activeSampler(6);
        ////terrain.getHeightmapNormal().bind();
        //terrain.getProgram().use()
        //        .setUniform("projection", projection)
        //        .setUniform("view", view)
        //        .setUniform("model", terrain.getTransformMatrix())
        //        .setUniform("sunDirection", sun.getDirection())
        //        .setUniform("sunColor", sun.getColor())
        //        .setUniform("diffuseMap", 0)
        //        .setUniform("cliffDiffuse", 7)
        //        .setUniform("displacementMap", 5)
        //        //.setUniform("normalMap", 6)
        //        .setUniform("dispFactor", 768f)
        //        .setUniform("cameraPos", camera.getPosition());
        //terrain.render();
    }
}

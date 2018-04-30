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
package eu.matejkormuth.lpsim;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class Config {

    private static final String FILE_NAME = "config.xml";

    private int width = 1920;
    private int height = 1080;
    private int texturesLod = 0;
    private boolean debug = true;
    private int msaa = 0;
    private boolean fxaa;
    private int lights = 128;
    private List<String> contentRoots = new ArrayList<>();
    private String name = "player";

    // keybinds

    private Config() {
        contentRoots.add(".");
        contentRoots.add("C:\\Users\\Matej\\IdeaProjects\\lpsim\\src\\main\\resources/");
    }

    private static Config instance;

    public static Config getInstance() {
        if (instance == null) {
            Path fileName = Paths.get(System.getProperty("config", FILE_NAME)).toAbsolutePath();
            if (!Files.exists(fileName)) {
                log.error("Config file {} does not exists! Creating default.", fileName);
                try {
                    Config.save(fileName);
                } catch (Exception e) {
                    log.error("Can't save config", e);
                }
            }
            try {
                log.error("Loading configuration...");
                instance = Config.load(fileName);
            } catch (Exception e) {
                log.error("Can't load config", e);
            }
        }
        return instance;
    }

    private static void save(Path fileName) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(Config.class);
        Marshaller mar = ctx.createMarshaller();
        mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        mar.marshal(new Config(), fileName.toFile());
    }

    private static Config load(Path fileName) throws IOException, JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(Config.class);
        return (Config) ctx.createUnmarshaller().unmarshal(fileName.toFile());
    }
}

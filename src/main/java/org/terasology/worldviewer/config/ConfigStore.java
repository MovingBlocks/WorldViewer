/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.worldviewer.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.TerasologyConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Loads/saves config instances in JSON files
 * @author Martin Steiger
 */
public final class ConfigStore {

    private static final Logger logger = LoggerFactory.getLogger(ConfigStore.class);

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting().create();

    private ConfigStore() {
        // no instances
    }

    /**
     * Loads a JSON format configuration file as a new Config
     * @param configFile the json file
     * @return The loaded configuration
     */
    public static Config load(Path configFile) {

        if (!configFile.toFile().exists()) {
            logger.info("Config file does not exist - creating new config");
            return new Config();
        }

        logger.info("Reading config file {}", configFile);

        try (Reader reader = Files.newBufferedReader(configFile, TerasologyConstants.CHARSET)) {
            Config config = GSON.fromJson(reader, Config.class);
            return config;
        }
        catch (JsonParseException | IOException e) {
            logger.error("Could not load config file", e);
            return new Config();
        }
    }

    public static void save(Path configFile, Config config) {
        logger.info("Writing config file to {}", configFile);

        try (BufferedWriter writer = Files.newBufferedWriter(configFile, TerasologyConstants.CHARSET)) {
            GSON.toJson(config, writer);
        }
        catch (JsonParseException | IOException e) {
            logger.error("Could not save config file", e);
        }
    }

}

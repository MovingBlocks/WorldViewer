/*
 * Copyright 2015 MovingBlocks
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
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.TerasologyConstants;
import org.terasology.utilities.gson.UriTypeAdapterFactory;
import org.terasology.worldviewer.layers.FacetLayer;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;


/**
 * The root class for all configs
 * @author Martin Steiger
 */
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new ClassTypeAdapter())
            .registerTypeAdapterFactory(new UriTypeAdapterFactory())
            .setPrettyPrinting().create();

    private ConfigData data;

    public Config() {
        data = new ConfigData();
    }

    public Config(ConfigData data) {
        this.data = data;
    }

    ConfigData getData() {
        return data;
    }

    public ViewConfig getViewConfig() {
        return data.viewConfig;
    }

    public WorldConfig getWorldConfig() {
        return data.worldConfig;
    }

    public void storeLayers(SimpleUri wgUri, List<FacetLayer> layers) {
        WorldGenConfigData wgConfig = data.worldGenConfigs.get(wgUri);
        if (wgConfig == null) {
            wgConfig = new WorldGenConfigData();
            data.worldGenConfigs.put(wgUri, wgConfig);
        } else {
            wgConfig.layers.clear();
        }

        for (FacetLayer layer : layers) {
            JsonElement jsonTree = GSON.toJsonTree(layer.getConfig());
            wgConfig.layers.add(new ConfigEntry(layer, jsonTree));
        }
    }

    public List<FacetLayer> loadLayers(SimpleUri wgUri, List<FacetLayer> defaultFacets) {
        List<FacetLayer> confLayers = Lists.newArrayList();
        List<FacetLayer> defLayers = Lists.newArrayList(defaultFacets);

        for (ConfigEntry entry : data.worldGenConfigs.get(wgUri).layers) {

            // if a "similar" entry exists somewhere in the default config
            // replace it with a configured one
            if (removeDefault(entry.facetClass, defLayers)) {
                FacetLayer layer;
                if (entry.configClass != null) {
                    FacetConfig conf = GSON.fromJson(entry.data, entry.configClass);
                    layer = createInstance(entry.facetClass, conf);
                } else {
                    layer = createInstance(entry.facetClass);
                }

                if (layer != null) {
                    confLayers.add(layer);
                }
            } else {
                logger.warn("Found entry that does not correspond to any default layer: {}", entry.facetClass);
            }
        }

        for (FacetLayer layer : defLayers) {
            logger.info("No stored config available for {} - using defaults", layer.getClass());
            confLayers.add(layer);
        }

        return confLayers;
    }

    private FacetLayer createInstance(Class<? extends FacetLayer> facetClass) {
        try {
            Constructor<? extends FacetLayer> c = facetClass.getConstructor();
            return c.newInstance();
        } catch (NoSuchMethodException e) {
            logger.warn("Class {} does not have a public default constructor", facetClass);
            return null;
        } catch (ReflectiveOperationException e) {
            logger.warn("Could not create an instance of {}", facetClass);
            return null;
        }
    }

    private FacetLayer createInstance(Class<? extends FacetLayer> facetClass, FacetConfig conf) {
        try {
            Constructor<? extends FacetLayer> c = facetClass.getConstructor(conf.getClass());
            return c.newInstance(conf);
        } catch (NoSuchMethodException e) {
            logger.warn("Class {} does not have a public constructor for {}", facetClass, conf);
            return null;
        } catch (ReflectiveOperationException e) {
            logger.warn("Could not create an instance of {} with {}", facetClass, conf);
            return null;
        }
    }

    private boolean removeDefault(Class<? extends FacetLayer> facetClass, List<FacetLayer> defLayers) {
        Iterator<FacetLayer> it = defLayers.iterator();

        while (it.hasNext()) {
            if (facetClass.isInstance(it.next())) {
                it.remove();
                return true;
            }
        }

        return false;
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
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            return new Config(data);
        }

        catch (JsonParseException | IOException e) {
            logger.error("Could not load config file", e);
            return new Config();
        }
    }

    public static void save(Path configFile, Config config) {
        logger.info("Writing config file to {}", configFile);

        try (BufferedWriter writer = Files.newBufferedWriter(configFile, TerasologyConstants.CHARSET)) {
            GSON.toJson(config.getData(), writer);
        }
        catch (JsonParseException | IOException e) {
            logger.error("Could not save config file", e);
        }
    }
}


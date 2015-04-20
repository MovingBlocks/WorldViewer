/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License"){ }
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

package org.terasology.worldviewer.env;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.AssetFactory;
import org.terasology.asset.AssetManager;
import org.terasology.asset.AssetManagerImpl;
import org.terasology.asset.AssetType;
import org.terasology.asset.AssetUri;
import org.terasology.config.Config;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.subsystem.headless.assets.HeadlessTexture;
import org.terasology.module.ClasspathModule;
import org.terasology.module.DependencyResolver;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModuleMetadataReader;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureData;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.generator.plugin.WorldGeneratorPlugin;
import org.terasology.world.generator.plugin.WorldGeneratorPluginLibrary;

import com.google.common.collect.Lists;

/**
 * Setup a tiny Terasology environment
 * @author Martin Steiger
 */
public final class TinyEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(TinyEnvironment.class);

    private TinyEnvironment() {
        // empty
    }

    /**
     * Default setup order
     * @throws IOException if the engine could not be loaded
     */
    public static void setup() throws IOException {

        setupConfig();

        setupModuleManager();

        setupAssetManager();

        setupBlockManager();

        setupWorldGen();
    }

    private static void setupModuleManager() throws IOException {
        TinyModuleManager modMan = new TinyModuleManager();
        CoreRegistry.put(ModuleManager.class, modMan);
        CoreRegistry.put(TinyModuleManager.class, modMan);
    }

    private static void setupConfig() {
        Config config = new Config();
        CoreRegistry.put(Config.class, config);
    }

    private static void setupAssetManager() {
        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);
        AssetManager assetManager = new AssetManagerImpl(moduleManager.getEnvironment());
        AssetType.registerAssetTypes(assetManager);
        assetManager.setAssetFactory(AssetType.TEXTURE, new AssetFactory<TextureData, Texture>() {
            @Override
            public Texture buildAsset(AssetUri uri, TextureData data) {
                return new HeadlessTexture(uri, data);
            }
        });

        CoreRegistry.put(AssetManager.class, assetManager);
    }

    public static void addModules(List<File> jars) {
        TinyModuleManager moduleManager = CoreRegistry.get(TinyModuleManager.class);
        ModuleEnvironment oldEnv = moduleManager.getEnvironment();

        List<Module> existingMods = oldEnv.getModulesOrderedByDependencies();

        Set<Module> mods = new HashSet<>(existingMods);
        for (File file : jars) {
            try {
                Module mod = moduleManager.load(file.toPath());
                mods.add(mod);
            } catch (IOException e) {
                logger.error("Failed to load a module from {}", file);
            }
        }

        // TODO: merge with #setupAssetManager()
        ModuleEnvironment newEnv = moduleManager.loadEnvironment(mods, true);
        AssetManager assetManager = CoreRegistry.get(AssetManager.class);
        assetManager.setEnvironment(newEnv);
    }

    private static void setupBlockManager() {
        BlockManager blockManager = Mockito.mock(BlockManager.class);
        Block air = BlockManager.getAir();
        Mockito.when(blockManager.getBlock(Matchers.<BlockUri>any())).thenReturn(air);
        Mockito.when(blockManager.getBlock(Matchers.<String>any())).thenReturn(air);

        CoreRegistry.put(BlockManager.class, blockManager);
    }

    private static void setupWorldGen() {
        CoreRegistry.putPermanently(WorldGeneratorPluginLibrary.class, new WorldGeneratorPluginLibrary() {

            @Override
            public <U extends WorldGeneratorPlugin> List<U> instantiateAllOfType(Class<U> ofType) {
                return Collections.emptyList();
            }
        });
    }
}

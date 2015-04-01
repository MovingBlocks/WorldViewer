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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModuleMetadataReader;
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

        setupAssetManager();

        setupBlockManager();

        setupWorldGen();
    }

    private static void setupConfig() {
        Config config = new Config();
        CoreRegistry.put(Config.class, config);
    }

    private static void setupAssetManager() throws IOException {
        Collection<Module> mods = loadModules();

        PermissionProviderFactory securityManager = Mockito.mock(PermissionProviderFactory.class);
        ModuleEnvironment env = new ModuleEnvironment(mods, securityManager, Collections.emptyList());

        AssetManager assetManager = new AssetManagerImpl(env);
        AssetType.registerAssetTypes(assetManager);
        assetManager.setAssetFactory(AssetType.TEXTURE, new AssetFactory<TextureData, Texture>() {
            @Override
            public Texture buildAsset(AssetUri uri, TextureData data) {
                return new HeadlessTexture(uri, data);
            }
        });

        CoreRegistry.put(AssetManager.class, assetManager);
    }

    private static Collection<Module> loadModules() throws IOException {
        try {
            Module engine = loadEngineModule();

            String[] cpEntries = getClassPath();

            Collection<Module> mods = Lists.newArrayList(engine);

            ModuleLoader moduleLoader = new ModuleLoader();
            moduleLoader.setModuleInfoPath(TerasologyConstants.MODULE_INFO_FILENAME);
            for (String path : cpEntries) {
                Module mod = moduleLoader.load(Paths.get(path));
                if (mod != null) {
                    logger.info("Loading module: {}", mod);
                    mods.add(mod);
                }
            }
            return mods;
        } catch (URISyntaxException e) {
            throw new IOException("Failed to load engine module", e);
        }
    }

    private static String[] getClassPath() throws IOException {
        // If the application is launched from the command line through java -jar
        // the classpath attribute is ignored and read from the jar's MANIFEST.MF file
        // instead. We classpath will then just contain WorldViewer.jar. We need to
        // manually parse the entries in that case :-(

        // Use the classloader for this class, not the default one to ensure that
        // only MANIFEST.MF from this jar is loaded (if it exists).
        ClassLoader classLoader = TinyEnvironment.class.getClassLoader();
        URL manifestResource = classLoader.getResource("/META-INF/MANIFEST.MF");
        if (manifestResource != null) {
            try (InputStream is = manifestResource.openStream()) {
                Manifest manifest = new Manifest(is);
                String classpath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
                return classpath.split(" ");
            }
        } else {
            String classpath = System.getProperty("java.class.path");
            return classpath.split(File.pathSeparator);
        }
    }

    private static Module loadEngineModule() throws IOException, URISyntaxException {
        ModuleMetadataReader metadataReader = new ModuleMetadataReader();
        try (Reader reader = new InputStreamReader(ModuleManager.class.getResourceAsStream("/engine-module.txt"), TerasologyConstants.CHARSET)) {
            ModuleMetadata metadata = metadataReader.read(reader);
            return ClasspathModule.create(metadata, ModuleManager.class, Module.class);
        }
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

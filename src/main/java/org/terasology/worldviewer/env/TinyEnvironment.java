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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.terasology.asset.AssetFactory;
import org.terasology.asset.AssetManager;
import org.terasology.asset.AssetResolver;
import org.terasology.asset.AssetType;
import org.terasology.asset.AssetUri;
import org.terasology.config.Config;
import org.terasology.core.world.CoreBiome;
import org.terasology.core.world.generator.worldGenerators.HeightMapWorldGenerator;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.subsystem.headless.assets.HeadlessTexture;
import org.terasology.module.ClasspathModule;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModuleMetadataReader;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.PermissionProviderFactory;
import org.terasology.naming.Name;
import org.terasology.naming.Version;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureData;
import org.terasology.rendering.nui.skin.UISkin;
import org.terasology.rendering.nui.skin.UISkinData;
import org.terasology.rendering.opengl.OpenGLTexture;
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

    private TinyEnvironment() {
        // empty
    }

    /**
     * Default setup order
     */
    public static void setup() {

        setupConfig();

        setupAssetManager();

        setupBlockManager();

        setupWorldGen();
    }

    private static void setupConfig() {
        Config config = new Config();
        CoreRegistry.put(Config.class, config);
    }

    private static void setupAssetManager() {
        ModuleMetadataReader metadataReader = new ModuleMetadataReader();
        try (Reader reader = new InputStreamReader(ModuleManager.class.getResourceAsStream("/engine-module.txt"), TerasologyConstants.CHARSET)) {
            ModuleMetadata metadata = metadataReader.read(reader);
            ClasspathModule engineModule = ClasspathModule.create(metadata, ModuleManager.class, Module.class);
            ModuleMetadata coreMeta = new ModuleMetadata();
            coreMeta.setId(new Name("Core"));
            coreMeta.setVersion(new Version(50, 1, 0));
            ClasspathModule coreModule = ClasspathModule.create(coreMeta, CoreBiome.class);
            Iterable<Module> mods = Lists.newArrayList(coreModule, engineModule);
            PermissionProviderFactory securityManager = Mockito.mock(PermissionProviderFactory.class);
            ModuleEnvironment env = new ModuleEnvironment(mods, securityManager, Collections.emptyList());

            AssetManager assetManager = new AssetManager(env);
            AssetType.registerAssetTypes(assetManager);
            assetManager.setAssetFactory(AssetType.TEXTURE, new AssetFactory<TextureData, Texture>() {
                @Override
                public Texture buildAsset(AssetUri uri, TextureData data) {
                    return new HeadlessTexture(uri, data);
                }
            });
            CoreRegistry.put(AssetManager.class, assetManager);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read engine metadata", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to convert engine library location to path", e);
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

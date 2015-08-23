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

package org.terasology.world.viewer.env;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.AssetFactory;
import org.terasology.assets.management.AssetManager;
import org.terasology.assets.module.ModuleAwareAssetTypeManager;
import org.terasology.audio.StaticSound;
import org.terasology.audio.StaticSoundData;
import org.terasology.audio.StreamingSound;
import org.terasology.audio.StreamingSoundData;
import org.terasology.audio.nullAudio.NullSound;
import org.terasology.audio.nullAudio.NullStreamingSound;
import org.terasology.config.Config;
import org.terasology.context.Context;
import org.terasology.context.internal.ContextImpl;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.subsystem.headless.assets.HeadlessTexture;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.internal.PojoEntityManager;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabData;
import org.terasology.entitySystem.prefab.internal.PojoPrefab;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.assets.texture.PNGTextureFormat;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureData;
import org.terasology.splash.SplashScreen;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AttachedToSurfaceFamilyFactory;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.DefaultBlockFamilyFactoryRegistry;
import org.terasology.world.block.family.HorizontalBlockFamilyFactory;
import org.terasology.world.block.family.SymmetricFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;
import org.terasology.world.block.loader.BlockFamilyDefinitionData;
import org.terasology.world.block.loader.BlockFamilyDefinitionFormat;
import org.terasology.world.block.shapes.BlockShape;
import org.terasology.world.block.shapes.BlockShapeData;
import org.terasology.world.block.shapes.BlockShapeImpl;
import org.terasology.world.block.sounds.BlockSounds;
import org.terasology.world.block.sounds.BlockSoundsData;
import org.terasology.world.generator.internal.WorldGeneratorManager;
import org.terasology.world.generator.plugin.WorldGeneratorPlugin;
import org.terasology.world.generator.plugin.WorldGeneratorPluginLibrary;

/**
 * Setup a tiny Terasology environment
 */
public final class TinyEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(TinyEnvironment.class);

    private TinyEnvironment() {
        // empty
    }

    /**
     * Default setup order
     * @param splashScreen the splash screen
     * @return the generated context that refers to all created systems
     * @throws IOException if the engine could not be loaded
     */
    public static Context createContext(SplashScreen splashScreen) throws IOException {

        Context context = new ContextImpl();
        CoreRegistry.setContext(context);

        splashScreen.post("Loading config ..");
        setupConfig();

        splashScreen.post("Loading module manager ..");
        setupModuleManager();

        splashScreen.post("Loading asset manager ..");
        setupAssetManager(context);

        splashScreen.post("Loading block manager ..");
        setupBlockManager();

        splashScreen.post("Loading world generators ..");
        setupWorldGen(context);

        splashScreen.post("Loading entity manager ..");
        // Entity Manager
        PojoEntityManager entityManager = new PojoEntityManager();
        CoreRegistry.put(EntityManager.class, entityManager);

        return context;
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

    private static void setupAssetManager(Context context) {
        ModuleAwareAssetTypeManager assetTypeManager = new ModuleAwareAssetTypeManager();

        assetTypeManager.registerCoreAssetType(Prefab.class,
                (AssetFactory<Prefab, PrefabData>) PojoPrefab::new, false, "prefabs");
        assetTypeManager.registerCoreAssetType(BlockShape.class,
                (AssetFactory<BlockShape, BlockShapeData>) BlockShapeImpl::new, "shapes");
        assetTypeManager.registerCoreAssetType(BlockSounds.class,
                (AssetFactory<BlockSounds, BlockSoundsData>) BlockSounds::new, "blockSounds");
        assetTypeManager.registerCoreAssetType(Texture.class,
                (AssetFactory<Texture, TextureData>) HeadlessTexture::new, "textures", "fonts");
        assetTypeManager.registerCoreAssetType(BlockFamilyDefinition.class,
                (AssetFactory<BlockFamilyDefinition, BlockFamilyDefinitionData>) BlockFamilyDefinition::new, "blocks");

        assetTypeManager.registerCoreAssetType(StaticSound.class,
                (AssetFactory<StaticSound, StaticSoundData>) NullSound::new, "sounds");
        assetTypeManager.registerCoreAssetType(StreamingSound.class,
                (AssetFactory<StreamingSound, StreamingSoundData>) NullStreamingSound::new, "music");

        DefaultBlockFamilyFactoryRegistry blockFamilyFactoryRegistry = new DefaultBlockFamilyFactoryRegistry();
        blockFamilyFactoryRegistry.setBlockFamilyFactory("horizontal", new HorizontalBlockFamilyFactory());
        blockFamilyFactoryRegistry.setBlockFamilyFactory("alignToSurface", new AttachedToSurfaceFamilyFactory());
        assetTypeManager.registerCoreFormat(BlockFamilyDefinition.class, new BlockFamilyDefinitionFormat(assetTypeManager.getAssetManager(), blockFamilyFactoryRegistry));

        assetTypeManager.registerCoreAssetType(Texture.class,
                (AssetFactory<Texture, TextureData>) HeadlessTexture::new, "textures", "fonts");
        assetTypeManager.registerCoreFormat(Texture.class, new PNGTextureFormat(Texture.FilterMode.NEAREST,
                path -> path.getName(2).toString().equals("textures")));
        assetTypeManager.registerCoreFormat(Texture.class, new PNGTextureFormat(Texture.FilterMode.LINEAR,
                path -> path.getName(2).toString().equals("fonts")));

        assetTypeManager.switchEnvironment(context.get(ModuleManager.class).getEnvironment());

        context.put(ModuleAwareAssetTypeManager.class, assetTypeManager);
        context.put(AssetManager.class, assetTypeManager.getAssetManager());
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
        ModuleAwareAssetTypeManager assetTypeManager = CoreRegistry.get(ModuleAwareAssetTypeManager.class);
        assetTypeManager.switchEnvironment(newEnv);

        CoreRegistry.get(WorldGeneratorManager.class).refresh();
    }

    private static void setupBlockManager() {
        BlockManager blockManager = Mockito.mock(BlockManager.class);
        Block air = new Block();
        air.setTranslucent(true);
        air.setTargetable(false);
        air.setPenetrable(true);
        air.setReplacementAllowed(true);
        air.setShadowCasting(false);
        air.setAttachmentAllowed(false);
        air.setHardness(0);
        air.setId((short) 0);
        air.setDisplayName("Air");
        air.setUri(BlockManager.AIR_ID);

        BlockFamily airFamily = new SymmetricFamily(BlockManager.AIR_ID, air);

        Mockito.when(blockManager.getBlock(Matchers.<BlockUri>any())).thenReturn(air);
        Mockito.when(blockManager.getBlock(Matchers.<String>any())).thenReturn(air);
        Mockito.when(blockManager.getBlockFamily(Matchers.<String>any())).thenReturn(airFamily);

        CoreRegistry.put(BlockManager.class, blockManager);
    }

    private static void setupWorldGen(Context context) {
        CoreRegistry.put(WorldGeneratorManager.class, new WorldGeneratorManager(context));
        CoreRegistry.put(WorldGeneratorPluginLibrary.class, new WorldGeneratorPluginLibrary() {

            @Override
            public <U extends WorldGeneratorPlugin> List<U> instantiateAllOfType(Class<U> ofType) {
                return Collections.emptyList();
            }
        });
    }
}

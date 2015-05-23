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
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.Asset;
import org.terasology.asset.AssetData;
import org.terasology.asset.AssetFactory;
import org.terasology.asset.AssetManager;
import org.terasology.asset.AssetManagerImpl;
import org.terasology.asset.AssetType;
import org.terasology.asset.AssetUri;
import org.terasology.audio.AudioManager;
import org.terasology.audio.nullAudio.NullAudioManager;
import org.terasology.config.Config;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.bootstrap.EntitySystemBuilder;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.module.ModuleManagerImpl;
import org.terasology.engine.paths.PathManager;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.internal.EngineEntityManager;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabData;
import org.terasology.entitySystem.prefab.internal.PojoPrefab;
import org.terasology.module.ModuleEnvironment;
import org.terasology.network.NetworkSystem;
import org.terasology.reflection.reflect.ReflectionReflectFactory;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.skin.UISkin;
import org.terasology.rendering.nui.skin.UISkinData;
import org.terasology.world.WorldComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.family.AttachedToSurfaceFamilyFactory;
import org.terasology.world.block.family.DefaultBlockFamilyFactoryRegistry;
import org.terasology.world.block.family.HorizontalBlockFamilyFactory;
import org.terasology.world.block.internal.BlockManagerImpl;
import org.terasology.world.block.loader.NullWorldAtlas;
import org.terasology.world.block.loader.WorldAtlas;
import org.terasology.world.block.shapes.BlockShape;
import org.terasology.world.block.shapes.BlockShapeData;
import org.terasology.world.block.shapes.BlockShapeImpl;
import org.terasology.world.generator.WorldConfigurator;
import org.terasology.world.generator.WorldGenerator;

/**
 * Setup an empty Terasology environment
 * @author Martin Steiger
 */
public final class FullEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(FullEnvironment.class);

    private FullEnvironment() {
        // empty
    }

    /**
     * Default setup order
     * @param worldGen the world generator to set up
     * @throws IOException if the home path cannot be set
     */
    public static void setup(WorldGenerator worldGen) throws IOException {

        PathManager.getInstance().useDefaultHomePath();

        setupConfig();

        setupModuleManager();

        setupAudio();

        setupAssetManager();

        setupBlockManager();

        setupEntitySystem();

        setupComponentManager();

        setupWorldGen(worldGen);
    }

    private static void setupEntitySystem() {
        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);
        NetworkSystem networkSystem = CoreRegistry.get(NetworkSystem.class);

        EntitySystemBuilder builder = new EntitySystemBuilder();

        ModuleEnvironment env = moduleManager.getEnvironment();
        EngineEntityManager engineEntityManager = builder.build(env, networkSystem, new ReflectionReflectFactory());
        CoreRegistry.put(EngineEntityManager.class, engineEntityManager);
    }

    private static void setupBlockManager() {
        DefaultBlockFamilyFactoryRegistry blockFamilyFactoryRegistry = new DefaultBlockFamilyFactoryRegistry();
        blockFamilyFactoryRegistry.setBlockFamilyFactory("horizontal", new HorizontalBlockFamilyFactory());
        blockFamilyFactoryRegistry.setBlockFamilyFactory("alignToSurface", new AttachedToSurfaceFamilyFactory());
        WorldAtlas worldAtlas = new NullWorldAtlas();
        BlockManagerImpl blockManager = new BlockManagerImpl(worldAtlas, blockFamilyFactoryRegistry);
        CoreRegistry.put(BlockManager.class, blockManager);
    }

    private static void setupEmptyAssetManager() {
        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);

        ModuleEnvironment env = moduleManager.getEnvironment();
        AssetManager assetManager = new AssetManagerImpl(env);

        // mock an empy asset factory for all asset types
        for (AssetType type : AssetType.values()) {
            assetManager.setAssetFactory(type, new AssetFactory<AssetData, Asset<AssetData>>() {

                @Override
                public Asset<AssetData> buildAsset(AssetUri uri, AssetData data) {
                    return null;
                }
            });
        }

        CoreRegistry.put(AssetManager.class, assetManager);
    }

    private static void setupAssetManager() {
        setupEmptyAssetManager();

        AssetManager assetManager = CoreRegistry.get(AssetManager.class);
        AudioManager audioManager = CoreRegistry.get(AudioManager.class);
        AssetType.registerAssetTypes(assetManager);

        assetManager.setAssetFactory(AssetType.PREFAB, new AssetFactory<PrefabData, Prefab>() {

            @Override
            public Prefab buildAsset(AssetUri uri, PrefabData data) {
                return new PojoPrefab(uri, data);
            }
        });
        assetManager.setAssetFactory(AssetType.SHAPE, new AssetFactory<BlockShapeData, BlockShape>() {

            @Override
            public BlockShape buildAsset(AssetUri uri, BlockShapeData data) {
                return new BlockShapeImpl(uri, data);
            }
        });

        assetManager.setAssetFactory(AssetType.UI_SKIN, new AssetFactory<UISkinData, UISkin>() {
            @Override
            public UISkin buildAsset(AssetUri uri, UISkinData data) {
                return new UISkin(uri, data);
            }
        });

        assetManager.setAssetFactory(AssetType.SOUND, audioManager.getStaticSoundFactory());
        assetManager.setAssetFactory(AssetType.MUSIC, audioManager.getStreamingSoundFactory());
    }

    private static void setupAudio() {
        NullAudioManager audioManager = new NullAudioManager();
        CoreRegistry.put(AudioManager.class, audioManager);
    }

    private static void setupConfig() {
        Config config;
        try {
            config = Config.load(Config.getConfigFile());
        } catch (IOException e) {
            logger.error("Failed to load config", e);
            config = new Config();
        }

        CoreRegistry.put(Config.class, config);
    }

    private static void setupModuleManager() {
        ModuleManager moduleManager = new ModuleManagerImpl();
        CoreRegistry.put(ModuleManager.class, moduleManager);
    }

    private static void setupComponentManager() {
        ComponentSystemManager componentSystemManager = new ComponentSystemManager();
        componentSystemManager.initialise();
        CoreRegistry.put(ComponentSystemManager.class, componentSystemManager);
    }

    private static void setupWorldGen(WorldGenerator worldGen) {
        EntityManager entityManager = CoreRegistry.get(EntityManager.class);
        EntityRef worldEntity = entityManager.create();
        worldEntity.addComponent(new WorldComponent());

        SimpleUri uri = new SimpleUri("cities:city");

        Config config = CoreRegistry.get(Config.class);

        WorldConfigurator configurator = worldGen.getConfigurator();
        for (Entry<String, Component> entry : configurator.getProperties().entrySet()) {
            Component comp = config.getModuleConfig(uri, entry.getKey(), entry.getValue().getClass());

            if (comp == null) {
                comp = entry.getValue();
            }

            worldEntity.addComponent(comp);
        }
    }

}


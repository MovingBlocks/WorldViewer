/*
 * Copyright 2015 MovingBlocks
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.module.ModuleExtension;
import org.terasology.engine.module.ModuleManager;
import org.terasology.engine.module.StandardModuleExtension;
import org.terasology.module.ClasspathModule;
import org.terasology.module.DependencyInfo;
import org.terasology.module.Module;
import org.terasology.module.ModuleEnvironment;
import org.terasology.module.ModuleLoader;
import org.terasology.module.ModuleMetadata;
import org.terasology.module.ModuleMetadataReader;
import org.terasology.module.ModuleRegistry;
import org.terasology.module.TableModuleRegistry;
import org.terasology.module.sandbox.BytecodeInjector;
import org.terasology.module.sandbox.PermissionProviderFactory;

import com.google.common.collect.Sets;

public class TinyModuleManager implements ModuleManager {

    private static final Logger logger = LoggerFactory.getLogger(TinyModuleManager.class);

    private final ModuleRegistry registry = new TableModuleRegistry();
    private final ModuleMetadataReader metadataReader = new ModuleMetadataReader();
    private final ModuleLoader moduleLoader = new ModuleLoader(metadataReader);
    private final PermissionProviderFactory securityManager = new DummyPermissionProviderFactory();

    private ModuleEnvironment environment;

    public TinyModuleManager() throws IOException {
        for (ModuleExtension ext : StandardModuleExtension.values()) {
            metadataReader.registerExtension(ext.getKey(), ext.getValueType());
        }

        moduleLoader.setModuleInfoPath(TerasologyConstants.MODULE_INFO_FILENAME);

        Module engineModule = loadEngineModule();
        registry.add(engineModule);

        loadModules();

        DependencyInfo engineDep = new DependencyInfo();
        engineDep.setId(engineModule.getId());
        engineDep.setMinVersion(engineModule.getVersion());
        engineDep.setMaxVersion(engineModule.getVersion().getNextPatchVersion());

        for (Module mod : registry) {
            if (mod != engineModule) {
                mod.getMetadata().getDependencies().add(engineDep);
            }
        }

        loadEnvironment(Sets.newHashSet(registry), true);
    }

    private Module loadEngineModule() {
        // TODO: define an explicit marker class and rename package for class Terasology (which is not in engine)
        Class<?> marker = org.terasology.game.Game.class;
        try (Reader reader = new InputStreamReader(marker.getResourceAsStream("/engine-module.txt"), TerasologyConstants.CHARSET)) {
            ModuleMetadata metadata = metadataReader.read(reader);
            return ClasspathModule.create(metadata, marker, Module.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read engine metadata", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to convert engine library location to path", e);
        }
    }

    @Override
    public ModuleEnvironment loadEnvironment(Set<Module> modules, boolean asPrimary) {
        List<BytecodeInjector> injectors = Collections.emptyList();
        ModuleEnvironment newEnvironment = new ModuleEnvironment(modules, securityManager, injectors);
        if (asPrimary) {
            if (environment != null) {
                environment.close();
            }
            environment = newEnvironment;
        }
        return newEnvironment;
    }

    @Override
    public ModuleRegistry getRegistry() {
        return registry;
    }

    @Override
    public ModuleMetadataReader getModuleMetadataReader() {
        return metadataReader;
    }

    @Override
    public ModuleEnvironment getEnvironment() {
        return environment;
    }

    private void loadModules() throws IOException {
        String[] cpEntries = getClassPath();

        for (String pathStr : cpEntries) {
            Path modulePath = Paths.get(pathStr);
            Path codeLoc = moduleLoader.getDirectoryCodeLocation();
            if (modulePath.endsWith(codeLoc)) {
                for (int i = 0; i < codeLoc.getNameCount(); i++) {
                    modulePath = modulePath.getParent();
                }
            }
            Module mod = moduleLoader.load(modulePath);
            if (mod != null) {
                logger.info("Loading module: {}", mod);
                registry.add(mod);
            }
        }
    }

    private static String[] getClassPath() throws IOException {
        // If the application is launched from the command line through java -jar
        // the classpath attribute is ignored and read from the jar's MANIFEST.MF file
        // instead. The classpath will then just contain WorldViewer.jar. We need to
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

    public Module load(Path path) throws IOException {
        Module module = moduleLoader.load(path);
        if (!registry.contains(module)) {
            logger.info("Module loaded: {}", module);
            registry.add(module);
        }

        return module;
    }
}


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

package org.terasology.worldviewer;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.Component;
import org.terasology.rendering.nui.properties.Range;
import org.terasology.world.generator.WorldConfigurator;
import org.terasology.world.generator.WorldGenerator;

import com.google.common.base.Optional;

public class ConfigFrame extends JFrame {

    private static final long serialVersionUID = -2350103799660220648L;

    private static final Logger logger = LoggerFactory.getLogger(ConfigFrame.class);

    public ConfigFrame(WorldGenerator worldGen) {
        Container panel = getContentPane();

        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Optional<WorldConfigurator> configOpt = worldGen.getConfigurator();
        if (configOpt.isPresent()) {
            WorldConfigurator configurator = configOpt.get();
            for (Entry<String, Component> entry : configurator.getProperties().entrySet()) {
                String label = entry.getKey();
                gbc.gridwidth = 2;
                panel.add(new JLabel(label, SwingConstants.LEADING), gbc.clone());
                gbc.gridwidth = 1;
                gbc.gridy++;
                Component ccomp = entry.getValue();
                processComponent(panel, ccomp);
                System.out.println(entry.getKey() + " -> " + ccomp);
            }
        }
    }

    private void processComponent(Container panel, Component ccomp) {
        for (Field field : ccomp.getClass().getDeclaredFields()) {
            Annotation[] anns = field.getAnnotations();
            // check only on annotated fields
            if (anns.length > 0) {
                try {
                    boolean acc = field.isAccessible();
                    if (!acc) {
                        field.setAccessible(true);
                    }
                    process(panel, ccomp, field);
                    if (!acc) {
                        field.setAccessible(false);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    logger.warn("Could not access field \"{}-{}\"", ccomp.getClass(), field.getName());
                }
            }
        }
    }

    private void process(Container parent, Object obj, Field field) throws IllegalAccessException {
        Range r = field.getAnnotation(Range.class);
        if (r != null) {
//            float value = field.getFloat(obj);
//            parent.add(new JLabel("range"));
//            UIBindings.createSpinner(min, stepSize, max, getter, setter);

        }
    }
}

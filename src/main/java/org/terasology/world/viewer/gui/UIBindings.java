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

package org.terasology.world.viewer.gui;

import java.lang.reflect.Field;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.rendering.nui.properties.Checkbox;
import org.terasology.rendering.nui.properties.OneOf.Enum;
import org.terasology.rendering.nui.properties.OneOf.List;
import org.terasology.rendering.nui.properties.Range;

/**
 * Provides a set of static methods that creates
 * Swing UI elements based on individual fields.
 */
public final class UIBindings {

    private static final Logger logger = LoggerFactory.getLogger(UIBindings.class);

    private UIBindings() {
        // no instances
    }

    public static JCheckBox processCheckboxAnnotation(Object config, Field field, String text) {
        Checkbox checkbox = field.getAnnotation(Checkbox.class);

        if (checkbox != null) {
            try {
                boolean initial = field.getBoolean(config);
                JCheckBox component = createCheckbox(text, initial);
                component.setName(checkbox.label().isEmpty() ? field.getName() : checkbox.label());
                component.setToolTipText(checkbox.description().isEmpty() ? null : checkbox.description());

                return component;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to read field {}", field);
            }
        }

        return null;
    }

    public static JCheckBox createCheckbox(String text, boolean initial) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setSelected(initial);

        return checkBox;
    }

    public static JSpinner processRangeAnnotation(Object config, Field field) {
        Range range = field.getAnnotation(Range.class);

        if (range != null) {
            double min = range.min();
            double max = range.max();
            double stepSize = range.increment();
            try {
                double initial = field.getDouble(config);
                JSpinner spinner = createSpinner(min, stepSize, max, initial);
                spinner.setName(range.label().isEmpty() ? field.getName() : range.label());
                spinner.setToolTipText(range.description().isEmpty() ? null : range.description());

                return spinner;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to read field {}", field);
            }
        }

        return null;
    }

    public static JSpinner createSpinner(double min, double stepSize, double max, double initial) {

        SpinnerNumberModel model = new SpinnerNumberModel(initial, min, max, stepSize);
        JSpinner spinner = new JSpinner(model);
        return spinner;
    }

    /**
     * Maps an @Enum field to a combobox
     * @param config the object instance that is bound
     * @param field the (potentially annotated field)
     * @return a combobox for the annotated field or <code>null</code> if not applicable
     */
    public static JComboBox<?> processEnumAnnotation(Object config, Field field) {
        Enum en = field.getAnnotation(Enum.class);
        Class<?> clazz = field.getType(); // the enum class

        if (en != null && clazz.isEnum()) {
            try {
                Object init = field.get(config);
                JComboBox<?> combo = createCombo(clazz.getEnumConstants(), init);
                combo.setName(en.label().isEmpty() ? field.getName() : en.label());
                combo.setToolTipText(en.description().isEmpty() ? null : en.description());
                return combo;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to read field {}", field);
            }
        }

        return null;
    }

    public static JComboBox<String> processListAnnotation(Object config, Field field) {
        List list = field.getAnnotation(List.class);

        if (list != null) {
            try {
                String init = field.get(config).toString();  // this should be a String already
                JComboBox<String> combo = createCombo(list.items(), init);
                combo.setName(list.label().isEmpty() ? field.getName() : list.label());
                combo.setToolTipText(list.description().isEmpty() ? null : list.description());
                return combo;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to read field {}", field);
            }
        }

        return null;
    }

    public static <T> JComboBox<T> createCombo(T[] elements, T initValue) {

        ComboBoxModel<T> model = new DefaultComboBoxModel<T>(elements);
        JComboBox<T> combo = new JComboBox<T>(model);
        combo.setSelectedItem(initValue);
        return combo;
    }
}

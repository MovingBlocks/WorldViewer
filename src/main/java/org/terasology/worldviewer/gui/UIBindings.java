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

package org.terasology.worldviewer.gui;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Consumer;

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
import org.terasology.worldviewer.lambda.Lambda;

/**
 * Provides a set of static methods that binds a {@link Consumer} to a
 * Swing UI element based on individual fields.
 * @author Martin Steiger
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
                Consumer<Boolean> setter = Lambda.toRuntime(v -> field.setBoolean(config, v.booleanValue()));
                boolean initial = field.getBoolean(config);
                JCheckBox component = createCheckbox(text, initial, setter);
                component.setName(checkbox.label().isEmpty() ? field.getName() : checkbox.label());
                component.setToolTipText(checkbox.description().isEmpty() ? null : checkbox.description());

                return component;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to map field {} of instance {}", field, config);
            }
        }

        return null;
    }

    public static JCheckBox createCheckbox(String text, boolean initial, Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setModel(new JCheckBox.ToggleButtonModel() {
            private static final long serialVersionUID = 1061456311481259247L;

            @Override
            public void setSelected(boolean b) {
                // make sure that the setter is called before every other listener
                // note that (item) state change listeners are notified in reverse order
                if (isSelected() != b) {
                    setter.accept(b);
                }
                super.setSelected(b);
            }
        });
        checkBox.setSelected(initial);

        return checkBox;
    }

    public static JSpinner processRangeAnnotation(Object config, Field field) {
        Range range = field.getAnnotation(Range.class);

        if (range != null) {
            double min = range.min();
            double max = range.max();
            double stepSize = range.increment();
            Consumer<Number> setter;
            if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                setter = Lambda.toRuntime(v -> field.setInt(config, v.intValue()));
            } else {
                setter = Lambda.toRuntime(v -> field.setFloat(config, v.floatValue()));
            }
            try {
                double initial = field.getDouble(config);
                JSpinner spinner = createSpinner(min, stepSize, max, initial, setter);
                spinner.setName(range.label().isEmpty() ? field.getName() : range.label());
                spinner.setToolTipText(range.description().isEmpty() ? null : range.description());

                return spinner;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to map field {} of instance {}", field, config);
            }
        }

        return null;
    }

    public static JSpinner createSpinner(double min, double stepSize, double max, double initial, Consumer<Number> setter) {

        final SpinnerNumberModel model = new SpinnerNumberModel(initial, min, max, stepSize) {

            private static final long serialVersionUID = -3432393757871165133L;

            @Override
            public void setValue(Object value) {
                // make sure that the setter is called before every other listener
                // note that change listeners are notified in reverse order
                if ((value == null) || !(value instanceof Number)) {
                    throw new IllegalArgumentException("illegal value");
                }
                if (!value.equals(getValue())) {
                    setter.accept((Number) value);
                }

                super.setValue(value);
            }
        };
        final JSpinner spinner = new JSpinner(model);
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
                Consumer<Object> setter = Lambda.toRuntime(v -> field.set(config, v));
                JComboBox<?> combo = createCombo(clazz.getEnumConstants(), init, setter);
                combo.setName(en.label().isEmpty() ? field.getName() : en.label());
                combo.setToolTipText(en.description().isEmpty() ? null : en.description());
                return combo;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to map field {} of instance {}", field, config);
            }
        }

        return null;
    }

    public static JComboBox<String> processListAnnotation(Object config, Field field) {
        List list = field.getAnnotation(List.class);

        if (list != null) {
            try {
                Consumer<String> setter = Lambda.toRuntime(v -> field.set(config, v));
                String init = field.get(config).toString();  // this should be a String already
                JComboBox<String> combo = createCombo(list.items(), init, setter);
                combo.setName(list.label().isEmpty() ? field.getName() : list.label());
                combo.setToolTipText(list.description().isEmpty() ? null : list.description());
                return combo;
            } catch (IllegalAccessException e) {
                logger.warn("Unable to map field {} of instance {}", field, config);
            }
        }

        return null;
    }

    public static <T> JComboBox<T> createCombo(T[] elements, T initValue, Consumer<T> setter) {

        ComboBoxModel<T> model = new DefaultComboBoxModel<T>(elements) {

            private static final long serialVersionUID = 1L;

            @Override
            public void setSelectedItem(Object anObject) {
                // make sure that the setter is called before every other listener
                // note that change listeners are notified in reverse order
                // also note that this method is called when contents are changed, too.

                if (!Objects.equals(getSelectedItem(), anObject)) {
                    @SuppressWarnings("unchecked")
                    T selected = (T) anObject;
                    setter.accept(selected);
                }

                super.setSelectedItem(anObject);
            }
        };

        JComboBox<T> combo = new JComboBox<T>(model);
        combo.setSelectedItem(initValue);
        return combo;
    }
}

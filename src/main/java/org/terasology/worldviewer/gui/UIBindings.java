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

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * TODO Type description
 * @author Martin Steiger
 */
public final class UIBindings {

    private UIBindings() {
        // no instances
    }

    public static void createCheckbox(JComponent parent, String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        JCheckBox checkBox = new JCheckBox("visible");
        checkBox.setSelected(getter.get());
        checkBox.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                checkBox.setSelected(getter.get());
            }
        });
        checkBox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                setter.accept(checkBox.isSelected());
            }
        });

        parent.add(new JLabel(name));
        parent.add(checkBox);
    }

    public static void createSpinner(JComponent parent, String name, double min, double stepSize, double max, Supplier<Double> getter, Consumer<Double> setter) {
        double initValue = getter.get().doubleValue();

        final SpinnerNumberModel model = new SpinnerNumberModel(initValue, min, max, stepSize);
        final JSpinner spinner = new JSpinner(model);
        spinner.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                spinner.setValue(getter.get());
            }
        });
        spinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Double value = (Double) model.getValue();
                setter.accept(value);
            }
        });
        parent.add(new JLabel(name));
        parent.add(spinner);
    }

}

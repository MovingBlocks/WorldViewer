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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;

/**
 * Works only for {@link JList} with {@link DefaultListModel}.
 * Adapted from http://stackoverflow.com/questions/16586562/reordering-jlist-with-drag-and-drop
 * @param <T> the item type
 * @author Martin Steiger
 */
public class ListItemTransferHandler<T> extends TransferHandler
{
    private static final long serialVersionUID = -8755359045727856083L;

    private static final Logger logger = LoggerFactory.getLogger(ListItemTransferHandler.class);

    private final DataFlavor localObjectFlavor;

    private List<T> transferedObjects = null;

    private int[] indices = null;
    private int addIndex = -1; // Location where items were added
    private int addCount = 0; // Number of items added.

    public ListItemTransferHandler() {
        localObjectFlavor = new ActivationDataFlavor(ArrayList.class, DataFlavor.javaJVMLocalObjectMimeType, "ArrayList of items");
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        @SuppressWarnings("unchecked")
        JList<T> list = (JList<T>) c;
        indices = list.getSelectedIndices();
        transferedObjects = Lists.newArrayList(list.getSelectedValuesList());
        return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
    }

    @Override
    public boolean canImport(TransferSupport info) {
        if (!info.isDrop() || !info.isDataFlavorSupported(localObjectFlavor)) {
            return false;
        }

        return true;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE; // TransferHandler.COPY_OR_MOVE;
    }

    @Override
    public boolean importData(TransferSupport info) {

        if (!canImport(info)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        JList<T> target = (JList<T>) info.getComponent();
        JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
        DefaultListModel<T> listModel = (DefaultListModel<T>) target.getModel();

        int index = dl.getIndex();
        int max = listModel.getSize();
        if (index < 0 || index > max) {
            index = max;
        }
        addIndex = index;

        try {
            @SuppressWarnings("unchecked")
            List<T> values = (List<T>) info.getTransferable().getTransferData(localObjectFlavor);

            addCount = values.size();
            for (int i = 0; i < values.size(); i++) {
                int idx = index++;
                listModel.add(idx, values.get(i));
                target.addSelectionInterval(idx, idx);
            }
            return true;
        } catch (UnsupportedFlavorException | IOException e) {
            logger.warn("Could not import dnd data", e);
            return false;
        }
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        if (action == MOVE) {
            removeEntries(c);
        }
    }

    private void removeEntries(JComponent c) {
        if (indices != null) {
            @SuppressWarnings("unchecked")
            JList<T> source = (JList<T>) c;
            DefaultListModel<T> model = (DefaultListModel<T>) source.getModel();

            if (addCount > 0) {
                // http://java-swing-tips.googlecode.com/svn/trunk/DnDReorderList/src/java/example/MainPanel.java
                for (int i = 0; i < indices.length; i++) {
                    if (indices[i] >= addIndex) {
                        indices[i] += addCount;
                    }
                }
            }
            for (int i = indices.length - 1; i >= 0; i--) {
                model.remove(indices[i]);
            }
        }
        indices = null;
        addCount = 0;
        addIndex = -1;
    }
}

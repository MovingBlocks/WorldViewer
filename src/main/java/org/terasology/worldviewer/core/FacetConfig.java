package org.terasology.worldviewer.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.terasology.world.generation.WorldFacet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Martin Steiger
 */
public class FacetConfig {

    private final Map<Class<? extends WorldFacet>, FacetLayer> layers = Maps.newHashMap();
    private final Set<FacetLayer> visibleLayers = Sets.newLinkedHashSet();
    private final Collection<Observer<FacetLayer>> observers = new CopyOnWriteArrayList<>();

    public void addObserver(Observer<FacetLayer> obs) {
        observers.add(obs);
    }

    public void removeObserver(Observer<FacetLayer> obs) {
        observers.remove(obs);
    }

    public void setVisible(FacetLayer layer, boolean visible)
    {
        boolean update = false;
        if (visible) { // TODO: check is layer is even valid
            update = visibleLayers.add(layer);
        } else {
            update = visibleLayers.remove(layer);
        }

        if (update) {
            notifyObservers(layer);
        }
    }

    public void notifyObservers(FacetLayer layer)
    {
        for (Observer<FacetLayer> obs : observers) {
            obs.update(layer);
        }
    }

    public Set<Class<? extends WorldFacet>> getFacets() {
        return Collections.unmodifiableSet(layers.keySet());
    }

    public void put(Class<? extends WorldFacet> facet, FacetLayer trait) {
        layers.put(facet, trait);
    }

    public Collection<? extends FacetLayer> getLayers()
    {
        return layers.values();
    }

    public boolean isVisible(FacetLayer trait)
    {
        return visibleLayers.contains(trait);
    }
}

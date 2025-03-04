/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Map that traces uses of its keys and values.
 * @param <K> the type of the keys
 * @param <V> the types of the values
 * @since 9.80.0
 * @version $Id$
 */
public class TracedMap<K, V> implements Map<K, V>
{
    private final Map<K, V> m;

    private final Collection<K> usedParameters;
    private final Set<K> unhandledValues;

    /**
     * @param m the backing map
     */
    public TracedMap(Map<K, V> m)
    {
        this.m = m;
        this.usedParameters = new HashSet<>(m.size());
        this.unhandledValues = new HashSet<>();
    }

    @Override
    public int size()
    {
        return m.size();
    }

    @Override
    public boolean isEmpty()
    {
        return m.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return m.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return m.containsValue(value);
    }

    /**
     * Regular Map#get(Object), but also marks the key as used.
     * @param key the key whose associated value is to be returned
     * @return the value of the key
     */
    @Override
    public V get(Object key)
    {
        markAsUsed((K) key);
        return m.get(key);
    }

    /**
     * @param key mark this key as used
     */
    public void markAsUsed(K key)
    {
        usedParameters.add(key);
    }

    /**
     * @param key mark this key as unused
     */
    public void markAsUnused(K key)
    {
        usedParameters.remove(key);
    }

    /**
     * @param key the key for which the value is considered unhandled
     */
    public void markAsUnhandledValue(K key)
    {
        unhandledValues.add(key);
    }

    /**
     * @return the collection of unhandled parameters
     */
    public Collection<K> getUnhandledParameters()
    {
        ArrayList<K> r = new ArrayList<>(this.keySet());
        for (K p : usedParameters) {
            r.remove(p);
        }

        return r;
    }

    /**
     * @return the collection of parameters having unhandled values
     */
    public Collection<K> getParametersWithUnhandledValues()
    {
        return unhandledValues;
    }

    void warnImmutable()
    {
        throw new RuntimeException("Modifying the Confluence parameter map is not allowed");
    }

    @Override
    public V put(K key, V value)
    {
        warnImmutable();
        return null;
    }

    @Override
    public V remove(Object key)
    {
        warnImmutable();
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        warnImmutable();
    }

    @Override
    public void clear()
    {
        warnImmutable();
    }

    @Override
    public Set<K> keySet()
    {
        return m.keySet();
    }

    @Override
    public Collection<V> values()
    {
        return m.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return m.entrySet();
    }
}

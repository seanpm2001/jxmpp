/**
 *
 * Copyright © 2014-2023 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jxmpp.util.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A cache which expires its values.
 *
 * @param <K> the type of the keys of this cache.
 * @param <V> the type of the values this cache caches.
 */
public class ExpirationCache<K, V> implements Cache<K, V>, Map<K, V>{

	private final LruCache<K, ExpireElement<V>> cache;

	private long defaultExpirationTime;

	/**
	 * Construct a new expiration cache.
	 *
	 * @param maxSize the maximum size.
	 * @param defaultExpirationTime the default expiration time in milliseconds.
	 */
	public ExpirationCache(int maxSize, long defaultExpirationTime) {
		cache = new LruCache<K, ExpireElement<V>>(maxSize);
		setDefaultExpirationTime(defaultExpirationTime);
	}

	/**
	 * Set the default expiration time in milliseconds.
	 *
	 * @param defaultExpirationTime the default expiration time.
	 */
	public void setDefaultExpirationTime(long defaultExpirationTime) {
		if (defaultExpirationTime <= 0) {
			throw new IllegalArgumentException();
		}
		this.defaultExpirationTime = defaultExpirationTime;
	}

	@Override
	public V put(K key, V value) {
		return put(key, value, defaultExpirationTime);
	}

	/**
	 * Put a value in the cache with the specified expiration time in milliseconds.
	 *
	 * @param key the key of the value.
	 * @param value the value.
	 * @param expirationTime the expiration time in milliseconds.
	 * @return the previous value or {@code null}.
	 */
	public V put(K key, V value, long expirationTime) {
		ExpireElement<V> eOld = cache.put(key, new ExpireElement<V>(value, expirationTime));
		if (eOld == null) {
			return null;
		}
		return eOld.element;
	}

    @Override
    public V lookup(K key) {
        return get(key);
    }

    @Override
	public V get(Object key) {
		ExpireElement<V> v = cache.get(key);
		if (v == null) {
			return null;
		}
		if (v.isExpired()) {
			remove(key);
			return null;
		}
		return v.element;
	}

	/**
	 * Remove a entry with the given key from the cache.
	 * 
	 * @param key the key of the value to remove.
	 * @return the remove value, or {@code null}.
	 */
	@Override
	public V remove(Object key) {
		ExpireElement<V> e = cache.remove(key);
		if (e == null) {
			return null;
		}
		return e.element;
	}

	@Override
	public int getMaxCacheSize() {
		return cache.getMaxCacheSize();
	}

	@Override
	public void setMaxCacheSize(int maxCacheSize) {
		cache.setMaxCacheSize(maxCacheSize);
	}

	private static class ExpireElement<V> {
		private final V element;
		private final long expirationTimestamp;

		private ExpireElement(V element, long expirationTime) {
			this.element = element;
			this.expirationTimestamp = System.currentTimeMillis() + expirationTime;
		}

		private boolean isExpired() {
			return System.currentTimeMillis() > expirationTimestamp;
		}

		@Override
		public int hashCode() {
			return element.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof ExpireElement))
				return false;
			ExpireElement<?> otherElement = (ExpireElement<?>) other;
			return element.equals(otherElement.element);
		}
	}

	@Override
	public int size() {
		return cache.size();
	}

	@Override
	public boolean isEmpty() {
		return cache.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return cache.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return cache.containsValue(value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public Set<K> keySet() {
		return cache.keySet();
	}

	@Override
	public Collection<V> values() {
		Set<V> res = new HashSet<V>();
		for (ExpireElement<V> value : cache.values()) {
			res.add(value.element);
		}
		return res;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<Entry<K, V>> res = new HashSet<Entry<K, V>>();
		for (Entry<K, ExpireElement<V>> entry : cache.entrySet()) {
			res.add(new EntryImpl<K, V>(entry.getKey(), entry.getValue().element));
		}
		return res;
	}

	private static class EntryImpl<K, V> implements Entry<K, V> {

		private final K key;
		private V value;

		EntryImpl(K key, V value) {
			this.key = key;
			this.value = value;
		}
		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}
	}
}

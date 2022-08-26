/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator.
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

  private final Cache delegate;

  /**
   * 使用 LinkedHashMap 保存的缓存数据的键
   */
  private Map<Object, Object> keyMap;

  /**
   * 最近最少使用的键
   */
  private Object eldestKey;

  public LruCache(Cache delegate) {
    this.delegate = delegate;
    setSize(1024);
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }


  /**
   * 设置缓存空间大小
   *
   * @param size 缓存空间大小
   */
  public void setSize(final int size) {
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;

      /**
       * 每次向 LinkedHashMap 放入数据时触发
       *
       * @param eldest The least recently inserted entry in the map, or if
       *           this is an access-ordered map, the least recently accessed
       *           entry.  This is the entry that will be removed it this
       *           method returns {@code true}.  If the map was empty prior
       *           to the {@code put} or {@code putAll} invocation resulting
       *           in this invocation, this will be the entry that was just
       *           inserted; in other words, if the map contains a single
       *           entry, the eldest entry is also the newest.
       * @return 最久未被访问的数据是否应该被删除
       */
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        boolean tooBig = size() > size;
        if (tooBig) {
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  @Override
  public void putObject(Object key, Object value) {
    delegate.putObject(key, value);
    // 向 keyMap 中也仿佛该键，并根据空间大小决定是否删除最久未访问的数据
    cycleKeyList(key);
  }

  @Override
  public Object getObject(Object key) {
    keyMap.get(key); // touch
    return delegate.getObject(key);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  /**
   * 向KeyMap中存入当前的键，并删除最久未访问的数据
   * @param key key
   */
  private void cycleKeyList(Object key) {
    keyMap.put(key, key);
    if (eldestKey != null) {
      delegate.removeObject(eldestKey);
      eldestKey = null;
    }
  }

}

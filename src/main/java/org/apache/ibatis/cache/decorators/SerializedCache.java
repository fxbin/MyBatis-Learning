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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.SerialFilterChecker;

/**
 * 序列化装饰器，为缓存增加序列化功能
 *
 * <p>
 *   问题描述：
 *   对象（也就是数据）放入缓存后，如果被多次读取出来，则多次读取的是同一个对象的引用。
 *   也就是说，缓存中的对象是在多个引用之间共享的。
 *   这意味着，如果读取后修改了该对象的属性，会直接导致缓存中的对象也发生变化
 * </p>
 *
 * <p> SerializedCache后，每次向缓存中写入对象时，实际写入的是对象的序列化串；而每次读取对象时，会将序列化串反序列化后再返回
 * <p> 通过序列化和反序列化的过程保证了每一次缓存给出的对象都是一个全新的对象，对该对象的修改不会影响缓存中的对象
 *
 * @author Clinton Begin
 */
public class SerializedCache implements Cache {

  private final Cache delegate;

  public SerializedCache(Cache delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public void putObject(Object key, Object object) {
    if (object == null || object instanceof Serializable) {
      delegate.putObject(key, serialize((Serializable) object));
    } else {
      throw new CacheException("SharedCache failed to make a copy of a non-serializable object: " + object);
    }
  }

  @Override
  public Object getObject(Object key) {
    Object object = delegate.getObject(key);
    return object == null ? null : deserialize((byte[]) object);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  private byte[] serialize(Serializable value) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(value);
      oos.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      throw new CacheException("Error serializing object.  Cause: " + e, e);
    }
  }

  private Serializable deserialize(byte[] value) {
    SerialFilterChecker.check();
    Serializable result;
    try (ByteArrayInputStream bis = new ByteArrayInputStream(value);
        ObjectInputStream ois = new CustomObjectInputStream(bis)) {
      result = (Serializable) ois.readObject();
    } catch (Exception e) {
      throw new CacheException("Error deserializing object.  Cause: " + e, e);
    }
    return result;
  }

  public static class CustomObjectInputStream extends ObjectInputStream {

    public CustomObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
      return Resources.classForName(desc.getName());
    }

  }

}

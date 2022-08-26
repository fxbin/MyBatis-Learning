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

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 阻塞装饰器，为缓存增加阻塞功能
 * <p>缓存在收到多条相同的查询请求时会暂时阻塞住后面的查询，等待数据库结果返回时将所有的请求一并返回
 *
 * <p>Simple blocking decorator
 *
 * <p>Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 *
 * <p>By its nature, this implementation can cause deadlock when used incorrecly.
 *
 * @author Eduardo Macarron
 *
 */
public class BlockingCache implements Cache {

  /**
   * 获取锁时的运行等待时间
   */
  private long timeout;

  /**
   * 被装饰对象
   */
  private final Cache delegate;

  /**
   * 锁的映射表，键为缓存记录的键，值为对应的锁
   * 锁住的是单条记录，而不是整个缓存数据
   */
  private final ConcurrentHashMap<Object, CountDownLatch> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
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
  public void putObject(Object key, Object value) {
    try {
      delegate.putObject(key, value);
    } finally {
      releaseLock(key);
    }
  }

  @Override
  public Object getObject(Object key) {
    // 获取锁
    acquireLock(key);
    // 读取结果
    Object value = delegate.getObject(key);
    if (value != null) {
      // 释放锁
      releaseLock(key);
    }
    // 如果缓存中没有读取到结果，则不会释放锁。 对应的锁会再从数据库读取了结果并写入缓存之后，从 putObject 中释放
    return value;
  }

  @Override
  public Object removeObject(Object key) {
    // despite of its name, this method is called only to release locks
    releaseLock(key);
    return null;
  }

  @Override
  public void clear() {
    delegate.clear();
  }


  /**'
   * 找出某个键的锁
   *
   * @param key
   */
  private void acquireLock(Object key) {
    CountDownLatch newLatch = new CountDownLatch(1);
    while (true) {
      CountDownLatch latch = locks.putIfAbsent(key, newLatch);
      if (latch == null) {
        break;
      }
      try {
        if (timeout > 0) {
          boolean acquired = latch.await(timeout, TimeUnit.MILLISECONDS);
          if (!acquired) {
            throw new CacheException(
                "Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
          }
        } else {
          latch.await();
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    }
  }

  private void releaseLock(Object key) {
    CountDownLatch latch = locks.remove(key);
    if (latch == null) {
      throw new IllegalStateException("Detected an attempt at releasing unacquired lock. This should never happen.");
    }
    latch.countDown();
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}

package org.apache.ibatis.cache;

import org.junit.jupiter.api.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * WeakReferenceTest
 *
 * @author fxbin
 * @version v1.0
 * @since 2022/8/26 11:11
 */
public class WeakReferenceTest {

  @Test
  public void testWeakReference() {

    // 创建 ReferenceQueue
    ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

    // 存储弱引用的目标对象
    List<WeakReference> weakRefList = new ArrayList<>();
    for (int i = 0; i < 100000; i++) {
      // 创建弱引用对象，并传入 ReferenceQueue
      WeakReference<User> weakReference = new WeakReference<>(
        new User(UUID.randomUUID().toString()),
        referenceQueue);

      // 引用弱引用对象
      weakRefList.add(weakReference);
    }

    WeakReference weakReference;
    Integer count = 0;

    // 处理被回收的弱引用
    // 即通过检查弱引用列表，
    while ((weakReference = (WeakReference) referenceQueue.poll()) != null) {
      // 虽然弱引用存在，但是引用的目标对象已经为空
      System.out.println("JVM清理了：" + weakReference + ", 从WeakReference 中取出对象值为：" + weakReference.get());
      count ++;
    }

    // 被回收的弱引用总数
    System.out.println("WeakReference 中的元素数目为：" + count);

    // 在弱引用的目标对象不被清理时，可以引用目标对象
    System.out.println("在不被清理的情况下，可以从WeakReference中取出的对象值为：" + new WeakReference<>(new User(UUID.randomUUID().toString()), referenceQueue).get());
  }


  static class User {
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public User() {
    }

    public User(String id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return "User{" +
        "id='" + id + '\'' +
        '}';
    }
  }


}

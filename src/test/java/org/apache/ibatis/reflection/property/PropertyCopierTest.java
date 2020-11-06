package org.apache.ibatis.reflection.property;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * CopierTest
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/11/6 17:51
 */
public class PropertyCopierTest {

  @Test
  public void testCopierUser() {
    User user1 = new User(1, "fxbin");
    User user2 = new User();

    // 将user1 的属性全部复制给 user2
    PropertyCopier.copyBeanProperties(user1.getClass(), user1, user2);

    assertEquals(1, user2.getId());
    assertEquals("fxbin", user2.getName());
  }

}

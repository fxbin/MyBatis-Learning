package org.apache.ibatis.reflection.property;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * PropertyTokenizerTest
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/11/6 18:01
 */
public class PropertyTokenizerTest {

  @Test
  public void testPropertyTokenizer() {
    PropertyTokenizer propertyTokenizer = new PropertyTokenizer("student[sId].name");

    System.out.println("children: " + propertyTokenizer.getChildren());
    System.out.println("index: " + propertyTokenizer.getIndex());
    System.out.println("index name: " + propertyTokenizer.getIndexedName());
    System.out.println("name: " + propertyTokenizer.getName());
  }

}

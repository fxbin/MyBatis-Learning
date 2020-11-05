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
package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 *
 * @author Clinton Begin
 */
public interface ObjectFactory {

  /**
   * 设置工厂的属性
   * Sets configuration properties.
   * @param properties configuration properties
   */
  default void setProperties(Properties properties) {
    // NOP
  }

  /**
   * 传入一个类型，采用无参构造方法生成这个类型的实例
   * Creates a new object with default constructor.
   *
   * @param <T>
   *          the generic type
   * @param type
   *          Object type
   * @return the t
   */
  <T> T create(Class<T> type);

  /**
   * 传入一个目标类型，一个参数类型列表，一个参数值列表，根据参数列表找到相应的含参构造方法生成这个类型的实例
   * Creates a new object with the specified constructor and params.
   *
   * @param <T>
   *          the generic type 实例类型
   * @param type
   *          Object type 要创建的实例
   * @param constructorArgTypes
   *          Constructor argument types 构造方法输入参数类型
   * @param constructorArgs
   *          Constructor argument values 构造方法输入参数值
   * @return the t
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

  /**
   * 判断传入类型是否是集合类型
   * Returns true if this object can have a set of other objects.
   * It's main purpose is to support non-java.util.Collection objects like Scala collections.
   *
   * @param <T>
   *          the generic type
   * @param type
   *          Object type
   * @return whether it is a collection or not
   * @since 3.1.0
   */
  <T> boolean isCollection(Class<T> type);

}

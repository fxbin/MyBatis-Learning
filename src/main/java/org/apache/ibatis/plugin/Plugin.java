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
package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Clinton Begin
 */
public class Plugin implements InvocationHandler {

  /**
   * 被代理对象
   */
  private final Object target;

  /**
   * 拦截器
   */
  private final Interceptor interceptor;

  /**
   * 拦截器要拦截的所有的类，以及类中的方法
   */
  private final Map<Class<?>, Set<Method>> signatureMap;

  private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
    this.target = target;
    this.interceptor = interceptor;
    this.signatureMap = signatureMap;
  }

  /**
   * 根据拦截器的配置来生成一个对象用来替代被代理对象
   *
   * @param target 被代理的对象
   * @param interceptor 拦截器
   * @return 用来替换被代理对象的对象
   */
  public static Object wrap(Object target, Interceptor interceptor) {
    // 得到拦截器 interceptor 要拦截的类型和方法
    Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
    // 被代理对象的类型
    Class<?> type = target.getClass();
    // 逐级寻找代理对象类型的父类，将父类中需要被拦截的全部找出
    Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
    // 只要父类中有一个需要被拦截，就说明被代理对象是需要被拦截的
    if (interfaces.length > 0) {
      // 创建并返回一个代理对象，是Plugin 类的实例
      return Proxy.newProxyInstance(
          type.getClassLoader(),
          interfaces,
          new Plugin(target, interceptor, signatureMap));
    }
    // 直接返回原有被代理对象
    return target;
  }


  /**
   * 代理方法
   *
   * @param proxy the proxy instance that the method was invoked on
   *
   * @param method the {@code Method} instance corresponding to
   * the interface method invoked on the proxy instance.  The declaring
   * class of the {@code Method} object will be the interface that
   * the method was declared in, which may be a superinterface of the
   * proxy interface that the proxy class inherits the method through.
   *
   * @param args an array of objects containing the values of the
   * arguments passed in the method invocation on the proxy instance,
   * or {@code null} if interface method takes no arguments.
   * Arguments of primitive types are wrapped in instances of the
   * appropriate primitive wrapper class, such as
   * {@code java.lang.Integer} or {@code java.lang.Boolean}.
   *
   * @return 方法执行结果
   * @throws Throwable
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // 获取该类所有需要被拦截的方法
      Set<Method> methods = signatureMap.get(method.getDeclaringClass());
      if (methods != null && methods.contains(method)) {
        // 该方法确实需要被拦截器拦截，会交给拦截器处理
        return interceptor.intercept(new Invocation(target, method, args));
      }
      // 不需要拦截器处理，交给代理对象处理
      return method.invoke(target, args);
    } catch (Exception e) {
      throw ExceptionUtil.unwrapThrowable(e);
    }
  }

  /**
   * 获取拦截器要拦截的所有类和类中的方法
   *
   * @param interceptor 拦截器
   * @return 输入参数拦截器要拦截的所有类和类中的方法
   */
  private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
    // 获取拦截器的 Intercepts 注解
    Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
    // issue #251
    if (interceptsAnnotation == null) {
      throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
    }
    // 将 Intercepts 注解的 value 信息提取出来，是一个 Signature 数组
    Signature[] sigs = interceptsAnnotation.value();

    // 将 Signature 数组放入一个 Map 中，键为 Signature 注解的 type 类型，值为该类型下的方法集合
    Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
    for (Signature sig : sigs) {
      Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
      try {
        Method method = sig.type().getMethod(sig.method(), sig.args());
        methods.add(method);
      } catch (NoSuchMethodException e) {
        throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
      }
    }
    return signatureMap;
  }

  private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    Set<Class<?>> interfaces = new HashSet<>();
    while (type != null) {
      for (Class<?> c : type.getInterfaces()) {
        if (signatureMap.containsKey(c)) {
          interfaces.add(c);
        }
      }
      type = type.getSuperclass();
    }
    return interfaces.toArray(new Class<?>[0]);
  }

}

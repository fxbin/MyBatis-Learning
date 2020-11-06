package org.apache.ibatis.reflection.property;

/**
 * User
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/11/6 17:50
 */
public class User {

  private Integer id;

  private String name;

  public User() {
  }

  public User(Integer id, String name) {
    this.id = id;
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}

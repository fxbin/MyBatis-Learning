/**
 *    Copyright 2009-2019 the original author or authors.
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
package org.apache.ibatis.cursor.defaults;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetWrapper;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This is the default implementation of a MyBatis Cursor.
 * This implementation is not thread safe.
 *
 * @author Guillaume Darmont / guillaume@dropinocean.com
 */
public class DefaultCursor<T> implements Cursor<T> {

  // ResultSetHandler stuff
  private final DefaultResultSetHandler resultSetHandler;
  private final ResultMap resultMap;

  /**
   * 返回结果的详细信息
   */
  private final ResultSetWrapper rsw;

  /**
   * 结果的起止信息
   */
  private final RowBounds rowBounds;

  /**
   * ResultHandler 的子类，起到暂存结果的作用
   */
  protected final ObjectWrapperResultHandler<T> objectWrapperResultHandler = new ObjectWrapperResultHandler<>();

  /**
   * 内部迭代器
   */
  private final CursorIterator cursorIterator = new CursorIterator();

  /**
   * 迭代器存在标志位
   */
  private boolean iteratorRetrieved;

  /**
   * 游标状态
   */
  private CursorStatus status = CursorStatus.CREATED;

  /**
   * 记录已经开始的行
   */
  private int indexWithRowBound = -1;

  private enum CursorStatus {

    /**
     * A freshly created cursor, database ResultSet consuming has not started.
     */
    CREATED,
    /**
     * A cursor currently in use, database ResultSet consuming has started.
     */
    OPEN,
    /**
     * A closed cursor, not fully consumed.
     */
    CLOSED,
    /**
     * A fully consumed cursor, a consumed cursor is always closed.
     */
    CONSUMED
  }

  public DefaultCursor(DefaultResultSetHandler resultSetHandler, ResultMap resultMap, ResultSetWrapper rsw, RowBounds rowBounds) {
    this.resultSetHandler = resultSetHandler;
    this.resultMap = resultMap;
    this.rsw = rsw;
    this.rowBounds = rowBounds;
  }

  @Override
  public boolean isOpen() {
    return status == CursorStatus.OPEN;
  }

  @Override
  public boolean isConsumed() {
    return status == CursorStatus.CONSUMED;
  }

  @Override
  public int getCurrentIndex() {
    return rowBounds.getOffset() + cursorIterator.iteratorIndex;
  }

  @Override
  public Iterator<T> iterator() {
    if (iteratorRetrieved) {
      throw new IllegalStateException("Cannot open more than one iterator on a Cursor");
    }
    if (isClosed()) {
      throw new IllegalStateException("A Cursor is already closed.");
    }
    iteratorRetrieved = true;
    return cursorIterator;
  }

  @Override
  public void close() {
    if (isClosed()) {
      return;
    }

    ResultSet rs = rsw.getResultSet();
    try {
      if (rs != null) {
        rs.close();
      }
    } catch (SQLException e) {
      // ignore
    } finally {
      status = CursorStatus.CLOSED;
    }
  }

  protected T fetchNextUsingRowBound() {
    // 从数据库查询结果中取出下一个对象
    T result = fetchNextObjectFromDatabase();
    // 如果对象存在，但是不满足边界限制，则持续读取数据库结果中的下一个，直到边界起始位置
    while (objectWrapperResultHandler.fetched && indexWithRowBound < rowBounds.getOffset()) {
      result = fetchNextObjectFromDatabase();
    }
    return result;
  }

  /**
   * 从数据库获取下一个对象
   */
  protected T fetchNextObjectFromDatabase() {
    if (isClosed()) {
      return null;
    }

    try {
      objectWrapperResultHandler.fetched = false;
      status = CursorStatus.OPEN;
      // 结果集尚未关闭
      if (!rsw.getResultSet().isClosed()) {
        // 从结果集中取出一条记录，将其转化为对象，并存入 objectWrapperResultHandler 中
        resultSetHandler.handleRowValues(rsw, resultMap, objectWrapperResultHandler, RowBounds.DEFAULT, null);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    // 获得存入 objectWrapperResultHandler 中的对象
    T next = objectWrapperResultHandler.result;
    if (objectWrapperResultHandler.fetched) {
      // 索引记录 +1
      indexWithRowBound++;
    }
    // No more object or limit reached
    if (!objectWrapperResultHandler.fetched || getReadItemsCount() == rowBounds.getOffset() + rowBounds.getLimit()) {
      close();
      status = CursorStatus.CONSUMED;
    }
    objectWrapperResultHandler.result = null;

    return next;
  }

  private boolean isClosed() {
    return status == CursorStatus.CLOSED || status == CursorStatus.CONSUMED;
  }

  private int getReadItemsCount() {
    return indexWithRowBound + 1;
  }

  protected static class ObjectWrapperResultHandler<T> implements ResultHandler<T> {

    protected T result;
    protected boolean fetched;


    /**
     * 从结果上下文中取出并处理结果
     *
     * @param context 结果上下文
     */
    @Override
    public void handleResult(ResultContext<? extends T> context) {
      this.result = context.getResultObject();
      context.stop();
      fetched = true;
    }
  }

  protected class CursorIterator implements Iterator<T> {

    /**
     * Holder for the next object to be returned.
     */
    T object;

    /**
     * Index of objects returned using next(), and as such, visible to users.
     */
    int iteratorIndex = -1;

    @Override
    public boolean hasNext() {
      if (!objectWrapperResultHandler.fetched) {
        object = fetchNextUsingRowBound();
      }
      return objectWrapperResultHandler.fetched;
    }

    @Override
    public T next() {
      // Fill next with object fetched from hasNext()
      T next = object;

      if (!objectWrapperResultHandler.fetched) {
        next = fetchNextUsingRowBound();
      }

      if (objectWrapperResultHandler.fetched) {
        objectWrapperResultHandler.fetched = false;
        object = null;
        iteratorIndex++;
        return next;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove element from Cursor");
    }
  }
}

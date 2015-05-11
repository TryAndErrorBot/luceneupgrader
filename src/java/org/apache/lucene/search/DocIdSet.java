package org.apache.lucene.search;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

/**
 * A DocIdSet contains a set of doc ids. Implementing classes must
 * only implement {@code #iterator} to provide access to the set.
 */
public abstract class DocIdSet {

  /** Provides a {@code DocIdSetIterator} to access the set.
   * This implementation can return <code>null</code> or
   * <code>{@code #EMPTY_DOCIDSET}.iterator()</code> if there
   * are no docs that match. */
  public abstract DocIdSetIterator iterator() throws IOException;

}

/*
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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.util;

import java.io.IOException;

/**
 * A Function that may throw an IOException
 *
 * @see java.util.function.Function
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
@SuppressWarnings("removal")
public interface IOFunction<T, R> extends IOUtils.IOFunction<T, R> {
  /**
   * Applies this function to the given argument.
   *
   * @param t the function argument
   * @return the function result
   * @throws IOException if producing the result throws an {@link IOException}
   */
  @Override
  R apply(T t) throws IOException;
}

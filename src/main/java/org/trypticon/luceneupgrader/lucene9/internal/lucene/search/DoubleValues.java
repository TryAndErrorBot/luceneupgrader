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

package org.trypticon.luceneupgrader.lucene9.internal.lucene.search;

import java.io.IOException;

/** Per-segment, per-document double values, which can be calculated at search-time */
public abstract class DoubleValues {

  /** Get the double value for the current document */
  public abstract double doubleValue() throws IOException;

  /**
   * Advance this instance to the given document id
   *
   * @return true if there is a value for this document
   */
  public abstract boolean advanceExact(int doc) throws IOException;

  /** Wrap a DoubleValues instance, returning a default if the wrapped instance has no value */
  public static DoubleValues withDefault(DoubleValues in, double missingValue) {
    return new DoubleValues() {

      boolean hasValue = false;

      @Override
      public double doubleValue() throws IOException {
        return hasValue ? in.doubleValue() : missingValue;
      }

      @Override
      public boolean advanceExact(int doc) throws IOException {
        hasValue = in.advanceExact(doc);
        return true;
      }
    };
  }

  /**
   * An empty DoubleValues instance that always returns {@code false} from {@link
   * #advanceExact(int)}
   */
  public static final DoubleValues EMPTY =
      new DoubleValues() {
        @Override
        public double doubleValue() throws IOException {
          throw new UnsupportedOperationException();
        }

        @Override
        public boolean advanceExact(int doc) throws IOException {
          return false;
        }
      };
}

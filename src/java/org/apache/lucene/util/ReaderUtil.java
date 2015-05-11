package org.apache.lucene.util;

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

import org.apache.lucene.index.IndexReader;

import java.util.List;

/**
 * Common util methods for dealing with {@code IndexReader}s.
 *
 * @lucene.internal
 */
public final class ReaderUtil {

  private ReaderUtil() {} // no instance

  /**
   * Gathers sub-readers from reader into a List.
   * 
   * @param allSubReaders ...
   * @param reader ...
   */
  public static void gatherSubReaders(List<IndexReader> allSubReaders, IndexReader reader) {
    IndexReader[] subReaders = reader.getSequentialSubReaders();
    if (subReaders == null) {
      // Add the reader itself, and do not recurse
      allSubReaders.add(reader);
    } else {
      for (IndexReader subReader : subReaders) {
        gatherSubReaders(allSubReaders, subReader);
      }
    }
  }

  /**
   * Returns index of the searcher/reader for document <code>n</code> in the
   * array used to construct this searcher/reader.
   */
  public static int subIndex(int n, int[] docStarts) { // find
    // searcher/reader for doc n:
    int size = docStarts.length;
    int lo = 0; // search starts array
    int hi = size - 1; // for first element less than n, return its index
    while (hi >= lo) {
      int mid = (lo + hi) >>> 1;
      int midValue = docStarts[mid];
      if (n < midValue)
        hi = mid - 1;
      else if (n > midValue)
        lo = mid + 1;
      else { // found a match
        while (mid + 1 < size && docStarts[mid + 1] == midValue) {
          mid++; // scan to last match
        }
        return mid;
      }
    }
    return hi;
  }

}

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

/**
 * {@link Sorter} implementation based on the merge-sort algorithm that merges in place (no extra
 * memory will be allocated). Small arrays are sorted with binary sort.
 *
 * <p>This algorithm is stable. It's especially suited to sorting small lists where we'd rather
 * optimize for avoiding allocating memory for this task. It performs well on lists that are already
 * sorted.
 *
 * @lucene.internal
 */
public abstract class InPlaceMergeSorter extends Sorter {

  /** Create a new {@link InPlaceMergeSorter} */
  public InPlaceMergeSorter() {}

  @Override
  public final void sort(int from, int to) {
    checkRange(from, to);
    mergeSort(from, to);
  }

  void mergeSort(int from, int to) {
    if (to - from < BINARY_SORT_THRESHOLD) {
      binarySort(from, to);
    } else {
      final int mid = (from + to) >>> 1;
      mergeSort(from, mid);
      mergeSort(mid, to);
      mergeInPlace(from, mid, to);
    }
  }
}

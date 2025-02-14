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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.util.automaton;

// The following code was generated with the moman/finenight pkg
// This package is available under the MIT License, see NOTICE.txt
// for more details.
// This source file is auto-generated, Please do not modify it directly.
// You should modify the gradle/generation/moman/createAutomata.py instead.

import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.automaton.LevenshteinAutomata.ParametricDescription;

/**
 * Parametric description for generating a Levenshtein automaton of degree 1, with transpositions as
 * primitive edits. The comment in Lev1ParametricDescription may be helpful for you to understand
 * this class.
 *
 * @see Lev1ParametricDescription
 */
class Lev1TParametricDescription extends ParametricDescription {

  @Override
  int transition(int absState, int position, int vector) {
    // null absState should never be passed in
    assert absState != -1;

    // decode absState -> state, offset
    int state = absState / (w + 1);
    int offset = absState % (w + 1);
    assert offset >= 0;

    if (position == w) {
      if (state < 2) {
        final int loc = vector * 2 + state;
        offset += unpack(offsetIncrs0, loc, 1);
        state = unpack(toStates0, loc, 2) - 1;
      }
    } else if (position == w - 1) {
      if (state < 3) {
        final int loc = vector * 3 + state;
        offset += unpack(offsetIncrs1, loc, 1);
        state = unpack(toStates1, loc, 2) - 1;
      }
    } else if (position == w - 2) {
      if (state < 6) {
        final int loc = vector * 6 + state;
        offset += unpack(offsetIncrs2, loc, 2);
        state = unpack(toStates2, loc, 3) - 1;
      }
    } else {
      if (state < 6) {
        final int loc = vector * 6 + state;
        offset += unpack(offsetIncrs3, loc, 2);
        state = unpack(toStates3, loc, 3) - 1;
      }
    }

    if (state == -1) {
      // null state
      return -1;
    } else {
      // translate back to abs
      return state * (w + 1) + offset;
    }
  }

  // 1 vectors; 2 states per vector; array length = 2
  private static final long[] toStates0 = new long[] /*2 bits per value */ {0x2L};
  private static final long[] offsetIncrs0 = new long[] /*1 bits per value */ {0x0L};

  // 2 vectors; 3 states per vector; array length = 6
  private static final long[] toStates1 = new long[] /*2 bits per value */ {0xa43L};
  private static final long[] offsetIncrs1 = new long[] /*1 bits per value */ {0x38L};

  // 4 vectors; 6 states per vector; array length = 24
  private static final long[] toStates2 =
      new long[] /*3 bits per value */ {0xb45a491412180003L, 0x69L};
  private static final long[] offsetIncrs2 = new long[] /*2 bits per value */ {0x5555558a0000L};

  // 8 vectors; 6 states per vector; array length = 48
  private static final long[] toStates3 =
      new long[] /*3 bits per value */ {0xa1904864900c0003L, 0x5a6d196a45a49169L, 0x9634L};
  private static final long[] offsetIncrs3 =
      new long[] /*2 bits per value */ {0x5555ba08a0fc0000L, 0x55555555L};

  // state map
  //   0 -> [(0, 0)]
  //   1 -> [(0, 1)]
  //   2 -> [(0, 1), (1, 1)]
  //   3 -> [(0, 1), (1, 1), (2, 1)]
  //   4 -> [(0, 1), (2, 1)]
  //   5 -> [t(0, 1), (0, 1), (1, 1), (2, 1)]

  public Lev1TParametricDescription(int w) {
    super(w, 1, new int[] {0, 1, 0, -1, -1, -1});
  }
}

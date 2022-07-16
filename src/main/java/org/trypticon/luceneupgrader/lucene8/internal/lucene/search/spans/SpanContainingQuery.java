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
package org.trypticon.luceneupgrader.lucene8.internal.lucene.search.spans;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.LeafReaderContext;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.Term;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.TermStates;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.IndexSearcher;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.ScoreMode;

public final class SpanContainingQuery extends SpanContainQuery {
  public SpanContainingQuery(SpanQuery big, SpanQuery little) {
    super(big, little);
  }

  @Override
  public String toString(String field) {
    return toString(field, "SpanContaining");
  }

  @Override
  public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
    SpanWeight bigWeight = big.createWeight(searcher, scoreMode, boost);
    SpanWeight littleWeight = little.createWeight(searcher, scoreMode, boost);
    return new SpanContainingWeight(searcher, scoreMode.needsScores() ? getTermStates(bigWeight, littleWeight) : null,
                                      bigWeight, littleWeight, boost);
  }

  public class SpanContainingWeight extends SpanContainWeight {

    public SpanContainingWeight(IndexSearcher searcher, Map<Term, TermStates> terms,
                                SpanWeight bigWeight, SpanWeight littleWeight, float boost) throws IOException {
      super(searcher, terms, bigWeight, littleWeight, boost);
    }

    @Override
    public Spans getSpans(final LeafReaderContext context, Postings requiredPostings) throws IOException {
      ArrayList<Spans> containerContained = prepareConjunction(context, requiredPostings);
      if (containerContained == null) {
        return null;
      }

      Spans big = containerContained.get(0);
      Spans little = containerContained.get(1);

      return new ContainSpans(big, little, big) {

        @Override
        boolean twoPhaseCurrentDocMatches() throws IOException {
          oneExhaustedInCurrentDoc = false;
          assert littleSpans.startPosition() == -1;
          while (bigSpans.nextStartPosition() != NO_MORE_POSITIONS) {
            while (littleSpans.startPosition() < bigSpans.startPosition()) {
              if (littleSpans.nextStartPosition() == NO_MORE_POSITIONS) {
                oneExhaustedInCurrentDoc = true;
                return false;
              }
            }
            if (bigSpans.endPosition() >= littleSpans.endPosition()) {
              atFirstInCurrentDoc = true;
              return true;
            }
          }
          oneExhaustedInCurrentDoc = true;
          return false;
        }

        @Override
        public int nextStartPosition() throws IOException {
          if (atFirstInCurrentDoc) {
            atFirstInCurrentDoc = false;
            return bigSpans.startPosition();
          }
          while (bigSpans.nextStartPosition() != NO_MORE_POSITIONS) {
            while (littleSpans.startPosition() < bigSpans.startPosition()) {
              if (littleSpans.nextStartPosition() == NO_MORE_POSITIONS) {
                oneExhaustedInCurrentDoc = true;
                return NO_MORE_POSITIONS;
              }
            }
            if (bigSpans.endPosition() >= littleSpans.endPosition()) {
              return bigSpans.startPosition();
            }
          }
          oneExhaustedInCurrentDoc = true;
          return NO_MORE_POSITIONS;
        }
      };
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return bigWeight.isCacheable(ctx) && littleWeight.isCacheable(ctx);
    }

  }
}
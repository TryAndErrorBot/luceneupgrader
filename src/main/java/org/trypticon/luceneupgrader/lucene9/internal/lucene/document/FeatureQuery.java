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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.document;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.document.FeatureField.FeatureFunction;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.*;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.search.*;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.search.similarities.Similarity.SimScorer;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Objects;

final class FeatureQuery extends Query {

  private final String fieldName;
  private final String featureName;
  private final FeatureFunction function;

  FeatureQuery(String fieldName, String featureName, FeatureFunction function) {
    this.fieldName = Objects.requireNonNull(fieldName);
    this.featureName = Objects.requireNonNull(featureName);
    this.function = Objects.requireNonNull(function);
  }

  @Override
  public Query rewrite(IndexSearcher indexSearcher) throws IOException {
    FeatureFunction rewritten = function.rewrite(indexSearcher);
    if (function != rewritten) {
      return new FeatureQuery(fieldName, featureName, rewritten);
    }
    return super.rewrite(indexSearcher);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FeatureQuery that = (FeatureQuery) obj;
    return Objects.equals(fieldName, that.fieldName)
        && Objects.equals(featureName, that.featureName)
        && Objects.equals(function, that.function);
  }

  @Override
  public int hashCode() {
    int h = getClass().hashCode();
    h = 31 * h + fieldName.hashCode();
    h = 31 * h + featureName.hashCode();
    h = 31 * h + function.hashCode();
    return h;
  }

  @Override
  public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
      throws IOException {
    if (!scoreMode.needsScores()) {
      // We don't need scores (e.g. for faceting), and since features are stored as terms,
      // allow TermQuery to optimize in this case
      TermQuery tq = new TermQuery(new Term(fieldName, featureName));
      return searcher.rewrite(tq).createWeight(searcher, scoreMode, boost);
    }

    return new Weight(this) {

      @Override
      public boolean isCacheable(LeafReaderContext ctx) {
        return false;
      }

      @Override
      public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        String desc = "weight(" + getQuery() + " in " + doc + ") [" + function + "]";

        Terms terms = context.reader().terms(fieldName);
        if (terms == null) {
          return Explanation.noMatch(desc + ". Field " + fieldName + " doesn't exist.");
        }
        TermsEnum termsEnum = terms.iterator();
        if (termsEnum.seekExact(new BytesRef(featureName)) == false) {
          return Explanation.noMatch(desc + ". Feature " + featureName + " doesn't exist.");
        }

        PostingsEnum postings = termsEnum.postings(null, PostingsEnum.FREQS);
        if (postings.advance(doc) != doc) {
          return Explanation.noMatch(desc + ". Feature " + featureName + " isn't set.");
        }

        return function.explain(fieldName, featureName, boost, postings.freq());
      }

      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        Terms terms = Terms.getTerms(context.reader(), fieldName);
        TermsEnum termsEnum = terms.iterator();
        if (termsEnum.seekExact(new BytesRef(featureName)) == false) {
          return null;
        }

        final SimScorer scorer = function.scorer(boost);
        final ImpactsEnum impacts = termsEnum.impacts(PostingsEnum.FREQS);
        final ImpactsDISI impactsDisi = new ImpactsDISI(impacts, impacts, scorer);

        return new Scorer(this) {

          @Override
          public int docID() {
            return impacts.docID();
          }

          @Override
          public float score() throws IOException {
            return scorer.score(impacts.freq(), 1L);
          }

          @Override
          public DocIdSetIterator iterator() {
            return impactsDisi;
          }

          @Override
          public int advanceShallow(int target) throws IOException {
            return impactsDisi.advanceShallow(target);
          }

          @Override
          public float getMaxScore(int upTo) throws IOException {
            return impactsDisi.getMaxScore(upTo);
          }

          @Override
          public void setMinCompetitiveScore(float minScore) {
            impactsDisi.setMinCompetitiveScore(minScore);
          }
        };
      }
    };
  }

  @Override
  public void visit(QueryVisitor visitor) {
    if (visitor.acceptField(fieldName)) {
      visitor.visitLeaf(this);
    }
  }

  @Override
  public String toString(String field) {
    return "FeatureQuery(field="
        + fieldName
        + ", feature="
        + featureName
        + ", function="
        + function
        + ")";
  }
}

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
import java.util.Objects;

import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.DocIdSetIterator;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.LeafSimScorer;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.Scorer;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.TwoPhaseIterator;

public class SpanScorer extends Scorer {

  protected final Spans spans;
  protected final LeafSimScorer docScorer;

  private float freq;
  private int numMatches;
  private int lastScoredDoc = -1; // last doc we called setFreqCurrentDoc() for

  public SpanScorer(SpanWeight weight, Spans spans, LeafSimScorer docScorer) {
    super(weight);
    this.spans = Objects.requireNonNull(spans);
    this.docScorer = docScorer;
  }

  public Spans getSpans() {
    return spans;
  }

  @Override
  public int docID() {
    return spans.docID();
  }

  @Override
  public DocIdSetIterator iterator() {
    return spans;
  }

  @Override
  public TwoPhaseIterator twoPhaseIterator() {
    return spans.asTwoPhaseIterator();
  }

  protected float scoreCurrentDoc() throws IOException {
    assert docScorer != null : getClass() + " has a null docScorer!";
    return docScorer.score(docID(), freq);
  }

  protected final void setFreqCurrentDoc() throws IOException {
    freq = 0.0f;
    numMatches = 0;

    spans.doStartCurrentDoc();

    assert spans.startPosition() == -1 : "incorrect initial start position, " + spans;
    assert spans.endPosition() == -1 : "incorrect initial end position, " + spans;
    int prevStartPos = -1;
    int prevEndPos = -1;

    int startPos = spans.nextStartPosition();
    assert startPos != Spans.NO_MORE_POSITIONS : "initial startPos NO_MORE_POSITIONS, " + spans;
    do {
      assert startPos >= prevStartPos;
      int endPos = spans.endPosition();
      assert endPos != Spans.NO_MORE_POSITIONS;
      // This assertion can fail for Or spans on the same term:
      // assert (startPos != prevStartPos) || (endPos > prevEndPos) : "non increased endPos="+endPos;
      assert (startPos != prevStartPos) || (endPos >= prevEndPos) : "decreased endPos="+endPos;
      numMatches++;
      if (docScorer == null) {  // scores not required, break out here
        freq = 1;
        return;
      }
      freq += (1.0 / (1.0 + spans.width()));
      spans.doCurrentSpans();
      prevStartPos = startPos;
      prevEndPos = endPos;
      startPos = spans.nextStartPosition();
    } while (startPos != Spans.NO_MORE_POSITIONS);

    assert spans.startPosition() == Spans.NO_MORE_POSITIONS : "incorrect final start position, " + spans;
    assert spans.endPosition() == Spans.NO_MORE_POSITIONS : "incorrect final end position, " + spans;
  }

  private void ensureFreq() throws IOException {
    int currentDoc = docID();
    if (lastScoredDoc != currentDoc) {
      setFreqCurrentDoc();
      lastScoredDoc = currentDoc;
    }
  }

  @Override
  public final float score() throws IOException {
    ensureFreq();
    return scoreCurrentDoc();
  }

  @Override
  public float getMaxScore(int upTo) throws IOException {
    return Float.POSITIVE_INFINITY;
  }

  final float sloppyFreq() throws IOException {
    ensureFreq();
    return freq;
  }

}

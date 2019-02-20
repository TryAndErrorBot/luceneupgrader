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
package org.trypticon.luceneupgrader.lucene6.internal.lucene.search.spans;


import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.trypticon.luceneupgrader.lucene6.internal.lucene.index.Term;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.index.TermContext;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.search.IndexSearcher;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.search.Query;

public abstract class SpanQuery extends Query {

  public abstract String getField();

  @Override
  public abstract SpanWeight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException;

  public static Map<Term, TermContext> getTermContexts(SpanWeight... weights) {
    Map<Term, TermContext> terms = new TreeMap<>();
    for (SpanWeight w : weights) {
      w.extractTermContexts(terms);
    }
    return terms;
  }

  public static Map<Term, TermContext> getTermContexts(Collection<SpanWeight> weights) {
    Map<Term, TermContext> terms = new TreeMap<>();
    for (SpanWeight w : weights) {
      w.extractTermContexts(terms);
    }
    return terms;
  }
}

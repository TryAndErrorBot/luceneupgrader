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
package org.trypticon.luceneupgrader.lucene4.internal.lucene.search.payloads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.trypticon.luceneupgrader.lucene4.internal.lucene.index.AtomicReaderContext;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.index.IndexReader;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.index.IndexReaderContext;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.index.Term;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.index.TermContext;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.BooleanClause;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.BooleanQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.DisjunctionMaxQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.FilteredQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.MultiPhraseQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.PhraseQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.Query;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.TermQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.spans.SpanNearQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.spans.SpanOrQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.spans.SpanQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.spans.SpanTermQuery;
import org.trypticon.luceneupgrader.lucene4.internal.lucene.search.spans.Spans;

public class PayloadSpanUtil {
  private IndexReaderContext context;

  public PayloadSpanUtil(IndexReaderContext context) {
    this.context = context;
  }

  public Collection<byte[]> getPayloadsForQuery(Query query) throws IOException {
    Collection<byte[]> payloads = new ArrayList<>();
    queryToSpanQuery(query, payloads);
    return payloads;
  }

  private void queryToSpanQuery(Query query, Collection<byte[]> payloads)
      throws IOException {
    if (query instanceof BooleanQuery) {
      BooleanClause[] queryClauses = ((BooleanQuery) query).getClauses();

      for (int i = 0; i < queryClauses.length; i++) {
        if (!queryClauses[i].isProhibited()) {
          queryToSpanQuery(queryClauses[i].getQuery(), payloads);
        }
      }

    } else if (query instanceof PhraseQuery) {
      Term[] phraseQueryTerms = ((PhraseQuery) query).getTerms();
      SpanQuery[] clauses = new SpanQuery[phraseQueryTerms.length];
      for (int i = 0; i < phraseQueryTerms.length; i++) {
        clauses[i] = new SpanTermQuery(phraseQueryTerms[i]);
      }

      int slop = ((PhraseQuery) query).getSlop();
      boolean inorder = false;

      if (slop == 0) {
        inorder = true;
      }

      SpanNearQuery sp = new SpanNearQuery(clauses, slop, inorder);
      sp.setBoost(query.getBoost());
      getPayloads(payloads, sp);
    } else if (query instanceof TermQuery) {
      SpanTermQuery stq = new SpanTermQuery(((TermQuery) query).getTerm());
      stq.setBoost(query.getBoost());
      getPayloads(payloads, stq);
    } else if (query instanceof SpanQuery) {
      getPayloads(payloads, (SpanQuery) query);
    } else if (query instanceof FilteredQuery) {
      queryToSpanQuery(((FilteredQuery) query).getQuery(), payloads);
    } else if (query instanceof DisjunctionMaxQuery) {

      for (Iterator<Query> iterator = ((DisjunctionMaxQuery) query).iterator(); iterator
          .hasNext();) {
        queryToSpanQuery(iterator.next(), payloads);
      }

    } else if (query instanceof MultiPhraseQuery) {
      final MultiPhraseQuery mpq = (MultiPhraseQuery) query;
      final List<Term[]> termArrays = mpq.getTermArrays();
      final int[] positions = mpq.getPositions();
      if (positions.length > 0) {

        int maxPosition = positions[positions.length - 1];
        for (int i = 0; i < positions.length - 1; ++i) {
          if (positions[i] > maxPosition) {
            maxPosition = positions[i];
          }
        }

        @SuppressWarnings({"rawtypes","unchecked"}) final List<Query>[] disjunctLists =
            new List[maxPosition + 1];
        int distinctPositions = 0;

        for (int i = 0; i < termArrays.size(); ++i) {
          final Term[] termArray = termArrays.get(i);
          List<Query> disjuncts = disjunctLists[positions[i]];
          if (disjuncts == null) {
            disjuncts = (disjunctLists[positions[i]] = new ArrayList<>(
                termArray.length));
            ++distinctPositions;
          }
          for (final Term term : termArray) {
            disjuncts.add(new SpanTermQuery(term));
          }
        }

        int positionGaps = 0;
        int position = 0;
        final SpanQuery[] clauses = new SpanQuery[distinctPositions];
        for (int i = 0; i < disjunctLists.length; ++i) {
          List<Query> disjuncts = disjunctLists[i];
          if (disjuncts != null) {
            clauses[position++] = new SpanOrQuery(disjuncts
                .toArray(new SpanQuery[disjuncts.size()]));
          } else {
            ++positionGaps;
          }
        }

        final int slop = mpq.getSlop();
        final boolean inorder = (slop == 0);

        SpanNearQuery sp = new SpanNearQuery(clauses, slop + positionGaps,
            inorder);
        sp.setBoost(query.getBoost());
        getPayloads(payloads, sp);
      }
    }
  }

  private void getPayloads(Collection<byte []> payloads, SpanQuery query)
      throws IOException {
    Map<Term,TermContext> termContexts = new HashMap<>();
    TreeSet<Term> terms = new TreeSet<>();
    query.extractTerms(terms);
    for (Term term : terms) {
      termContexts.put(term, TermContext.build(context, term));
    }
    for (AtomicReaderContext atomicReaderContext : context.leaves()) {
      final Spans spans = query.getSpans(atomicReaderContext, atomicReaderContext.reader().getLiveDocs(), termContexts);
      while (spans.next() == true) {
        if (spans.isPayloadAvailable()) {
          Collection<byte[]> payload = spans.getPayload();
          for (byte [] bytes : payload) {
            payloads.add(bytes);
          }
        }
      }
    }
  }
}

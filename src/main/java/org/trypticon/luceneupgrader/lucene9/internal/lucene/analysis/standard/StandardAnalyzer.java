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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.standard;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.*;

import java.io.IOException;
import java.io.Reader;

/**
 * Filters {@link StandardTokenizer} with {@link LowerCaseFilter} and {@link StopFilter}, using a
 * configurable list of stop words.
 *
 * @since 3.1
 */
public final class StandardAnalyzer extends StopwordAnalyzerBase {


  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;


  public StandardAnalyzer(CharArraySet stopWords) {
    super(stopWords);
  }

  public StandardAnalyzer() {
    this(CharArraySet.EMPTY_SET);
  }


  public StandardAnalyzer(Reader stopwords) throws IOException {
    this(loadStopwordSet(stopwords));
  }


  public void setMaxTokenLength(int length) {
    maxTokenLength = length;
  }

  /**
   * Returns the current maximum token length
   *
   * @see #setMaxTokenLength
   */
  public int getMaxTokenLength() {
    return maxTokenLength;
  }

  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    final StandardTokenizer src = new StandardTokenizer();
    src.setMaxTokenLength(maxTokenLength);
    TokenStream tok = new LowerCaseFilter(src);
    tok = new StopFilter(tok, stopwords);
    return new TokenStreamComponents(
        r -> {
          src.setMaxTokenLength(StandardAnalyzer.this.maxTokenLength);
          src.setReader(r);
        },
        tok);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
}

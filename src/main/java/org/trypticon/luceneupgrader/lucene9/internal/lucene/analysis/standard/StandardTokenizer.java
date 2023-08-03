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


import org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.Tokenizer;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.tokenattributes.CharTermAttribute;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.tokenattributes.OffsetAttribute;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.analysis.tokenattributes.TypeAttribute;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.AttributeFactory;

import java.io.IOException;

/**
 * A grammar-based tokenizer constructed with JFlex.
 *
 * <p>This class implements the Word Break rules from the Unicode Text Segmentation algorithm, as
 * specified in <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>.
 *
 * <p>Many applications have specific tokenizer needs. If this tokenizer does not suit your
 * application, please consider copying this source code directory to your project and maintaining
 * your own grammar-based tokenizer.
 */
public final class StandardTokenizer extends Tokenizer {
  /** A private instance of the JFlex-constructed scanner */
  private StandardTokenizerImpl scanner;

  /** Alpha/numeric token type */
  public static final int ALPHANUM = 0;
  /** Numeric token type */
  public static final int NUM = 1;
  /** Southeast Asian token type */
  public static final int SOUTHEAST_ASIAN = 2;
  /** Ideographic token type */
  public static final int IDEOGRAPHIC = 3;
  /** Hiragana token type */
  public static final int HIRAGANA = 4;
  /** Katakana token type */
  public static final int KATAKANA = 5;
  /** Hangul token type */
  public static final int HANGUL = 6;
  /** Emoji token type. */
  public static final int EMOJI = 7;

  /** String token types that correspond to token type int constants */
  public static final String[] TOKEN_TYPES =
      new String[] {
        "<ALPHANUM>",
        "<NUM>",
        "<SOUTHEAST_ASIAN>",
        "<IDEOGRAPHIC>",
        "<HIRAGANA>",
        "<KATAKANA>",
        "<HANGUL>",
        "<EMOJI>"
      };

  /** Absolute maximum sized token */
  public static final int MAX_TOKEN_LENGTH_LIMIT = 1024 * 1024;

  private int skippedPositions;

  private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;


  public void setMaxTokenLength(int length) {
    if (length < 1) {
      throw new IllegalArgumentException("maxTokenLength must be greater than zero");
    } else if (length > MAX_TOKEN_LENGTH_LIMIT) {
      throw new IllegalArgumentException("maxTokenLength may not exceed " + MAX_TOKEN_LENGTH_LIMIT);
    }
    if (length != maxTokenLength) {
      maxTokenLength = length;
      scanner.setBufferSize(length);
    }
  }


  public int getMaxTokenLength() {
    return maxTokenLength;
  }


  public StandardTokenizer() {
    init();
  }

  public StandardTokenizer(AttributeFactory factory) {
    super(factory);
    init();
  }

  private void init() {
    this.scanner = new StandardTokenizerImpl(input);
  }

  // this tokenizer generates three attributes:
  // term offset, positionIncrement and type
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private final PositionIncrementAttribute posIncrAtt =
      addAttribute(PositionIncrementAttribute.class);
  private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

  /*
   * (non-Javadoc)
   *
   * @see org.apache.lucene.analysis.TokenStream#next()
   */
  @Override
  public final boolean incrementToken() throws IOException {
    clearAttributes();
    skippedPositions = 0;

    while (true) {
      int tokenType = scanner.getNextToken();

      if (tokenType == StandardTokenizerImpl.YYEOF) {
        return false;
      }

      if (scanner.yylength() <= maxTokenLength) {
        posIncrAtt.setPositionIncrement(skippedPositions + 1);
        scanner.getText(termAtt);
        final int start = scanner.yychar();
        offsetAtt.setOffset(correctOffset(start), correctOffset(start + termAtt.length()));
        typeAtt.setType(StandardTokenizer.TOKEN_TYPES[tokenType]);
        return true;
      } else
        // When we skip a too-long term, we still increment the
        // position increment
        skippedPositions++;
    }
  }

  @Override
  public final void end() throws IOException {
    super.end();
    // set final offset
    int finalOffset = correctOffset(scanner.yychar() + scanner.yylength());
    offsetAtt.setOffset(finalOffset, finalOffset);
    // adjust any skipped tokens
    posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
  }

  @Override
  public void close() throws IOException {
    super.close();
    scanner.yyreset(input);
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    scanner.yyreset(input);
    skippedPositions = 0;
  }
}

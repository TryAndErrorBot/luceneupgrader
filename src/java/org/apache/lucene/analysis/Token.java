package org.apache.lucene.analysis;

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

import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.index.Payload;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

/** 
  A Token is an occurrence of a term from the text of a field.  It consists of
  a term's text, the start and end offset of the term in the text of the field,
  and a type string.
  <p>
  The start and end offsets permit applications to re-associate a token with
  its source text, e.g., to display highlighted query terms in a document
  browser, or to show matching text fragments in a <abbr title="KeyWord In Context">KWIC</abbr>
  display, etc.
  <p>
  The type is a string, assigned by a lexical analyzer
  (a.k.a. tokenizer), naming the lexical or syntactic class that the token
  belongs to.  For example an end of sentence marker token might be implemented
  with type "eos".  The default token type is "word".  
  <p>
  A Token can optionally have metadata (a.k.a. Payload) in the form of a variable
  length byte array. Use {@code TermPositions#getPayloadLength()} and
  {@code TermPositions#getPayload(byte[], int)} to retrieve the payloads from the index.
  
  <br><br>
  
  <p><b>NOTE:</b> As of 2.9, Token implements all {@code Attribute} interfaces
  that are part of core Lucene and can be found in the {@code tokenattributes} subpackage.
  Even though it is not necessary to use Token anymore, with the new TokenStream API it can
  be used as convenience class that implements all {@code Attribute}s, which is especially useful
  to easily switch from the old to the new TokenStream API.
  
  <br><br>
  
  <p>Tokenizers and TokenFilters should try to re-use a Token
  instance when possible for best performance, by
  implementing the {@code TokenStream#incrementToken()} API.
  Failing that, to create a new Token you should first use
  one of the constructors that starts with null text.  To load
  the token from a char[] use {@code #copyBuffer(char[], int, int)}.
  To load from a String use {@code #setEmpty} followed by {@code #append(CharSequence)} or {@code #append(CharSequence, int, int)}.
  Alternatively you can get the Token's termBuffer by calling either {@code #buffer()},
  if you know that your text is shorter than the capacity of the termBuffer
  or {@code #resizeBuffer(int)}, if there is any possibility
  that you may need to grow the buffer. Fill in the characters of your term into this
  buffer, with {@code String#getChars(int, int, char[], int)} if loading from a string,
  or with {@code System#arraycopy(Object, int, Object, int, int)}, and finally call {@code #setLength(int)} to
  set the length of the term text.  See <a target="_top"
  href="https://issues.apache.org/jira/browse/LUCENE-969">LUCENE-969</a>
  for details.</p>
  <p>Typical Token reuse patterns:
  <ul>
  <li> Copying text from a string (type is reset to {@code #DEFAULT_TYPE} if not specified):<br/>
  <pre>
    return reusableToken.reinit(string, startOffset, endOffset[, type]);
  </pre>
  </li>
  <li> Copying some text from a string (type is reset to {@code #DEFAULT_TYPE} if not specified):<br/>
  <pre>
    return reusableToken.reinit(string, 0, string.length(), startOffset, endOffset[, type]);
  </pre>
  </li>
  </li>
  <li> Copying text from char[] buffer (type is reset to {@code #DEFAULT_TYPE} if not specified):<br/>
  <pre>
    return reusableToken.reinit(buffer, 0, buffer.length, startOffset, endOffset[, type]);
  </pre>
  </li>
  <li> Copying some text from a char[] buffer (type is reset to {@code #DEFAULT_TYPE} if not specified):<br/>
  <pre>
    return reusableToken.reinit(buffer, start, end - start, startOffset, endOffset[, type]);
  </pre>
  </li>
  <li> Copying from one one Token to another (type is reset to {@code #DEFAULT_TYPE} if not specified):<br/>
  <pre>
    return reusableToken.reinit(source.buffer(), 0, source.length(), source.startOffset(), source.endOffset()[, source.type()]);
  </pre>
  </li>
  </ul>
  A few things to note:
  <ul>
  <li>clear() initializes all of the fields to default values. This was changed in contrast to Lucene 2.4, but should affect no one.</li>
  <li>Because <code>TokenStreams</code> can be chained, one cannot assume that the <code>Token's</code> current type is correct.</li>
  <li>The startOffset and endOffset represent the start and offset in the source text, so be careful in adjusting them.</li>
  <li>When caching a reusable token, clone it. When injecting a cached token into a stream that can be reset, clone it again.</li>
  </ul>
  </p>
  <p>
  <b>Please note:</b> With Lucene 3.1, the <code>{@codeplain #toString toString()}</code> method had to be changed to match the
  {@code CharSequence} interface introduced by the interface {@code org.apache.lucene.analysis.tokenattributes.CharTermAttribute}.
  This method now only prints the term text, no additional information anymore.
  </p>

*/
// TODO: change superclass to CharTermAttribute in 4.0! Maybe deprecate the whole class?
public class Token extends TermAttributeImpl 
                   implements TypeAttribute, PositionIncrementAttribute,
                              FlagsAttribute, OffsetAttribute, PayloadAttribute, PositionLengthAttribute {

  private int startOffset,endOffset;
  private String type = DEFAULT_TYPE;
  private int flags;
  private Payload payload;
  private int positionIncrement = 1;
  private int positionLength = 1;

  /** Constructs a Token will null text. */
  public Token() {
  }

  /** Constructs a Token with null text and start & end
   *  offsets.
   *  @param start start offset in the source text
   *  @param end end offset in the source text */
  public Token(int start, int end) {
    startOffset = start;
    endOffset = end;
  }

  /** Constructs a Token with null text and start & end
   *  offsets plus the Token type.
   *  @param start start offset in the source text
   *  @param end end offset in the source text
   *  @param typ the lexical type of this Token */
  public Token(int start, int end, String typ) {
    startOffset = start;
    endOffset = end;
    type = typ;
  }

  /**
   * Constructs a Token with null text and start & end
   *  offsets plus flags. NOTE: flags is EXPERIMENTAL.
   *  @param start start offset in the source text
   *  @param end end offset in the source text
   *  @param flags The bits to set for this token
   */
  public Token(int start, int end, int flags) {
    startOffset = start;
    endOffset = end;
    this.flags = flags;
  }

  /** Constructs a Token with the given term text, and start
   *  & end offsets.  The type defaults to "word."
   *  <b>NOTE:</b> for better indexing speed you should
   *  instead use the char[] termBuffer methods to set the
   *  term text.
   *  @param text term text
   *  @param start start offset
   *  @param end end offset
   */
  public Token(String text, int start, int end) {
    append(text);
    startOffset = start;
    endOffset = end;
  }

  /** Constructs a Token with the given text, start and end
   *  offsets, & type.  <b>NOTE:</b> for better indexing
   *  speed you should instead use the char[] termBuffer
   *  methods to set the term text.
   *  @param text term text
   *  @param start start offset
   *  @param end end offset
   *  @param typ token type
   */
  public Token(String text, int start, int end, String typ) {
    append(text);
    startOffset = start;
    endOffset = end;
    type = typ;
  }

  /**
   *  Constructs a Token with the given text, start and end
   *  offsets, & type.  <b>NOTE:</b> for better indexing
   *  speed you should instead use the char[] termBuffer
   *  methods to set the term text.
   * @param text
   * @param start
   * @param end
   * @param flags token type bits
   */
  public Token(String text, int start, int end, int flags) {
    append(text);
    startOffset = start;
    endOffset = end;
    this.flags = flags;
  }

  /**
   *  Constructs a Token with the given term buffer (offset
   *  & length), start and end
   *  offsets
   * @param startTermBuffer
   * @param termBufferOffset
   * @param termBufferLength
   * @param start
   * @param end
   */
  public Token(char[] startTermBuffer, int termBufferOffset, int termBufferLength, int start, int end) {
    copyBuffer(startTermBuffer, termBufferOffset, termBufferLength);
    startOffset = start;
    endOffset = end;
  }

  /** Set the position increment.  This determines the position of this token
   * relative to the previous Token in a {@code TokenStream}, used in phrase
   * searching.
   *
   * <p>The default value is one.
   *
   * <p>Some common uses for this are:<ul>
   *
   * <li>Set it to zero to put multiple terms in the same position.  This is
   * useful if, e.g., a word has multiple stems.  Searches for phrases
   * including either stem will match.  In this case, all but the first stem's
   * increment should be set to zero: the increment of the first instance
   * should be one.  Repeating a token with an increment of zero can also be
   * used to boost the scores of matches on that token.
   *
   * <li>Set it to values greater than one to inhibit exact phrase matches.
   * If, for example, one does not want phrases to match across removed stop
   * words, then one could build a stop word filter that removes stop words and
   * also sets the increment to the number of stop words removed before each
   * non-stop word.  Then exact phrase queries will only match when the terms
   * occur with no intervening stop words.
   *
   * </ul>
   * @param positionIncrement the distance from the prior term
   *
   */
  public void setPositionIncrement(int positionIncrement) {
    if (positionIncrement < 0)
      throw new IllegalArgumentException
        ("Increment must be zero or greater: " + positionIncrement);
    this.positionIncrement = positionIncrement;
  }

  /** Returns the position increment of this Token.
   *
   */
  public int getPositionIncrement() {
    return positionIncrement;
  }

  /** Set the position length.
   * */
  public void setPositionLength(int positionLength) {
    this.positionLength = positionLength;
  }

  /** Get the position length.
   * */
  public int getPositionLength() {
    return positionLength;
  }

  /** Returns this Token's starting offset, the position of the first character
    corresponding to this token in the source text.

    Note that the difference between endOffset() and startOffset() may not be
    equal to {@code #length}, as the term text may have been altered by a
    stemmer or some other filter. */
  public final int startOffset() {
    return startOffset;
  }

  /** Returns this Token's ending offset, one greater than the position of the
    last character corresponding to this token in the source text. The length
    of the token in the source text is (endOffset - startOffset). */
  public final int endOffset() {
    return endOffset;
  }

  /** Set the starting and ending offset.
  */
  public void setOffset(int startOffset, int endOffset) {
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  /** Returns this Token's lexical type.  Defaults to "word". */
  public final String type() {
    return type;
  }

  /** Set the lexical type.
      */
  public final void setType(String type) {
    this.type = type;
  }

  /**
   * <p/>
   *
   * Get the bitset for any bits that have been set.  This is completely distinct from {@code #type()}, although they do share similar purposes.
   * The flags can be used to encode information about the token for use by other {@code org.apache.lucene.analysis.TokenFilter}s.
   *
   * 
   * @return The bits
   * @lucene.experimental While we think this is here to stay, we may want to change it to be a long.
   */
  public int getFlags() {
    return flags;
  }

  /**
   *
   */
  public void setFlags(int flags) {
    this.flags = flags;
  }

  /**
   * Returns this Token's payload.
   */ 
  public Payload getPayload() {
    return this.payload;
  }

  /** 
   * Sets this Token's payload.
   */
  public void setPayload(Payload payload) {
    this.payload = payload;
  }
  
  /** Resets the term text, payload, flags, and positionIncrement,
   * startOffset, endOffset and token type to default.
   */
  @Override
  public void clear() {
    super.clear();
    payload = null;
    positionIncrement = 1;
    flags = 0;
    startOffset = endOffset = 0;
    type = DEFAULT_TYPE;
  }

  @Override
  public Object clone() {
    Token t = (Token)super.clone();
    // Do a deep clone
    if (payload != null) {
      t.payload = (Payload) payload.clone();
    }
    return t;
  }

  /** Makes a clone, but replaces the term buffer &
   * start/end offset in the process.  This is more
   * efficient than doing a full clone (and then calling
   * {@code #copyBuffer}) because it saves a wasted copy of the old
   * termBuffer. */
  public Token clone(char[] newTermBuffer, int newTermOffset, int newTermLength, int newStartOffset, int newEndOffset) {
    final Token t = new Token(newTermBuffer, newTermOffset, newTermLength, newStartOffset, newEndOffset);
    t.positionIncrement = positionIncrement;
    t.flags = flags;
    t.type = type;
    if (payload != null)
      t.payload = (Payload) payload.clone();
    return t;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;

    if (obj instanceof Token) {
      final Token other = (Token) obj;
      return (startOffset == other.startOffset &&
          endOffset == other.endOffset && 
          flags == other.flags &&
          positionIncrement == other.positionIncrement &&
          (type == null ? other.type == null : type.equals(other.type)) &&
          (payload == null ? other.payload == null : payload.equals(other.payload)) &&
          super.equals(obj)
      );
    } else
      return false;
  }

  @Override
  public int hashCode() {
    int code = super.hashCode();
    code = code * 31 + startOffset;
    code = code * 31 + endOffset;
    code = code * 31 + flags;
    code = code * 31 + positionIncrement;
    if (type != null)
      code = code * 31 + type.hashCode();
    if (payload != null)
      code = code * 31 + payload.hashCode();
    return code;
  }

  /**
   * Copy the prototype token's fields into this one. Note: Payloads are shared.
   * @param prototype
   */
  public void reinit(Token prototype) {
    copyBuffer(prototype.buffer(), 0, prototype.length());
    positionIncrement = prototype.positionIncrement;
    flags = prototype.flags;
    startOffset = prototype.startOffset;
    endOffset = prototype.endOffset;
    type = prototype.type;
    payload =  prototype.payload;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    if (target instanceof Token) {
      final Token to = (Token) target;
      to.reinit(this);
      // reinit shares the payload, so clone it:
      if (payload !=null) {
        to.payload = (Payload) payload.clone();
      }
    } else {
      super.copyTo(target);
      ((OffsetAttribute) target).setOffset(startOffset, endOffset);
      ((PositionIncrementAttribute) target).setPositionIncrement(positionIncrement);
      ((PayloadAttribute) target).setPayload((payload == null) ? null : (Payload) payload.clone());
      ((FlagsAttribute) target).setFlags(flags);
      ((TypeAttribute) target).setType(type);
    }
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    super.reflectWith(reflector);
    reflector.reflect(OffsetAttribute.class, "startOffset", startOffset);
    reflector.reflect(OffsetAttribute.class, "endOffset", endOffset);
    reflector.reflect(PositionIncrementAttribute.class, "positionIncrement", positionIncrement);
    reflector.reflect(PayloadAttribute.class, "payload", payload);
    reflector.reflect(FlagsAttribute.class, "flags", flags);
    reflector.reflect(TypeAttribute.class, "type", type);
  }

}

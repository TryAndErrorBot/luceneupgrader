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

import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.IndexOptions;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.BytesRef;

/**
 * A field that is indexed but not tokenized: the entire String value is indexed as a single token.
 * For example this might be used for a 'country' field or an 'id' field. If you also need to sort
 * on this field, separately add a {@link SortedDocValuesField} to your document.
 */
public final class StringField extends Field {

  /** Indexed, not tokenized, omits norms, indexes DOCS_ONLY, not stored. */
  public static final FieldType TYPE_NOT_STORED = new FieldType();

  /** Indexed, not tokenized, omits norms, indexes DOCS_ONLY, stored */
  public static final FieldType TYPE_STORED = new FieldType();

  static {
    TYPE_NOT_STORED.setOmitNorms(true);
    TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS);
    TYPE_NOT_STORED.setTokenized(false);
    TYPE_NOT_STORED.freeze();

    TYPE_STORED.setOmitNorms(true);
    TYPE_STORED.setIndexOptions(IndexOptions.DOCS);
    TYPE_STORED.setStored(true);
    TYPE_STORED.setTokenized(false);
    TYPE_STORED.freeze();
  }

  private BytesRef binaryValue;
  private final StoredValue storedValue;

  /**
   * Creates a new textual StringField, indexing the provided String value as a single token.
   *
   * @param name field name
   * @param value String value
   * @param stored Store.YES if the content should also be stored
   * @throws IllegalArgumentException if the field name or value is null.
   */
  public StringField(String name, String value, Store stored) {
    super(name, value, stored == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
    binaryValue = new BytesRef(value);
    if (stored == Store.YES) {
      storedValue = new StoredValue(value);
    } else {
      storedValue = null;
    }
  }

  /**
   * Creates a new binary StringField, indexing the provided binary (BytesRef) value as a single
   * token.
   *
   * @param name field name
   * @param value BytesRef value. The provided value is not cloned so you must not change it until
   *     the document(s) holding it have been indexed.
   * @param stored Store.YES if the content should also be stored
   * @throws IllegalArgumentException if the field name or value is null.
   */
  public StringField(String name, BytesRef value, Store stored) {
    super(name, value, stored == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
    binaryValue = value;
    if (stored == Store.YES) {
      storedValue = new StoredValue(value);
    } else {
      storedValue = null;
    }
  }

  @Override
  public InvertableType invertableType() {
    return InvertableType.BINARY;
  }

  @Override
  public BytesRef binaryValue() {
    return binaryValue;
  }

  @Override
  public void setStringValue(String value) {
    super.setStringValue(value);
    binaryValue = new BytesRef(value);
    if (storedValue != null) {
      storedValue.setStringValue(value);
    }
  }

  @Override
  public void setBytesValue(BytesRef value) {
    super.setBytesValue(value);
    binaryValue = value;
    if (storedValue != null) {
      storedValue.setBinaryValue(value);
    }
  }

  @Override
  public StoredValue storedValue() {
    return storedValue;
  }
}

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

package org.trypticon.luceneupgrader.lucene8.internal.lucene.index;

import java.io.IOException;
import java.util.Objects;

public abstract class FilterNumericDocValues extends NumericDocValues {

  protected final NumericDocValues in;
  
  protected FilterNumericDocValues(NumericDocValues in) {
    Objects.requireNonNull(in);
    this.in = in;
  }

  @Override
  public int docID() {
    return in.docID();
  }
  
  @Override
  public int nextDoc() throws IOException {
    return in.nextDoc();
  }

  @Override
  public int advance(int target) throws IOException {
    return in.advance(target);
  }
  
  @Override
  public boolean advanceExact(int target) throws IOException {
    return in.advanceExact(target);
  }
  
  @Override
  public long cost() {
    return in.cost();
  }

  @Override
  public long longValue() throws IOException {
    return in.longValue();
  }
}

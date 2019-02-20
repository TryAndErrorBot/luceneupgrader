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
package org.trypticon.luceneupgrader.lucene3.internal.lucene.search.function;

import org.trypticon.luceneupgrader.lucene3.internal.lucene.index.IndexReader;
import org.trypticon.luceneupgrader.lucene3.internal.lucene.search.FieldCache;

import java.io.IOException;

public class ShortFieldSource extends FieldCacheSource {
  private FieldCache.ShortParser parser;

  public ShortFieldSource(String field) {
    this(field, null);
  }

  public ShortFieldSource(String field, FieldCache.ShortParser parser) {
    super(field);
    this.parser = parser;
  }

  @Override
  public String description() {
    return "short(" + super.description() + ')';
  }

  @Override
  public DocValues getCachedFieldValues (FieldCache cache, String field, IndexReader reader) throws IOException {
    final short[] arr = cache.getShorts(reader, field, parser);
    return new DocValues() {
      @Override
      public float floatVal(int doc) { 
        return arr[doc];
      }
      @Override
      public  int intVal(int doc) { 
        return arr[doc]; 
      }
      @Override
      public String toString(int doc) { 
        return  description() + '=' + intVal(doc);  
      }
      @Override
      Object getInnerArray() {
        return arr;
      }
    };
  }

  @Override
  public boolean cachedFieldSourceEquals(FieldCacheSource o) {
    if (o.getClass() !=  ShortFieldSource.class) {
      return false;
    }
    ShortFieldSource other = (ShortFieldSource)o;
    return this.parser==null ? 
      other.parser==null :
      this.parser.getClass() == other.parser.getClass();
  }

  @Override
  public int cachedFieldSourceHashCode() {
    return parser==null ? 
      Short.class.hashCode() : parser.getClass().hashCode();
  }

}

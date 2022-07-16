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
package org.trypticon.luceneupgrader.lucene8.internal.lucene.search.similarities;


import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.Explanation;

public abstract class Normalization {
  
  public Normalization() {}

  public abstract double tfn(BasicStats stats, double tf, double len);
  
  public Explanation explain(BasicStats stats, double tf, double len) {
    return Explanation.match(
        (float) tfn(stats, tf, len),
        getClass().getSimpleName() + ", computed from:",
        Explanation.match((float) tf,
            "tf, number of occurrences of term in the document"),
        Explanation.match((float) stats.getAvgFieldLength(),
            "avgfl, average length of field across all documents"),
        Explanation.match((float) len, "fl, field length of the document"));
  }

  public static final class NoNormalization extends Normalization {
    
    public NoNormalization() {}
    
    @Override
    public double tfn(BasicStats stats, double tf, double len) {
      return tf;
    }

    @Override
    public Explanation explain(BasicStats stats, double tf, double len) {
      return Explanation.match(1, "no normalization");
    }
    
    @Override
    public String toString() {
      return "";
    }
  }
  
  @Override
  public abstract String toString();
}

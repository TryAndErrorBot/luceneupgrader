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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.search.similarities;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.search.Explanation;

/**
 * This class acts as the base class for the specific <em>basic model</em> implementations in the
 * DFR framework. Basic models compute the <em>informative content Inf<sub>1</sub> =
 * -log<sub>2</sub>Prob<sub>1</sub> </em>.
 *
 * @see DFRSimilarity
 * @lucene.experimental
 */
public abstract class BasicModel {

  /** Sole constructor. (For invocation by subclass constructors, typically implicit.) */
  public BasicModel() {}

  /**
   * Returns the informative content score combined with the after effect, more specifically {@code
   * informationContentScore * aeTimes1pTfn / (1 + tfn)}. This function must be non-decreasing with
   * {@code tfn}.
   */
  public abstract double score(BasicStats stats, double tfn, double aeTimes1pTfn);

  /** Returns an explanation for the score. Subclasses must override this method. */
  public abstract Explanation explain(BasicStats stats, double tfn, double aeTimes1pTfn);

  /**
   * Subclasses must override this method to return the code of the basic model formula. Refer to
   * the original paper for the list.
   */
  @Override
  public abstract String toString();
}

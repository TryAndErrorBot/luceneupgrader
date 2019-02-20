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
package org.trypticon.luceneupgrader.lucene5.internal.lucene.util.packed;


import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.trypticon.luceneupgrader.lucene5.internal.lucene.store.IndexInput;
import org.trypticon.luceneupgrader.lucene5.internal.lucene.store.RandomAccessInput;
import org.trypticon.luceneupgrader.lucene5.internal.lucene.util.Accountable;
import org.trypticon.luceneupgrader.lucene5.internal.lucene.util.LongValues;
import org.trypticon.luceneupgrader.lucene5.internal.lucene.util.RamUsageEstimator;

public final class DirectMonotonicReader {

  private static final LongValues EMPTY = new LongValues() {

    @Override
    public long get(long index) {
      return 0;
    }

  };

  public static class Meta implements Accountable {
    private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(Meta.class);

    final long numValues;
    final int blockShift;
    final int numBlocks;
    final long[] mins;
    final float[] avgs;
    final byte[] bpvs;
    final long[] offsets;

    Meta(long numValues, int blockShift) {
      this.numValues = numValues;
      this.blockShift = blockShift;
      long numBlocks = numValues >>> blockShift;
      if ((numBlocks << blockShift) < numValues) {
        numBlocks += 1;
      }
      this.numBlocks = (int) numBlocks;
      this.mins = new long[this.numBlocks];
      this.avgs = new float[this.numBlocks];
      this.bpvs = new byte[this.numBlocks];
      this.offsets = new long[this.numBlocks];
    }

    @Override
    public long ramBytesUsed() {
      return BASE_RAM_BYTES_USED
          + RamUsageEstimator.sizeOf(mins)
          + RamUsageEstimator.sizeOf(avgs)
          + RamUsageEstimator.sizeOf(bpvs)
          + RamUsageEstimator.sizeOf(offsets);
    }

    @Override
    public Collection<Accountable> getChildResources() {
      return Collections.emptyList();
    }
  }

  public static Meta loadMeta(IndexInput metaIn, long numValues, int blockShift) throws IOException {
    Meta meta = new Meta(numValues, blockShift);
    for (int i = 0; i < meta.numBlocks; ++i) {
      meta.mins[i] = metaIn.readLong();
      meta.avgs[i] = Float.intBitsToFloat(metaIn.readInt());
      meta.offsets[i] = metaIn.readLong();
      meta.bpvs[i] = metaIn.readByte();
    }
    return meta;
  }

  public static LongValues getInstance(Meta meta, RandomAccessInput data) throws IOException {
    final LongValues[] readers = new LongValues[meta.numBlocks];
    for (int i = 0; i < meta.mins.length; ++i) {
      if (meta.bpvs[i] == 0) {
        readers[i] = EMPTY;
      } else {
        readers[i] = DirectReader.getInstance(data, meta.bpvs[i], meta.offsets[i]);
      }
    }
    final int blockShift = meta.blockShift;

    final long[] mins = meta.mins;
    final float[] avgs = meta.avgs;
    return new LongValues() {

      @Override
      public long get(long index) {
        final int block = (int) (index >>> blockShift);
        final long blockIndex = index & ((1 << blockShift) - 1);
        final long delta = readers[block].get(blockIndex);
        return mins[block] + (long) (avgs[block] * blockIndex) + delta;
      }

    };
  }
}

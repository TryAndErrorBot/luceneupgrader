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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.codecs.lucene80;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.search.DocIdSetIterator;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.store.IndexInput;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.store.IndexOutput;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.store.RandomAccessInput;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.ArrayUtil;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.BitSetIterator;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.FixedBitSet;

import java.io.IOException;

final class IndexedDISI extends DocIdSetIterator {

  // jump-table time/space trade-offs to consider:
  // The block offsets and the block indexes could be stored in more compressed form with
  // two PackedInts or two MonotonicDirectReaders.
  // The DENSE ranks (default 128 shorts = 256 bytes) could likewise be compressed. But as there is
  // at least 4096 set bits in DENSE blocks, there will be at least one rank with 2^12 bits, so it
  // is doubtful if there is much to gain here.
  
  private static final int BLOCK_SIZE = 65536;   // The number of docIDs that a single block represents

  private static final int DENSE_BLOCK_LONGS = BLOCK_SIZE/Long.SIZE; // 1024
  public static final byte DEFAULT_DENSE_RANK_POWER = 9; // Every 512 docIDs / 8 longs

  static final int MAX_ARRAY_LENGTH = (1 << 12) - 1;

  private static void flush(
      int block, FixedBitSet buffer, int cardinality, byte denseRankPower, IndexOutput out) throws IOException {
    assert block >= 0 && block < 65536;
    out.writeShort((short) block);
    assert cardinality > 0 && cardinality <= 65536;
    out.writeShort((short) (cardinality - 1));
    if (cardinality > MAX_ARRAY_LENGTH) {
      if (cardinality != 65536) { // all docs are set
        if (denseRankPower != -1) {
          final byte[] rank = createRank(buffer, denseRankPower);
          out.writeBytes(rank, rank.length);
        }
        for (long word : buffer.getBits()) {
          out.writeLong(word);
        }
      }
    } else {
      BitSetIterator it = new BitSetIterator(buffer, cardinality);
      for (int doc = it.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = it.nextDoc()) {
        out.writeShort((short) doc);
      }
    }
  }

  // Creates a DENSE rank-entry (the number of set bits up to a given point) for the buffer.
  // One rank-entry for every {@code 2^denseRankPower} bits, with each rank-entry using 2 bytes.
  // Represented as a byte[] for fast flushing and mirroring of the retrieval representation.
  private static byte[] createRank(FixedBitSet buffer, byte denseRankPower) {
    final int longsPerRank = 1 << (denseRankPower-6);
    final int rankMark = longsPerRank-1;
    final int rankIndexShift = denseRankPower-7; // 6 for the long (2^6) + 1 for 2 bytes/entry
    final byte[] rank = new byte[DENSE_BLOCK_LONGS >> rankIndexShift];
    final long[] bits = buffer.getBits();
    int bitCount = 0;
    for (int word = 0 ; word < DENSE_BLOCK_LONGS ; word++) {
      if ((word & rankMark) == 0) { // Every longsPerRank longs
        rank[word >> rankIndexShift] = (byte)(bitCount>>8);
        rank[(word >> rankIndexShift)+1] = (byte)(bitCount & 0xFF);
      }
      bitCount += Long.bitCount(bits[word]);
    }
    return rank;
  }

  static short writeBitSet(DocIdSetIterator it, IndexOutput out) throws IOException {
    return writeBitSet(it, out, DEFAULT_DENSE_RANK_POWER);
  }

  static short writeBitSet(DocIdSetIterator it, IndexOutput out, byte denseRankPower) throws IOException {
    final long origo = out.getFilePointer(); // All jumps are relative to the origo
    if ((denseRankPower < 7 || denseRankPower > 15) && denseRankPower != -1) {
      throw new IllegalArgumentException("Acceptable values for denseRankPower are 7-15 (every 128-32768 docIDs). " +
          "The provided power was " + denseRankPower + " (every " + (int)Math.pow(2, denseRankPower) + " docIDs)");
    }
    int totalCardinality = 0;
    int blockCardinality = 0;
    final FixedBitSet buffer = new FixedBitSet(1<<16);
    int[] jumps = new int[ArrayUtil.oversize(1, Integer.BYTES*2)];
    int prevBlock = -1;
    int jumpBlockIndex = 0;

    for (int doc = it.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = it.nextDoc()) {
      final int block = doc >>> 16;
      if (prevBlock != -1 && block != prevBlock) {
        // Track offset+index from previous block up to current
        jumps = addJumps(jumps, out.getFilePointer()-origo, totalCardinality, jumpBlockIndex, prevBlock+1);
        jumpBlockIndex = prevBlock+1;
        // Flush block
        flush(prevBlock, buffer, blockCardinality, denseRankPower, out);
        // Reset for next block
        buffer.clear(0, buffer.length());
        totalCardinality += blockCardinality;
        blockCardinality = 0;
      }
      buffer.set(doc & 0xFFFF);
      blockCardinality++;
      prevBlock = block;
    }
    if (blockCardinality > 0) {
      jumps = addJumps(jumps, out.getFilePointer()-origo, totalCardinality, jumpBlockIndex, prevBlock+1);
      totalCardinality += blockCardinality;
      flush(prevBlock, buffer, blockCardinality, denseRankPower, out);
      buffer.clear(0, buffer.length());
      prevBlock++;
    }
    final int lastBlock = prevBlock == -1 ? 0 : prevBlock; // There will always be at least 1 block (NO_MORE_DOCS)
    // Last entry is a SPARSE with blockIndex == 32767 and the single entry 65535, which becomes the docID NO_MORE_DOCS
    // To avoid creating 65K jump-table entries, only a single entry is created pointing to the offset of the
    // NO_MORE_DOCS block, with the jumpBlockIndex set to the logical EMPTY block after all real blocks.
    jumps = addJumps(jumps, out.getFilePointer()-origo, totalCardinality, lastBlock, lastBlock+1);
    buffer.set(DocIdSetIterator.NO_MORE_DOCS & 0xFFFF);
    flush(DocIdSetIterator.NO_MORE_DOCS >>> 16, buffer, 1, denseRankPower, out);
    // offset+index jump-table stored at the end
    return flushBlockJumps(jumps, lastBlock+1, out, origo);
  }

  // Adds entries to the offset & index jump-table for blocks
  private static int[] addJumps(int[] jumps, long offset, int index, int startBlock, int endBlock) {
    assert offset < Integer.MAX_VALUE : "Logically the offset should not exceed 2^30 but was >= Integer.MAX_VALUE";
    jumps = ArrayUtil.grow(jumps, (endBlock+1)*2);
    for (int b = startBlock; b < endBlock; b++) {
      jumps[b*2] = index;
      jumps[b*2+1] = (int) offset;
    }
    return jumps;
  }

  // Flushes the offset & index jump-table for blocks. This should be the last data written to out
  // This method returns the blockCount for the blocks reachable for the jump_table or -1 for no jump-table
  private static short flushBlockJumps(int[] jumps, int blockCount, IndexOutput out, long origo) throws IOException {
    if (blockCount == 2) { // Jumps with a single real entry + NO_MORE_DOCS is just wasted space so we ignore that
      blockCount = 0;
    }
    for (int i = 0 ; i < blockCount ; i++) {
      out.writeInt(jumps[i*2]); // index
      out.writeInt(jumps[i*2+1]); // offset
    }
    // As there are at most 32k blocks, the count is a short
    // The jumpTableOffset will be at lastPos - (blockCount * Long.BYTES)
    return (short)blockCount;
  }

  // Members are pkg-private to avoid synthetic accessors when accessed from the `Method` enum

  final IndexInput slice;
  final int jumpTableEntryCount;
  final byte denseRankPower;
  final RandomAccessInput jumpTable; // Skip blocks of 64K bits
  final byte[] denseRankTable;
  final long cost;

  IndexedDISI(IndexInput in, long offset, long length, int jumpTableEntryCount, byte denseRankPower, long cost) throws IOException {
    this(createBlockSlice(in,"docs", offset, length, jumpTableEntryCount),
        createJumpTable(in, offset, length, jumpTableEntryCount),
        jumpTableEntryCount, denseRankPower, cost);
  }

  IndexedDISI(IndexInput blockSlice, RandomAccessInput jumpTable, int jumpTableEntryCount, byte denseRankPower, long cost) throws IOException {
    if ((denseRankPower < 7 || denseRankPower > 15) && denseRankPower != -1) {
      throw new IllegalArgumentException("Acceptable values for denseRankPower are 7-15 (every 128-32768 docIDs). " +
          "The provided power was " + denseRankPower + " (every " + (int)Math.pow(2, denseRankPower) + " docIDs). ");
    }

    this.slice = blockSlice;
    this.jumpTable = jumpTable;
    this.jumpTableEntryCount = jumpTableEntryCount;
    this.denseRankPower = denseRankPower;
    final int rankIndexShift = denseRankPower-7;
    this.denseRankTable = denseRankPower == -1 ? null : new byte[DENSE_BLOCK_LONGS >> rankIndexShift];
    this.cost = cost;
  }

  public static IndexInput createBlockSlice(
      IndexInput slice, String sliceDescription, long offset, long length, int jumpTableEntryCount) throws IOException {
    long jumpTableBytes = jumpTableEntryCount < 0 ? 0 : jumpTableEntryCount*Integer.BYTES*2;
    return slice.slice(sliceDescription, offset, length - jumpTableBytes);
  }

  public static RandomAccessInput createJumpTable(
      IndexInput slice, long offset, long length, int jumpTableEntryCount) throws IOException {
    if (jumpTableEntryCount <= 0) {
      return null;
    } else {
      int jumpTableBytes = jumpTableEntryCount*Integer.BYTES*2;
      return slice.randomAccessSlice(offset + length - jumpTableBytes, jumpTableBytes);
    }
  }

  int block = -1;
  long blockEnd;
  long denseBitmapOffset = -1; // Only used for DENSE blocks
  int nextBlockIndex = -1;
  Method method;

  int doc = -1;
  int index = -1;

  // SPARSE variables
  boolean exists;

  // DENSE variables
  long word;
  int wordIndex = -1;
  // number of one bits encountered so far, including those of `word`
  int numberOfOnes;
  // Used with rank for jumps inside of DENSE as they are absolute instead of relative
  int denseOrigoIndex;

  // ALL variables
  int gap;

  @Override
  public int docID() {
    return doc;
  }

  @Override
  public int advance(int target) throws IOException {
    final int targetBlock = target & 0xFFFF0000;
    if (block < targetBlock) {
      advanceBlock(targetBlock);
    }
    if (block == targetBlock) {
      if (method.advanceWithinBlock(this, target)) {
        return doc;
      }
      readBlockHeader();
    }
    boolean found = method.advanceWithinBlock(this, block);
    assert found;
    return doc;
  }

  public boolean advanceExact(int target) throws IOException {
    final int targetBlock = target & 0xFFFF0000;
    if (block < targetBlock) {
      advanceBlock(targetBlock);
    }
    boolean found = block == targetBlock && method.advanceExactWithinBlock(this, target);
    this.doc = target;
    return found;
  }

  private void advanceBlock(int targetBlock) throws IOException {
    final int blockIndex = targetBlock >> 16;
    // If the destination block is 2 blocks or more ahead, we use the jump-table.
    if (jumpTable != null && blockIndex >= (block >> 16)+2) {
      // If the jumpTableEntryCount is exceeded, there are no further bits. Last entry is always NO_MORE_DOCS
      final int inRangeBlockIndex = blockIndex < jumpTableEntryCount ? blockIndex : jumpTableEntryCount-1;
      final int index = jumpTable.readInt(inRangeBlockIndex*Integer.BYTES*2);
      final int offset = jumpTable.readInt(inRangeBlockIndex*Integer.BYTES*2+Integer.BYTES);
      this.nextBlockIndex = index-1; // -1 to compensate for the always-added 1 in readBlockHeader
      slice.seek(offset);
      readBlockHeader();
      return;
    }

    // Fallback to iteration of blocks
    do {
      slice.seek(blockEnd);
      readBlockHeader();
    } while (block < targetBlock);
  }

  private void readBlockHeader() throws IOException {
    block = Short.toUnsignedInt(slice.readShort()) << 16;
    assert block >= 0;
    final int numValues = 1 + Short.toUnsignedInt(slice.readShort());
    index = nextBlockIndex;
    nextBlockIndex = index + numValues;
    if (numValues <= MAX_ARRAY_LENGTH) {
      method = Method.SPARSE;
      blockEnd = slice.getFilePointer() + (numValues << 1);
    } else if (numValues == 65536) {
      method = Method.ALL;
      blockEnd = slice.getFilePointer();
      gap = block - index - 1;
    } else {
      method = Method.DENSE;
      denseBitmapOffset = slice.getFilePointer() + (denseRankTable == null ?  0 : denseRankTable.length);
      blockEnd = denseBitmapOffset + (1 << 13);
      // Performance consideration: All rank (default 128 * 16 bits) are loaded up front. This should be fast with the
      // reusable byte[] buffer, but it is still wasted if the DENSE block is iterated in small steps.
      // If this results in too great a performance regression, a heuristic strategy might work where the rank data
      // are loaded on first in-block advance, if said advance is > X docIDs. The hope being that a small first
      // advance means that subsequent advances will be small too.
      // Another alternative is to maintain an extra slice for DENSE rank, but IndexedDISI is already slice-heavy.
      if (denseRankPower != -1) {
        slice.readBytes(denseRankTable, 0, denseRankTable.length);
      }
      wordIndex = -1;
      numberOfOnes = index + 1;
      denseOrigoIndex = numberOfOnes;
    }
  }

  @Override
  public int nextDoc() throws IOException {
    return advance(doc + 1);
  }

  public int index() {
    return index;
  }

  @Override
  public long cost() {
    return cost;
  }

  enum Method {
    SPARSE {
      @Override
      boolean advanceWithinBlock(IndexedDISI disi, int target) throws IOException {
        final int targetInBlock = target & 0xFFFF;
        // TODO: binary search
        for (; disi.index < disi.nextBlockIndex;) {
          int doc = Short.toUnsignedInt(disi.slice.readShort());
          disi.index++;
          if (doc >= targetInBlock) {
            disi.doc = disi.block | doc;
            disi.exists = true;
            return true;
          }
        }
        return false;
      }
      @Override
      boolean advanceExactWithinBlock(IndexedDISI disi, int target) throws IOException {
        final int targetInBlock = target & 0xFFFF;
        // TODO: binary search
        if (target == disi.doc) {
          return disi.exists;
        }
        for (; disi.index < disi.nextBlockIndex;) {
          int doc = Short.toUnsignedInt(disi.slice.readShort());
          disi.index++;
          if (doc >= targetInBlock) {
            if (doc != targetInBlock) {
              disi.index--;
              disi.slice.seek(disi.slice.getFilePointer() - Short.BYTES);
              break;
            }
            disi.exists = true;
            return true;
          }
        }
        disi.exists = false;
        return false;
      }
    },
    DENSE {
      @Override
      boolean advanceWithinBlock(IndexedDISI disi, int target) throws IOException {
        final int targetInBlock = target & 0xFFFF;
        final int targetWordIndex = targetInBlock >>> 6;

        // If possible, skip ahead using the rank cache
        // If the distance between the current position and the target is < rank-longs
        // there is no sense in using rank
        if (disi.denseRankPower != -1 && targetWordIndex - disi.wordIndex >= (1 << (disi.denseRankPower-6) )) {
          rankSkip(disi, targetInBlock);
        }

        for (int i = disi.wordIndex + 1; i <= targetWordIndex; ++i) {
          disi.word = disi.slice.readLong();
          disi.numberOfOnes += Long.bitCount(disi.word);
        }
        disi.wordIndex = targetWordIndex;

        long leftBits = disi.word >>> target;
        if (leftBits != 0L) {
          disi.doc = target + Long.numberOfTrailingZeros(leftBits);
          disi.index = disi.numberOfOnes - Long.bitCount(leftBits);
          return true;
        }

        // There were no set bits at the wanted position. Move forward until one is reached
        while (++disi.wordIndex < 1024) {
          // This could use the rank cache to skip empty spaces >= 512 bits, but it seems unrealistic
          // that such blocks would be DENSE
          disi.word = disi.slice.readLong();
          if (disi.word != 0) {
            disi.index = disi.numberOfOnes;
            disi.numberOfOnes += Long.bitCount(disi.word);
            disi.doc = disi.block | (disi.wordIndex << 6) | Long.numberOfTrailingZeros(disi.word);
            return true;
          }
        }
        // No set bits in the block at or after the wanted position.
        return false;
      }

      @Override
      boolean advanceExactWithinBlock(IndexedDISI disi, int target) throws IOException {
        final int targetInBlock = target & 0xFFFF;
        final int targetWordIndex = targetInBlock >>> 6;

        // If possible, skip ahead using the rank cache
        // If the distance between the current position and the target is < rank-longs
        // there is no sense in using rank
        if (disi.denseRankPower != -1 && targetWordIndex - disi.wordIndex >= (1 << (disi.denseRankPower-6) )) {
          rankSkip(disi, targetInBlock);
        }

        for (int i = disi.wordIndex + 1; i <= targetWordIndex; ++i) {
          disi.word = disi.slice.readLong();
          disi.numberOfOnes += Long.bitCount(disi.word);
        }
        disi.wordIndex = targetWordIndex;

        long leftBits = disi.word >>> target;
        disi.index = disi.numberOfOnes - Long.bitCount(leftBits);
        return (leftBits & 1L) != 0;
      }


    },
    ALL {
      @Override
      boolean advanceWithinBlock(IndexedDISI disi, int target) {
        disi.doc = target;
        disi.index = target - disi.gap;
        return true;
      }
      @Override
      boolean advanceExactWithinBlock(IndexedDISI disi, int target) {
        disi.index = target - disi.gap;
        return true;
      }
    };

    abstract boolean advanceWithinBlock(IndexedDISI disi, int target) throws IOException;

    abstract boolean advanceExactWithinBlock(IndexedDISI disi, int target) throws IOException;
  }

  private static void rankSkip(IndexedDISI disi, int targetInBlock) throws IOException {
    assert disi.denseRankPower >= 0 : disi.denseRankPower;
    // Resolve the rank as close to targetInBlock as possible (maximum distance is 8 longs)
    // Note: rankOrigoOffset is tracked on block open, so it is absolute (e.g. don't add origo)
    final int rankIndex = targetInBlock >> disi.denseRankPower; // Default is 9 (8 longs: 2^3 * 2^6 = 512 docIDs)

    final int rank =
        (disi.denseRankTable[rankIndex<<1] & 0xFF) << 8 |
        (disi.denseRankTable[(rankIndex<<1)+1] & 0xFF);

    // Position the counting logic just after the rank point
    final int rankAlignedWordIndex = rankIndex << disi.denseRankPower >> 6;
    disi.slice.seek(disi.denseBitmapOffset + rankAlignedWordIndex*Long.BYTES);
    long rankWord = disi.slice.readLong();
    int denseNOO = rank + Long.bitCount(rankWord);

    disi.wordIndex = rankAlignedWordIndex;
    disi.word = rankWord;
    disi.numberOfOnes = disi.denseOrigoIndex + denseNOO;
  }
}

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

package org.trypticon.luceneupgrader.lucene8.internal.lucene.search;

import java.io.IOException;
import java.util.Objects;

import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.DocValues;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.LeafReaderContext;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.NumericDocValues;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.PointValues;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.comparators.LongComparator;

public abstract class LongValuesSource implements SegmentCacheable {

  public abstract LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException;

  public abstract boolean needsScores();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();

  public abstract LongValuesSource rewrite(IndexSearcher searcher) throws IOException;

  public SortField getSortField(boolean reverse) {
    return new LongValuesSortField(this, reverse);
  }

  public DoubleValuesSource toDoubleValuesSource() {
    return new DoubleLongValuesSource(this);
  }

  private static class DoubleLongValuesSource extends DoubleValuesSource {

    private final LongValuesSource inner;

    private DoubleLongValuesSource(LongValuesSource inner) {
      this.inner = inner;
    }

    @Override
    public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
      LongValues v = inner.getValues(ctx, scores);
      return new DoubleValues() {
        @Override
        public double doubleValue() throws IOException {
          return (double) v.longValue();
        }

        @Override
        public boolean advanceExact(int doc) throws IOException {
          return v.advanceExact(doc);
        }
      };
    }

    @Override
    public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
      return inner.rewrite(searcher).toDoubleValuesSource();
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return inner.isCacheable(ctx);
    }

    @Override
    public String toString() {
      return "double(" + inner.toString() + ")";
    }

    @Override
    public boolean needsScores() {
      return inner.needsScores();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DoubleLongValuesSource that = (DoubleLongValuesSource) o;
      return Objects.equals(inner, that.inner);
    }

    @Override
    public int hashCode() {
      return Objects.hash(inner);
    }
  }

  public static LongValuesSource fromLongField(String field) {
    return new FieldValuesSource(field);
  }

  public static LongValuesSource fromIntField(String field) {
    return fromLongField(field);
  }

  public static LongValuesSource constant(long value) {
    return new ConstantLongValuesSource(value);
  }

  private static class ConstantLongValuesSource extends LongValuesSource {

    private final long value;

    private ConstantLongValuesSource(long value) {
      this.value = value;
    }

    @Override
    public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
      return new LongValues() {
        @Override
        public long longValue() throws IOException {
          return value;
        }

        @Override
        public boolean advanceExact(int doc) throws IOException {
          return true;
        }
      };
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return true;
    }

    @Override
    public boolean needsScores() {
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConstantLongValuesSource that = (ConstantLongValuesSource) o;
      return value == that.value;
    }

    @Override
    public String toString() {
      return "constant(" + value + ")";
    }

    @Override
    public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
      return this;
    }

  }

  private static class FieldValuesSource extends LongValuesSource {

    final String field;

    private FieldValuesSource(String field) {
      this.field = field;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FieldValuesSource that = (FieldValuesSource) o;
      return Objects.equals(field, that.field);
    }

    @Override
    public String toString() {
      return "long(" + field + ")";
    }

    @Override
    public int hashCode() {
      return Objects.hash(field);
    }

    @Override
    public LongValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
      final NumericDocValues values = DocValues.getNumeric(ctx.reader(), field);
      return toLongValues(values);
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return DocValues.isCacheable(ctx, field);
    }

    @Override
    public boolean needsScores() {
      return false;
    }

    @Override
    public LongValuesSource rewrite(IndexSearcher searcher) throws IOException {
      return this;
    }
  }

  private static class LongValuesSortField extends SortField {

    final LongValuesSource producer;

    public LongValuesSortField(LongValuesSource producer, boolean reverse) {
      super(producer.toString(), new LongValuesComparatorSource(producer), reverse);
      this.producer = producer;
    }

    @Override
    public void setMissingValue(Object missingValue) {
      if (missingValue instanceof Number) {
        this.missingValue = missingValue;
        ((LongValuesComparatorSource) getComparatorSource()).setMissingValue(((Number) missingValue).longValue());
      } else {
          super.setMissingValue(missingValue);
      }
    }

    @Override
    public boolean needsScores() {
      return producer.needsScores();
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder("<");
      buffer.append(getField()).append(">");
      if (reverse)
        buffer.append("!");
      return buffer.toString();
    }

    @Override
    public SortField rewrite(IndexSearcher searcher) throws IOException {
      LongValuesSortField rewritten = new LongValuesSortField(producer.rewrite(searcher), reverse);
      if (missingValue != null) {
        rewritten.setMissingValue(missingValue);
      }
      return rewritten;
    }
  }

  private static class LongValuesHolder {
    LongValues values;
  }

  private static class LongValuesComparatorSource extends FieldComparatorSource {
    private final LongValuesSource producer;
    private long missingValue;

    public LongValuesComparatorSource(LongValuesSource producer) {
      this.producer = producer;
      this.missingValue = 0L;
    }

    void setMissingValue(long missingValue) {
      this.missingValue = missingValue;
    }

    @Override
    public FieldComparator<Long> newComparator(String fieldname, int numHits,
                                               int sortPos, boolean reversed) {
      return new LongComparator(numHits, fieldname, missingValue, reversed, sortPos) {
        @Override
        public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
          LongValuesHolder holder = new LongValuesHolder();

          return new LongComparator.LongLeafComparator(context) {
            LeafReaderContext ctx;

            @Override
            protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) {
              ctx = context;
              return asNumericDocValues(holder);
            }

            @Override
            protected PointValues getPointValues(LeafReaderContext context, String field) {
              return null;
            }

            @Override
            public void setScorer(Scorable scorer) throws IOException {
              holder.values = producer.getValues(ctx, DoubleValuesSource.fromScorer(scorer));
              super.setScorer(scorer);
            }
          };
        }
      };
    }
  }

  private static LongValues toLongValues(NumericDocValues in) {
    return new LongValues() {
      @Override
      public long longValue() throws IOException {
        return in.longValue();
      }

      @Override
      public boolean advanceExact(int target) throws IOException {
        return in.advanceExact(target);
      }

    };
  }

  private static NumericDocValues asNumericDocValues(LongValuesHolder in) {
    return new NumericDocValues() {
      @Override
      public long longValue() throws IOException {
        return in.values.longValue();
      }

      @Override
      public boolean advanceExact(int target) throws IOException {
        return in.values.advanceExact(target);
      }

      @Override
      public int docID() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int nextDoc() throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public int advance(int target) throws IOException {
        throw new UnsupportedOperationException();
      }

      @Override
      public long cost() {
        throw new UnsupportedOperationException();
      }
    };
  }

}

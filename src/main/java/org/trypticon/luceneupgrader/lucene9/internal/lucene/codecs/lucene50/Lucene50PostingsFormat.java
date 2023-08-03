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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.codecs.lucene50;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.codecs.*;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.codecs.blocktree.BlockTreeTermsReader;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.SegmentReadState;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.SegmentWriteState;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.TermState;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.IOUtils;

import java.io.IOException;


public class Lucene50PostingsFormat extends PostingsFormat {

  public static final String DOC_EXTENSION = "doc";

  public static final String POS_EXTENSION = "pos";

  public static final String PAY_EXTENSION = "pay";

  static final int MAX_SKIP_LEVELS = 10;

  final static String TERMS_CODEC = "Lucene50PostingsWriterTerms";
  final static String DOC_CODEC = "Lucene50PostingsWriterDoc";
  final static String POS_CODEC = "Lucene50PostingsWriterPos";
  final static String PAY_CODEC = "Lucene50PostingsWriterPay";

  // Increment version to change it
  final static int VERSION_START = 0;
  final static int VERSION_IMPACT_SKIP_DATA = 1;
  final static int VERSION_CURRENT = VERSION_IMPACT_SKIP_DATA;

  // NOTE: must be multiple of 64 because of PackedInts long-aligned encoding/decoding
  public final static int BLOCK_SIZE = 128;

  public Lucene50PostingsFormat() {
    super("Lucene50");
  }

  @Override
  public String toString() {
    return getName() + "(blocksize=" + BLOCK_SIZE + ")";
  }

  @Override
  public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
    throw new UnsupportedOperationException("Old formats can't be used for writing");
  }

  @Override
  public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
    PostingsReaderBase postingsReader = new Lucene50PostingsReader(state);
    boolean success = false;
    try {
      FieldsProducer ret = new BlockTreeTermsReader(postingsReader, state);
      success = true;
      return ret;
    } finally {
      if (!success) {
        IOUtils.closeWhileHandlingException(postingsReader);
      }
    }
  }

  public static final class IntBlockTermState extends BlockTermState {
    public long docStartFP;
    public long posStartFP;
    public long payStartFP;
    public long skipOffset;
    public long lastPosBlockOffset;
    public int singletonDocID;

    public IntBlockTermState() {
      skipOffset = -1;
      lastPosBlockOffset = -1;
      singletonDocID = -1;
    }

    @Override
    public IntBlockTermState clone() {
      IntBlockTermState other = new IntBlockTermState();
      other.copyFrom(this);
      return other;
    }

    @Override
    public void copyFrom(TermState _other) {
      super.copyFrom(_other);
      IntBlockTermState other = (IntBlockTermState) _other;
      docStartFP = other.docStartFP;
      posStartFP = other.posStartFP;
      payStartFP = other.payStartFP;
      lastPosBlockOffset = other.lastPosBlockOffset;
      skipOffset = other.skipOffset;
      singletonDocID = other.singletonDocID;
    }

    @Override
    public String toString() {
      return super.toString() + " docStartFP=" + docStartFP + " posStartFP=" + posStartFP + " payStartFP=" + payStartFP + " lastPosBlockOffset=" + lastPosBlockOffset + " singletonDocID=" + singletonDocID;
    }
  }
}

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
package org.trypticon.luceneupgrader.lucene6.internal.lucene.codecs.lucene50;


import org.trypticon.luceneupgrader.lucene6.internal.lucene.codecs.CodecUtil;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.codecs.SegmentInfoFormat;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.index.CorruptIndexException;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.index.IndexFileNames;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.index.SegmentInfo;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.store.ChecksumIndexInput;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.store.Directory;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.store.IOContext;
import org.trypticon.luceneupgrader.lucene6.internal.lucene.util.Version;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Deprecated
public class Lucene50SegmentInfoFormat extends SegmentInfoFormat {

  public Lucene50SegmentInfoFormat() {
  }
  
  @Override
  public SegmentInfo read(Directory dir, String segment, byte[] segmentID, IOContext context) throws IOException {
    final String fileName = IndexFileNames.segmentFileName(segment, "", Lucene50SegmentInfoFormat.SI_EXTENSION);
    try (ChecksumIndexInput input = dir.openChecksumInput(fileName, context)) {
      Throwable priorE = null;
      SegmentInfo si = null;
      try {
        int format = CodecUtil.checkIndexHeader(input, Lucene50SegmentInfoFormat.CODEC_NAME,
                                          Lucene50SegmentInfoFormat.VERSION_START,
                                          Lucene50SegmentInfoFormat.VERSION_CURRENT,
                                          segmentID, "");
        final Version version = Version.fromBits(input.readInt(), input.readInt(), input.readInt());
        
        final int docCount = input.readInt();
        if (docCount < 0) {
          throw new CorruptIndexException("invalid docCount: " + docCount, input);
        }
        final boolean isCompoundFile = input.readByte() == SegmentInfo.YES;
        
        final Map<String,String> diagnostics;
        final Set<String> files;
        final Map<String,String> attributes;
        
        if (format >= VERSION_SAFE_MAPS) {
          diagnostics = input.readMapOfStrings();
          files = input.readSetOfStrings();
          attributes = input.readMapOfStrings();
        } else {
          diagnostics = Collections.unmodifiableMap(input.readStringStringMap());
          files = Collections.unmodifiableSet(input.readStringSet());
          attributes = Collections.unmodifiableMap(input.readStringStringMap());
        }
        
        si = new SegmentInfo(dir, version, segment, docCount, isCompoundFile, null, diagnostics, segmentID, attributes, null);
        si.setFiles(files);
      } catch (Throwable exception) {
        priorE = exception;
      } finally {
        CodecUtil.checkFooter(input, priorE);
      }
      return si;
    }
  }

  @Override
  public void write(Directory dir, SegmentInfo si, IOContext ioContext) throws IOException {
    throw new UnsupportedOperationException("this codec can only be used for reading");
  }

  public final static String SI_EXTENSION = "si";
  static final String CODEC_NAME = "Lucene50SegmentInfo";
  static final int VERSION_START = 0;
  static final int VERSION_SAFE_MAPS = 1;
  static final int VERSION_CURRENT = VERSION_SAFE_MAPS;
}

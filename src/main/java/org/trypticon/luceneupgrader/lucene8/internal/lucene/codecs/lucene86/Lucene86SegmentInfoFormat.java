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

package org.trypticon.luceneupgrader.lucene8.internal.lucene.codecs.lucene86;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.trypticon.luceneupgrader.lucene8.internal.lucene.codecs.CodecUtil;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.codecs.SegmentInfoFormat;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.CorruptIndexException;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.IndexFileNames;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.IndexSorter;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.IndexWriter;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.SegmentInfo;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.SegmentInfos;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.index.SortFieldProvider;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.Sort;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.search.SortField;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.store.ChecksumIndexInput;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.store.DataInput;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.store.DataOutput;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.store.Directory;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.store.IOContext;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.store.IndexOutput;
import org.trypticon.luceneupgrader.lucene8.internal.lucene.util.Version;

public class Lucene86SegmentInfoFormat extends SegmentInfoFormat {

  public Lucene86SegmentInfoFormat() {
  }

  @Override
  public SegmentInfo read(Directory dir, String segment, byte[] segmentID, IOContext context) throws IOException {
    final String fileName = IndexFileNames.segmentFileName(segment, "", SI_EXTENSION);
    try (ChecksumIndexInput input = dir.openChecksumInput(fileName, context)) {
      Throwable priorE = null;
      SegmentInfo si = null;
      try {
        int format = CodecUtil.checkIndexHeader(input, CODEC_NAME,
            VERSION_START,
            VERSION_CURRENT,
            segmentID, "");
        final Version version = Version.fromBits(input.readInt(), input.readInt(), input.readInt());
        byte hasMinVersion = input.readByte();
        final Version minVersion;
        switch (hasMinVersion) {
          case 0:
            minVersion = null;
            break;
          case 1:
            minVersion = Version.fromBits(input.readInt(), input.readInt(), input.readInt());
            break;
          default:
            throw new CorruptIndexException("Illegal boolean value " + hasMinVersion, input);
        }

        final int docCount = input.readInt();
        if (docCount < 0) {
          throw new CorruptIndexException("invalid docCount: " + docCount, input);
        }
        final boolean isCompoundFile = input.readByte() == SegmentInfo.YES;

        final Map<String,String> diagnostics = input.readMapOfStrings();
        final Set<String> files = input.readSetOfStrings();
        final Map<String,String> attributes = input.readMapOfStrings();

        int numSortFields = input.readVInt();
        Sort indexSort;
        if (numSortFields > 0) {
          SortField[] sortFields = new SortField[numSortFields];
          for(int i=0;i<numSortFields;i++) {
            String name = input.readString();
            sortFields[i] = SortFieldProvider.forName(name).readSortField(input);
          }
          indexSort = new Sort(sortFields);
        } else if (numSortFields < 0) {
          throw new CorruptIndexException("invalid index sort field count: " + numSortFields, input);
        } else {
          indexSort = null;
        }

        si = new SegmentInfo(dir, version, minVersion, segment, docCount, isCompoundFile, null, diagnostics, segmentID, attributes, indexSort);
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
    final String fileName = IndexFileNames.segmentFileName(si.name, "", SI_EXTENSION);

    try (IndexOutput output = dir.createOutput(fileName, ioContext)) {
      // Only add the file once we've successfully created it, else IFD assert can trip:
      si.addFile(fileName);
      CodecUtil.writeIndexHeader(output,
          CODEC_NAME,
          VERSION_CURRENT,
          si.getId(),
          "");
      Version version = si.getVersion();
      if (version.major < 7) {
        throw new IllegalArgumentException("invalid major version: should be >= 7 but got: " + version.major + " segment=" + si);
      }
      // Write the Lucene version that created this segment, since 3.1
      output.writeInt(version.major);
      output.writeInt(version.minor);
      output.writeInt(version.bugfix);

      // Write the min Lucene version that contributed docs to the segment, since 7.0
      if (si.getMinVersion() != null) {
        output.writeByte((byte) 1);
        Version minVersion = si.getMinVersion();
        output.writeInt(minVersion.major);
        output.writeInt(minVersion.minor);
        output.writeInt(minVersion.bugfix);
      } else {
        output.writeByte((byte) 0);
      }

      assert version.prerelease == 0;
      output.writeInt(si.maxDoc());

      output.writeByte((byte) (si.getUseCompoundFile() ? SegmentInfo.YES : SegmentInfo.NO));
      output.writeMapOfStrings(si.getDiagnostics());
      Set<String> files = si.files();
      for (String file : files) {
        if (!IndexFileNames.parseSegmentName(file).equals(si.name)) {
          throw new IllegalArgumentException("invalid files: expected segment=" + si.name + ", got=" + files);
        }
      }
      output.writeSetOfStrings(files);
      output.writeMapOfStrings(si.getAttributes());

      Sort indexSort = si.getIndexSort();
      int numSortFields = indexSort == null ? 0 : indexSort.getSort().length;
      output.writeVInt(numSortFields);
      for (int i = 0; i < numSortFields; ++i) {
        SortField sortField = indexSort.getSort()[i];
        IndexSorter sorter = sortField.getIndexSorter();
        if (sorter == null) {
          throw new IllegalArgumentException("cannot serialize SortField " + sortField);
        }
        output.writeString(sorter.getProviderName());
        SortFieldProvider.write(sortField, output);
      }

      CodecUtil.writeFooter(output);
    }
  }

  public final static String SI_EXTENSION = "si";
  static final String CODEC_NAME = "Lucene86SegmentInfo";
  static final int VERSION_START = 0;
  static final int VERSION_CURRENT = VERSION_START;
}

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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.tests;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.DirectoryReader;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.IndexWriter;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.index.SegmentCommitInfo;

import java.io.IOException;

/**
 * Access to {@link org.trypticon.luceneupgrader.lucene9.internal.lucene.index.IndexWriter} internals exposed to the test framework.
 *
 * @lucene.internal
 */
public interface IndexWriterAccess {
  String segString(IndexWriter iw);

  int getSegmentCount(IndexWriter iw);

  boolean isClosed(IndexWriter iw);

  DirectoryReader getReader(IndexWriter iw, boolean applyDeletions, boolean writeAllDeletes)
      throws IOException;

  int getDocWriterThreadPoolSize(IndexWriter iw);

  boolean isDeleterClosed(IndexWriter iw);

  SegmentCommitInfo newestSegment(IndexWriter iw);
}

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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.index;

import org.trypticon.luceneupgrader.lucene9.internal.lucene.store.Directory;
import org.trypticon.luceneupgrader.lucene9.internal.lucene.util.InfoStream;

/**
 * {@link FlushPolicy} controls when segments are flushed from a RAM resident internal
 * data-structure to the {@link IndexWriter}s {@link Directory}.
 *
 * <p>Segments are traditionally flushed by:
 *
 * <ul>
 *   <li>RAM consumption - configured via {@link IndexWriterConfig#setRAMBufferSizeMB(double)}
 *   <li>Number of RAM resident documents - configured via {@link
 *       IndexWriterConfig#setMaxBufferedDocs(int)}
 * </ul>
 *
 * <p>{@link IndexWriter} consults the provided {@link FlushPolicy} to control the flushing process.
 * The policy is informed for each added or updated document as well as for each delete term. Based
 * on the {@link FlushPolicy}, the information provided via {@link DocumentsWriterPerThread} and
 * {@link DocumentsWriterFlushControl}, the {@link FlushPolicy} decides if a {@link
 * DocumentsWriterPerThread} needs flushing and mark it as flush-pending via {@link
 * DocumentsWriterFlushControl#setFlushPending}, or if deletes need to be applied.
 *
 * @see DocumentsWriterFlushControl
 * @see DocumentsWriterPerThread
 * @see IndexWriterConfig#setFlushPolicy(FlushPolicy)
 */
abstract class FlushPolicy {
  protected LiveIndexWriterConfig indexWriterConfig;
  protected InfoStream infoStream;

  /**
   * Called for each delete, insert or update. For pure deletes, the given {@link
   * DocumentsWriterPerThread} may be {@code null}.
   *
   * <p>Note: This method is called synchronized on the given {@link DocumentsWriterFlushControl}
   * and it is guaranteed that the calling thread holds the lock on the given {@link
   * DocumentsWriterPerThread}
   */
  public abstract void onChange(
      DocumentsWriterFlushControl control, DocumentsWriterPerThread perThread);

  /** Called by DocumentsWriter to initialize the FlushPolicy */
  protected synchronized void init(LiveIndexWriterConfig indexWriterConfig) {
    this.indexWriterConfig = indexWriterConfig;
    infoStream = indexWriterConfig.getInfoStream();
  }

  /**
   * Returns the current most RAM consuming non-pending {@link DocumentsWriterPerThread} with at
   * least one indexed document.
   *
   * <p>This method will never return <code>null</code>
   */
  protected DocumentsWriterPerThread findLargestNonPendingWriter(
      DocumentsWriterFlushControl control, DocumentsWriterPerThread perThread) {
    assert perThread.getNumDocsInRAM() > 0;
    // the dwpt which needs to be flushed eventually
    DocumentsWriterPerThread maxRamUsingWriter = control.findLargestNonPendingWriter();
    assert assertMessage("set largest ram consuming thread pending on lower watermark");
    return maxRamUsingWriter;
  }

  private boolean assertMessage(String s) {
    if (infoStream.isEnabled("FP")) {
      infoStream.message("FP", s);
    }
    return true;
  }
}

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
package org.trypticon.luceneupgrader.lucene9.internal.lucene.store;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/** A {@link DataOutput} wrapping a plain {@link OutputStream}. */
public class OutputStreamDataOutput extends DataOutput implements Closeable {
  private final OutputStream os;

  public OutputStreamDataOutput(OutputStream os) {
    this.os = os;
  }

  @Override
  public void writeByte(byte b) throws IOException {
    os.write(b);
  }

  @Override
  public void writeBytes(byte[] b, int offset, int length) throws IOException {
    os.write(b, offset, length);
  }

  @Override
  public void close() throws IOException {
    os.close();
  }
}

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
package org.trypticon.luceneupgrader.lucene3.internal.lucene.store;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


public class SimpleFSDirectory extends FSDirectory {
    

  public SimpleFSDirectory(File path, LockFactory lockFactory) throws IOException {
    super(path, lockFactory);
  }
  

  public SimpleFSDirectory(File path) throws IOException {
    super(path, null);
  }

  @Override
  public IndexInput openInput(String name, int bufferSize) throws IOException {
    ensureOpen();
    final File path = new File(directory, name);
    return new SimpleFSIndexInput("SimpleFSIndexInput(path=\"" + path.getPath() + "\")", path, bufferSize, getReadChunkSize());
  }

  protected static class SimpleFSIndexInput extends BufferedIndexInput {
  
    protected static class Descriptor extends RandomAccessFile {
      // remember if the file is open, so that we don't try to close it
      // more than once
      protected volatile boolean isOpen;
      long position;
      final long length;
      
      public Descriptor(File file, String mode) throws IOException {
        super(file, mode);
        isOpen=true;
        length=length();
      }
  
      @Override
      public void close() throws IOException {
        if (isOpen) {
          isOpen=false;
          super.close();
        }
      }
    }
  
    protected final Descriptor file;
    boolean isClone;
    //  LUCENE-1566 - maximum read length on a 32bit JVM to prevent incorrect OOM 
    protected final int chunkSize;

    @Deprecated
    public SimpleFSIndexInput(File path, int bufferSize, int chunkSize) throws IOException {
      this("anonymous SimpleFSIndexInput", path, bufferSize, chunkSize);
    }

    public SimpleFSIndexInput(String resourceDesc, File path, int bufferSize, int chunkSize) throws IOException {
      super(resourceDesc, bufferSize);
      file = new Descriptor(path, "r");
      this.chunkSize = chunkSize;
    }
  
    @Override
    protected void readInternal(byte[] b, int offset, int len)
         throws IOException {
      synchronized (file) {
        long position = getFilePointer();
        if (position != file.position) {
          file.seek(position);
          file.position = position;
        }
        int total = 0;

        try {
          do {
            final int readLength;
            if (total + chunkSize > len) {
              readLength = len - total;
            } else {
              // LUCENE-1566 - work around JVM Bug by breaking very large reads into chunks
              readLength = chunkSize;
            }
            final int i = file.read(b, offset + total, readLength);
            if (i == -1) {
              throw new EOFException("read past EOF: " + this);
            }
            file.position += i;
            total += i;
          } while (total < len);
        } catch (OutOfMemoryError e) {
          // propagate OOM up and add a hint for 32bit VM Users hitting the bug
          // with a large chunk size in the fast path.
          final OutOfMemoryError outOfMemoryError = new OutOfMemoryError(
              "OutOfMemoryError likely caused by the Sun VM Bug described in "
              + "https://issues.apache.org/jira/browse/LUCENE-1566; try calling FSDirectory.setReadChunkSize "
              + "with a value smaller than the current chunk size (" + chunkSize + ")");
          outOfMemoryError.initCause(e);
          throw outOfMemoryError;
        } catch (IOException ioe) {
          IOException newIOE = new IOException(ioe.getMessage() + ": " + this);
          newIOE.initCause(ioe);
          throw newIOE;
        }
      }
    }
  
    @Override
    public void close() throws IOException {
      // only close the file if this is not a clone
      if (!isClone) file.close();
    }
  
    @Override
    protected void seekInternal(long position) {
    }
  
    @Override
    public long length() {
      return file.length;
    }
  
    @Override
    public Object clone() {
      SimpleFSIndexInput clone = (SimpleFSIndexInput)super.clone();
      clone.isClone = true;
      return clone;
    }
  
    boolean isFDValid() throws IOException {
      return file.getFD().valid();
    }
    
    @Override
    public void copyBytes(IndexOutput out, long numBytes) throws IOException {
      numBytes -= flushBuffer(out, numBytes);
      // If out is FSIndexOutput, the copy will be optimized
      out.copyBytes(this, numBytes);
    }
  }
}

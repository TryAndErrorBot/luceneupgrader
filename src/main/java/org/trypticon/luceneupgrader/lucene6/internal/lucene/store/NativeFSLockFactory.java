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
package org.trypticon.luceneupgrader.lucene6.internal.lucene.store;


import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.trypticon.luceneupgrader.lucene6.internal.lucene.util.IOUtils;

public final class NativeFSLockFactory extends FSLockFactory {
  
  public static final NativeFSLockFactory INSTANCE = new NativeFSLockFactory();

  private static final Set<String> LOCK_HELD = Collections.synchronizedSet(new HashSet<String>());

  private NativeFSLockFactory() {}

  @Override
  protected Lock obtainFSLock(FSDirectory dir, String lockName) throws IOException {
    Path lockDir = dir.getDirectory();
    
    // Ensure that lockDir exists and is a directory.
    // note: this will fail if lockDir is a symlink
    Files.createDirectories(lockDir);
    
    Path lockFile = lockDir.resolve(lockName);

    try {
      Files.createFile(lockFile);
    } catch (IOException ignore) {
      // we must create the file to have a truly canonical path.
      // if it's already created, we don't care. if it cant be created, it will fail below.
    }
    
    // fails if the lock file does not exist
    final Path realPath = lockFile.toRealPath();
    
    // used as a best-effort check, to see if the underlying file has changed
    final FileTime creationTime = Files.readAttributes(realPath, BasicFileAttributes.class).creationTime();
    
    if (LOCK_HELD.add(realPath.toString())) {
      FileChannel channel = null;
      FileLock lock = null;
      try {
        channel = FileChannel.open(realPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        lock = channel.tryLock();
        if (lock != null) {
          return new NativeFSLock(lock, channel, realPath, creationTime);
        } else {
          throw new LockObtainFailedException("Lock held by another program: " + realPath);
        }
      } finally {
        if (lock == null) { // not successful - clear up and move out
          IOUtils.closeWhileHandlingException(channel); // TODO: addSuppressed
          clearLockHeld(realPath);  // clear LOCK_HELD last 
        }
      }
    } else {
      throw new LockObtainFailedException("Lock held by this virtual machine: " + realPath);
    }
  }
  
  private static final void clearLockHeld(Path path) throws IOException {
    boolean remove = LOCK_HELD.remove(path.toString());
    if (remove == false) {
      throw new AlreadyClosedException("Lock path was cleared but never marked as held: " + path);
    }
  }

  // TODO: kind of bogus we even pass channel:
  // FileLock has an accessor, but mockfs doesnt yet mock the locks, too scary atm.

  static final class NativeFSLock extends Lock {
    final FileLock lock;
    final FileChannel channel;
    final Path path;
    final FileTime creationTime;
    volatile boolean closed;
    
    NativeFSLock(FileLock lock, FileChannel channel, Path path, FileTime creationTime) {
      this.lock = lock;
      this.channel = channel;
      this.path = path;
      this.creationTime = creationTime;
    }

    @Override
    public void ensureValid() throws IOException {
      if (closed) {
        throw new AlreadyClosedException("Lock instance already released: " + this);
      }
      // check we are still in the locks map (some debugger or something crazy didn't remove us)
      if (!LOCK_HELD.contains(path.toString())) {
        throw new AlreadyClosedException("Lock path unexpectedly cleared from map: " + this);
      }
      // check our lock wasn't invalidated.
      if (!lock.isValid()) {
        throw new AlreadyClosedException("FileLock invalidated by an external force: " + this);
      }
      // try to validate the underlying file descriptor.
      // this will throw IOException if something is wrong.
      long size = channel.size();
      if (size != 0) {
        throw new AlreadyClosedException("Unexpected lock file size: " + size + ", (lock=" + this + ")");
      }
      // try to validate the backing file name, that it still exists,
      // and has the same creation time as when we obtained the lock. 
      // if it differs, someone deleted our lock file (and we are ineffective)
      FileTime ctime = Files.readAttributes(path, BasicFileAttributes.class).creationTime(); 
      if (!creationTime.equals(ctime)) {
        throw new AlreadyClosedException("Underlying file changed by an external force at " + ctime + ", (lock=" + this + ")");
      }
    }

    @Override
    public synchronized void close() throws IOException {
      if (closed) {
        return;
      }
      // NOTE: we don't validate, as unlike SimpleFSLockFactory, we can't break others locks
      // first release the lock, then the channel
      try (FileChannel channel = this.channel;
           FileLock lock = this.lock) {
        assert lock != null;
        assert channel != null;
      } finally {
        closed = true;
        clearLockHeld(path);
      }
    }

    @Override
    public String toString() {
      return "NativeFSLock(path=" + path + ",impl=" + lock + ",creationTime=" + creationTime + ")"; 
    }
  }
}

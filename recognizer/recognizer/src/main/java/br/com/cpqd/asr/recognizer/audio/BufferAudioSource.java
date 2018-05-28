/*******************************************************************************
 * Copyright 2017 CPqD. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package br.com.cpqd.asr.recognizer.audio;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Audio source implementation of a piped circular buffer. The application
 * writes audio content in the circular buffer which is read by the recognition
 * thread. If there is no content available, the read process is blocked until
 * some data is written. The implementation used PipedInputStream /
 * PipedOutputStream objects to connect the streams and provide the blocking
 * mechanism.
 */
public class BufferAudioSource implements AudioSource {

    /**
     * default buffer size.
     */
    private static final int PIPE_SIZE = 5 * 1024 * 1024; // 5 MB

    private PipedOutputStream output;
    private PipedInputStream input;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private boolean finished = false;

    /**
     * Creates an audio source, where bytes can be written in a circular buffer by
     * one thread (application's), and read by a different thread (to send data to
     * the ASR Server).
     *
     * @throws IOException if an I/O error occurs.
     */
    public BufferAudioSource() throws IOException {
        super();
        output = new PipedOutputStream();
        input = new PipedInputStream(output, PIPE_SIZE);
    }

    /**
     * Creates an audio source, where bytes can be written in a circular buffer by
     * one thread (application's), and read by a different thread (to send data to
     * the ASR Server).
     *
     * @param size the buffer size (in bytes)
     * @throws IOException if an I/O error occurs.
     */
    public BufferAudioSource(int size) throws IOException {
        super();
        output = new PipedOutputStream();
        input = new PipedInputStream(output, size);
    }

    @Override
    public int read(byte[] b) throws IOException, NullPointerException {
        return input.read(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        lock.writeLock().lock();
        finished = true;
        lock.writeLock().unlock();
        input.close();
    }

    /**
     * Writes the specified byte array to the circular buffer.
     *
     * @param b   the byte array
     * @param len number of characters to write
     * @return returns 'false' if the buffer was finished and the byte array was not
     * written.
     * @throws IOException if the buffer is broken, unconnected or if an I/O error occurs.
     */
    public boolean write(byte[] b, int len) throws IOException {
        try {
            lock.readLock().lock();
            if (!finished) {
                output.write(b, 0, len);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Flushes the output stream and forces any buffered output bytes to be written
     * out. This will notify any readers that bytes are waiting in the pipe.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void flush() throws IOException {
        output.flush();
    }

    /**
     * Closes the output stream and releases any system resources associated with
     * this stream. This stream may no longer be used for writing bytes.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void finish() throws IOException {
        lock.writeLock().lock();
        finished = true;
        lock.writeLock().unlock();
        output.flush();
        output.close();
    }

    /**
     * Returns the circular buffer size.
     *
     * @return the buffer size in bytes.
     */
    public int getBufferSize() {
        return PIPE_SIZE;
    }

    @Override
    public String toString() {
        return "BufferAudioSource [" + getBufferSize() / 1024 + " kBytes]";
    }

}

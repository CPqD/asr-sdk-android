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
package br.com.cpqd.asr.recognizer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import br.com.cpqd.asr.recognizer.audio.AudioSource;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.PartialRecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionError;
import br.com.cpqd.asr.recognizer.model.RecognitionErrorCode;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.util.Util;

/**
 * Class that implements the library API.
 */
class SpeechRecognizerImpl implements SpeechRecognizerInterface, RecognitionListener {

    /**
     * Log tag.
     */
    private static final String TAG = SpeechRecognizerImpl.class.getSimpleName();

    /**
     * Max timeout waiting for response from server.
     */
    private static final int MAX_RESPONSE_TIMEOUT = 10000;

    /**
     * State indicating the library is idle, waiting for requests.
     */
    private static final int STATE_IDLE = 1;

    /**
     * State indicating the library received a start request.
     */
    private static final int STATE_STARTING = 2;

    /**
     * State indicating the library started recording audio.
     */
    private static final int STATE_RECORDING = 3;

    /**
     * State indicating the library stopped recording and is waiting for recognition.
     */
    private static final int STATE_WAITING_RECOGNITION = 4;

    /**
     * State indicating the library is waiting for create session.
     */
    private static final int STATE_WAITING_CREATE_SESSION = 5;

    /**
     * State indicating the library stopped recording and is waiting for cancel the recognition.
     */
    private static final int STATE_WAITING_CANCEL_RECOGNITION = 6;

    /**
     * State indicating the library is waiting for release session.
     */
    private static final int STATE_WAITING_RELEASE_SESSION = 7;

    /**
     * Handler message code for indicating the audio recording should be stopped.
     */
    public static final int MESSAGE_STOP = 1;

    /**
     * Handler message code for indicating that a partial recognition result was received.
     */
    public static final int MESSAGE_ON_PARTIAL_RESULT = 2;

    /**
     * Handler message code for indicating that the recognition result was received.
     */
    public static final int MESSAGE_ON_RESULT = 3;

    /**
     * Handler message code for indicating that an error was raised.
     */
    public static final int MESSAGE_ON_ERROR = 4;

    /**
     * Handler message code for indicating if the connection finished.
     */
    public static final int MESSAGE_ON_CREATE_SESSION = 6;

    /**
     * Handler message code for indicating if the connection finished.
     */
    public static final int MESSAGE_ON_START_RECOGNITION = 7;

    /**
     * Handler message code for indicating the cancel recognition
     */
    public static final int MESSAGE_ON_CANCEL_RECOGNITION = 8;

    /**
     * Handler message code for indicating the start input timers
     */
    public static final int MESSAGE_ON_START_INPUT_TIMERS = 9;

    /**
     * Handler message code for indicating the release session
     */
    public static final int MESSAGE_ON_RELEASE_SESSION = 10;

    /**
     * Thread that handles the connection to the server.
     */
    private final AsrServerConnectionThread mAsrServerConnectionThread;

    /**
     * Handler that allows managed threads do communicate back here.
     */
    private final Handler mHandler;

    /**
     * Lock object to server response
     */
    private final Object mSeverResponseLock;

    /**
     * Registered listener interfaces.
     */
    private final List<RecognitionListener> mListeners;

    /**
     * The current library state.
     * It can be one of the {@code STATE_*} values.
     */
    private int mState;

    /**
     * Blocking queue to read recognition result.
     */
    private final BlockingQueue<RecognitionResult> mSentencesQueue;

    /**
     * The Builder object.
     */
    private SpeechRecognizer.Builder mBuilder;

    /**
     * The recognition configuration
     */
    private RecognitionConfig mRecognitionConfig;

    /**
     * The asynchronous reader task.
     */
    private ReaderTask mReaderTask;

    /**
     * Flag to know if server response.
     */
    private boolean mServerResponse;

    /**
     * The recognition error.
     */
    private RecognitionError mError;

    /**
     * Status definition of the reader task.
     */
    public enum ReaderTaskStatus {
        IDLE, RUNNING, FINISHED, CANCELED
    }

    /**
     * Constructor.
     *
     * @param context the Context reference.
     * @param builder the Builder object.
     */
    SpeechRecognizerImpl(Context context, SpeechRecognizer.Builder builder) throws URISyntaxException {

        // Start the asr connection thread
        mAsrServerConnectionThread = new AsrServerConnectionThread(context, this,
                builder.uri, builder.credentials, builder.maxSessionIdleSeconds, builder.userAgent);
        mAsrServerConnectionThread.start();

        // Start the handler thread
        HandlerThread handlerThread = new HandlerThread("AsrHandlerThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), new CPqDASRHandlerCallback());

        mSentencesQueue = new LinkedBlockingQueue<>();

        mSeverResponseLock = new Object();

        mState = STATE_IDLE;

        mBuilder = builder;

        mRecognitionConfig = mBuilder.recogConfig;

        mListeners = new ArrayList<>();
        mListeners.add(this);
        if (mBuilder.listeners.size() > 0) {
            mListeners.addAll(mBuilder.listeners);
        }

        // Ask connection thread to establish connection.
        if (!builder.connectOnRecognize) {

            mState = STATE_WAITING_CREATE_SESSION;

            Message message = mAsrServerConnectionThread.obtainMessage();
            message.arg1 = AsrServerConnectionThread.MESSAGE_CONNECT_TO_SERVER;
            message.sendToTarget();

            try {
                synchronized (mSeverResponseLock) {
                    mSeverResponseLock.wait(MAX_RESPONSE_TIMEOUT);
                }
            } catch (Exception e) {
                // ignoring
            }
        }
    }

    /**
     * @see Handler#obtainMessage()
     */
    public Message obtainMessage() {
        return mHandler.obtainMessage();
    }

    /**
     * Class that brings the inter-thread communication framework to the main thread.
     *
     * @see AbstractMessagingThread
     */
    @SuppressWarnings("unchecked")
    private class CPqDASRHandlerCallback implements Handler.Callback {

        /**
         * @see AbstractMessagingThread#handleMessage(Message)
         */
        @Override
        public boolean handleMessage(Message msg) {

            if (msg.arg1 == MESSAGE_STOP) {

                // Check if library is in expected state to accept message.
                if (mState == STATE_RECORDING) {

                    mState = STATE_WAITING_RECOGNITION;

                    // Finalize audio capture.
                    if (mReaderTask != null) {
                        mReaderTask.finish();
                    }
                } else {
                    Log.i(TAG, "ignoring handle stop message");
                }

            } else if (msg.arg1 == MESSAGE_ON_PARTIAL_RESULT) {

                // Notify callback listener of partial result.
                for (RecognitionListener listener : mListeners) {
                    listener.onPartialRecognitionResult((PartialRecognitionResult) msg.obj);
                }

            } else if (msg.arg1 == MESSAGE_ON_RESULT) {

                // Notify callback listener of recognition result.
                for (RecognitionListener listener : mListeners) {
                    listener.onRecognitionResult((RecognitionResult) msg.obj);
                }

            } else if (msg.arg1 == MESSAGE_ON_ERROR) {

                // Notify callback listener.
                if (mState != STATE_IDLE) {
                    for (RecognitionListener listener : mListeners) {
                        listener.onError((RecognitionError) msg.obj);
                    }
                }

            } else if (msg.arg1 == MESSAGE_ON_CREATE_SESSION) {

                if (mState == STATE_WAITING_CREATE_SESSION) {

                    mState = STATE_IDLE;

                    // Notify the server response
                    synchronized (mSeverResponseLock) {
                        mSeverResponseLock.notifyAll();
                    }

                } else if (mState == STATE_STARTING) {

                    // Start recognition in connection thread.
                    Message message = mAsrServerConnectionThread.obtainMessage();
                    message.arg1 = AsrServerConnectionThread.MESSAGE_START_RECOGNITION;
                    message.obj = mRecognitionConfig;
                    message.sendToTarget();

                } else {
                    Log.i(TAG, "ignoring on create session message");
                }

            } else if (msg.arg1 == MESSAGE_ON_START_RECOGNITION) {

                // Notify callback listener of the start of audio recording.
                if (mState == STATE_STARTING) {

                    // Set the state to recording
                    mState = STATE_RECORDING;

                    // Set response from server to true
                    mServerResponse = true;

                    // Notify the listeners the server is listening
                    for (RecognitionListener listener : mListeners) {
                        listener.onListening();
                    }

                    // Notify the server response
                    synchronized (mSeverResponseLock) {
                        mSeverResponseLock.notifyAll();
                    }

                } else {
                    Log.i(TAG, "ignoring on recording start handle message");
                }

            } else if (msg.arg1 == MESSAGE_ON_CANCEL_RECOGNITION) {

                if (mState == STATE_WAITING_CANCEL_RECOGNITION) {

                    // Back state to idle
                    mState = STATE_IDLE;

                    // Set response from server to true
                    mServerResponse = true;

                    // Notify the server response
                    synchronized (mSeverResponseLock) {
                        mSeverResponseLock.notifyAll();
                    }

                } else {
                    Log.i(TAG, "ignoring on cancel recognition message");
                }

            } else if (msg.arg1 == MESSAGE_ON_RELEASE_SESSION) {

                if (mState == STATE_WAITING_RELEASE_SESSION) {

                    // Back state to idle
                    mState = STATE_IDLE;

                    // Set response from server to true
                    mServerResponse = true;

                    // Notify the server response
                    synchronized (mSeverResponseLock) {
                        mSeverResponseLock.notifyAll();
                    }

                } else {
                    Log.i(TAG, "ignoring on release session message");
                }

            } else {

                Log.i(TAG, "ignoring handler message with code: " + Integer.toString(msg.arg1));
            }

            return true;
        }
    }

    @Override
    public void recognize(AudioSource audio, LanguageModelList lmList) throws RecognitionException {
        recognize(audio, lmList, null);
    }

    @Override
    public void recognize(AudioSource audio, LanguageModelList lmList, RecognitionConfig config) throws RecognitionException {

        // Check if library is in expected state to accept message.
        if (mState != STATE_IDLE && mState != STATE_WAITING_RELEASE_SESSION) {
            return;
        }

        // Wait release session to start another recognize
        if (mState == STATE_WAITING_RELEASE_SESSION) {
            try {
                synchronized (mSeverResponseLock) {
                    mSeverResponseLock.wait(MAX_RESPONSE_TIMEOUT);
                }
                Thread.sleep(500);
            } catch (Exception e) {
                // ignoring
            }
        }

        mState = STATE_STARTING;

        mSentencesQueue.clear();

        mServerResponse = false;

        mError = null;

        if (config != null) {
            mRecognitionConfig = config;
        }

        // Creates a thread to read the audio source and send the packets to the server
        mReaderTask = new ReaderTask(audio, mBuilder);

        // Set language model URI into connection thread.
        Message message = mAsrServerConnectionThread.obtainMessage();
        message.arg1 = AsrServerConnectionThread.MESSAGE_SET_LANGUAGE_MODEL_URI;
        message.obj = lmList.getUriList().get(0);
        message.sendToTarget();

        // Connect to server session
        message = mAsrServerConnectionThread.obtainMessage();
        message.arg1 = AsrServerConnectionThread.MESSAGE_CONNECT_TO_SERVER;
        message.sendToTarget();

        // Check if is in the correct state
        if (mState == STATE_STARTING) {
            try {
                synchronized (mSeverResponseLock) {
                    mSeverResponseLock.wait(MAX_RESPONSE_TIMEOUT);
                }
            } catch (Exception e) {
                // ignoring
            }
        }

        if (!mServerResponse && mError == null) {
            for (RecognitionListener listener : mListeners) {
                listener.onError(new RecognitionError(RecognitionErrorCode.FAILURE, "Recognition operation timeout"));
            }
            throw new RecognitionException(RecognitionErrorCode.FAILURE, "Recognition timeout");
        } else if (mError != null) {
            throw new RecognitionException(mError);
        }
    }

    @Override
    public List<RecognitionResult> waitRecognitionResult() throws RecognitionException {
        return waitRecognitionResult(mBuilder.maxWaitSeconds);
    }

    @Override
    public List<RecognitionResult> waitRecognitionResult(int timeout) throws RecognitionException {

        // Server not listening
        if (mReaderTask == null || mReaderTask.isIdle() || mReaderTask.isCancelled()) {
            return new ArrayList<>();
        }

        // Waits for receipt of the result if the server is processing
        if (mState == STATE_RECORDING || mState == STATE_WAITING_RECOGNITION) {
            try {
                synchronized (mSentencesQueue) {
                    mSentencesQueue.wait(timeout * 1000);
                }
            } catch (InterruptedException e) {
                // ignoring
            }
        }

        if (mReaderTask.isCancelled()) {
            return new ArrayList<>();
        }

        try {
            if (mSentencesQueue.size() == 0 && mError == null) {

                // Send error message to the connection thread
                Message message = mAsrServerConnectionThread.obtainMessage();
                message.arg1 = AsrServerConnectionThread.MESSAGE_ON_CPQD_ASR_LIBRARY_ERROR;
                message.sendToTarget();

                for (RecognitionListener listener : mListeners) {
                    listener.onError(new RecognitionError(RecognitionErrorCode.FAILURE, "Recognition timeout"));
                }

                throw new RecognitionException(RecognitionErrorCode.FAILURE, "Recognition timeout");
            } else if (mError != null) {
                throw new RecognitionException(mError);
            } else {
                return Arrays.asList(mSentencesQueue.toArray(new RecognitionResult[mSentencesQueue.size()]));
            }

        } finally {
            // returns to original state; if there are calls in sequence to the wait () method, avoiding timeout occurring
            if (mReaderTask != null) {
                mReaderTask.finish();
            }
            mReaderTask = null;
        }
    }

    @Override
    public void close() throws RecognitionException {

        mState = STATE_WAITING_RELEASE_SESSION;

        mServerResponse = false;

        mError = null;

        // Cancel the audio recorder thread.
        if (mReaderTask != null) {
            mReaderTask.cancel();
        }

        // Notify the sentences queue
        synchronized (mSentencesQueue) {
            mSentencesQueue.notifyAll();
        }

        // Ask connection thread to establish connection with given URL.
        Message message = mAsrServerConnectionThread.obtainMessage();
        message.arg1 = AsrServerConnectionThread.MESSAGE_RELEASE_SESSION;
        message.sendToTarget();

        // Check if is in the correct state
        if (mState == STATE_WAITING_RELEASE_SESSION) {
            try {
                synchronized (mSeverResponseLock) {
                    mSeverResponseLock.wait(MAX_RESPONSE_TIMEOUT);
                }
            } catch (Exception e) {
                // ignoring
            }
        }

        if (!mServerResponse && mError == null) {

            // Send error message to the connection thread
            message = mAsrServerConnectionThread.obtainMessage();
            message.arg1 = AsrServerConnectionThread.MESSAGE_ON_CPQD_ASR_LIBRARY_ERROR;
            message.sendToTarget();

            for (RecognitionListener listener : mListeners) {
                listener.onError(new RecognitionError(RecognitionErrorCode.FAILURE, "Close operation timeout"));
            }

            throw new RecognitionException(RecognitionErrorCode.FAILURE, "Close timeout");
        } else if (mError != null) {
            throw new RecognitionException(mError);
        }
    }

    @Override
    public void cancelRecognition() throws RecognitionException {

        // Check if library is in expected state to accept message.
        if (mState != STATE_RECORDING && mState != STATE_WAITING_RECOGNITION) {
            return;
        }

        mState = STATE_WAITING_CANCEL_RECOGNITION;

        mServerResponse = false;

        mError = null;

        // Ask connection thread to cancel recognition.
        Message message = mAsrServerConnectionThread.obtainMessage();
        message.arg1 = AsrServerConnectionThread.MESSAGE_CANCEL_RECOGNITION;
        message.sendToTarget();

        // Cancel the audio recorder thread.
        if (mReaderTask != null) {
            mReaderTask.cancel();
        }

        // Notify the sentences queue
        synchronized (mSentencesQueue) {
            mSentencesQueue.notifyAll();
        }

        // Check if is in the correct state
        if (mState == STATE_WAITING_CANCEL_RECOGNITION) {
            try {
                synchronized (mSeverResponseLock) {
                    mSeverResponseLock.wait(MAX_RESPONSE_TIMEOUT);
                }
            } catch (Exception e) {
                // ignoring
            }
        }

        if (!mServerResponse && mError == null) {

            // Send error message to the connection thread
            message = mAsrServerConnectionThread.obtainMessage();
            message.arg1 = AsrServerConnectionThread.MESSAGE_ON_CPQD_ASR_LIBRARY_ERROR;
            message.sendToTarget();

            for (RecognitionListener listener : mListeners) {
                listener.onError(new RecognitionError(RecognitionErrorCode.FAILURE, "Cancel recognition operation timeout"));
            }

            throw new RecognitionException(RecognitionErrorCode.FAILURE, "Cancel recognition timeout");
        } else if (mError != null) {
            throw new RecognitionException(mError);
        }
    }

    @Override
    public void onListening() {
        Log.d(TAG, "[onListening]");

        // Change state of the reader task
        mReaderTask.readerStatus = ReaderTaskStatus.RUNNING;

        // Start the reader task
        new Thread(mReaderTask).start();
    }

    @Override
    public void onSpeechStart(Integer time) {
        Log.d(TAG, "[onSpeechStart] " + time);
    }

    @Override
    public void onSpeechStop(Integer time) {
        Log.d(TAG, "[onSpeechStop] " + time);
    }

    @Override
    public void onPartialRecognitionResult(PartialRecognitionResult result) {
        Log.d(TAG, "[onPartialRecognitionResult] " + result.toString());
    }

    @Override
    public void onRecognitionResult(RecognitionResult result) {

        if (!mSentencesQueue.offer(result)) {
            Log.w(TAG, "[onRecognitionResult] Messsage discarded, sentences queue is full");
        }

        // Received final result of the last segment
        if (result.isLastSpeechSegment()) {

            // Back the state to idle
            mState = STATE_IDLE;

            // The recognition is over. close the session
            if (mBuilder.autoClose) {

                mState = STATE_WAITING_RELEASE_SESSION;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            close();
                        } catch (RecognitionException e) {
                            //ignoring
                        }
                    }
                }).start();
            }

            // Finalize the reader task
            if (mReaderTask != null) {
                mReaderTask.finish();
            }

            // Notify the sentence queue
            synchronized (mSentencesQueue) {
                mSentencesQueue.notifyAll();
            }
        }
    }

    @Override
    public void onError(RecognitionError error) {

        // Back the state to idle
        mState = STATE_IDLE;

        // Set the error
        mError = error;

        // Cancel the reader thread.
        if (mReaderTask != null) {
            mReaderTask.cancel();
        }

        // Notify the server response
        synchronized (mSeverResponseLock) {
            mSeverResponseLock.notifyAll();
        }

        // Notify the sentence queue
        synchronized (mSentencesQueue) {
            mSentencesQueue.notifyAll();
        }
    }

    private class ReaderTask implements Runnable {

        /* Status of the reader task. */
        private ReaderTaskStatus readerStatus;

        /* The Builder object. */
        private SpeechRecognizer.Builder builder;

        private final AudioSource audio;

        ReaderTask(AudioSource audio, SpeechRecognizer.Builder builder) {
            super();
            this.audio = audio;
            this.builder = builder;
            this.readerStatus = ReaderTaskStatus.IDLE;
        }

        boolean isCancelled() {
            return readerStatus == ReaderTaskStatus.CANCELED;
        }

        boolean isFinished() {
            return readerStatus == ReaderTaskStatus.FINISHED;
        }

        boolean isIdle() {
            return readerStatus == ReaderTaskStatus.IDLE;
        }

        void cancel() {
            if (readerStatus != ReaderTaskStatus.FINISHED) {
                readerStatus = ReaderTaskStatus.CANCELED;
            }
        }

        void finish() {
            if (readerStatus != ReaderTaskStatus.CANCELED) {
                readerStatus = ReaderTaskStatus.FINISHED;
            }
        }

        @Override
        public void run() {

            // The buffer size
            final int chunkSize = Util.calculateBufferSize(builder.chunkLength,
                    builder.audioSampleRate, builder.encoding.getSampleSize());

            // Initiate the buffer
            byte[] buffer = new byte[chunkSize];

            // Delay is the duration of the package adjusted by the RTF
            final int DELAY = (int) (builder.chunkLength * builder.serverRTF);

            try {

                int read = 0;

                while (read != -1 && !isCancelled() && !isFinished()) {

                    read = audio.read(buffer);

                    byte[] bufferToSend;

                    if (read > 0 && read != chunkSize) {
                        bufferToSend = Arrays.copyOf(buffer, read);
                    } else {
                        bufferToSend = buffer;
                    }

                    if (read > 0) {
                        Message message = mAsrServerConnectionThread.obtainMessage();
                        message.arg1 = AsrServerConnectionThread.MESSAGE_HANDLE_AUDIO_PACKET;
                        message.arg2 = 0;
                        message.obj = bufferToSend;
                        message.sendToTarget();
                    } else if (read < 0) {
                        Message message = mAsrServerConnectionThread.obtainMessage();
                        message.arg1 = AsrServerConnectionThread.MESSAGE_HANDLE_AUDIO_PACKET;
                        message.arg2 = 1;
                        message.obj = new byte[]{};
                        message.sendToTarget();
                    }

                    Thread.sleep(DELAY);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                // Close the audio
                try {
                    audio.close();
                } catch (Exception e) {
                    //ignoring
                }
            }
        }
    }
}

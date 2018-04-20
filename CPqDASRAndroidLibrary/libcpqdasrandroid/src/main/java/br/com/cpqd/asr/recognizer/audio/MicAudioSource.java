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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/**
 * Audio source implementation for microphone input.
 */
public class MicAudioSource implements AudioSource {

    /* Log tag. */
    private static final String TAG = MicAudioSource.class.getSimpleName();

    /**
     * Recorder buffer size.
     * See {@link AudioRecord#getMinBufferSize(int, int, int)}.
     */
    private final int RECORDER_BUFFER_SIZE;

    /* Audio sample rate in hertz. */
    private int RECORDER_SAMPLE_RATE = 16000;

    /* Number of audio input channels. */
    private static final int RECORDER_NUMBER_OF_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    /* Audio format. */
    private static final int RECORDER_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /* Audio source. */
    private static final int RECORDER_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;

    private AudioRecord recorder;

    private boolean stopped;

    private boolean started;

    /**
     * Sets up object initial state.
     */
    public MicAudioSource(int sampleRate) throws IllegalArgumentException {

        if (sampleRate == 8000) {
            RECORDER_SAMPLE_RATE = 8000;
        } else {
            RECORDER_SAMPLE_RATE = 16000;
        }

        int minimumBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_NUMBER_OF_CHANNELS, RECORDER_AUDIO_FORMAT);

        if (minimumBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.w(TAG, "could not get minimum buffer size for AudioRecord instance: ERROR_BAD_VALUE");
            RECORDER_BUFFER_SIZE = 4096;
        } else if (minimumBufferSize == AudioRecord.ERROR) {
            Log.w(TAG, "could not get minimum buffer size for AudioRecord instance: ERROR");
            RECORDER_BUFFER_SIZE = 4096;
        } else {
            RECORDER_BUFFER_SIZE = minimumBufferSize;
        }

        recorder = new AudioRecord(RECORDER_AUDIO_SOURCE, RECORDER_SAMPLE_RATE, RECORDER_NUMBER_OF_CHANNELS, RECORDER_AUDIO_FORMAT, RECORDER_BUFFER_SIZE);

        stopped = false;
        started = false;
    }

    @Override
    public int read(byte[] b) throws IOException, NullPointerException {

        if (!started) {
            try {
                recorder.startRecording();
                started = true;
            } catch (IllegalStateException e) {
                Log.w(TAG, "could not start recording on AudioRecord instance", e);
                return -1;
            }
            return 0;
        }

        if (!stopped) {
            int shortsRead = recorder.read(b, 0, b.length);
            return shortsRead;
        } else {
            return -1;
        }
    }

    @Override
    public void close() throws IOException {
        Log.i(TAG, "close");
        finish();
    }

    @Override
    public void finish() throws IOException {

        Log.i(TAG, "finish");

        stopped = true;

        recorder.stop();

        recorder.release();
    }
}

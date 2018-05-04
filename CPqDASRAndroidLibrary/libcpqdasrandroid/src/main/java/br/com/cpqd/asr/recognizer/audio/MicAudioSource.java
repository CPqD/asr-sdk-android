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

/**
 * Audio source implementation for microphone input.
 */
public class MicAudioSource implements AudioSource {

    /* Log tag. */
    private static final String TAG = MicAudioSource.class.getSimpleName();

    /* Number of audio input channels. */
    private static final int RECORDER_NUMBER_OF_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    /* Audio format. */
    private static final int RECORDER_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /* Audio source. */
    private static final int RECORDER_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION;

    /* Audio record. */
    private AudioRecord recorder;

    /* Flag to indicate if the capture started. */
    private boolean started;

    /* Flag to indicate if is to stop the capture. */
    private boolean stopped;

    /**
     * Sets up object initial state.
     */
    public MicAudioSource(int sampleRate) throws IllegalArgumentException {

        int recorderSampleRate;

        if (sampleRate == 8000) {
            recorderSampleRate = 8000;
        } else {
            recorderSampleRate = 16000;
        }

        int minimumBufferSize = AudioRecord.getMinBufferSize(recorderSampleRate, RECORDER_NUMBER_OF_CHANNELS, RECORDER_AUDIO_FORMAT);

        int recorderBufferSize;

        if (minimumBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.w(TAG, "could not get minimum buffer size for AudioRecord instance: ERROR_BAD_VALUE");
            recorderBufferSize = 4096;
        } else if (minimumBufferSize == AudioRecord.ERROR) {
            Log.w(TAG, "could not get minimum buffer size for AudioRecord instance: ERROR");
            recorderBufferSize = 4096;
        } else {
            recorderBufferSize = minimumBufferSize;
        }

        recorder = new AudioRecord(RECORDER_AUDIO_SOURCE, recorderSampleRate, RECORDER_NUMBER_OF_CHANNELS, RECORDER_AUDIO_FORMAT, recorderBufferSize);

        stopped = false;

        started = false;
    }

    @Override
    public int read(byte[] b) throws NullPointerException {

        // Start the recorder if is the first reading
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

        // Read the audio data if the microphone has not been closed
        if (!stopped) {

            //noinspection UnnecessaryLocalVariable
            int shortsRead = recorder.read(b, 0, b.length);
            return shortsRead;

        } else {

            return -1;
        }
    }

    @Override
    public void close() {

        Log.d(TAG, "close");

        finish();
    }

    @Override
    public void finish() {

        Log.d(TAG, "finish");

        stopped = true;

        recorder.stop();

        recorder.release();
    }
}

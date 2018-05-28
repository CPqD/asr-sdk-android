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

import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.model.PartialRecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionError;

/**
 * Asynchronous callback interface to receive speech recognition events from the
 * server.
 */
public interface RecognitionListener {

    /**
     * Called when the server is ready to recognize and is listening for audio.
     */
    void onListening();

    /**
     * Called when the server detects start of speech in the audio samples. The
     * server keeps on listening for audio packets and performing the speech
     * recognition.
     *
     * @param time the audio position when the speech start was detected (in
     *             milis).
     */
    void onSpeechStart(Integer time);

    /**
     * Called when the server detects the end of speech in the audio samples.
     * The server stops listening and process the final recognition result.
     *
     * @param time the audio position when the speech stop was detected (in
     *             milis).
     */
    void onSpeechStop(Integer time);

    /**
     * Called when the server generates a partial recognition result.
     *
     * @param result the recognition result.
     */
    void onPartialRecognitionResult(PartialRecognitionResult result);

    /**
     * Called when the server generates a recognition result.
     *
     * @param result the recognition result.
     */
    void onRecognitionResult(RecognitionResult result);

    /**
     * Called when an error in the recognition process occurs.
     *
     * @param error the error object.
     */
    void onError(RecognitionError error);

}

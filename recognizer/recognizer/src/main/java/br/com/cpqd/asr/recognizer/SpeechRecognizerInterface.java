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

import java.io.IOException;
import java.util.List;

import br.com.cpqd.asr.recognizer.audio.AudioSource;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;

/**
 * The SpeechRecognizer allows a client application to submit an audio input
 * source to the CPqD Speech Recognition Server.
 */
public interface SpeechRecognizerInterface {

    /**
     * Release resources and close the server connection.
     *
     * @throws IOException some sort of I/O exception has ocurred.
     */
    void close() throws IOException, RecognitionException;

    /**
     * Cancels the current recognition, closing the audio source.
     *
     * @throws IOException          some sort of I/O exception has ocurred.
     * @throws RecognitionException in case the operation fails.
     */
    void cancelRecognition() throws IOException, RecognitionException;

    /**
     * Recognizes an audio source. The recognition session with the server must
     * be created previously. The recognition result will be notified in the
     * registered AsrListener callbacks. The audio source is automatically
     * closed after the end of the recognition process.
     *
     * @param lmList the language model to use.
     * @param audio  audio source.
     * @throws IOException          some sort of I/O exception has ocurred.
     * @throws RecognitionException in case the operation fails.
     */
    void recognize(AudioSource audio, LanguageModelList lmList) throws IOException, RecognitionException;

    /**
     * Recognizes an audio source. The recognition session with the server must
     * be created previously. The recognition result will be notified in the
     * registered AsrListener callbacks. The audio source is automatically
     * closed after the end of the recognition process.
     *
     * @param lmList the language model to use.
     * @param audio  audio source.
     * @param config recognition configuration parameters.
     * @throws IOException          some sort of I/O exception has ocurred.
     * @throws RecognitionException in case the operation fails.
     */
    void recognize(AudioSource audio, LanguageModelList lmList, RecognitionConfig config)
            throws IOException, RecognitionException;

    /**
     * Returns the recognition result. If audio packets are still being sent to
     * the server, the method blocks and waits for the end of the recognition
     * process.
     *
     * @return the recognition result or null if there is no result available.
     * @throws RecognitionException in case an error in the recognition occurs.
     */
    List<RecognitionResult> waitRecognitionResult() throws RecognitionException;

    /**
     * Returns the recognition result. If audio packets are still being sent to the
     * server, the method blocks and waits for the end of the recognition process.
     *
     * @param timeout the max wait time for a recognition result (in seconds). The timer
     *                is started after the last audio packet is sent.
     * @return the recognition result or null if there is no result available.
     * @throws RecognitionException in case an error in the recognition occurs.
     */
    List<RecognitionResult> waitRecognitionResult(int timeout) throws RecognitionException;
}

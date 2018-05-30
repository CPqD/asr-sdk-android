/*******************************************************************************
 * Copyright 2018 CPqD. All Rights Reserved.
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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.util.List;

import br.com.cpqd.asr.recognizer.audio.AudioSource;
import br.com.cpqd.asr.recognizer.audio.FileAudioSource;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.PartialRecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionError;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class RecognizerBuilderTest {

    private static final Context mContext = InstrumentationRegistry.getTargetContext();

    @Test
    public void urlNull() {
        try {
            SpeechRecognizer.builder().build(mContext);
            fail("URISyntaxException expected");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("NullPointerException expected, instead of " + e.getCause());
        }
    }

    @Test
    public void urlInvalid() {
        try {
            String url = "abcdasr";
            SpeechRecognizer.builder().serverURL(url).build(mContext);
            fail("URISyntaxException expected");
        } catch (URISyntaxException e) {
            assertTrue(e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("URISyntaxException expected, instead of " + e.getCause());
        }
    }

    @Test
    public void recogConfig() {
        int numberOfSentences = 3;
        try {
            RecognitionConfig recognitionConfig = RecognitionConfig.builder().maxSentences(numberOfSentences).build();
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL).credentials(TestConstants.ASR_User, TestConstants.ASR_Pass)
                    .recogConfig(recognitionConfig).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.PizzaVegAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + numberOfSentences, results.get(0).getAlternatives().size() == numberOfSentences);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void multipleListeners() {

        final int[] startCounter = new int[2];
        final int[] stopCounter = new int[2];
        final int[] listeningCounter = new int[2];
        final int[] partialCounter = new int[2];
        final int[] finalCounter = new int[2];
        final int pos1 = 0;
        final int pos2 = 1;

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL).credentials(TestConstants.ASR_User, TestConstants.ASR_Pass)
                    .recogConfig(RecognitionConfig.builder().build()).addListener(new RecognitionListener() {

                        @Override
                        public void onSpeechStop(Integer time) {
                            stopCounter[pos1]++;
                        }

                        @Override
                        public void onSpeechStart(Integer time) {
                            startCounter[pos1]++;
                        }

                        @Override
                        public void onRecognitionResult(RecognitionResult result) {
                            finalCounter[pos1]++;
                        }

                        @Override
                        public void onPartialRecognitionResult(PartialRecognitionResult result) {
                            partialCounter[pos1]++;
                        }

                        @Override
                        public void onListening() {
                            listeningCounter[pos1]++;
                        }

                        @Override
                        public void onError(RecognitionError error) {
                        }
                    }).addListener(new RecognitionListener() {

                        @Override
                        public void onSpeechStop(Integer time) {
                            stopCounter[pos2]++;
                        }

                        @Override
                        public void onSpeechStart(Integer time) {
                            startCounter[pos2]++;
                        }

                        @Override
                        public void onRecognitionResult(RecognitionResult result) {
                            finalCounter[pos2]++;
                        }

                        @Override
                        public void onPartialRecognitionResult(PartialRecognitionResult result) {
                            partialCounter[pos2]++;
                        }

                        @Override
                        public void onListening() {
                            listeningCounter[pos2]++;
                        }

                        @Override
                        public void onError(RecognitionError error) {
                        }
                    }).build(mContext);

            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.PizzaVegAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            recognizer.waitRecognitionResult();
            assertTrue("Compare start counter", startCounter[pos1] == startCounter[pos2]);
            assertTrue("Compare stop counter", stopCounter[pos1] == stopCounter[pos2]);
            assertTrue("Compare listen counter", listeningCounter[pos1] == listeningCounter[pos2]);
            assertTrue("Compare partial counter", partialCounter[pos1] == partialCounter[pos2]);
            assertTrue("Compare final counter", finalCounter[pos1] == finalCounter[pos2]);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }
}

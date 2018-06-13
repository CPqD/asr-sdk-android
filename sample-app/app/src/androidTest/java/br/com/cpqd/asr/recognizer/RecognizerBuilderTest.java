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

import org.junit.Assert;
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
import br.com.cpqd.asr.recognizer.model.RecognitionResultCode;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
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
            fail("URISyntaxException was expected");
        } catch (URISyntaxException e) {
            assertNotNull(e.getMessage());
            System.out.println("### URISyntaxException is expected.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("URISyntaxException was expected, not: " + e.getCause());
        }
    }

    @Test
    public void credentialValid() {
        try {
            SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL)
                    .credentials(TestConstants.ASR_User, TestConstants.ASR_Pass).build(mContext);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception was expected");
        }
    }

    @Test
    public void credentialInvalid() {
        try {
            SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL)
                    .credentials("blabla", "blublu").build(mContext);
            fail("IOException expected");
        } catch (Exception e) {
            e.printStackTrace();
            fail("IOException expected, instead of " + e.getCause());
        }
    }

    @Test
    public void recogConfig() {
        try {
            RecognitionConfig recognitionConfig = RecognitionConfig.builder()
                    .confidenceThreshold(100)
                    .continuousMode(false)
                    .endPointerAutoLevelLen(350)
                    .endPointerLevelMode(0)
                    .endPointerLevelThreshold(4)
                    .headMarginMilis(250)
                    .maxSentences(3)
                    .noInputTimeoutEnabled(false)
                    .noInputTimeoutMilis(2000)
                    .recognitionTimeoutEnabled(false)
                    .recognitionTimeoutMilis(65000)
                    .startInputTimers(false)
                    .tailMarginMilis(450)
                    .waitEndMilis(900)
                    .build();

            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .recogConfig(recognitionConfig).build(mContext);

            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.PizzaVegAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            Assert.assertEquals("Result Status is not the expected.", RecognitionResultCode.NO_MATCH, results.get(0).getResultCode());
            Assert.assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());
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
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .recogConfig(RecognitionConfig.builder().build()).addListener(new RecognitionListener() {

                        @Override
                        public void onSpeechStart(Integer time) {
                            startCounter[pos1]++;
                        }

                        @Override
                        public void onSpeechStop(Integer time) {
                            stopCounter[pos1]++;
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
                        public void onSpeechStart(Integer time) {
                            startCounter[pos2]++;
                        }

                        @Override
                        public void onSpeechStop(Integer time) {
                            stopCounter[pos2]++;
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
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.PizzaGramHttp).build());
            recognizer.waitRecognitionResult();

            Thread.sleep(3000);

            int i;
            for (i = pos1; i <= pos2; i++) {
                assertEquals("Listen counter was not incremented.", 1, listeningCounter[i]);
                assertEquals("Final counter was not incremented.", 1, finalCounter[i]);
//                assertEquals("Start counter was not incremented.", 1, startCounter[i]);
//                assertEquals("Stop counter was not incremented.", 1, stopCounter[i]);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void maxWaitSeconds() {

        int maxWaitSec = 1;
        long startTimeMS = 0, stopTimeMS, elapsedTimeMS;

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).maxWaitSeconds(maxWaitSec).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.BigAudio));

            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());

            startTimeMS = System.currentTimeMillis();

            recognizer.waitRecognitionResult();

        } catch (RecognitionException e) {
            System.out.println("### RecognitionException is expected.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("RecognitionException was expected, not: " + e.getMessage());
        }

        stopTimeMS = System.currentTimeMillis();
        elapsedTimeMS = stopTimeMS - startTimeMS;
        System.out.println("### Elapsed Time is: " + elapsedTimeMS + " ms");
        assertTrue("Recognition time must be smaller than maxWait", elapsedTimeMS <= (maxWaitSec * 1000) * 1.1);
    }
}

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

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import br.com.cpqd.asr.recognizer.audio.AudioSource;
import br.com.cpqd.asr.recognizer.audio.BufferAudioSource;
import br.com.cpqd.asr.recognizer.audio.FileAudioSource;
import br.com.cpqd.asr.recognizer.model.Interpretation;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionResultCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class SpeechRecognizerTest {

    private static final Context mContext = InstrumentationRegistry.getTargetContext();

    private int defaultPacketDelay = 100;

    @Test
    public void basicGrammar() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.CpfAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.CpfGramHttp).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            List<Interpretation> interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();

            String textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            String firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.CpfText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.CpfInterp, firstInterpFromFirstAlt);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void basicSLM() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.NoEndSilenceAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            String textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.NoEndSilenceText, textFromFirstAlternative);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void noMatchGrammar() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.PizzaVegAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.CpfGramHttp).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.NO_MATCH, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void noSpeech() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.SilenceAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.NO_SPEECH, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void noInputTimeout() {

        int noInputTimeout = 2000;

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .recogConfig(RecognitionConfig.builder().noInputTimeoutEnabled(true)
                            .noInputTimeoutMilis(noInputTimeout).startInputTimers(true).build())
                    .build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.SilenceAudio, defaultPacketDelay);

            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.NO_INPUT_TIMEOUT, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void recognizeBufferAudioSource() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.PizzaGramHttp).build());
            WriteToBufferAudioSource(audio, TestConstants.PizzaVegAudio, defaultPacketDelay);

            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            List<Interpretation> interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();

            String textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            String firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();


            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.PizzaVegText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.PizzaVegInterp, firstInterpFromFirstAlt);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void recognizeBufferBlockRead() {

        int noInputTimeout = 15000;
        int maxSentences = 2;

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .recogConfig(RecognitionConfig.builder().noInputTimeoutEnabled(true)
                            .noInputTimeoutMilis(noInputTimeout).startInputTimers(true).maxSentences(maxSentences).build())
                    .build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.PizzaVegAudio, 2 * defaultPacketDelay);

            List<RecognitionResult> results = recognizer.waitRecognitionResult(noInputTimeout);
            String textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", maxSentences, results.get(0).getAlternatives().size());
            assertEquals("Recognized Text is not the expected.", TestConstants.PizzaVegText, textFromFirstAlternative);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void recognizeMaxWaitSeconds() {

        // Use wait timer very short to force timeout
        int maxWait = 1;
        long startTimeMS = 0, stopTimeMS, elapsedTimeMS;

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.NoEndSilenceAudio, defaultPacketDelay);

            startTimeMS = System.currentTimeMillis();

            recognizer.waitRecognitionResult(maxWait);
            // There is no assert hear, because de debug from the last assert is more useful
        } catch (RecognitionException e) {
            System.out.println("### RecognitionException is expected.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed, because RecognitionException was expected, not: " + e.getMessage());
        }

        stopTimeMS = System.currentTimeMillis();
        elapsedTimeMS = stopTimeMS - startTimeMS;
        System.out.println("### Elapsed Time is: " + elapsedTimeMS + " ms");
        assertTrue("Recognition time must be smaller than " + maxWait + " seconds", elapsedTimeMS <= (maxWait * 1000) * 1.1);
    }

    @Test
    public void closeWhileRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.NoEndSilenceAudio, defaultPacketDelay);

            recognizer.close();

            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            assertTrue("Result is not empty.", results.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void closeWithoutRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);

            recognizer.close();

            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Result is not empty.", results.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void cancelWhileRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.NoEndSilenceAudio, defaultPacketDelay);

            recognizer.cancelRecognition();
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Result is not empty.", results.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void cancelNoRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            recognizer.cancelRecognition();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void waitNoRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Result is not empty.", results.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void waitRecognitionResultDuplicate() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(TestConstants.CpfAudio));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.CpfGramHttp).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            List<Interpretation> interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();

            String textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            String firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.CpfText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.CpfInterp, firstInterpFromFirstAlt);

            long startTimeMS, elapsedTimeMS;

            startTimeMS = System.currentTimeMillis();
            results = recognizer.waitRecognitionResult();
            elapsedTimeMS = System.currentTimeMillis() - startTimeMS;

            assertTrue("Elapsed time should be very short.", elapsedTimeMS < 10);
            assertTrue("Result is not empty.", results.isEmpty());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void duplicateRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.NoEndSilenceAudio, defaultPacketDelay);

            Thread.sleep(1500);
            try {
                recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            } catch (RecognitionException e) {
                System.out.println("### RecognitionException is expected.");
            } catch (Exception e) {
                e.printStackTrace();
                fail("RecognitionException was expected, not: " + e.getMessage());
            }

            // Send silence to finish the recognition
            WriteToBufferAudioSource(audio, TestConstants.SilenceAudio, defaultPacketDelay);

            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            String textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.NoEndSilenceText, textFromFirstAlternative);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void multipleRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .connectOnRecognize(false).autoClose(false).build(mContext);

            BufferAudioSource audioBuffer1 = new BufferAudioSource();
            BufferAudioSource audioBuffer2 = new BufferAudioSource();
            BufferAudioSource audioBuffer3 = new BufferAudioSource();
            FileAudioSource audioFile = new FileAudioSource(mContext.getAssets().open(TestConstants.NoEndSilenceAudio));
            FileAudioSource audioSilenceFile = new FileAudioSource(mContext.getAssets().open(TestConstants.SilenceAudio));

            List<RecognitionResult> results;
            List<Interpretation> interpretationsFromFirstAlt;
            String textFromFirstAlternative, firstInterpFromFirstAlt;

            System.out.println("### Recognition #1");
            recognizer.recognize(audioFile, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.NoEndSilenceText, textFromFirstAlternative);

            System.out.println("### Recognition #2");
            recognizer.recognize(audioBuffer1, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audioBuffer1, TestConstants.ContinuousModeAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.ContinuousModeTextSeg1, textFromFirstAlternative);
            assertTrue("Interpretations list should be empty.", interpretationsFromFirstAlt.isEmpty());

            System.out.println("### Recognition #3");
            recognizer.recognize(audioSilenceFile, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.NO_SPEECH, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());

            System.out.println("### Recognition #4");
            recognizer.recognize(audioBuffer2, LanguageModelList.builder().addFromURI(TestConstants.PizzaGramHttp).build());
            WriteToBufferAudioSource(audioBuffer2, TestConstants.PizzaVegAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.PizzaVegText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.PizzaVegInterp, firstInterpFromFirstAlt);

            System.out.println("### Recognition #5");
            Thread.sleep(5000);
            recognizer.recognize(audioBuffer3, LanguageModelList.builder().addFromURI(TestConstants.BankGramHttp).build());
            WriteToBufferAudioSource(audioBuffer3, TestConstants.BancoTransfiraAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.BancoTransfiraText, textFromFirstAlternative);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void multiplesConnectOnRecognize() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .connectOnRecognize(true).autoClose(false).build(mContext);

            BufferAudioSource audioBuffer1 = new BufferAudioSource();
            BufferAudioSource audioBuffer2 = new BufferAudioSource();
            BufferAudioSource audioBuffer3 = new BufferAudioSource();
            FileAudioSource audioFile1 = new FileAudioSource(mContext.getAssets().open(TestConstants.BancoTransfiraAudio));
            FileAudioSource audioFile2 = new FileAudioSource(mContext.getAssets().open(TestConstants.NoEndSilenceAudio));

            List<RecognitionResult> results;
            List<Interpretation> interpretationsFromFirstAlt;
            String textFromFirstAlternative, firstInterpFromFirstAlt;

            System.out.println("### Recognition #1");
            recognizer.recognize(audioBuffer1, LanguageModelList.builder().addFromURI(TestConstants.PizzaGramHttp).build());
            WriteToBufferAudioSource(audioBuffer1, TestConstants.PizzaVegAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.PizzaVegText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.PizzaVegInterp, firstInterpFromFirstAlt);

            System.out.println("### Recognition #2");
            recognizer.recognize(audioBuffer2, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audioBuffer2, TestConstants.SilenceAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.NO_INPUT_TIMEOUT, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());

            System.out.println("### Recognition #3");
            recognizer.recognize(audioFile1, LanguageModelList.builder().addFromURI(TestConstants.BankGramHttp).build());
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.BancoTransfiraText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.BancoTransfiraInterp, firstInterpFromFirstAlt);

            System.out.println("### Recognition #4");
            recognizer.recognize(audioBuffer3, LanguageModelList.builder().addFromURI(TestConstants.BankGramHttp).build());
            WriteToBufferAudioSource(audioBuffer3, TestConstants.BancoTransfiraAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.BancoTransfiraText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.BancoTransfiraInterp, firstInterpFromFirstAlt);

            System.out.println("### Recognition #5");
            Thread.sleep(5000);
            recognizer.recognize(audioFile2, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.NoEndSilenceText, textFromFirstAlternative);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void multiplesAutoClose() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .recogConfig(RecognitionConfig.builder().recognitionTimeoutEnabled(true)
                            .recognitionTimeoutMilis(5000).startInputTimers(true).build())
                    .connectOnRecognize(true).autoClose(true).build(mContext);

            BufferAudioSource audioBuffer1 = new BufferAudioSource();
            BufferAudioSource audioBuffer2 = new BufferAudioSource();
            FileAudioSource audioFile1 = new FileAudioSource(mContext.getAssets().open(TestConstants.BancoTransfiraAudio));
            FileAudioSource audioFile2 = new FileAudioSource(mContext.getAssets().open(TestConstants.NoEndSilenceAudio));
            FileAudioSource audioFile3 = new FileAudioSource(mContext.getAssets().open(TestConstants.PizzaVegAudio));

            List<RecognitionResult> results;
            List<Interpretation> interpretationsFromFirstAlt;
            String textFromFirstAlternative, firstInterpFromFirstAlt;

            System.out.println("### Recognition #1");
            recognizer.recognize(audioFile1, LanguageModelList.builder().addFromURI(TestConstants.BankGramHttp).build());
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.BancoTransfiraText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.BancoTransfiraInterp, firstInterpFromFirstAlt);

            System.out.println("### Recognition #2");
            recognizer.recognize(audioBuffer1, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audioBuffer1, TestConstants.BigAudio, 2 * defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNITION_TIMEOUT, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());

            System.out.println("### Recognition #3");
            recognizer.recognize(audioFile2, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.NoEndSilenceText, textFromFirstAlternative);


            System.out.println("### Recognition #4");
            recognizer.recognize(audioBuffer2, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audioBuffer2, TestConstants.NoEndSilenceAudio, 2 * defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNITION_TIMEOUT, results.get(0).getResultCode());
            assertEquals("Number of alternatives is not the expected.", 0, results.get(0).getAlternatives().size());

            System.out.println("### Recognition #5");
            Thread.sleep(5000);
            recognizer.recognize(audioFile3, LanguageModelList.builder().addFromURI(TestConstants.PizzaGramHttp).build());
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.PizzaVegText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.PizzaVegInterp, firstInterpFromFirstAlt);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void recogAfterSessionTimeout() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal).build(mContext);

            List<RecognitionResult> results;
            List<Interpretation> interpretationsFromFirstAlt;
            String textFromFirstAlternative, firstInterpFromFirstAlt;
            long startTimeMS, elapsedTimeMS;

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(TestConstants.BancoTransfiraAudio)),
                    LanguageModelList.builder().addFromURI(TestConstants.BankGramHttp).build());

            Thread.sleep(6500);

            startTimeMS = System.currentTimeMillis();
            results = recognizer.waitRecognitionResult();
            elapsedTimeMS = System.currentTimeMillis() - startTimeMS;

            assertTrue("Elapsed time should be very short.", elapsedTimeMS < 10);

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.BancoTransfiraText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.BancoTransfiraInterp, firstInterpFromFirstAlt);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void continuousMode() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .recogConfig(RecognitionConfig.builder().continuousMode(true).build()).build(mContext);

            List<RecognitionResult> results;
            String textFromFirstAlternative;

            List<String> ContinuousModeTextSegments = Arrays.asList(TestConstants.ContinuousModeTextSeg1, TestConstants.ContinuousModeTextSeg2,
                    TestConstants.ContinuousModeTextSeg3, TestConstants.ContinuousModeTextSeg4);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.FreeLanguageModel).build());
            WriteToBufferAudioSource(audio, TestConstants.ContinuousModeAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            int expectedAlternatives, i;
            expectedAlternatives = ContinuousModeTextSegments.size();
            // +1 is because of No Last Pack sign will be sent
            assertEquals("Number of Results is not the expected.", expectedAlternatives + 1, results.size());

            for (i = 0; i < expectedAlternatives; i++) {
                textFromFirstAlternative = results.get(i).getAlternatives().get(0).getText();
                assertEquals("Recognized Text is not the expected.", ContinuousModeTextSegments.get(i), textFromFirstAlternative);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void strangeBehaviorAudioCpfLastPacketFalse() {

        try {
            SpeechRecognizerInterface recognizer = SpeechRecognizer.builder().serverURL(TestConstants.ASR_URL_Internal)
                    .connectOnRecognize(true).autoClose(false).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();

            List<RecognitionResult> results;
            List<Interpretation> interpretationsFromFirstAlt;
            String textFromFirstAlternative, firstInterpFromFirstAlt;

            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(TestConstants.CpfGramHttp).build());
            WriteToBufferAudioSource(audio, TestConstants.CpfAudio, defaultPacketDelay);
            results = recognizer.waitRecognitionResult();

            textFromFirstAlternative = results.get(0).getAlternatives().get(0).getText();
            interpretationsFromFirstAlt = results.get(0).getAlternatives().get(0).getInterpretations();
            firstInterpFromFirstAlt = interpretationsFromFirstAlt.get(0).getInterpretation();
            assertEquals("Result Status is not the expected.", RecognitionResultCode.RECOGNIZED, results.get(0).getResultCode());
            assertEquals("Recognized Text is not the expected.", TestConstants.CpfText, textFromFirstAlternative);
            assertEquals("Interpretation is not the expected.", TestConstants.CpfInterp, firstInterpFromFirstAlt);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    // Aux function
    private void WriteToBufferAudioSource(BufferAudioSource audio, String audioName, int packetDelay) throws Exception {
        InputStream input = mContext.getAssets().open(audioName);
        // Read the audio file and write into AudioSource
        byte[] buffer = new byte[1600]; // 100 ms segment (tx 8kHz)
        int len;
        boolean keepWriting = true;
        while ((len = input.read(buffer)) != -1 && keepWriting) {
            // delays the audio writing to simulate real time
            Thread.sleep(packetDelay);
            keepWriting = audio.write(buffer, len);
        }
        input.close();
        // It's not expected of BufferAudioSource to send Last Packet = true... theoretically it doesn't know the when the stream end.
        //audio.finish();
    }
}
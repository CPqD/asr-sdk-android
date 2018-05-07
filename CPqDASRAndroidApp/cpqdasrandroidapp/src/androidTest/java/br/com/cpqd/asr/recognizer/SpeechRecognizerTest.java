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
import java.util.List;

import br.com.cpqd.asr.recognizer.audio.AudioSource;
import br.com.cpqd.asr.recognizer.audio.BufferAudioSource;
import br.com.cpqd.asr.recognizer.audio.FileAudioSource;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionResultCode;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class SpeechRecognizerTest {

    private static final Context mContext = InstrumentationRegistry.getTargetContext();

    private static final String url = "wss://speech.cpqd.com.br/asr/ws/estevan/recognize/8k"; //"wss://speech.cpqd.com.br/asr/ws/v2/recognize/8k"; // "wss://speech.cpqd.com.br/asr/ws/estevan/recognize/8k";
    private static final String user = "estevan";
    private static final String passwd = "Thect195";
    private static final String filename = "pizza-veg-8k.wav";
    private static final String lmName = "builtin:slm/general";

    @Test
    public void basicGrammar() {

        String filename = "cpf_8k.wav";
        String lmName = "builtin:grammar/cpf";

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(filename));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);
            assertTrue("Contains interpretation",
                    results.get(0).getAlternatives().get(0).getInterpretations().size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Test
    public void basicSLM() {

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(filename));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Test
    public void noMatchGrammar() {
        String lmName = "builtin:grammar/number";

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(filename));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() == 0);
            assertTrue("Result is " + results.get(0).getResultCode(),
                    results.get(0).getResultCode() == RecognitionResultCode.NO_MATCH);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Test
    public void noSpeech() {
        String filename = "silence-8k.wav";

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(filename));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is 0", results.get(0).getAlternatives().size() == 0);
            assertTrue("Result is " + results.get(0).getResultCode(),
                    results.get(0).getResultCode() == RecognitionResultCode.NO_SPEECH);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Test
    public void noInputTimeout() {
        int packetDelay = 130;
        int noInputTimeout = 2000;
        String filename = "silence-8k.wav";

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd)
                    .recogConfig(RecognitionConfig.builder().noInputTimeoutEnabled(true)
                            .noInputTimeoutMilis(noInputTimeout).startInputTimers(true).build())
                    .build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            InputStream input = mContext.getAssets().open(filename);
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[1600]; // segmento de 100 ms (tx 8kHz)
            int len;
            boolean keepWriting = true;
            while ((len = input.read(buffer)) != -1 && keepWriting) {
                // atrasa o envio do proximo segmento para estourar a temporização de inicio de
                // fala
                Thread.sleep(packetDelay);
                keepWriting = audio.write(buffer, len);
            }
            input.close();
            audio.finish();

            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() == 0);
            assertTrue("Result is " + results.get(0).getResultCode(),
                    results.get(0).getResultCode() == RecognitionResultCode.NO_INPUT_TIMEOUT);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void recognizeBufferAudioSource() {
        int packetDelay = 90;

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            InputStream input = mContext.getAssets().open(filename);
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[1600]; // segmento de 100 ms (tx 8kHz)
            int len;
            boolean keepWriting = true;
            while ((len = input.read(buffer)) != -1 && keepWriting) {
                // atrasa o envio do proximo segmento para simular uma captura de audio em tempo
                // real.
                Thread.sleep(packetDelay);
                keepWriting = audio.write(buffer, len);
            }
            input.close();
            audio.finish();

            List<RecognitionResult> results = recognizer.waitRecognitionResult(10);

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() == 1);
            assertTrue("Result is " + results.get(0).getResultCode(),
                    results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void recognizeBufferBlockRead() {
        int packetDelay = 100; // delay grande para bloquear a thread de leitura
        int noInputTimeout = 8000;

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder()
                    .serverURL(url).credentials(user, passwd)
                    .recogConfig(RecognitionConfig.builder().noInputTimeoutEnabled(true).noInputTimeoutMilis(noInputTimeout).startInputTimers(true).build())
                    .build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            InputStream input = mContext.getAssets().open(filename);
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[16000]; // segmento de 100 ms (tx 8kHz)
            int len;
            boolean keepWriting = true;
            while ((len = input.read(buffer)) != -1 && keepWriting) {
                // atrasa o envio do proximo segmento
                Thread.sleep(packetDelay);
                keepWriting = audio.write(buffer, len);
            }
            input.close();
            audio.finish();

            List<RecognitionResult> results = recognizer.waitRecognitionResult(10);

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() == 1);

            assertTrue("Result is " + results.get(0).getResultCode(),
                    results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void recognizeMaxWaitSeconds() {
        // utiliza um tempo de espera muito curto e audio grande para forçar o timeout
        int maxWait = 1;
        int packetDelay = 50;

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).maxWaitSeconds(maxWait).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            InputStream input = mContext.getAssets().open(filename);
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[1600]; // segmento de 100 ms (tx 8kHz)
            int len;
            boolean keepWriting = true;
            while ((len = input.read(buffer)) != -1 && keepWriting) {
                Thread.sleep(packetDelay);
                keepWriting = audio.write(buffer, len);
            }
            input.close();
            audio.finish();

            recognizer.waitRecognitionResult();

            fail("Timeout expected");
        } catch (RecognitionException e) {
            assertTrue("Recognition timeout", true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void closeWhileRecognizing() {
        int packetDelay = 100;

        SpeechRecognizerInterface recognizer;
        try (InputStream input = mContext.getAssets().open(filename)) {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[1600]; // segmento de 100 ms (tx 8kHz)
            int len;
            int counter = 2;
            while ((len = input.read(buffer)) != -1 && counter > 0) {
                Thread.sleep(packetDelay);
                audio.write(buffer, len);
                // conta quantos pacotes sao enviados para interromper
                counter--;
            }

            // fecha a conexao no meio do reconhecimento
            recognizer.close();

            List<RecognitionResult> results = recognizer.waitRecognitionResult();
            assertTrue("Result is " + (results.isEmpty() ? "empty" : Integer.toString(results.size())), results.isEmpty());
        } catch (RecognitionException e) {
            fail("Empty result expected");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void closeWithoutRecognize() {

        SpeechRecognizerInterface recognizer;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);

            recognizer.close();

            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Result is " + (results.isEmpty() ? "empty" : Integer.toString(results.size())), results.isEmpty());
        } catch (RecognitionException e) {
            fail("Empty result expected");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void cancelWhileRecognize() {
        int packetDelay = 100;

        SpeechRecognizerInterface recognizer = null;
        try (InputStream input = mContext.getAssets().open(filename)) {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            BufferAudioSource audio = new BufferAudioSource();

            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[1600]; // segmento de 100 ms (tx 8kHz)
            int len;
            int counter = 2;
            while ((len = input.read(buffer)) != -1 && counter > 0) {
                Thread.sleep(packetDelay);
                audio.write(buffer, len);
                // conta quantos pacotes sao enviados para interromper
                counter--;
            }

            recognizer.cancelRecognition();
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Result is " + (results.isEmpty() ? "empty" : Integer.toString(results.size())),
                    results.isEmpty());
        } catch (RecognitionException e) {
            fail("Empty result expected");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void cancelNoRecognize() {

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            recognizer.cancelRecognition();
            assertTrue("Normal return", true);

        } catch (RecognitionException e) {
            fail("Failure: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void waitNoRecognize() {

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Result is " + (results.isEmpty() ? "empty" : Integer.toString(results.size())),
                    results.isEmpty());
        } catch (RecognitionException e) {
            fail("Failure: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void waitRecognitionResultDuplicate() {

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);
            AudioSource audio = new FileAudioSource(mContext.getAssets().open(filename));
            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            results = recognizer.waitRecognitionResult();
            assertTrue("Result is " + (results.isEmpty() ? "empty" : Integer.toString(results.size())),
                    results.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void duplicateRecognize() {

        SpeechRecognizerInterface recognizer = null;
        try (InputStream input = mContext.getAssets().open(filename)) {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd).build(mContext);

            BufferAudioSource audio = new BufferAudioSource();

            recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());

            // faz a leitura do arquivo e escreve no AudioSource
            byte[] buffer = new byte[1600]; // segmento de 100 ms (tx 8kHz)
            int len;
            int counter = 1;
            while ((len = input.read(buffer)) != -1 && counter > 0) {
                audio.write(buffer, len);
                // conta quantos pacotes sao enviados para interromper
                counter--;
            }

            try {
                recognizer.recognize(audio, LanguageModelList.builder().addFromURI(lmName).build());
            } catch (Exception e) {
                assertTrue("Failure expected", true);
            }

            // continua a enviar o audio
            while ((len = input.read(buffer)) != -1) {
                audio.write(buffer, len);
            }
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);
        } catch (RecognitionException e) {
            fail("Empty result expected");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void multipleRecognize() {

        int maxSessionIdleSeconds = 5;
        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd)
                    .maxSessionIdleSeconds(maxSessionIdleSeconds).build(mContext);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // aguarda e repete o recog
            Thread.sleep(4000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // aguarda e repete o recog
            Thread.sleep(4000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // força o timeout
            Thread.sleep(6000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void multiplesConnectOnRecognize() {
        int maxSessionIdleSeconds = 5;

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd)
                    .maxSessionIdleSeconds(maxSessionIdleSeconds).connectOnRecognize(true).build(mContext);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // aguarda e repete o recog
            Thread.sleep(4000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // aguarda e repete o recog
            Thread.sleep(4000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // força o timeout
            Thread.sleep(6000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void multiplesAutoClose() {
        int maxSessionIdleSeconds = 5;

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd)
                    .maxSessionIdleSeconds(maxSessionIdleSeconds).autoClose(true).build(mContext);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // aguarda e repete o recog
            Thread.sleep(5000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // aguarda e repete o recog
            Thread.sleep(5000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

            // força o timeout
            Thread.sleep(5000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            results = recognizer.waitRecognitionResult();
            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void recogAfterSessionTimeout() {
        int maxSessionIdleSeconds = 2;

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd)
                    .maxSessionIdleSeconds(maxSessionIdleSeconds).connectOnRecognize(false).autoClose(false).build(mContext);

            // aguarda o timeout e repete o recog
            Thread.sleep(4000);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of alternatives is " + results.get(0).getAlternatives().size(),
                    results.get(0).getAlternatives().size() > 0);
            assertTrue("Score is higher than 90", results.get(0).getAlternatives().get(0).getConfidence() > 90);
            assertTrue("Result is recognized", results.get(0).getResultCode() == RecognitionResultCode.RECOGNIZED);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void continuousMode() {
        String filename = "hetero_segments_8k.wav";

        SpeechRecognizerInterface recognizer = null;
        try {
            recognizer = SpeechRecognizer.builder().serverURL(url).credentials(user, passwd)
                    .recogConfig(RecognitionConfig.builder().continuousMode(true).build()).build(mContext);

            recognizer.recognize(new FileAudioSource(mContext.getAssets().open(filename)),
                    LanguageModelList.builder().addFromURI(lmName).build());
            List<RecognitionResult> results = recognizer.waitRecognitionResult();

            assertTrue("Number of results is " + results.size(), results.size() > 0);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        } finally {
            try {
                if (recognizer != null)
                    recognizer.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}

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
package br.com.cpqd.asr.recognizer.util;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.com.cpqd.asr.recognizer.model.Interpretation;
import br.com.cpqd.asr.recognizer.model.PartialRecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionAlternative;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionResultCode;
import br.com.cpqd.asr.recognizer.model.Word;

/**
 * Some utilities.
 */
public class Util {

    private static final String TAG = Util.class.getSimpleName();

    /**
     * Calculates the size (in bytes) of an audio segment (WAV).
     *
     * @param audioLength audio length, in milliseconds.
     * @param sampleRate  audio rate, in bps (ex: 16000).
     * @param sampleSize  sample size in bits (ex: 16).
     * @return buffer size (number of bytes).
     */
    public static int calculateBufferSize(int audioLength, int sampleRate, int sampleSize) {
        float bufferSize = audioLength * (sampleRate * sampleSize) / 1000L / 8;
        return (int) bufferSize;
    }

    /**
     * Transform JSON into PartialRecognitionResult
     */
    public static PartialRecognitionResult getPartialRecogResult(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            JSONArray alternatives = jsonObject.getJSONArray("alternatives");
            JSONObject alternative = alternatives.getJSONObject(0);

            PartialRecognitionResult partialResult = new PartialRecognitionResult();
            partialResult.setSpeechSegmentIndex(alternative.optInt("segment_index"));
            partialResult.setText(alternative.optString("text"));

            return partialResult;
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Transform JSON into RecognitionResult
     */
    public static RecognitionResult getRecogResult(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);

            RecognitionResult recognitionResult = new RecognitionResult();
            recognitionResult.setSpeechSegmentIndex(jsonObject.optInt("segment_index"));
            recognitionResult.setLastSpeechSegment(jsonObject.optBoolean("last_segment"));
            recognitionResult.setFinalResult(jsonObject.optBoolean("final_result"));
            recognitionResult.setSegmentStartTime(jsonObject.optLong("start_time"));
            recognitionResult.setSegmentEndTime(jsonObject.optLong("end_time"));
            recognitionResult.setResultCode(RecognitionResultCode.valueOf(jsonObject.optString("result_status")));

            JSONArray alternatives = jsonObject.optJSONArray("alternatives");
            if (alternatives != null) {
                List<RecognitionAlternative> recognitionAlternatives = new ArrayList<>();
                for (int i = 0; i < alternatives.length(); i++) {
                    JSONObject alternative = alternatives.getJSONObject(i);

                    RecognitionAlternative recognitionAlternative = new RecognitionAlternative();
                    recognitionAlternative.setText(alternative.optString("text"));
                    recognitionAlternative.setConfidence(alternative.optInt("score"));
                    recognitionAlternative.setLanguageModel(alternative.optString("lm"));

                    JSONArray words = alternative.optJSONArray("words");
                    if (words != null) {
                        List<Word> wordAlignment = new ArrayList<>();
                        for (int j = 0; j < words.length(); j++) {
                            JSONObject wordJson = words.getJSONObject(j);

                            Word word = new Word();
                            word.setWord(wordJson.optString("text"));
                            word.setConfidence(wordJson.optInt("score"));
                            word.setStartTime(wordJson.optLong("start_time"));
                            word.setEndTime(wordJson.optLong("end_time"));

                            wordAlignment.add(word);
                        }

                        // add the words to the alternative
                        recognitionAlternative.setWordAlignment(wordAlignment);
                    }

                    JSONArray interpretations = alternative.optJSONArray("interpretations");
                    if (interpretations != null) {
                        List<Interpretation> interpretationList = new ArrayList<>();
                        for (int j = 0; j < interpretations.length(); j++) {
                            Interpretation interpretation = new Interpretation();
                            interpretation.setInterpretation(String.valueOf(interpretations.getString(j)));
                            interpretationList.add(interpretation);
                        }

                        // add the interpretations to the alternative
                        recognitionAlternative.setInterpretations(interpretationList);
                    }

                    // add the alternative to the alternative list
                    recognitionAlternatives.add(recognitionAlternative);
                }

                // add the alternatives to the result
                recognitionResult.setAlternatives(recognitionAlternatives);
            }

            return recognitionResult;
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage(), e);
        }

        return null;
    }
}

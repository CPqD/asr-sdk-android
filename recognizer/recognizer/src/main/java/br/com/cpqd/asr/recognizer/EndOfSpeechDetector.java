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

/**
 * <p>End of speech detector, adapted from
 * <a href="https://github.com/Kaljurand/speechutils/blob/master/app/src/ee/ioc/phon/android/speechutils/AbstractAudioRecorder.java">speechutils</a>.</p>
 * <p>A new instance of this class should be created for each
 * {@link android.media.AudioRecord} object, i.e. an end of speech detector
 * should not be reused among different recording sessions.</p>
 */
public class EndOfSpeechDetector {

    private double mAvgEnergy;

    public EndOfSpeechDetector() {

        mAvgEnergy = 0.0;
    }

    public boolean isPausing(byte[] audio) {

        return getPauseScore(audio) > 7.0;
    }

    private double getPauseScore(byte[] audio) {

        long t2 = getRms(audio);

        if (t2 == 0L) {

            return 0.0;
        }

        double t = mAvgEnergy / (double) t2;

        mAvgEnergy = (mAvgEnergy * 2.0 + (double) t2) / 3.0;

        return t;
    }

    private long getRms(byte[] audio) {

        long rms = 0L;

        for (int i = 0; i < audio.length; i += 2) {

            // Assemble a little-endian short from two bytes.
            short sample = (short) (audio[i + 1] << 8 | audio[i]);

            rms += (long) (sample * sample);
        }

        return rms;
    }
}

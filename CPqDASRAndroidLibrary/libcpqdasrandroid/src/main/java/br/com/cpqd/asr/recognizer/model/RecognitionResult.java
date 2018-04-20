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
package br.com.cpqd.asr.recognizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the final result of the recognition process.
 *
 */
public class RecognitionResult {

	/** the recognition result code. */
	private RecognitionResultCode resultCode;

	/** the speech segment index. */
	private int speechSegmentIndex;

	/**
	 * indicates if this is the last recognized segment.
	 */
	private boolean lastSpeechSegment;

	/** the audio position when the speech start was detected (in secs). */
	private float segmentStartTime;

	/** the audio position when the speech stop was detected (in secs). */
	private float segmentEndTime;

	/** the list of recognition result alternative sentences. */
	private List<RecognitionAlternative> alternatives = new ArrayList<>();

	public RecognitionResultCode getResultCode() {
		return resultCode;
	}

	public void setResultCode(RecognitionResultCode resultCode) {
		this.resultCode = resultCode;
	}

	public int getSpeechSegmentIndex() {
		return speechSegmentIndex;
	}

	public void setSpeechSegmentIndex(int speechSegmentIndex) {
		this.speechSegmentIndex = speechSegmentIndex;
	}

	public boolean isLastSpeechSegment() {
		return lastSpeechSegment;
	}

	public void setLastSpeechSegment(boolean lastSpeechSegment) {
		this.lastSpeechSegment = lastSpeechSegment;
	}

	public float getSegmentStartTime() {
		return segmentStartTime;
	}

	public void setSegmentStartTime(float segmentStartTime) {
		this.segmentStartTime = segmentStartTime;
	}

	public float getSegmentEndTime() {
		return segmentEndTime;
	}

	public void setSegmentEndTime(float segmentEndTime) {
		this.segmentEndTime = segmentEndTime;
	}

	public List<RecognitionAlternative> getAlternatives() {
		return alternatives;
	}

	public void setAlternatives(List<RecognitionAlternative> alternatives) {
		this.alternatives = alternatives;
	}

	@Override
	public String toString() {
		return "[code=" + resultCode + ", alternatives=" + alternatives + "]";
	}

}

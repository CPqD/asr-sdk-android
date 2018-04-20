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
package br.com.cpqd.asr.recognizer.model;

/**
 * Represents the interpretation result of a recognition result.
 * 
 */
public class Interpretation {

	/** the interpretation confidence score. */
	private int interpretationConfidence;

	/** a json representation of the interpretation result. */
	private String interpretation;

	public int getInterpretationConfidence() {
		return interpretationConfidence;
	}

	public void setInterpretationConfidence(int interpretationConfidence) {
		this.interpretationConfidence = interpretationConfidence;
	}

	public String getInterpretation() {
		return interpretation;
	}

	public void setInterpretation(String interpretation) {
		this.interpretation = interpretation;
	}

	@Override
	public String toString() {
		return "Interpretation [confidence=" + interpretationConfidence + ", content=" + interpretation + "]";
	}

}

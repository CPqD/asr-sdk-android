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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a alternative sentence or speech segment result of the recognition
 * process.
 *
 */
public class RecognitionAlternative {

	/** the languagem model considered in the recognition. */
	private String languageModel;

	/** the recognized text. */
	private String text;

	/** the recognition confidence score. */
	private int confidence;

	/** the interpretations result. */
	private List<Interpretation> interpretations = new ArrayList<>();

	/** the word alignment list. */
	private List<Word> wordAlignment = new ArrayList<>();

	public String getLanguageModel() {
		return languageModel;
	}

	public void setLanguageModel(String languageModel) {
		this.languageModel = languageModel;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getConfidence() {
		return confidence;
	}

	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public List<Interpretation> getInterpretations() {
		return interpretations;
	}

	public void setInterpretations(List<Interpretation> interpretations) {
		this.interpretations = interpretations;
	}

	public List<Word> getWordAlignment() {
		return wordAlignment;
	}

	public void setWordAlignment(List<Word> wordAlignment) {
		this.wordAlignment = wordAlignment;
	}

	@Override
	public String toString() {
		return "RecognitionAlternative [lm=" + languageModel + ", text=" + text + ", confidence=" + confidence
				+ ", interpretations=" + interpretations + ", wordAlignment=" + wordAlignment + "]";
	}

}

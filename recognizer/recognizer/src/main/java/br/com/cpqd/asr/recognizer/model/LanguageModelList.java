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
 * Represents the language models used during the speech recognition process.
 * 
 */
@SuppressWarnings("unused")
public class LanguageModelList {

	/** the language model URI list. */
	private List<String> uriList;

	/** the inline grammar body list. */
	private List<String[]> grammarList;
	
	/** the phrase rule list. */
	private List<String> phraseRuleList;

	private Integer timeToLive;
	private Integer timeToIdle;
	private Boolean cacheEnabled;

	private LanguageModelList(Builder builder) {
		super();
		this.uriList = builder.uriList;
		this.grammarList = builder.grammarList;
		this.phraseRuleList = builder.phraseRuleList;
		this.timeToIdle = builder.timeToIdle;
		this.timeToLive = builder.timeToLive;
		this.cacheEnabled = builder.cacheEnabled;
	}

	/**
	 * Creates a new instance of the object builder.
	 * 
	 * @return the Builder object.
	 */
	public static Builder builder() {
		return new Builder();
	}

	public List<String> getUriList() {
		return uriList;
	}

	public List<String[]> getGrammarList() {
		return grammarList;
	}

	private List<String> getPhraseRuleList() {
		return phraseRuleList;
	}

	private Integer getTimeToLive() {
		return timeToLive;
	}

	private Integer getTimeToIdle() {
		return timeToIdle;
	}

	private Boolean getCacheEnabled() {
		return cacheEnabled;
	}

	/**
	 * The Builder object.
	 *
	 */
	public static class Builder {
		private List<String> uriList = new ArrayList<>(1);
		private List<String[]> grammarList = new ArrayList<>(1);
		private List<String> phraseRuleList = new ArrayList<>();
		private Integer timeToLive;
		private Integer timeToIdle;
		private Boolean cacheEnabled;

		/**
		 * Creates a new instance of the LanguageModelList object.
		 * 
		 * @return a LanguageModelList instance.
		 */
		public LanguageModelList build() {
			return new LanguageModelList(this);
		}

		/**
		 * Adds a new language model from its URI.
		 * 
		 * @param uri
		 *            the languagem model URI.
		 * @return the builder object.
		 */
		public Builder addFromURI(String uri) {
			if (this.uriList.size() > 0) throw new IndexOutOfBoundsException("Only one URI is supported.");
			this.uriList.add(uri);
			return this;
		}

		/**
		 * Adds a new grammar content.
		 * 
		 * @param id
		 *            the grammar identification.
		 * @param body
		 *            the grammar body content.
		 * @return the builder object.
		 */
		public Builder addInlineGrammar(String id, String body) {
			if (this.grammarList.size() > 0) throw new IndexOutOfBoundsException("Only one grammar is supported.");
			this.grammarList.add(new String[] { id, body });
			return this;
		}

		/**
		 * Adds a new phrase rule.
		 * 
		 * @param phrase
		 *            the phrase rule.
		 * @return the builder object.
		 */
		private Builder addPhraseRule(String phrase) {
			this.phraseRuleList.add(phrase);
			return this;
		}

		/**
		 * Sets the time to live attribute.
		 * 
		 * @param value
		 *            the time to live value (in seconds).
		 * @return the builder object.
		 */
		private Builder timeToLive(Integer value) {
			this.timeToLive = value;
			return this;
		}

		/**
		 * Sets the time to idle attribute.
		 * 
		 * @param value
		 *            the time to idle value (in seconds).
		 * @return the builder object.
		 */
		private Builder timeToIdle(Integer value) {
			this.timeToIdle = value;
			return this;
		}

		/**
		 * Sets the cache enabled attribute.
		 * 
		 * @param enabled
		 *            the cache enabled value.
		 * @return the builder object.
		 */
		private Builder cacheEnabled(Boolean enabled) {
			this.cacheEnabled = enabled;
			return this;
		}
	}
}

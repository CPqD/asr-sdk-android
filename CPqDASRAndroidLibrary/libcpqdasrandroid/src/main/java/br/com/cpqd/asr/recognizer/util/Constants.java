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

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Class that provides constants used throughout the library.
 */
public class Constants {

    /**
     * Locale that standardizes occurrences of {@link String#format(Locale, String, Object...)}
     * and other places that can make use of Locale as well.
     */
    public static final Locale DEFAULT_LOCALE = Locale.US;

    /**
     * Default character encoding for all textual stuff of this library.
     * UTF-8, of course.
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * Character encoding used for arranging network-obtained octets into text.
     */
    public static final Charset NETWORK_CHARSET = Charset.forName("UTF-8");
}

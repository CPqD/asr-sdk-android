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

import br.com.cpqd.asr.recognizer.model.RecognitionError;
import br.com.cpqd.asr.recognizer.model.RecognitionErrorCode;

/**
 * Exception thrown when there is an error in the speech recognition process.
 */
public class RecognitionException extends Exception {

    /**
     * the recognition error code.
     */
    private RecognitionErrorCode code;

    /**
     * Constructor.
     *
     * @param code    the error code.
     * @param message the error message.
     * @param cause   the underlying exception.
     */
    public RecognitionException(RecognitionErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code    the error code.
     * @param message the error message.
     */
    public RecognitionException(RecognitionErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param error the error wrapper class.
     */
    public RecognitionException(RecognitionError error) {
        super(error.getMessage());
        this.code = error.getCode();
    }

    public RecognitionErrorCode getErrorCode() {
        return code;
    }

    @Override
    public String toString() {
        return "[" + code + "] " + getMessage();
    }

}

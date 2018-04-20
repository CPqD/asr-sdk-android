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

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import br.com.cpqd.asr.recognizer.util.Constants;

/**
 * <p>Model class of the messages exchanged between client and ASR server.</p>
 * <p>A message looks roughly like this:</p>
 * <p><tt>
 * &nbsp;&nbsp;&nbsp;&nbsp;ASR 2.0 METHOD(CRLF)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;field-name: field-value(CRLF)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;(CRLF)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;(body if any)
 * </tt></p>
 */
class AsrMessage {

    /**
     * Log tag.
     */
    private static final String TAG = AsrMessage.class.getSimpleName();

    /**
     * ASR protocol.
     */
    private static final String ASR_PROTOCOL = "ASR";

    /**
     * ASR major version.
     */
    private static final String ASR_MAJOR_VERSION = "2";

    /**
     * ASR minor version.
     */
    private static final String ASR_MINOR_VERSION = "1";

    /**
     * Method to cancel recognition.
     */
    static final String METHOD_CANCEL_RECOGNITION = "CANCEL_RECOGNITION";

    /**
     * Method for creating a new ASR session.
     */
    static final String METHOD_CREATE_SESSION = "CREATE_SESSION";

    /**
     * Method for starting a new speech recognition, after creation of ASR session.
     */
    static final String METHOD_START_RECOGNITION = "START_RECOGNITION";

    /**
     * Method for stopping an ongoing recognition.
     */
    static final String METHOD_STOP_RECOGNITION = "STOP_RECOGNITION";

    /**
     * Method for sending audio samples to server.
     */
    static final String METHOD_SEND_AUDIO = "SEND_AUDIO";

    /**
     * Method for getting current ASR session status.
     */
    static final String METHOD_GET_SESSION_STATUS = "GET_SESSION_STATUS";

    /**
     * Method for releasing an ASR session, after recognition is done.
     */
    static final String METHOD_RELEASE_SESSION = "RELEASE_SESSION";

    /**
     * Method for obtaining the recognition result.
     */
    static final String METHOD_RECOGNITION_RESULT = "RECOGNITION_RESULT";

    /**
     * Method for responding a request from the client.
     */
    static final String METHOD_RESPONSE = "RESPONSE";

    /**
     * Method sent by the server to indicate recognition of start of speech.
     */
    static final String METHOD_START_OF_SPEECH = "START_OF_SPEECH";

    /**
     * Method sent by the server to indicate recognition of end of speech.
     */
    static final String METHOD_END_OF_SPEECH = "END_OF_SPEECH";

    /**
     * Method to start input timers.
     */
    static final String METHOD_START_INPUT_TIMERS = "START_INPUT_TIMERS";

    /**
     * Regex that matches an HTTP token.
     * Refer to <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230, Section 3.2.6</a>.
     */
    private static final String TOKEN_REGEX = "[\\Q!#$%&'*+-.^_`|~\\E0-9A-Za-z]+";

    /**
     * Regex that matches a version.
     */
    private static final String VERSION_REGEX = "(" + ASR_MAJOR_VERSION + "\\.\\d+)";

    /**
     * Protocol of this message.
     * It should be <tt>ASR</tt>.
     */
    private String mProtocol;

    /**
     * Version of the protocol of this message.
     * It should be something like <tt>2.0</tt>.
     */
    private String mVersion;

    /**
     * Method of this message.
     * It should be one of the {@code METHOD_*} values defined in this class.
     */
    @NonNull
    private String mMethod;

    /**
     * Map of header fields names and values of this message.
     * It may be {@code null}, indicating there is no header field.
     */
    private Map<String, String> mHeaderFields;

    /**
     * Body of this message.
     * It may be {@code null}, indicating there is no body.
     */
    private byte[] mBody;

    /**
     * Constructs an ASR message by setting values to its variables directly.
     *
     * @param method       a valid method.
     *                     It should be one of the {@code METHOD_*} values defined in this class.
     * @param headerFields a map of header fields names and values.  It may be {@code null}.
     * @param body         a payload body.  It may be {@code null}.
     * @throws IllegalArgumentException if {@code method} is not a valid method.
     */
    AsrMessage(@NonNull String method, Map<String, String> headerFields, byte[] body) {

        if (!isValidMethod(method)) {
            throw new IllegalArgumentException("invalid method: " + method);
        }

        mProtocol = ASR_PROTOCOL;
        mVersion = ASR_MAJOR_VERSION + "." + ASR_MINOR_VERSION;

        mMethod = method;

        mHeaderFields = headerFields;

        mBody = body;
    }

    /**
     * <p>Constructs an ASR message from serialized octets.</p>
     * <p>The ASR protocol does not define any specific character encoding,
     * so this Android library implementation reads these octets as UTF-8 encoded text.</p>
     *
     * @param serializedMessage octet-serialized ASR message.
     * @throws IllegalArgumentException if {@code serializedMessage} does not represent a valid ASR message.
     */
    AsrMessage(@NonNull byte[] serializedMessage) {

        // Wrap the byte array into a ByteBuffer so we can take advantage of its properties
        // like limit and position pointers, as well as relative get methods.
        ByteBuffer serializedMessageByteBuffer = ByteBuffer.wrap(serializedMessage);

        // Read first message line.  It should be something like this:
        //
        // ASR 2.1 METHOD
        byte[] messageLineBytes = readMessageLineBytes(serializedMessageByteBuffer);

        if (messageLineBytes == null) {

            throw new IllegalArgumentException("invalid message: unexpected end of message");
        }

        String messageLine = new String(messageLineBytes, Constants.NETWORK_CHARSET);

        // Check if message line matches "ASR 2.1 METHOD" format.
        if (!messageLine.matches(String.format(Constants.DEFAULT_LOCALE, "%1$s\\u0020%2$s\\u0020%3$s", ASR_PROTOCOL, VERSION_REGEX, TOKEN_REGEX))) {

            throw new IllegalArgumentException("invalid message: invalid start line");
        }

        // Split "ASR 2.1 METHOD" into three tokens.
        String[] startLineSplit = messageLine.split("\\u0020");

        // Check if "METHOD" is a valid ASR method.
        if (!isValidMethod(startLineSplit[2])) {

            throw new IllegalArgumentException("invalid method: " + startLineSplit[2]);
        }

        // At last, set "ASR 2.0 METHOD" into corresponding fields.
        mProtocol = startLineSplit[0];
        mVersion = startLineSplit[1];
        mMethod = startLineSplit[2];

        mHeaderFields = null;

        // Read message lines up to an empty one.
        // Non-empty lines should be a header field name and value pair, like:
        //
        // field-name: field-value

        messageLineBytes = readMessageLineBytes(serializedMessageByteBuffer);

        if (messageLineBytes == null) {

            throw new IllegalArgumentException("invalid message: unexpected end of message");
        }

        while (!(messageLine = new String(messageLineBytes, Constants.NETWORK_CHARSET)).isEmpty()) {

            // This regex roughly does the job:
            // https://tools.ietf.org/html/rfc7230#section-3.2
            if (messageLine.matches(TOKEN_REGEX + ":.*")) {

                if (mHeaderFields == null) {

                    mHeaderFields = new HashMap<>();
                }

                // Split header field at the colon char
                // and trim optional spaces of field value.
                String[] headerFieldSplit = messageLine.split(":");
                mHeaderFields.put(headerFieldSplit[0], headerFieldSplit[1].trim());

            } else {

                Log.i(TAG, "ignoring invalid header field: " + messageLine);
            }

            messageLineBytes = readMessageLineBytes(serializedMessageByteBuffer);

            if (messageLineBytes == null) {

                throw new IllegalArgumentException("invalid message: unexpected end of message");
            }
        }

        // Read message body.
        //
        // The message body is only read if a valid Content-Length header field has been provided
        // and if there are remaining bytes to be consumed.

        String contentLengthAsString = getHeaderFieldValueForName("Content-Length");

        int contentLength;

        if (contentLengthAsString == null) {

            contentLength = -1;

        } else {

            try {

                contentLength = Integer.parseInt(contentLengthAsString);

            } catch (NumberFormatException e) {

                Log.i(TAG, "ignoring invalid content length: " + contentLengthAsString);

                contentLength = -1;
            }
        }

        // If a Content-Length header field has not been provided
        // or a invalid one has been found, contentLength should be -1,
        // which leads to a no-body in the end.

        if (contentLength > 0) {

            // If there are less available bytes than what was informed
            // in Content-Length, the number of bytes to be read diminishes
            // to the quantity of available bytes.
            if (serializedMessageByteBuffer.remaining() < contentLength) {

                Log.i(TAG, "provided body is smaller than content length");

                contentLength = serializedMessageByteBuffer.remaining();
            }

            // Finally, read the body.
            if (contentLength > 0) {

                mBody = new byte[contentLength];

                serializedMessageByteBuffer.get(mBody);

            } else {

                mBody = null;
            }

        } else {

            mBody = null;
        }
    }

    /**
     * Evaluates whether the given method is a valid ASR method.
     *
     * @param method the method to be evaluated.
     * @return {@code true} if the given method is a valid ASR method, {@code false} otherwise.
     */
    private boolean isValidMethod(@NonNull String method) {

        return method.contentEquals(METHOD_CANCEL_RECOGNITION)
                || method.contentEquals(METHOD_CREATE_SESSION)
                || method.contentEquals(METHOD_START_RECOGNITION)
                || method.contentEquals(METHOD_STOP_RECOGNITION)
                || method.contentEquals(METHOD_SEND_AUDIO)
                || method.contentEquals(METHOD_GET_SESSION_STATUS)
                || method.contentEquals(METHOD_RELEASE_SESSION)
                || method.contentEquals(METHOD_RECOGNITION_RESULT)
                || method.contentEquals(METHOD_RESPONSE)
                || method.contentEquals(METHOD_START_OF_SPEECH)
                || method.contentEquals(METHOD_END_OF_SPEECH)
                || method.contentEquals(METHOD_START_INPUT_TIMERS);
    }

    /**
     * Reads a text line from the buffer, up to a line break (CRLF).
     * If a line is successfully read, the buffer's position will advance
     * past the CRLF bytes, pointing to the start of the next line.
     *
     * @param messageByteBuffer the buffer to be searched on.
     * @return the bytes corresponding to the text line sans CRLF, or
     * {@code null} if no CRLF was found in the remainder of the buffer.
     */
    private byte[] readMessageLineBytes(ByteBuffer messageByteBuffer) {

        // Find the next CRLF.  If it is -1, there is no CRLF remaining.
        int messageLineSeparator = getMessageLineSeparator(messageByteBuffer);

        if (messageLineSeparator == -1) {

            return null;
        }

        // messageLineSeparator should point to the position of the next CR byte.

        // Note that this array will house the text line without CRLF.
        byte[] messageLineBytes = new byte[messageLineSeparator - messageByteBuffer.position()];

        // Get the text line.
        messageByteBuffer.get(messageLineBytes);

        // Walk the buffer's position past CRLF.
        messageByteBuffer.position(messageByteBuffer.position() + 2);

        return messageLineBytes;
    }

    /**
     * Returns the position of the next line separator, i.e. CRLF,
     * i.e. the sequence of bytes 13 and 10.
     *
     * @param messageByteBuffer the buffer to be searched on.
     * @return the position of the next CRLF (actually the position of CR),
     * or {@code -1} if no CRLF was found in the remainder of the buffer.
     */
    private int getMessageLineSeparator(ByteBuffer messageByteBuffer) {

        // TODO: search for chars in char array instead of bytes in byte array.
        //
        // Although searching for CRLF in UTF-8 works correctly in byte form,
        // this algorithm might not do well in the general case of an arbitrary
        // char as input.

        // The algorithm is:
        //
        // Get the position of the next CR.
        // Peek the next byte, if any.
        // If the next byte is a LF, return the position of CR.
        // Or else, get the next CR and do it all over.
        //
        // If there is no CR remaining, return -1.

        int indexOf13 = indexOf(messageByteBuffer, (byte) 13);

        while (indexOf13 != -1) {

            if (indexOf13 + 1 < messageByteBuffer.limit() && messageByteBuffer.get(indexOf13 + 1) == 10) {

                return indexOf13;
            }

            indexOf13 = indexOf(messageByteBuffer, (byte) 13, indexOf13 + 1);
        }

        return -1;
    }

    /**
     * Returns the position of the first occurrence of the specified value
     * in the buffer.
     *
     * @param buffer the buffer to be searched on.
     * @param value  the value one is after.
     * @return the position of the first occurrence of {@code value} in {@code buffer},
     * or {@code -1} if the value does not occur.
     */
    private int indexOf(ByteBuffer buffer, byte value) {

        return indexOf(buffer, value, buffer.position());
    }

    /**
     * Returns the position of the first occurrence of the specified value
     * in the buffer, starting the search at the specified offset.
     *
     * @param buffer the buffer to be searched on.
     * @param value  the value one is after.
     * @param offset the offset to start the search from.
     * @return the position of the first occurrence of {@code value} in {@code buffer}
     * that is greater than or equal to {@code offset},
     * or {@code -1} if the value does not occur.
     */
    private int indexOf(ByteBuffer buffer, byte value, int offset) {

        if (offset < buffer.position() || offset >= buffer.limit()) {

            return -1;
        }

        for (int i = offset; i < buffer.limit(); i++) {

            if (buffer.get(i) == value) {

                return i;
            }
        }

        return -1;
    }

    /**
     * Serializes this ASR message as a byte array.
     *
     * @return this ASR message serialized as a byte array.
     */
    byte[] toByteArray() {

        final byte[] SPACE = " ".getBytes(Constants.NETWORK_CHARSET);
        final byte[] CRLF = "\r\n".getBytes(Constants.NETWORK_CHARSET);
        final byte[] COLON = ":".getBytes(Constants.NETWORK_CHARSET);

        ByteArrayOutputStream messageBaos = new ByteArrayOutputStream();

        try {

            // Write first message line.
            messageBaos.write(mProtocol.getBytes(Constants.NETWORK_CHARSET));
            messageBaos.write(SPACE);
            messageBaos.write(mVersion.getBytes(Constants.NETWORK_CHARSET));
            messageBaos.write(SPACE);
            messageBaos.write(mMethod.getBytes(Constants.NETWORK_CHARSET));
            messageBaos.write(CRLF);

            // Write header fields, if any.
            if (mHeaderFields != null) {

                for (Map.Entry<String, String> headerField : mHeaderFields.entrySet()) {

                    messageBaos.write(headerField.getKey().getBytes(Constants.NETWORK_CHARSET));
                    messageBaos.write(COLON);
                    messageBaos.write(SPACE);
                    messageBaos.write(headerField.getValue().getBytes(Constants.NETWORK_CHARSET));
                    messageBaos.write(CRLF);
                }
            }

            // Write empty line that indicates the end of the header section.
            messageBaos.write(CRLF);

            // Write message body, if any.
            if (mBody != null) {

                messageBaos.write(mBody);
            }

        } catch (IOException e) {

            Log.e(TAG, "could not serialize asr message into byte array", e);

            throw new RuntimeException(e);
        }

        return messageBaos.toByteArray();
    }

    /**
     * Logs this ASR message into {@link Log} for debugging purposes.
     */
    void logItself() {

        // Log first message line.
        Log.d(TAG, String.format(Constants.DEFAULT_LOCALE, "%1$s %2$s %3$s(CRLF)", mProtocol, mVersion, mMethod));

        // Log header fields, if any.
        if (mHeaderFields != null) {

            for (Map.Entry<String, String> headerField : mHeaderFields.entrySet()) {

                Log.d(TAG, String.format(Constants.DEFAULT_LOCALE, "%1$s: %2$s(CRLF)", headerField.getKey(), headerField.getValue()));
            }
        }

        // Log empty line that indicates the end of the header section.
        Log.d(TAG, "(CRLF)");

        // Log message body, if any.
        if (mBody != null) {

            Log.d(TAG, String.format(Constants.DEFAULT_LOCALE, "(body size: %1$d)", mBody.length));

            // If the message body is of text type, log body text.

            String contentType = getHeaderFieldValueForName("Content-Type");

            if (contentType != null && contentType.contentEquals("application/json")) {

                Log.d(TAG, new String(mBody, Constants.DEFAULT_CHARSET));

            } else {

                Log.d(TAG, "(body present)");
            }

        } else {

            Log.d(TAG, "(no body)");
        }
    }

    /**
     * Gets the method of this ASR message.
     *
     * @return the method, e.g. {@code CREATE_SESSION}.
     */
    @NonNull
    String getMethod() {

        return mMethod;
    }

    /**
     * Gets the body of this ASR message.
     *
     * @return the message body, e.g. audio payload or recognition result.
     */
    byte[] getBody() {

        return mBody;
    }

    /**
     * Gets the value of a header field for a given name.
     *
     * @param headerFieldName the header field name requested for value.
     * @return the header field value, or {@code null} if {@code fieldName} is {@code null}
     * or if the requested field is not present.
     */
    String getHeaderFieldValueForName(String headerFieldName) {

        if (mHeaderFields == null || headerFieldName == null) {

            return null;
        }

        if (!mHeaderFields.containsKey(headerFieldName)) {

            return null;
        }

        return mHeaderFields.get(headerFieldName);
    }
}

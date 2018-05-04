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

import android.content.Context;
import android.os.Message;
import android.util.Log;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.glassfish.tyrus.client.auth.AuthConfig;
import org.glassfish.tyrus.client.auth.AuthenticationException;
import org.glassfish.tyrus.client.auth.Credentials;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionError;
import br.com.cpqd.asr.recognizer.model.RecognitionErrorCode;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.util.Constants;
import br.com.cpqd.asr.recognizer.util.Util;

/**
 * Thread that manages the connection to the ASR server and exchanges messages with it.
 */
class AsrServerConnectionThread extends AbstractMessagingThread {

    /**
     * Log tag.
     */
    private static final String TAG = AsrServerConnectionThread.class.getSimpleName();

    /**
     * Connection state indicating there is an active connection at the time.
     */
    private static final int CONNECTION_STATE_IDLE = 1;

    /**
     * Connection state indicating there is no active connection at the time.
     */
    private static final int CONNECTION_STATE_DISCONNECTED = 2;

    /**
     * Connection state indicating the client is waiting for
     * a server response to a new connection request.
     */
    private static final int CONNECTION_STATE_WAITING_SERVER_HANDSHAKE = 3;

    /**
     * Connection state indicating the client is waiting for
     * a server response to a create session request.
     */
    private static final int CONNECTION_STATE_WAITING_CREATE_SESSION = 4;

    /**
     * Connection state indicating the client is waiting for
     * a server response to a start recognition request.
     */
    private static final int CONNECTION_STATE_WAITING_START_RECOGNITION = 5;

    /**
     * Connection state indicating the client is streaming audio to the server.
     */
    private static final int CONNECTION_STATE_STREAMING_AUDIO = 6;

    /**
     * Connection state indicating the client is waiting for
     * the recognition result.
     */
    private static final int CONNECTION_STATE_WAITING_RECOGNITION_RESULT = 7;

    /**
     * Connection state indicating the client is waiting for
     * a server response to a release session request.
     */
    private static final int CONNECTION_STATE_WAITING_RELEASE_SESSION = 8;

    /**
     * Connection state indicating the client is waiting for
     * a server response to a cancel recognition request.
     */
    private static final int CONNECTION_STATE_WAITING_CANCEL_RECOGNITION = 9;

    /**
     * Handler message code for setting language model URI.
     */
    public static final int MESSAGE_SET_LANGUAGE_MODEL_URI = 1;

    /**
     * Handler message code for establishing a connection
     * to the server.
     */
    public static final int MESSAGE_CONNECT_TO_SERVER = 2;

    /**
     * Handler message code for start recognition.
     */
    public static final int MESSAGE_START_RECOGNITION = 3;

    /**
     * Handler message code for release server session.
     */
    public static final int MESSAGE_RELEASE_SESSION = 4;

    /**
     * Handler message code to cancel recognition.
     */
    public static final int MESSAGE_CANCEL_RECOGNITION = 5;

    /**
     * Handler message code for handling an audio packet.
     */
    public static final int MESSAGE_HANDLE_AUDIO_PACKET = 6;

    /**
     * Handler message code for indicating a library error occurred.
     */
    public static final int MESSAGE_ON_CPQD_ASR_LIBRARY_ERROR = 7;

    /**
     * Handler message code to start input timers.
     */
    public static final int MESSAGE_START_INPUT_TIMERS = 8;

    /**
     * Handler message code for resetting network timeout.
     */
    private static final int INTERNAL_MESSAGE_RESET_NETWORK_TIMEOUT = 20;

    /**
     * Handler message code for setting a websocket session.
     */
    private static final int INTERNAL_MESSAGE_SET_WEBSOCKET_SESSION = 21;

    /**
     * Handler message code for creating an ASR session.
     */
    private static final int INTERNAL_MESSAGE_CREATE_ASR_SESSION = 22;

    /**
     * Handler message code for handling an ASR message.
     */
    private static final int INTERNAL_MESSAGE_HANDLE_ASR_MESSAGE = 23;

    /**
     * Handler message code for indicating the connection has been closed.
     */
    private static final int INTERNAL_MESSAGE_ON_CONNECTION_CLOSE = 24;

    /**
     * Handler message code for indicating a websocket library error.
     */
    private static final int INTERNAL_MESSAGE_ON_WEBSOCKET_LIBRARY_ERROR = 25;

    /**
     * Handler message code for raising network timeout.
     */
    private static final int INTERNAL_MESSAGE_RAISE_NETWORK_TIMEOUT = 26;

    /**
     * What code for network timeout message.
     * Blame Google for this stupid name.
     */
    private static final int WHAT_NETWORK_TIMEOUT = 1;

    /**
     * The current connection state.
     * It can be on of the {@code CONNECTION_STATE_*} values.
     */
    private int mConnectionState;

    /**
     * User agent.
     */
    private String mUserAgent;

    /**
     * Reference to the current {@link ClientManager}.
     * Who knows if we really need it.
     */
    private ClientManager mClientManager;

    /**
     * Reference to the current {@link Session}.
     * It is used to send messages to the server and to close the websocket.
     */
    private Session mWebsocketSession;

    /**
     * Implementation of the websocket library callbacks.
     */
    private final AsrClientEndpoint mAsrClientEndpoint;

    /**
     * Buffer that accumulates audio samples to be sent to the server
     * while the connection has not been established.
     */
    private final ByteArrayOutputStream mAudioBufferBaos;

    /**
     * Flag indicating whether the audio buffered in {@link #mAudioBufferBaos}
     * should be sent to the server as the last packet.
     */
    private boolean mAudioBufferIsLastPacket;

    /**
     * Network timeout period, in milliseconds.
     */
    private int mNetworkTimeoutPeriod;

    /**
     * Language model URI.
     */
    private String mLanguageModelUri;

    /**
     * Custom websocket close reason when a network timeout occurs.
     */
    private final NetworkTimeoutCloseReason mNetworkTimeoutCloseReason;

    /**
     * Custom websocket close reason when an internal library error occurs.
     */
    private final LibraryErrorCloseReason mLibraryErrorCloseReason;

    /**
     * The server URI
     */
    private URI mServerURI;

    /**
     * Reference to SpeechRecognizerImpl
     */
    private SpeechRecognizerImpl mRecognizer;

    /**
     * Sets up object initial state.
     */
    public AsrServerConnectionThread(Context context, SpeechRecognizerImpl recognizer,
                                     URI serverURI, String[] credentials,
                                     int timeout, String userAgent) throws URISyntaxException {

        super("asr-server-connection");

        if (serverURI == null) {
            throw new NullPointerException("Server URI cannot be null");
        } else if (serverURI.getScheme() == null
                || !serverURI.getScheme().toLowerCase().startsWith("ws")
                && !serverURI.getScheme().toLowerCase().startsWith("wss")) {
            throw new URISyntaxException("Invalid Server URI", serverURI.toString());
        }

        mRecognizer = recognizer;

        mServerURI = serverURI;

        mNetworkTimeoutPeriod = timeout * 1000;

        mUserAgent = userAgent;

        mAsrClientEndpoint = new AsrClientEndpoint();

        mAudioBufferBaos = new ByteArrayOutputStream();

        mNetworkTimeoutCloseReason = new NetworkTimeoutCloseReason();

        mLibraryErrorCloseReason = new LibraryErrorCloseReason();

        mClientManager = ClientManager.createClient();

        // Inform trusted CAs to the connection.
        if (serverURI.getScheme().toLowerCase().startsWith("wss")) {

            ByteArrayOutputStream trustStoreBaos;
            boolean haveCertificatesBeenSetSuccessfully;
            try {

                // Load CAs from an InputStream
                Certificate ca;
                InputStream caIs = context.getAssets().open("GlobalSignRootCA.pem");

                // noinspection TryFinallyCanBeTryWithResources
                try {
                    ca = CertificateFactory.getInstance("X.509").generateCertificate(caIs);
                } finally {
                    caIs.close();
                }

                // Create a KeyStore containing our trusted CAs
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                trustStore.setCertificateEntry("ca", ca);

                // Set trust store into configuration.
                trustStoreBaos = new ByteArrayOutputStream();
                trustStore.store(trustStoreBaos, null);

                haveCertificatesBeenSetSuccessfully = true;
            } catch (Exception e) {
                Log.w(TAG, e.getMessage(), e);
                trustStoreBaos = null;
                haveCertificatesBeenSetSuccessfully = false;
            }

            if (haveCertificatesBeenSetSuccessfully) {
                SslContextConfigurator defaultConfig = new SslContextConfigurator();
                defaultConfig.setTrustStoreBytes(trustStoreBaos.toByteArray());

                mClientManager.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR,
                        new SslEngineConfigurator(defaultConfig, true, false, false));
            } else {
                Log.w(TAG, "could not set certificates");
            }

            // Perform basic access authentication.
            if (credentials != null && credentials.length == 2) {
                mClientManager.getProperties().put(ClientProperties.AUTH_CONFIG, AuthConfig.Builder.create().disableProvidedDigestAuth().build());
                mClientManager.getProperties().put(ClientProperties.CREDENTIALS, new Credentials(credentials[0], credentials[1]));
            }
        }

        resetConnectionState(false, null);
    }

    /**
     * Method to set network timeout
     */
    private void setNetworkTimeout() {

        // Set network timeout.
        Message message = obtainMessage();
        message.arg1 = INTERNAL_MESSAGE_RAISE_NETWORK_TIMEOUT;
        message.what = WHAT_NETWORK_TIMEOUT;

        if (!sendMessageDelayed(message, mNetworkTimeoutPeriod)) {
            Log.w(TAG, "error send network timeout message delayed");
        }
    }

    /**
     * Establishes connection to server.
     */
    private void connectToServer() {

        // Connect to server and properly deal with possible errors.
        try {

            mClientManager.connectToServer(mAsrClientEndpoint, mServerURI);

        } catch (IOException e) {

            Log.w(TAG, "IOException while connecting to server", e);

            Message message = mRecognizer.obtainMessage();
            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
            message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Network error");
            message.sendToTarget();

            return;

        } catch (DeploymentException e) {

            Log.w(TAG, "DeploymentException while connecting to server", e);

            if (e.getClass() == DeploymentException.class && e.getMessage().contentEquals("SSL handshake has failed")
                    && e.getCause() != null && e.getCause().getClass() == SSLHandshakeException.class && e.getCause().getMessage().contentEquals("Handshake failed")
                    && e.getCause().getCause() != null && e.getCause().getCause().getClass() == CertificateException.class && e.getCause().getCause().getMessage().contentEquals("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.")
                    && e.getCause().getCause().getCause() != null && e.getCause().getCause().getCause().getClass() == CertPathValidatorException.class && e.getCause().getCause().getCause().getMessage().contentEquals("Trust anchor for certification path not found.")) {

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
                message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Invalid TLS certificate");
                message.sendToTarget();

            } else if (e.getClass() == DeploymentException.class && e.getMessage().contentEquals("Handshake error.")
                    && e.getCause() != null && e.getCause().getClass() == AuthenticationException.class && (e.getCause().getMessage().contentEquals("Credentials are missing.") || e.getCause().getMessage().contentEquals("Authentication failed."))) {

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
                message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Invalid username or password");
                message.sendToTarget();

            } else {

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
                message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Network error");
                message.sendToTarget();
            }

            return;
        }

        mConnectionState = CONNECTION_STATE_WAITING_SERVER_HANDSHAKE;

        // Set network timeout.
        setNetworkTimeout();
    }

    /**
     * Cleans up connection state, i.e.
     * closes resources and sets everything to {@code null} and {@code false}.
     *
     * @param shouldCloseSession {@code true} if
     *                           {@link #mWebsocketSession} should be closed, or {@code false} otherwise.
     * @param closeReason        a close reason in case
     *                           {@link #mWebsocketSession} should be closed; may be {@code null}.
     */
    private void resetConnectionState(boolean shouldCloseSession, CloseReason closeReason) {

        if (shouldCloseSession && mWebsocketSession != null) {
            try {
                if (closeReason != null) {
                    mWebsocketSession.close(closeReason);
                } else {
                    mWebsocketSession.close();
                }
            } catch (IOException e) {
                Log.i(TAG, "IOException while closing connection to server", e);
            } catch (IllegalStateException e) {
                Log.i(TAG, "IllegalStateException while closing connection to server", e);
            }
        }

        mConnectionState = CONNECTION_STATE_DISCONNECTED;

        mWebsocketSession = null;

        mAudioBufferBaos.reset();

        mAudioBufferIsLastPacket = false;
    }

    /**
     * Processes an incoming audio packet.
     * Depending on the connection state, the audio packet will be uploaded
     * to the server, stored into an internal buffer to be sent later,
     * or dropped.
     *
     * @param audioPacket  the audio packet to be processed.
     * @param isLastPacket flag indicating whether this audio packet
     *                     is the last one of this ASR recognition session.
     */
    private void handleAudioPacket(byte[] audioPacket, boolean isLastPacket) {

        if (mConnectionState == CONNECTION_STATE_IDLE
                || mConnectionState == CONNECTION_STATE_DISCONNECTED
                || mConnectionState == CONNECTION_STATE_WAITING_SERVER_HANDSHAKE
                || mConnectionState == CONNECTION_STATE_WAITING_CREATE_SESSION
                || mConnectionState == CONNECTION_STATE_WAITING_START_RECOGNITION) {

            // If this thread receives an audio packet while trying to establish
            // a connection to the server, store the audio packet.
            if (audioPacket != null) {
                try {
                    mAudioBufferBaos.write(audioPacket);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while writing to audio buffer", e);
                    throw new RuntimeException(e);
                }
            }

            mAudioBufferIsLastPacket = mAudioBufferIsLastPacket || isLastPacket;

        } else if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO) {

            // If this thread is ready to stream audio, upload audio packet to server.
            Map<String, String> headerFields = new HashMap<>(3);
            headerFields.put("LastPacket", Boolean.toString(isLastPacket));
            headerFields.put("Content-Length", Integer.toString(audioPacket != null ? audioPacket.length : 0));
            headerFields.put("Content-Type", "application/octet-stream");

            if (!sendAsrMessage(new AsrMessage(AsrMessage.METHOD_SEND_AUDIO, headerFields, audioPacket))) {
                return;
            }

            if (isLastPacket) {
                mConnectionState = CONNECTION_STATE_WAITING_RECOGNITION_RESULT;
            }
        }
    }

    /**
     * Sends an ASR message to server requesting creation of an ASR session.
     */
    private void createAsrSession() {

        // Put user agent in header and send ASR message.
        Map<String, String> headerFields = new HashMap<>();
        if (mUserAgent != null) {
            headerFields.put("User-Agent", mUserAgent);
        }

        if (!sendAsrMessage(new AsrMessage(AsrMessage.METHOD_CREATE_SESSION, headerFields, null))) {
            return;
        }

        mConnectionState = CONNECTION_STATE_WAITING_CREATE_SESSION;
    }

    /**
     * Sends ASR message to server.
     * If a problem occurs, an error message is sent to the main {@link android.os.Handler}.
     *
     * @param asrMessage ASR message to be sent.
     * @return {@code true} if the message was sent successfully,
     * or {@code false} otherwise.
     */
    private boolean sendAsrMessage(AsrMessage asrMessage) {

        if (mWebsocketSession == null) {

            Log.w(TAG, "unexpected null mWebsocketSession while sending asr message to server");

            Message message = mRecognizer.obtainMessage();
            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
            message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Internal library error");
            message.sendToTarget();

            return false;
        }

        try {

            mWebsocketSession.getBasicRemote().sendObject(asrMessage);

        } catch (IOException e) {

            Log.w(TAG, "IOException while sending asr message", e);

            Message message = mRecognizer.obtainMessage();
            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
            message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Network error");
            message.sendToTarget();

            return false;

        } catch (EncodeException e) {

            Log.w(TAG, "EncodeException while sending asr message", e);

            Message message = mRecognizer.obtainMessage();
            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
            message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Internal library error");
            message.sendToTarget();

            return false;
        }

        return true;
    }

    /**
     * Processes ASR messages received from server.
     *
     * @param asrMessage ASR message received from server.
     */
    private void handleAsrMessage(AsrMessage asrMessage) {

        String method = asrMessage.getMethod();

        // Note that method is never null.

        if (method.contentEquals(AsrMessage.METHOD_RESPONSE)) {

            // The ASR message is a response to a previous request.

            String responseMethod = asrMessage.getHeaderFieldValueForName("Method");

            if (responseMethod != null) {

                if (responseMethod.contentEquals(AsrMessage.METHOD_CREATE_SESSION)) {

                    // The ASR message is a response to create session.
                    //
                    // Send start recognition to server.

                    if (mConnectionState == CONNECTION_STATE_WAITING_CREATE_SESSION) {

                        String createSessionResult = asrMessage.getHeaderFieldValueForName("Result");

                        if (createSessionResult != null && createSessionResult.contentEquals("SUCCESS")) {

                            removeMessages(WHAT_NETWORK_TIMEOUT);

                            Message message = mRecognizer.obtainMessage();
                            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_CREATE_SESSION;
                            message.sendToTarget();

                            mConnectionState = CONNECTION_STATE_IDLE;

                        } else {

                            mConnectionState = CONNECTION_STATE_IDLE;

                            Message message = mRecognizer.obtainMessage();
                            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
                            message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Internal library error");
                            message.sendToTarget();

                        }

                    } else {

                        Log.i(TAG, "ignoring response to create session asr message");
                    }

                } else if (responseMethod.contentEquals(AsrMessage.METHOD_START_RECOGNITION)) {

                    // The ASR message is a response to start recognition.
                    //
                    // Start streaming audio to server.

                    if (mConnectionState == CONNECTION_STATE_WAITING_START_RECOGNITION) {

                        String startRecogResult = asrMessage.getHeaderFieldValueForName("Result");

                        if (startRecogResult != null && startRecogResult.contentEquals("SUCCESS")) {

                            // Notify the speech recognizer that server is listening
                            Message message = mRecognizer.obtainMessage();
                            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_START_RECOGNITION;
                            message.sendToTarget();

                            mConnectionState = CONNECTION_STATE_STREAMING_AUDIO;

                            // The thread state has just become "streaming audio".
                            // Send buffered audio, if any, to server.
                            if (mAudioBufferBaos.size() > 0 || mAudioBufferIsLastPacket) {
                                handleAudioPacket(mAudioBufferBaos.toByteArray(), mAudioBufferIsLastPacket);
                            }

                        } else {

                            mConnectionState = CONNECTION_STATE_IDLE;

                            String startRecogErrorCode = asrMessage.getHeaderFieldValueForName("Error-Code");

                            Message message = mRecognizer.obtainMessage();
                            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;

                            if (startRecogErrorCode != null) {
                                if (startRecogErrorCode.contentEquals("ERR_FILE_OPEN")) {
                                    message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Language model not found");
                                } else if (startRecogErrorCode.contentEquals("ERR_ARG_INVALID")) {
                                    message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Required AM not loaded");
                                } else if (startRecogErrorCode.contentEquals("ERR_CORRUPTED_LM")) {
                                    message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Corrupted language model");
                                } else if (startRecogErrorCode.contentEquals("ERR_NO_ACTIVE_LM")) {
                                    message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "No active language model");
                                } else {
                                    message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Internal library error");
                                }
                            } else {
                                message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "Internal library error");
                            }

                            message.sendToTarget();

                        }

                    } else {

                        Log.i(TAG, "ignoring response to start recognition asr message");
                    }

                } else if (responseMethod.contentEquals(AsrMessage.METHOD_SEND_AUDIO)) {

                    // The ASR message is a response to send audio.
                    //
                    // Check if server is still listening to us.
                    // If the server stopped listening, send a stop message to the main handler.

                    String sessionStatus = asrMessage.getHeaderFieldValueForName("Session-Status");

                    if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO
                            && sessionStatus != null && !sessionStatus.contentEquals("ASR_LISTENING")) {

                        mConnectionState = CONNECTION_STATE_WAITING_RECOGNITION_RESULT;

                        Message message = mRecognizer.obtainMessage();
                        message.arg1 = SpeechRecognizerImpl.MESSAGE_STOP;
                        message.sendToTarget();

                    }

                } else if (responseMethod.contentEquals(AsrMessage.METHOD_RELEASE_SESSION)) {

                    // The ASR message is a response to release session.
                    //
                    // Remove network timeout and close connection to server.

                    if (mConnectionState == CONNECTION_STATE_WAITING_RELEASE_SESSION) {

                        mConnectionState = CONNECTION_STATE_IDLE;

                        removeMessages(WHAT_NETWORK_TIMEOUT);

                        resetConnectionState(true, mLibraryErrorCloseReason);

                        Message message = mRecognizer.obtainMessage();
                        message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_RELEASE_SESSION;
                        message.sendToTarget();

                    } else {

                        Log.i(TAG, "ignoring response to release session asr message");
                    }

                } else if (responseMethod.contentEquals(AsrMessage.METHOD_CANCEL_RECOGNITION)) {

                    // The ASR message is a response to cancel recognition.
                    //
                    // Notify the main thread.

                    if (mConnectionState == CONNECTION_STATE_WAITING_CANCEL_RECOGNITION) {

                        mConnectionState = CONNECTION_STATE_IDLE;

                        removeMessages(WHAT_NETWORK_TIMEOUT);

                        Message message = mRecognizer.obtainMessage();
                        message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_CANCEL_RECOGNITION;
                        message.sendToTarget();

                    } else {

                        Log.i(TAG, "ignoring response to cancel recognition asr message");
                    }

                } else if (responseMethod.contentEquals(AsrMessage.METHOD_START_INPUT_TIMERS)) {

                    // The ASR message is a response to start input timers.
                    //
                    // Notify the main thread.
                    if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO || mConnectionState == CONNECTION_STATE_WAITING_RECOGNITION_RESULT) {
                        Message message = mRecognizer.obtainMessage();
                        message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_START_INPUT_TIMERS;
                        message.sendToTarget();
                    } else {
                        Log.i(TAG, "ignoring response to start input timers asr message");
                    }

                } else {

                    Log.i(TAG, "ignoring response asr message to method: " + responseMethod);
                }

            } else {

                Log.i(TAG, "ignoring malformed response asr message without method header field");
            }

        } else if (method.contentEquals(AsrMessage.METHOD_END_OF_SPEECH)) {

            // The ASR message is a end of speech.
            //
            // Send a stop message to the main handler.

            if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO) {

                mConnectionState = CONNECTION_STATE_WAITING_RECOGNITION_RESULT;

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_STOP;
                message.sendToTarget();
            }

        } else if (method.contentEquals(AsrMessage.METHOD_RECOGNITION_RESULT)) {

            // The ASR message is a recognition result.
            //
            // If result is partial, send partial result message to main handler.
            // If result is final, send final result message to main handler.
            //      Final recognition result
            //      RECOGNIZED, NO_MATCH, NO_INPUT_TIMEOUT, MAX_SPEECH,
            //      NO_SPEECH, EARLY_SPEECH, RECOGNITION_TIMEOUT, FAILURE

            if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO || mConnectionState == CONNECTION_STATE_WAITING_RECOGNITION_RESULT) {

                String resultStatusHeaderField = asrMessage.getHeaderFieldValueForName("Result-Status");

                if (resultStatusHeaderField != null) {

                    String result;

                    if (asrMessage.getBody() != null) {
                        result = new String(asrMessage.getBody(), Constants.DEFAULT_CHARSET);
                    } else {
                        result = null;
                    }

                    RecognitionResult recognitionResult = Util.getRecogResult(result);

                    if (recognitionResult != null && recognitionResult.isFinalResult()) {

                        // back state to idle if is the last segment
                        if (recognitionResult.isLastSpeechSegment()) {
                            mConnectionState = CONNECTION_STATE_IDLE;
                        }

                        Message message = mRecognizer.obtainMessage();
                        message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_RESULT;
                        message.obj = recognitionResult;
                        message.sendToTarget();

                    } else {

                        Message message = mRecognizer.obtainMessage();
                        message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_PARTIAL_RESULT;
                        message.obj = Util.getPartialRecogResult(result);
                        message.sendToTarget();

                    }

                } else {

                    Log.i(TAG, "ignoring malformed recognition result asr message without result status header field");
                }

            } else {

                Log.i(TAG, "ignoring recognition result asr message");
            }

        } else {

            if (!method.contentEquals(AsrMessage.METHOD_START_OF_SPEECH)) {

                Log.i(TAG, "ignoring asr message with method: " + method);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean handleMessage(Message msg) {

        if (msg.arg1 == MESSAGE_SET_LANGUAGE_MODEL_URI) {

            // Set language model uri.
            mLanguageModelUri = (String) msg.obj;

        } else if (msg.arg1 == MESSAGE_CONNECT_TO_SERVER) {

            // Connect to server if thread is in correct state.
            if (mConnectionState == CONNECTION_STATE_DISCONNECTED) {
                connectToServer();
            } else if (mConnectionState == CONNECTION_STATE_IDLE) {
                // Already connected
                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_CREATE_SESSION;
                message.sendToTarget();
            } else {
                Log.i(TAG, "ignoring connect to server handler message");
            }

        } else if (msg.arg1 == MESSAGE_START_RECOGNITION) {

            // Connect to server if thread is in correct state.

            if (mConnectionState == CONNECTION_STATE_IDLE) {

                byte[] languageModel = mLanguageModelUri.getBytes(Constants.NETWORK_CHARSET);

                RecognitionConfig config = (msg.obj != null ? (RecognitionConfig) msg.obj : null);

                Map<String, String> headerFields = new HashMap<>();
                headerFields.put("Content-Type", "text/uri-list");
                headerFields.put("Content-Length", Integer.toString(languageModel.length));

                // define os parametros do reconhecimento
                if (config != null) {
                    HashMap<String, String> map = config.getParameterMap();
                    // adiciona header extras (parametros para o reconhecimento)
                    for (String key : map.keySet()) {
                        headerFields.put(key, map.get(key));
                    }
                }

                if (!sendAsrMessage(new AsrMessage(AsrMessage.METHOD_START_RECOGNITION, headerFields, languageModel))) {

                    Log.w(TAG, "error sending start recognition");
                }

                // Set Network Timeout
                setNetworkTimeout();

                mConnectionState = CONNECTION_STATE_WAITING_START_RECOGNITION;

            } else {
                Log.i(TAG, "ignoring start recognition handler message");
            }

        } else if (msg.arg1 == MESSAGE_RELEASE_SESSION) {

            // Handle release session if thread is in correct state.
            if (mConnectionState != CONNECTION_STATE_DISCONNECTED) {

                if (!sendAsrMessage(new AsrMessage(AsrMessage.METHOD_RELEASE_SESSION, null, null))) {

                    Log.w(TAG, "error sending release session");
                }

                // Set Network Timeout
                setNetworkTimeout();

                mConnectionState = CONNECTION_STATE_WAITING_RELEASE_SESSION;

            } else {

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_RELEASE_SESSION;
                message.sendToTarget();
            }

        } else if (msg.arg1 == MESSAGE_CANCEL_RECOGNITION) {

            // Handle cancel recognition if thread is in correct state.
            if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO) {

                if (!sendAsrMessage(new AsrMessage(AsrMessage.METHOD_CANCEL_RECOGNITION, null, null))) {

                    Log.w(TAG, "error sending cancel recognition");
                }

                // Set Network Timeout
                setNetworkTimeout();

                mConnectionState = CONNECTION_STATE_WAITING_CANCEL_RECOGNITION;

            } else {

                Log.i(TAG, "ignoring cancel recognition to server handler message");
            }

        } else if (msg.arg1 == MESSAGE_HANDLE_AUDIO_PACKET) {

            // Handle incoming audio packet if thread is in correct state.

            if (mConnectionState == CONNECTION_STATE_IDLE
                    || mConnectionState == CONNECTION_STATE_DISCONNECTED
                    || mConnectionState == CONNECTION_STATE_WAITING_SERVER_HANDSHAKE
                    || mConnectionState == CONNECTION_STATE_WAITING_CREATE_SESSION
                    || mConnectionState == CONNECTION_STATE_WAITING_START_RECOGNITION
                    || mConnectionState == CONNECTION_STATE_STREAMING_AUDIO) {

                handleAudioPacket((byte[]) msg.obj, msg.arg2 == 1);

            } else {

                Log.i(TAG, "ignoring handle audio packet handler message");
            }

        } else if (msg.arg1 == MESSAGE_ON_CPQD_ASR_LIBRARY_ERROR) {

            // Remove network timeout.
            removeMessages(WHAT_NETWORK_TIMEOUT);

            if (mConnectionState != CONNECTION_STATE_DISCONNECTED) {
                mConnectionState = CONNECTION_STATE_IDLE;
            }

        } else if (msg.arg1 == MESSAGE_START_INPUT_TIMERS) {

            // Start input timers if thread is in correct state.
            if (mConnectionState == CONNECTION_STATE_STREAMING_AUDIO
                    || mConnectionState == CONNECTION_STATE_WAITING_RECOGNITION_RESULT) {

                if (!sendAsrMessage(new AsrMessage(AsrMessage.METHOD_START_INPUT_TIMERS, null, null))) {
                    Log.w(TAG, "error sending start input timers");
                }
            } else {
                Log.i(TAG, "ignoring start input timers to server handler message");
            }

        } else if (msg.arg1 == INTERNAL_MESSAGE_RESET_NETWORK_TIMEOUT) {

            // Reset network timeout.

            removeMessages(WHAT_NETWORK_TIMEOUT);

            setNetworkTimeout();

        } else if (msg.arg1 == INTERNAL_MESSAGE_SET_WEBSOCKET_SESSION) {

            // Set websocket session reference if thread is in correct state.
            if (mConnectionState == CONNECTION_STATE_WAITING_SERVER_HANDSHAKE) {
                mWebsocketSession = (Session) msg.obj;
            } else {
                Log.i(TAG, "ignoring set websocket session handler message");
            }

        } else if (msg.arg1 == INTERNAL_MESSAGE_CREATE_ASR_SESSION) {

            // Send a create ASR session ASR message if thread is in correct state.
            if (mConnectionState == CONNECTION_STATE_WAITING_SERVER_HANDSHAKE) {
                createAsrSession();
            } else {
                Log.i(TAG, "ignoring create asr session handler message");
            }

        } else if (msg.arg1 == INTERNAL_MESSAGE_HANDLE_ASR_MESSAGE) {

            // Handle ASR message received from server.
            handleAsrMessage((AsrMessage) msg.obj);

        } else if (msg.arg1 == INTERNAL_MESSAGE_ON_CONNECTION_CLOSE) {

            // Handle on connection close callback.
            if (mConnectionState != CONNECTION_STATE_IDLE && mConnectionState != CONNECTION_STATE_DISCONNECTED) {
                Log.w(TAG, "unexpected websocket session close");
            }

            mConnectionState = CONNECTION_STATE_DISCONNECTED;

            Message message = mRecognizer.obtainMessage();
            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
            message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Network error");
            message.sendToTarget();

        } else if (msg.arg1 == INTERNAL_MESSAGE_ON_WEBSOCKET_LIBRARY_ERROR) {

            // Handle websocket library error.
            //
            // There is this specific error that is thrown even when everything went
            // smoothly, so it is ignored.  Or else, an error is raised to the
            // main handler.

            Throwable throwable = (Throwable) msg.obj;

            if (throwable.getClass() == IllegalStateException.class
                    && throwable.getMessage().contentEquals("The connection has been closed.")) {

                Log.d(TAG, "ignoring closed connection IllegalStateException");

            } else if (throwable.getClass() == DecodeException.class) {

                Log.w(TAG, "asr header error", throwable);

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
                message.obj = new RecognitionError(RecognitionErrorCode.FAILURE, "ASR message header error");
                message.sendToTarget();
            } else {

                Log.w(TAG, "connection error", throwable);

                Message message = mRecognizer.obtainMessage();
                message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
                message.obj = new RecognitionError(RecognitionErrorCode.CONNECTION_FAILURE, "Network error");
                message.sendToTarget();
            }

        } else if (msg.arg1 == INTERNAL_MESSAGE_RAISE_NETWORK_TIMEOUT) {

            // Raise network timeout, i.e.
            // raise an error to the main handler.

            resetConnectionState(true, mNetworkTimeoutCloseReason);

            Message message = mRecognizer.obtainMessage();
            message.arg1 = SpeechRecognizerImpl.MESSAGE_ON_ERROR;
            message.obj = new RecognitionError(RecognitionErrorCode.SESSION_TIMEOUT, "Session timeout");
            message.sendToTarget();

        } else {

            Log.i(TAG, "ignoring handler message with code: " + Integer.toString(msg.arg1));
        }

        return true;
    }

    /**
     * <p>Client websocket endpoint that handles callbacks like
     * {@link OnOpen}, {@link OnMessage} etc.</p>
     * <p>These callbacks are executed in a thread managed by
     * the websocket library, so all processing in this object's
     * methods is delegated to {@link AsrServerConnectionThread}
     * in order to properly synchronize critical sections.</p>
     */
    @ClientEndpoint(encoders = AsrMessageEncoder.class, decoders = AsrMessageDecoder.class)
    private class AsrClientEndpoint {

        /**
         * <p>Websocket open callback.</p>
         * <p>Resets network timeout, stores a reference to websocket session
         * and starts recognition process.</p>
         *
         * @param session        websocket session that will be stored.
         * @param endpointConfig unused.
         * @see #mWebsocketSession
         */
        @SuppressWarnings("unused")
        @OnOpen
        public void onOpen(Session session, EndpointConfig endpointConfig) {

            Message message = obtainMessage();
            message.arg1 = INTERNAL_MESSAGE_RESET_NETWORK_TIMEOUT;
            message.sendToTarget();

            message = obtainMessage();
            message.arg1 = INTERNAL_MESSAGE_SET_WEBSOCKET_SESSION;
            message.obj = session;
            message.sendToTarget();

            message = obtainMessage();
            message.arg1 = INTERNAL_MESSAGE_CREATE_ASR_SESSION;
            message.sendToTarget();
        }

        /**
         * <p>Websocket close callback.</p>
         * <p>Notifies {@link AsrServerConnectionThread}.</p>
         *
         * @param session     unused.
         * @param closeReason unused.
         */
        @SuppressWarnings("unused")
        @OnClose
        public void onClose(Session session, CloseReason closeReason) {

            String reason = closeReason.getReasonPhrase();
            if (reason != null && !reason.contentEquals(LibraryErrorCloseReason.REASON_PHRASE)) {
                Message message = obtainMessage();
                message.arg1 = INTERNAL_MESSAGE_ON_CONNECTION_CLOSE;
                message.sendToTarget();
            }
        }

        /**
         * <p>Websocket error callback.</p>
         * <p>Notifies {@link AsrServerConnectionThread} of the error.</p>
         *
         * @param session   unused.
         * @param throwable the error cause;
         *                  it is sent to the notified thread.
         */
        @SuppressWarnings("unused")
        @OnError
        public void onError(Session session, Throwable throwable) {

            Message message = obtainMessage();
            message.arg1 = INTERNAL_MESSAGE_ON_WEBSOCKET_LIBRARY_ERROR;
            message.obj = throwable;
            message.sendToTarget();
        }

        /**
         * <p>Websocket message callback.</p>
         * <p>Resets network timeout
         * and notifies {@link AsrServerConnectionThread} of the message.</p>
         *
         * @param session    unused.
         * @param asrMessage message received from the server;
         *                   it is sent to the notified thread.
         */
        @SuppressWarnings("unused")
        @OnMessage
        public void onMessage(Session session, AsrMessage asrMessage) {

            Message message = obtainMessage();
            message.arg1 = INTERNAL_MESSAGE_RESET_NETWORK_TIMEOUT;
            message.sendToTarget();

            message = obtainMessage();
            message.arg1 = INTERNAL_MESSAGE_HANDLE_ASR_MESSAGE;
            message.obj = asrMessage;
            message.sendToTarget();
        }
    }

    /**
     * Serializer class used by the websocket library to
     * convert {@link AsrMessage} instances into byte arrays.
     */
    public static class AsrMessageEncoder implements Encoder.Binary<AsrMessage> {

        @Override
        public ByteBuffer encode(AsrMessage object) {

            object.logItself();

            return ByteBuffer.wrap(object.toByteArray());
        }

        @Override
        public void init(EndpointConfig config) {
            // Not used
        }

        @Override
        public void destroy() {
            // Not used
        }
    }

    /**
     * Deserializer class used by the websocket library to
     * assemble {@link AsrMessage} instances from byte arrays.
     */
    public static class AsrMessageDecoder implements Decoder.Binary<AsrMessage> {

        @Override
        public AsrMessage decode(ByteBuffer bytes) throws DecodeException {

            byte[] serializedMessage = new byte[bytes.remaining()];

            bytes.get(serializedMessage);

            AsrMessage asrMessage;

            try {

                asrMessage = new AsrMessage(serializedMessage);

            } catch (IllegalArgumentException e) {

                throw new DecodeException(bytes, "could not decode asr message", e);
            }

            asrMessage.logItself();

            return asrMessage;
        }

        @Override
        public boolean willDecode(ByteBuffer bytes) {

            return true;
        }

        @Override
        public void init(EndpointConfig config) {
            // Not used
        }

        @Override
        public void destroy() {
            // Not used
        }
    }

    /**
     * Network timeout {@link CloseReason}.
     *
     * @see #mNetworkTimeoutCloseReason
     */
    private static class NetworkTimeoutCloseReason extends CloseReason {

        // Note that the status codes 4000-4999 should be used, as defined in
        // http://tools.ietf.org/html/rfc6455#section-7.4.2

        /**
         * @see CloseCode#getCode()
         */
        private static final int CLOSE_CODE = 4001;

        /**
         * @see CloseReason#getReasonPhrase()
         */
        private static final String REASON_PHRASE = "network timeout";

        private NetworkTimeoutCloseReason() {

            super(new CloseCode() {

                @Override
                public int getCode() {

                    return CLOSE_CODE;
                }
            }, REASON_PHRASE);
        }
    }

    /**
     * Internal library error {@link CloseReason}.
     *
     * @see #mLibraryErrorCloseReason
     */
    private static class LibraryErrorCloseReason extends CloseReason {

        // Note that the status codes 4000-4999 should be used, as defined in
        // http://tools.ietf.org/html/rfc6455#section-7.4.2

        /**
         * @see CloseCode#getCode()
         */
        private static final int CLOSE_CODE = 4002;

        /**
         * @see CloseReason#getReasonPhrase()
         */
        static final String REASON_PHRASE = "User release session";

        private LibraryErrorCloseReason() {

            super(new CloseCode() {

                @Override
                public int getCode() {

                    return CLOSE_CODE;
                }
            }, REASON_PHRASE);
        }
    }
}

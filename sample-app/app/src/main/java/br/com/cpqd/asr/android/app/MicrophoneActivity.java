package br.com.cpqd.asr.android.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import br.com.cpqd.asr.recognizer.RecognitionListener;
import br.com.cpqd.asr.recognizer.SpeechRecognizer;
import br.com.cpqd.asr.recognizer.SpeechRecognizerInterface;
import br.com.cpqd.asr.recognizer.audio.AudioSource;
import br.com.cpqd.asr.recognizer.audio.MicAudioSource;
import br.com.cpqd.asr.recognizer.model.Interpretation;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.PartialRecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionAlternative;
import br.com.cpqd.asr.recognizer.model.RecognitionError;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;

/**
 * Sample activity that uses CPqD ASR Library.
 */
public class MicrophoneActivity extends AppCompatActivity {

    /**
     * Log tag.
     */
    private static final String TAG = MicrophoneActivity.class.getSimpleName();

    /**
     * Internal code used in permission requests.
     */
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    /**
     * Reference of the {@link TextView} that show recognition status or error messages.
     */
    private TextView mTvStatus;

    /**
     * Reference of the {@link TextView} that show recognition results.
     */
    private TextView mTvResult;

    /**
     * Reference of the {@link Handler}
     */
    private Handler mHandler;

    /**
     * Reference of the ASR library.
     */
    private SpeechRecognizerInterface recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity content view
        setContentView(R.layout.activity_microphone);

        // Set the activity title
        setTitle("Microphone");

        // Set references of UI objects.
        mTvStatus = findViewById(R.id.tv_status);
        mTvResult = findViewById(R.id.tv_result);

        ImageButton imageButtonMicrophone = findViewById(R.id.asr_activity_image_button_microphone);
        imageButtonMicrophone.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    showStatus("Cancel recognition");
                    recognizer.cancelRecognition();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return false;
            }
        });

        // Ask the user the permission to use the microphone
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);

        // Initialize the handler
        HandlerThread handlerThread = new HandlerThread("AsrHandlerThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Method to set the text into text view
     */
    @SuppressLint("SetTextI18n")
    private void showStatus(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvStatus.setText("Status: " + s);
            }
        });
    }

    /**
     * Method called when the microphone button is clicked.
     *
     * @param view the microphone button.
     */
    public void onClickMicrophone(View view) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvStatus.setText("");
                        mTvResult.setText("");
                    }
                });

                try {

                    // Initialize the recognizer
                    recognizer = SpeechRecognizer.builder()
                            .serverURL(Constants.URL)
                            .userAgent("client=Android")  // optional information for logging and server statistics
                            .credentials(Constants.USER, Constants.PWD)
                            .addListener(new RecognitionListener() {
                                @Override
                                public void onSpeechStop(Integer time) {
                                    Log.d(TAG, "End of speech");
                                }

                                @Override
                                public void onSpeechStart(Integer time) {
                                    Log.d(TAG, "Speech started");
                                }

                                @Override
                                public void onRecognitionResult(br.com.cpqd.asr.recognizer.model.RecognitionResult result) {
                                    Log.d(TAG, "Recognition result: " + result.getResultCode());
                                }

                                @Override
                                public void onPartialRecognitionResult(PartialRecognitionResult result) {
                                    Log.d(TAG, "Partial result: " + result.getText());
                                }

                                @Override
                                public void onListening() {
                                    Log.d(TAG, "Server is listening");
                                    showStatus("Server is listening");
                                }

                                @Override
                                public void onError(final RecognitionError error) {
                                    Log.d(TAG, String.format("Recognition error: [%s] %s", error.getCode(), error.getMessage()));
                                    showStatus(error.toString());
                                }

                            }).build(getApplicationContext());

                    // Initiate the audio source
                    AudioSource audio = new MicAudioSource(8000);

                    // Starts the recognize
                    recognizer.recognize(audio, LanguageModelList.builder().addFromURI("builtin:slm/general").build());

                    // Awaits the result of recognition
                    RecognitionResult result = recognizer.waitRecognitionResult().get(0);

                    //Format the result to show
                    String sResult = "";

                    if (result.getAlternatives().size() > 0) {

                        int i = 0;
                        for (RecognitionAlternative alt : result.getAlternatives()) {
                            sResult = sResult.concat(String.format("Alternative [%s] (score = %s): %s \n\n", i++, alt.getConfidence(), alt.getText()));

                            int j = 0;
                            for (Interpretation interpretation : alt.getInterpretations()) {
                                sResult = sResult.concat(String.format("\t Interpretation [%s]: %s \n\n", j++, interpretation));
                            }
                        }

                    } else {
                        sResult = result.getResultCode().toString();
                    }

                    final String formatedResult = sResult;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvResult.setText(formatedResult);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    // If you do not have more audio to recognize, log out
                    try {
                        recognizer.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        });
    }
}

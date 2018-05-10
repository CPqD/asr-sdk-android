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
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.PartialRecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionAlternative;
import br.com.cpqd.asr.recognizer.model.RecognitionConfig;
import br.com.cpqd.asr.recognizer.model.RecognitionError;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;
import br.com.cpqd.asr.recognizer.model.RecognitionResultCode;

/**
 * Sample activity that uses CPqD ASR Library.
 */
public class MicrophoneContinousModeActivity extends AppCompatActivity {

    /**
     * Log tag.
     */
    private static final String TAG = MicrophoneContinousModeActivity.class.getSimpleName();

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

    private AudioSource mAudioSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity content view
        setContentView(R.layout.activity_microphone);

        // Set the activity title
        setTitle("Microphone Continous Mode");

        // Set references of UI objects.
        mTvStatus = findViewById(R.id.tv_status);
        mTvResult = findViewById(R.id.tv_result);

        ImageButton imageButtonMicrophone = findViewById(R.id.asr_activity_image_button_microphone);
        imageButtonMicrophone.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                // finish the audio capture
                try {
                    if (mAudioSource != null) {
                        mAudioSource.close();
                    }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // finish the audio capture
        try {
            if (mAudioSource != null) {
                mAudioSource.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Method to set the text into text view
     */
    @SuppressLint("SetTextI18n")
    private void showText(final String s) {
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
                            .recogConfig(RecognitionConfig.builder().continuousMode(true).build())
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
                                @SuppressLint("SetTextI18n")
                                public void onRecognitionResult(RecognitionResult result) {
                                    Log.d(TAG, "Recognition result: " + result.getResultCode());
                                    if (result.getResultCode().equals(RecognitionResultCode.RECOGNIZED)) {
                                        if (result.getAlternatives().size() > 0) {
                                            final RecognitionAlternative alternative = result.getAlternatives().get(0);
                                            if (alternative.getText() != null && !alternative.getText().isEmpty()) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        String textToShow = mTvResult.getText().toString();
                                                        mTvResult.setText(textToShow + " " + alternative.getText());
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onPartialRecognitionResult(PartialRecognitionResult result) {
                                    Log.d(TAG, "Partial result: " + result.getText());
                                }

                                @Override
                                public void onListening() {
                                    Log.d(TAG, "Server is listening");
                                    showText("Server is listening");
                                }

                                @Override
                                public void onError(final RecognitionError error) {
                                    Log.d(TAG, String.format("Recognition error: [%s] %s", error.getCode(), error.getMessage()));
                                    showText(error.toString());
                                }

                            }).build(getApplicationContext());

                    // Initiate the audio source
                    mAudioSource = new MicAudioSource(8000);

                    // Language model (as installed in the server environment)
                    LanguageModelList lmList = LanguageModelList.builder().addFromURI("builtin:slm/general").build();

                    // Starts the recognize
                    recognizer.recognize(mAudioSource, lmList);

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }
}

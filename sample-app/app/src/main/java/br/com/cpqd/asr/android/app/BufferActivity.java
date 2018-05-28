package br.com.cpqd.asr.android.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;

import br.com.cpqd.asr.recognizer.SpeechRecognizer;
import br.com.cpqd.asr.recognizer.SpeechRecognizerInterface;
import br.com.cpqd.asr.recognizer.audio.BufferAudioSource;
import br.com.cpqd.asr.recognizer.model.Interpretation;
import br.com.cpqd.asr.recognizer.model.LanguageModelList;
import br.com.cpqd.asr.recognizer.model.RecognitionAlternative;
import br.com.cpqd.asr.recognizer.model.RecognitionResult;

/**
 * Sample activity that uses CPqD ASR Library.
 */
public class BufferActivity extends AppCompatActivity {

    /**
     * Log tag.
     */
    private static final String TAG = BufferActivity.class.getSimpleName();

    /**
     * Reference of the {@link TextView} that show recognition results or error messages.
     */
    private TextView mTextView;

    /**
     * Reference of the {@link Handler}
     */
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity content view
        setContentView(R.layout.activity_buffer_and_file);

        // Set the activity title
        setTitle("Buffer");

        // Set references of UI objects.
        mTextView = findViewById(R.id.tv_result);

        // Initialize the handler
        HandlerThread handlerThread = new HandlerThread("AsrHandlerThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Method to set the text into text view
     */
    private void showText(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(s);
            }
        });
    }

    /**
     * Method called when the button is clicked.
     *
     * @param view the button.
     */
    public void onClick(View view) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {

                showText("");


                SpeechRecognizerInterface recognizer = null;

                try {

                    // Initialize the recognizer
                    recognizer = SpeechRecognizer.builder().serverURL(Constants.URL).credentials(Constants.USER, Constants.PWD).build(getApplicationContext());

                    // Initiate the buffer audio source
                    BufferAudioSource audio = new BufferAudioSource();

                    // Starts the recognize
                    recognizer.recognize(audio, LanguageModelList.builder().addFromURI("builtin:slm/general").build());

                    // Create a inputStream from file
                    InputStream input = getApplicationContext().getAssets().open("pizza-veg-8k.wav");

                    // Reads the file and writes to AudioSource
                    byte[] buffer = new byte[1600]; // 100ms segment (tx 8kHz)
                    int len;
                    boolean keepWriting = true;
                    while ((len = input.read(buffer)) != -1 && keepWriting) {
                        keepWriting = audio.write(buffer, len);
                    }
                    input.close();
                    audio.finish();

                    // Awaits the results of recognition
                    List<RecognitionResult> results = recognizer.waitRecognitionResult();

                    // Get the first result
                    RecognitionResult result = results.get(0);

                    // Format the result to show
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

                    showText(sResult);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                } finally {
                    // If you do not have more audio to recognize, log out
                    try {
                        if (recognizer != null)
                            recognizer.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        });
    }
}

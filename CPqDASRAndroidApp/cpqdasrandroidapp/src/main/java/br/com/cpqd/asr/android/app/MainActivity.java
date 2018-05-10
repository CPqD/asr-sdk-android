package br.com.cpqd.asr.android.app;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

/**
 * Class responsible to show the examples
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final RadioGroup radioGroup = findViewById(R.id.radioGroup);
        Button btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Get selected radio button from radioGroup
                int selectedId = radioGroup.getCheckedRadioButtonId();

                switch (selectedId) {
                    case R.id.radioPos1:
                        Intent iBuffer = new Intent(MainActivity.this, BufferActivity.class);
                        startActivity(iBuffer);
                        break;
                    case R.id.radioPos2:
                        Intent iFile = new Intent(MainActivity.this, FileActivity.class);
                        startActivity(iFile);
                        break;
                    case R.id.radioPos3:
                        Intent iMic = new Intent(MainActivity.this, MicrophoneActivity.class);
                        startActivity(iMic);
                        break;
                    default:
                        Intent iMicContinousMode = new Intent(MainActivity.this, MicrophoneContinousModeActivity.class);
                        startActivity(iMicContinousMode);
                        break;
                }
            }
        });
    }
}
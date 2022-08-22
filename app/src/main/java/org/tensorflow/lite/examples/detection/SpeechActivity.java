package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.util.Locale;

public class SpeechActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    public static TextToSpeech mTTS;
    public static EditText mEditText;
    public static SeekBar mSeekBarPitch;
    public static SeekBar mSeekBarSpeed;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);


        Button mButtonSpeak = findViewById(R.id.button_speak);

        mTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTTS.setLanguage(Locale.ENGLISH);

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            }else {
                Log.d("TTS", "Initialization failed");
            }
        });

        mEditText = findViewById(R.id.edit_text);
        mSeekBarPitch = findViewById(R.id.seek_bar_pitch);
        mSeekBarSpeed = findViewById(R.id.seek_bar_speed);
        mButtonSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak( "Hello World");
            }
        });

}


    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    speak("I'm selfish, impatient and a little insecure. I make mistakes, I am out of control and at times hard to handle. But if you can't handle me at my worst, then you sure as hell don't deserve me at my best");
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void speak(String text1) {
      /*  String text = MultiBoxTracker.labelString;
        float pitch = (float) mSeekBarPitch.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = (float) mSeekBarSpeed.getProgress() / 50;
        if (speed < 0.1) speed = 0.1f;

        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);*/

    mTTS.speak(text1, TextToSpeech.QUEUE_FLUSH, null, "");
        if(!mTTS.isSpeaking()) {
            mTTS = new TextToSpeech(this, this);
            Log.d("TTS", "Speeaking");
        }
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();
    }

    public void onInit(int i) {
        if (i == TextToSpeech.SUCCESS) {
            int result = mTTS.setLanguage(Locale.ENGLISH);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "Language not supported");
            }

        }else {
            Log.d("TTS", "Initialization failed");
        }


    }
}
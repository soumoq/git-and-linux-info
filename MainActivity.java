package parentsapppoc.krishworks.com.parentsapppoc;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import static android.support.design.widget.Snackbar.LENGTH_LONG;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech mTTS;
    private EditText voiceText;
    private SeekBar seekBerPitch;
    private SeekBar seekBerSpeed;
    private Button sayIt;
    private Button check;
    private ProgressBar loding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(new Locale("en", "IN"));

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        sayIt.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initilaization failed");
                }
            }
        });


        voiceText = (EditText) findViewById(R.id.voice_text);
        seekBerPitch = (SeekBar) findViewById(R.id.seek_ber_pitch);
        seekBerSpeed = (SeekBar) findViewById(R.id.seek_ber_speed);
        sayIt = (Button) findViewById(R.id.say_it);
        check = (Button) findViewById(R.id.check);
        loding = (ProgressBar) findViewById(R.id.loding);


        sayIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = voiceText.getText().toString();
                speak(text);
            }
        });


        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean available;

                available = checkStatus();
                if (available) {
                    Toast.makeText(MainActivity.this, "Voice data available", Toast.LENGTH_LONG).show();
                } else {
                    downloadData();
                }
            }
        });

    }


    private void downloadData() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            /*ImplementAsync implementAsync = new ImplementAsync(this);
            implementAsync.execute();*/
            Toast.makeText(this, "Downloading...", Toast.LENGTH_LONG).show();
            loding.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean status = checkStatus();
                    while (!status) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        status = checkStatus();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Boolean check = checkStatus();
                                if (check) {
                                    Toast.makeText(MainActivity.this, "Download complete", Toast.LENGTH_LONG).show();
                                    loding.setProgress(0);
                                    loding.setVisibility(View.INVISIBLE);
                                } else if (!check) {
                                    Toast.makeText(MainActivity.this, "Downloading...", Toast.LENGTH_LONG).show();
                                }

                            }
                        });
                    }
                }
            }).start();


        } else {
            Toast.makeText(this, "Turn on your internet.", Toast.LENGTH_LONG).show();
        }
    }


    private static class ImplementAsync extends AsyncTask<Integer, Integer, Boolean> {


        private WeakReference<MainActivity> activityWeakReference;

        ImplementAsync(MainActivity activity) {
            activityWeakReference = new WeakReference<MainActivity>(activity);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            MainActivity activity = activityWeakReference.get(); //for axcess main activity
            if (activity == null || activity.isFinishing()) {
                return;
            }
            activity.loding.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Integer... params) {


            final MainActivity activity = activityWeakReference.get(); //for axcess main activity
            if (activity == null || activity.isFinishing()) {
                //
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean status = activity.checkStatus();
                    while (!status) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        status = activity.checkStatus();
                    }
                }
            }).start();

            Boolean status = activity.checkStatus();
            return status;


        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            MainActivity activity = activityWeakReference.get(); //for axcess main activity
            if (activity == null || activity.isFinishing()) {
                return;
            }


            activity.loding.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            MainActivity activity = activityWeakReference.get(); //for axcess main activity
            if (activity == null || activity.isFinishing()) {
                return;
            }


            if (activity.checkStatus())
                Toast.makeText(activity, "Download Complete", Toast.LENGTH_LONG).show();
            else {

                Toast.makeText(activity, "Downloading...", Toast.LENGTH_LONG).show();
            }
            activity.loding.setProgress(0);
            activity.loding.setVisibility(View.INVISIBLE);
        }
    }


    private boolean checkStatus() {

        boolean available = false;
        if (mTTS != null) {
            Locale language = new Locale("en", "IN");
            switch (mTTS.isLanguageAvailable(language)) {
                case TextToSpeech.LANG_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                    mTTS.setLanguage(language);

                    Voice voice = mTTS.getVoice();
                    if (voice != null) {
                        Set<String> features = voice.getFeatures();
                        if (features != null && !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                            available = true;
                        }
                    } else {
                        available = false;
                    }
                    //mTTS.setLanguage(language);
                    break;

                case TextToSpeech.LANG_MISSING_DATA:
                case TextToSpeech.LANG_NOT_SUPPORTED:
                default:
                    break;
            }
            Log.i("TTS", "available: " + available);
            //Toast.makeText(getApplicationContext(), "voice data files: " + available, Toast.LENGTH_SHORT).show();
        }
        return available;
    }


    private void speak(String text) {

        if (text != null) {
            float pitch = (float) seekBerPitch.getProgress() / 50;
            if (pitch < 0.1)
                pitch = 0.1f;

            float speed = (float) seekBerSpeed.getProgress() / 50;
            if (speed < 0.1)
                speed = 0.1f;

            mTTS.setPitch(pitch);
            mTTS.setSpeechRate(speed);

            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        } else
            voiceText.setError("You need to enter a text");
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }
}

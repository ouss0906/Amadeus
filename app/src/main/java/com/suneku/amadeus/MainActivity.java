package com.suneku.amadeus;

/*
 * Credits to https://github.com/Yink for the whole idea
 * Code base: https://github.com/Yink/Amadeus
 */

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private String recogLang, spriteVariation;
    private SpeechRecognizer sr;
    private Boolean isSpeaking = false;
    private ImageView voiceButton, menuButton, kurisu;
    private boolean isEndOfSubtitle = false;
    private ResponseProvider responseProvider;
    private CommandProvider commandProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final int REQUEST_PERMISSION_RECORD_AUDIO = 11302;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String appLang = settings.getString("lang", "en");
        recogLang = settings.getString("recognition_lang", "ja-JP");
        spriteVariation = settings.getString("kurisu_outfits", "amadeus");

        voiceButton = (ImageView) findViewById(R.id.imageView_voice_activator);
        menuButton = (ImageView) findViewById(R.id.imageView_menu_button);
        kurisu = (ImageView) findViewById(R.id.imageView_kurisu);

        responseProvider = new ResponseProvider(this, appLang, spriteVariation);
        commandProvider = new CommandProvider(this, settings, recogLang);

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new listener());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_RECORD_AUDIO);
        }

        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    MainActivity host = (MainActivity) view.getContext();

                    int permissionCheck = ContextCompat.checkSelfPermission(host,
                            Manifest.permission.RECORD_AUDIO);

                    /* Input during loop produces bugs and mixes with output */
                    if (!isSpeaking) {
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            promptSpeechInput();
                        } else {
                            respond(responseProvider.getResponse("daga kotowaru"));
                        }
                    }

                } else if (!isSpeaking) {
                    promptSpeechInput();
                }
            }});

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.finish();
            }});

        // TODO: Random greeting line + first start line
        respond(responseProvider.getResponse("hello"));

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LangContext.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sr != null)
            sr.destroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recogLang);

        kurisu.setImageResource(getResources().getIdentifier("kurisu_sided_pleasant_" + spriteVariation , "drawable", getPackageName()));

        /* Temporary workaround for strange bug on 4.0.3-4.0.4 */
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            try {
                startActivityForResult(intent, 1);
            } catch (ActivityNotFoundException a) {
                a.printStackTrace();
            }
        } else {
            sr.startListening(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> input = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    respond(responseProvider.getResponse(input.get(0).toLowerCase()));
                }
                break;
            }

        }
    }

    private void hide_buttons() {
        voiceButton.setVisibility(View.INVISIBLE);
        menuButton.setVisibility(View.INVISIBLE);
    }

    private void show_buttons() {
        voiceButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.VISIBLE);
    }

    /*
    * TODO: Optimize animation and resources
    * Make separate sprite parts and draw them when necessary (Animation class and its derivatives?)
    * Switching to Canvas or OpenGL is preferable.
    */
    private void respond(Response response) {
        final TextView subtitlesView = (TextView) findViewById(R.id.textView_subtitles);
        final ImageView wholeScreen = (ImageView) findViewById(R.id.imageview_full_screen);

        final Handler textHandler = new Handler();

        hide_buttons();
        wholeScreen.setVisibility(View.VISIBLE);

        final int mood = response.getMood();
        kurisu.setImageResource(mood);

        final AnimationDrawable animation = (AnimationDrawable) kurisu.getDrawable();

        animation.start();

        final char[] subtitles = response.getSubtitle().toCharArray();

        wholeScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEndOfSubtitle) {
                    wholeScreen.setVisibility(View.INVISIBLE);
                    show_buttons();
                    subtitlesView.setText("");
                } else {
                    isEndOfSubtitle = true;
                }
            }
        });

        Runnable textRunnable = new Runnable() {
            String subsToDisplay = "";
            int text_character = 0;
            // TODO: DELAY option
            int TEXT_DELAY = 75;

            public void run() {
                // FIXME: Needs to be beautified
                if (text_character < subtitles.length) {
                    if (isEndOfSubtitle) {
                        subsToDisplay = String.valueOf(subtitles);
                        subtitlesView.setText(subsToDisplay);
                        animation.stop();
                        kurisu.setImageResource(mood);
                        textHandler.removeCallbacks(this);
                        return;
                    }
                    subsToDisplay += subtitles[text_character];
                    subtitlesView.setText(subsToDisplay);
                    text_character++;
                    textHandler.postDelayed(this, TEXT_DELAY);
                } else {
                    animation.stop();
                    kurisu.setImageResource(mood);
                    isEndOfSubtitle = true;
                    textHandler.removeCallbacks(this);
                }
            }
        };

        isEndOfSubtitle = false;

        textHandler.post(textRunnable);

    }

    private class listener implements RecognitionListener {

        private final String TAG = "VoiceListener";

        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Speech recognition start");
        }
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Listening to speech");
        }
        public void onRmsChanged(float rmsdB) {
            //Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech() {
            Log.d(TAG, "Speech recognition end");
        }
        public void onError(int error) {
            Log.d(TAG,  "Recognition error: " +  error);
            sr.cancel();
            // TODO: Make error-dependant
            kurisu.setImageResource(getResources().getIdentifier("kurisu_happy_" + spriteVariation , "drawable", getPackageName()));
        }
        public void onResults(Bundle results) {
            String input = "";
            String debug = "";
            Log.d(TAG, "Received results");
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (Object word: data) {
                debug += word + "\n";
            }
            Log.d(TAG, debug);

            input += data.get(0);
            input = input.toLowerCase();

            // Commands are available only for english or russian recognition
            if (recogLang.equals("en-US") || recogLang.equals("ru-RU")) {
                String command = commandProvider.findCommand(input);
                if (command != null) {
                    commandProvider.execute(command);
                } else {
                    respond(responseProvider.getResponse(input));
                }
            } else {
                respond(responseProvider.getResponse(input));
            }
        }
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }

    }

}

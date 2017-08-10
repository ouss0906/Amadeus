package com.suneku.amadeus;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LaunchActivity extends AppCompatActivity {

    private ImageView connect, cancel, settingsButton;
    private TextView status;
    private Boolean isPressed = false;
    private MediaPlayer m;
    private DrawLaunchActivityCanvas canvasView;

    private boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launch);

        RelativeLayout logo = (RelativeLayout) findViewById(R.id.imageView_logo);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final Window win = getWindow();

        connect = (ImageView) findViewById(R.id.imageView_connect);
        cancel = (ImageView) findViewById(R.id.imageView_cancel);
        status = (TextView) findViewById(R.id.textView_call);
        canvasView = new DrawLaunchActivityCanvas(this);
        settingsButton = (ImageView) findViewById(R.id.imageView_settings);

        logo.addView(canvasView);

        if (settings.getBoolean("show_notification", false)) {
            showNotification();
        }

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPressed && isAppInstalled(LaunchActivity.this, "com.google.android.googlequicksearchbox")) {
                    isPressed = true;

                    connect.setImageResource(R.drawable.connect_select);

                    settingsButton.setOnClickListener(null);
                    cancel.setOnClickListener(null);

                    if (!Alarm.isPlaying()) {
                        m = MediaPlayer.create(LaunchActivity.this, R.raw.tone);

                        m.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.start();
                                status.setText(R.string.connecting);
                            }
                        });

                        m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                                Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        });
                    } else {
                        Alarm.cancel(LaunchActivity.this);
                        win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                        win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

                        Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LangContext.wrap(newBase));
    }

    @Override
    protected void onPause() {
        super.onPause();
        canvasView.onPauseMySurfaceView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        final Window win = getWindow();

        if (m != null) {
            m.release();
        }

        Alarm.cancel(LaunchActivity.this);
        win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Window win = getWindow();

        canvasView.onResumeMySurfaceView();
        connect.setImageResource(R.drawable.connect_unselect);
        cancel.setImageResource(R.drawable.cancel_unselect);
        settingsButton.setImageResource(R.drawable.settings_unselected);

        if (isPressed) {
            status.setText(R.string.disconnected);
        } else if (!isAppInstalled(LaunchActivity.this, "com.google.android.googlequicksearchbox")) {
            status.setText(R.string.google_app_error);
        } else if (Alarm.isPlaying()) {
            status.setText(R.string.incoming_call);
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        } else {
            status.setText(R.string.call);
        }

        isPressed = false;

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(LaunchActivity.this, SettingsActivity.class);
                settingsButton.setImageResource(R.drawable.settings_selected);
                startActivity(settingIntent);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel.setImageResource(R.drawable.cancel_select);
                LaunchActivity.this.finish();
            }
        });
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(LaunchActivity.this)
                .setSmallIcon(R.drawable.xp2)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text));
        Intent resultIntent = new Intent(LaunchActivity.this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(LaunchActivity.this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}
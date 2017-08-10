package com.suneku.amadeus;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

class Alarm {

   /*
    * TODO: All code below is ultimate bullshit.
    * Well, not all, but needs rewriting.
    */

    private static MediaPlayer m;
    private static SharedPreferences settings;
    private static Vibrator v;

    static final int ALARM_ID = 104859;
    static final int ALARM_NOTIFICATION_ID = 102434;

    private static final String TAG = "Alarm";
    private static boolean isPlaying = false;
    private static PowerManager.WakeLock sCpuWakeLock;

    static void start(Context context, int ringtone) {

        acquireCpuWakeLock(context);

        settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (settings.getBoolean("vibrate", false)) {
            v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {500, 2000};
            v.vibrate(pattern, 0);
        }

        m = MediaPlayer.create(context, ringtone);

        m.setLooping(true);
        m.start();

        if (m.isPlaying()) {
            isPlaying = true;
        }

        Log.d(TAG, "Start");

    }

    static void cancel(Context context) {

        settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (settings.getBoolean("alarm_toggle", false)) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID,
                    new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

            alarmManager.cancel(pendingIntent);

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("alarm_toggle", false);
            editor.apply();

            if (isPlaying) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(ALARM_NOTIFICATION_ID);

                if (v != null) {
                    v.cancel();
                }

                m.release();

                releaseCpuLock();

                isPlaying = false;
            }

            Log.d(TAG, "Alarm has been cancelled.");
        } else {
            Log.d(TAG, "No alarm was set.");
        }
    }

    static boolean isPlaying() {
        return isPlaying;
    }

    private static void acquireCpuWakeLock(Context context) {
        if (sCpuWakeLock != null) {
            return;
        }

        PowerManager pm =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, TAG);
        sCpuWakeLock.acquire();
    }

    private static void releaseCpuLock() {
        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }

}

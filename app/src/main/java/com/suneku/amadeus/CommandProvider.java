package com.suneku.amadeus;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

class CommandProvider {

    private HashMap<String, Method> methodMap = new HashMap<>();
    private SharedPreferences settings;
    private List<ApplicationInfo> packages;
    private PackageManager pm;
    private Context context;

    private String[] commands, input;
    private String TAG = "CommandProvider";

    CommandProvider(Context context, SharedPreferences settings, String recogLang) {

        String[] desiredLang = recogLang.split("-");
        this.context = LangContext.load(context, desiredLang[0]);
        this.settings = settings;

        // Collect commands
        commands = collectCommands(this.context);

        // Preload list of applications
        packages = collectApps(this.context);

        // Put commands in map
        try {
            methodMap.put(commands[0], CommandProvider.class.getDeclaredMethod("open"));
            methodMap.put(commands[1], CommandProvider.class.getDeclaredMethod("setAlarm"));
            methodMap.put(commands[2], CommandProvider.class.getDeclaredMethod("cancelAlarm"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    String findCommand(String input) {
        this.input = input.split(" ");

        for (String command : commands) {
            // The first word must be a command's trigger word
            if (command.contains(this.input[0])) {
                // Don't think we'll have more than 2 levels of commands...
                if (command.contains(this.input[1])) {
                    Log.d(TAG, "Command level 2.");
                    return command;
                }
                Log.d(TAG, "Command level 1.");
                return command;
            }
        }
        return null;
    }

    void execute(String command) {
        try {
            methodMap.get(command).invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ApplicationInfo> collectApps(Context context) {
        pm = context.getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    private String[] collectCommands(Context context) {
        return new String[]{
                context.getString(R.string.open),
                context.getString(R.string.set_alarm),
                context.getString(R.string.cancel_alarm)
        };
    }

    private void open() {

        Boolean found;

        /* TODO: Dictionary for other languages' equivalents. */

        String requestedApp = "";
        for (int i = 1; i < input.length; i++) {
            requestedApp += " " + input[i];
        }
        requestedApp = requestedApp.trim().toLowerCase();

        for (ApplicationInfo packageInfo : packages) {

            found = true;

            String applicationName = (String) (packageInfo != null ? pm.getApplicationLabel(packageInfo) : "(unknown)");

            applicationName = applicationName.toLowerCase();

            if (!requestedApp.equals(applicationName)) {
                found = false;
            }

            if (found) {
                Log.d(TAG, "Found app!");
                Intent app;
                // TODO: Somehow open apps installed on system partition
                switch (packageInfo.packageName) {
                    /* Exceptional cases */
                    case "com.android.phone": {
                        app = new Intent(Intent.ACTION_DIAL, null);
                        context.startActivity(app);
                        break;
                    }
                    case "com.android.chrome": {
                        app = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
                        app.setPackage(packageInfo.packageName);
                        context.startActivity(app);
                        break;
                    }
                    default: {
                        app = context.getPackageManager().getLaunchIntentForPackage(packageInfo.packageName);
                        /* Check if intent is not null to avoid crash */
                        if (app != null) {
                            app.addCategory(Intent.CATEGORY_LAUNCHER);
                            context.startActivity(app);
                        }
                        break;
                    }
                }
                /* Don't need to search for other ones, so break this loop */
                break;
            }
        }
    }

    private void setAlarm() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Alarm.ALARM_ID,
                new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);

        SharedPreferences.Editor editor = settings.edit();

        ArrayList<String> formatInput = new ArrayList<>();
        for (int i = 3; i < input.length; i++) {
            formatInput.add(input[i]);
        }

        // TODO: "outsmart" Google Recognition somehow
        String stringTime = formatInput.get(0);
        Calendar calendar = Calendar.getInstance();
        if (stringTime.contains(":")) {
            String[] formatTime = stringTime.split(":");
            int hours = Integer.parseInt(formatTime[0]);
            int minutes = Integer.parseInt(formatTime[1]);
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
        } else {
            int hours = Integer.parseInt(stringTime);
            calendar.set(Calendar.HOUR_OF_DAY, hours);
        }

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        long timeInMillis = calendar.getTimeInMillis();

        Log.d(TAG, "" + timeInMillis);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Current API functions have been executed");
            setTime(timeInMillis, alarmManager, pendingIntent);
        } else {
            Log.d(TAG, "Legacy API functions have been executed");
            setTimeLegacy(timeInMillis, alarmManager, pendingIntent);
        }
        editor.putLong("alarm_time", timeInMillis);
        editor.putBoolean("alarm_toggle", true);
        editor.apply();
    }

    private void cancelAlarm() {
        Alarm.cancel(context);
    }

    // Utils
    @SuppressWarnings("deprecation")
    private void setTimeLegacy(long timeInMillis, AlarmManager alarmManager, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setTime(long time, AlarmManager alarmManager, PendingIntent pendingIntent) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

}

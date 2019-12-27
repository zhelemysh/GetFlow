package com.wentura.pomodoro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_SILENT;

public class NotificationButtonReceiver extends BroadcastReceiver {

    private static final String TAG = "pomodoro";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra(Constants.BUTTON_ACTION);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editPreferences = preferences.edit();

        if (action == null) {
            return;
        }

        switch (action) {
            case Constants.BUTTON_STOP: {
                stopNotificationService(context);
                stopEndNotificationService(context);

                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);

                int lastSessionDuration = preferences.getInt(Constants.LAST_SESSION_DURATION, 0);
                int timerLeft = preferences.getInt(Constants.TIMER_LEFT, 0);

                if (timerLeft != 0) {
                    if (isBreakState) {
                        new UpdateDatabaseBreaks(context, lastSessionDuration - timerLeft).execute();
                    } else {
                        new UpdateDatabaseIncompleteWorks(context, lastSessionDuration - timerLeft).execute();
                    }
                }

                editPreferences.putBoolean(Constants.IS_TIMER_RUNNING, false);
                editPreferences.putBoolean(Constants.IS_BREAK_STATE, false);
                editPreferences.putBoolean(Constants.IS_SKIP_BUTTON_VISIBLE, false);
                editPreferences.putBoolean(Constants.IS_START_BUTTON_VISIBLE, true);
                editPreferences.putBoolean(Constants.IS_PAUSE_BUTTON_VISIBLE, false);
                editPreferences.putBoolean(Constants.IS_STOP_BUTTON_VISIBLE, false);
                editPreferences.apply();

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.UPDATE_UI_ACTION, Constants.BUTTON_STOP);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);

                Log.d(TAG, "onReceive: Button Stop");

                Utility.toggleDoNotDisturb(context, RINGER_MODE_NORMAL);
                break;
            }
            case Constants.BUTTON_SKIP: {
                Log.d("Pomodoro", "onReceive: SKIP");
                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);
                stopEndNotificationService(context);
                stopNotificationService(context);

                int lastSessionDuration = preferences.getInt(Constants.LAST_SESSION_DURATION, 0);

                int timerLeft = preferences.getInt(Constants.TIMER_LEFT, 0);

                if (isBreakState) {
                    editPreferences.putBoolean(Constants.IS_BREAK_STATE, false);
                    editPreferences.putBoolean(Constants.IS_WORK_ICON_VISIBLE, true);
                    editPreferences.putBoolean(Constants.IS_BREAK_ICON_VISIBLE, false);

                    editPreferences.putInt(Constants.TIMER_LEFT,
                            Integer.parseInt(preferences.getString(Constants.WORK_DURATION_SETTING,
                                    Constants.DEFAULT_WORK_TIME)) * 60000);

                    Utility.toggleDoNotDisturb(context, RINGER_MODE_SILENT);
                } else {
                    editPreferences.putBoolean(Constants.IS_BREAK_STATE, true);
                    editPreferences.putBoolean(Constants.IS_WORK_ICON_VISIBLE, false);
                    editPreferences.putBoolean(Constants.IS_BREAK_ICON_VISIBLE, true);

                    editPreferences.putInt(Constants.TIMER_LEFT,
                            Integer.parseInt(preferences.getString(Constants.WORK_DURATION_SETTING,
                                    Constants.DEFAULT_WORK_TIME)) * 60000);

                    Utility.toggleDoNotDisturb(context, RINGER_MODE_NORMAL);
                }

                if (timerLeft != 0) {
                    if (isBreakState) {
                        new UpdateDatabaseBreaks(context, lastSessionDuration - timerLeft).execute();
                    } else {
                        new UpdateDatabaseIncompleteWorks(context, lastSessionDuration - timerLeft).execute();
                    }
                }

                editPreferences.putBoolean(Constants.IS_PAUSE_BUTTON_VISIBLE, true);
                editPreferences.putBoolean(Constants.IS_START_BUTTON_VISIBLE, false);
                editPreferences.putBoolean(Constants.IS_TIMER_RUNNING, true);
                editPreferences.apply();

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.UPDATE_UI_ACTION, Constants.BUTTON_SKIP);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);

                startNotificationService(context);
                break;
            }
            case Constants.BUTTON_START: {
                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);
                editPreferences.putBoolean(Constants.IS_TIMER_RUNNING, true);
                editPreferences.putBoolean(Constants.IS_SKIP_BUTTON_VISIBLE, true);
                editPreferences.putBoolean(Constants.IS_START_BUTTON_VISIBLE, false);
                editPreferences.putBoolean(Constants.IS_PAUSE_BUTTON_VISIBLE, true);
                editPreferences.putBoolean(Constants.IS_STOP_BUTTON_VISIBLE, true);
                editPreferences.apply();

                stopEndNotificationService(context);
                startNotificationService(context);

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.UPDATE_UI_ACTION, Constants.BUTTON_START);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);

                if (!isBreakState) {
                    Utility.toggleDoNotDisturb(context, RINGER_MODE_SILENT);
                }
                break;
            }
            case Constants.BUTTON_PAUSE: {
                boolean isBreakState = preferences.getBoolean(Constants.IS_BREAK_STATE, false);
                editPreferences.putBoolean(Constants.IS_TIMER_RUNNING, false);
                editPreferences.putBoolean(Constants.IS_SKIP_BUTTON_VISIBLE, true);
                editPreferences.putBoolean(Constants.IS_START_BUTTON_VISIBLE, true);
                editPreferences.putBoolean(Constants.IS_PAUSE_BUTTON_VISIBLE, false);
                editPreferences.putBoolean(Constants.IS_STOP_BUTTON_VISIBLE, true);
                editPreferences.apply();

                if (!isBreakState) {
                    Utility.toggleDoNotDisturb(context, RINGER_MODE_NORMAL);
                }

                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.putExtra(Constants.NOTIFICATION_SERVICE,
                        Constants.NOTIFICATION_SERVICE_PAUSE);
                context.startService(serviceIntent);

                Intent updateUI = new Intent(Constants.BUTTON_CLICKED);
                updateUI.putExtra(Constants.UPDATE_UI_ACTION, Constants.BUTTON_PAUSE);
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateUI);
                break;
            }
        }
    }

    private void stopNotificationService(Context context) {
        Intent stopService = new Intent(context, NotificationService.class);
        context.stopService(stopService);
    }

    private void stopEndNotificationService(Context context) {
        Intent stopService = new Intent(context, EndNotificationService.class);
        context.stopService(stopService);
    }

    private void startNotificationService(Context context) {
        Intent startService = new Intent(context, NotificationService.class);
        context.startService(startService);
    }
}

/*
 *  Copyright (C) 2015 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnigreen;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;

public class ScanService extends Service {
    private final static String TAG = "ScanService";
    private static boolean DEBUG = true;

    private ScanReceiver mReceiver;
    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;
    private int mUserId = -1;

    private static boolean mIsRunning;
    private static boolean mCommitSuicide;
    private AlarmManager mAlarmManager;
    private RunningState mState;

    public static boolean isRunning() {
        return mIsRunning;
    }

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            mUserId = UserHandle.myUserId();
            Log.d(TAG, "started ScanService " + mUserId);

            mReceiver = new ScanReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ScanReceiver.ACION_SCAN);
            filter.addAction(Intent.ACTION_USER_SWITCHED);
            filter.addAction(Intent.ACTION_SHUTDOWN);

            registerReceiver(mReceiver, filter);

            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            updatePrefs(mPrefs, null);

            mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs,
                        String key) {
                    try {
                        updatePrefs(prefs, key);
                    } catch(Exception e) {
                        Log.e(TAG, "updatePrefs", e);
                    }
                }
            };

            mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
            mState = RunningState.getInstance(this);

            mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent scanIntent = new Intent(ScanReceiver.ACION_SCAN);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 42,
                    scanIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, 1000, pendingIntent);
            mIsRunning = true;
        } catch(Exception e) {
            Log.e(TAG, "onCreate", e);
            commitSuicide();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "stopped ScanService " + mUserId);

        try {
            unregisterReceiver(mReceiver);
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        } catch(IllegalArgumentException e) {
            // ignored on purpose
        }

        mIsRunning = false;

        if (mCommitSuicide) {
            mCommitSuicide = false;
            // to get the "app has stopped alert"
            throw new RuntimeException("Failed to start OmniGreen");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public class ScanReceiver extends BroadcastReceiver {
        public static final String ACION_SCAN = "org.omnirom.omnigreen.ACION_SCAN";

        @Override
        public void onReceive(final Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if(DEBUG){
                    Log.d(TAG, "onReceive " + action);
                }
                if (ACION_SCAN.equals(action)) {
                    Log.d(TAG, "" + System.currentTimeMillis());
                    mState.updateNow();
                    ArrayList<RunningState.MergedItem> newItems = mState.getCurrentMergedItems();
                    for (RunningState.MergedItem item : newItems) {
                        try {
                            Log.d(TAG, "" + item.mPackageInfo.packageName + " " + item.mActiveSince + " " + item.mBackground + " " + item.mDescription);
                            for (RunningState.ServiceItem service : item.mServices) {
                                Log.d(TAG, "   " + service.mRunningService.service.getClassName() + " " + service.mBackground);
                                //stopActiveService(service);
                            }
                        } catch (Exception e) {
                        }
                    }
                    Intent scanIntent = new Intent(ScanReceiver.ACION_SCAN);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(ScanService.this, 42,
                            scanIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, 5000, pendingIntent);
                }
            } catch(Exception e) {
                Log.e(TAG,"onReceive", e);
            }
        }
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            if (mIsRunning) {
            }
        } catch(Exception e) {
            Log.e(TAG, "onConfigurationChanged", e);
        }
    }

    private void commitSuicide() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(SettingsActivity.PREF_START_ON_BOOT, false).commit();
        prefs.edit().putBoolean(SettingsActivity.PREF_ENABLE, false).commit();
        mCommitSuicide = true;
        Intent stopIntent = new Intent(this, ScanService.class);
        stopService(stopIntent);
    }
    
    private void stopActiveService(RunningState.ServiceItem si) {
        try {
            stopService(new Intent().setComponent(si.mRunningService.service));
        } catch(SecurityException e) {
        }
    }
}

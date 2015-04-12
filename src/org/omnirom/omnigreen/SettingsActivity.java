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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;

public class SettingsActivity extends PreferenceActivity implements
        OnPreferenceChangeListener  {
    private static final String TAG = "SettingsActivity";

    public static final String PREF_START_ON_BOOT = "start_on_boot";
    public static final String PREF_ENABLE = "enable";

    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;
    private Switch mToggleServiceSwitch;

    @Override
    public void onPause() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        super.onPause();
    }

    @Override
    public void onResume() {
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.settings);

        mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,
                    String key) {
                updatePrefs(prefs, key);
            }
        };

        updatePrefs(mPrefs, null);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        boolean startOnBoot = mPrefs.getBoolean(SettingsActivity.PREF_START_ON_BOOT, false);
        mToggleServiceSwitch = (Switch) menu.findItem(R.id.toggle_service).getActionView().findViewById(R.id.switch_item);
        mToggleServiceSwitch.setChecked(ScanService.isRunning() && mPrefs.getBoolean(SettingsActivity.PREF_ENABLE, startOnBoot));
        mToggleServiceSwitch.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                boolean value = ((Switch)v).isChecked();
                Intent svc = new Intent(SettingsActivity.this, ScanService.class);
                Log.d(TAG, "toggle service " + value);
                if (value) {
                    if (ScanService.isRunning()){
                        stopService(svc);
                    }
                    startService(svc);
                } else {
                    if (ScanService.isRunning()){
                        stopService(svc);
                    }
                }
                mPrefs.edit().putBoolean(PREF_ENABLE, value).commit();
            }});
        return true;
    }

    public void updatePrefs(SharedPreferences prefs, String key) {
        if (!ScanService.isRunning()){
        }
    }
}

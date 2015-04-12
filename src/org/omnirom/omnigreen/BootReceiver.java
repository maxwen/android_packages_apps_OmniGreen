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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "RecentsBootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean startOnBoot = prefs.getBoolean(SettingsActivity.PREF_START_ON_BOOT, false);
            if (startOnBoot && prefs.getBoolean(SettingsActivity.PREF_ENABLE, startOnBoot)) {
                Intent startIntent = new Intent(context, ScanService.class);
                context.startService(startIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't start load average service", e);
        }
    }
}
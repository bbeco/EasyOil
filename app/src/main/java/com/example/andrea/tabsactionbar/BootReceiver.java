package com.example.andrea.tabsactionbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Created by andrea on 2/24/17.
 */

/**
 * This class starts the timer that checks for new messages
 */
public class BootReceiver extends BroadcastReceiver {


	AlarmReceiver alarmReceiver = new AlarmReceiver();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			alarmReceiver.setAlarm(context);
		}
	}
}

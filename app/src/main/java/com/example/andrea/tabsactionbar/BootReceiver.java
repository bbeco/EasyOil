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
	private AlarmManager alarmMgr;
	private PendingIntent alarmIntent;

	/** This is the chosen period in milliseconds before the alarm go off */
	private long ALARM_INTERVAL = 5000;

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent alarmReceiverIntent = new Intent(context, AlarmReceiver.class);
		alarmIntent = PendingIntent.getBroadcast(context, 0, alarmReceiverIntent, 0);
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + ALARM_INTERVAL,
					ALARM_INTERVAL, alarmIntent);
		}
	}
}

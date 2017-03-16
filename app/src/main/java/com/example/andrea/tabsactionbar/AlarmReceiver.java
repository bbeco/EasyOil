package com.example.andrea.tabsactionbar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * This class is in charge of receiving the alarm timeout and start the sampleservice.
 *
 * Created by andrea on 2/27/17.
 */

public class AlarmReceiver extends WakefulBroadcastReceiver {
	private final String TAG = "AlarmReceiver";
	private AlarmManager alarmMgr;
	private PendingIntent alarmIntent;
	private static final long alarmPeriod = 5*1000; //alarm period in millisec

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "onReceive");
		Intent serviceIntent = new Intent(context, SampleService.class);
		startWakefulService(context, serviceIntent);
	}

	/**
	 * Sets a repeating alarm to check for notifications.
	 *
	 * @param context
	 */
	public void setAlarm(Context context) {
		Log.i(TAG, "setAlarm");
		alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime(),
				alarmPeriod, alarmIntent);

		/** Enabling BootReceiver to restart the alarm when device is rebooted */
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	/**
	 * Cancels the repeating alarm
	 */
	public void cancelAlarm(Context context) {
		Log.i(TAG, "cancelAlarm");
		alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		// If the alarm has been set, cancel it.
		if (alarmMgr!= null) {
			alarmMgr.cancel(alarmIntent);
		} else {
			Log.w(TAG, "Alarm not canceled: AlarmManager is null");
		}

		// Disable {@code SampleBootReceiver} so that it doesn't automatically restart the
		// alarm when the device is rebooted.
		ComponentName receiver = new ComponentName(context, BootReceiver.class);
		PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(receiver,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}

package com.example.andrea.tabsactionbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class is in charge of receiving the alarm timeout and start the sampleservice.
 *
 * Created by andrea on 2/27/17.
 */

public class AlarmReceiver extends Activity {
	private final String TAG = "AlarmReceiver";

	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "onReceive");
		Intent serviceIntent = new Intent(context, SampleService.class);
		startService(serviceIntent);
	}
}

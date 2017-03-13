package com.example.andrea.tabsactionbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class SettingActivity extends AppCompatActivity {

    private static final String TAG = "SettingActivity";
    public static final String USER_EMAIL_KEY = "UserEmailKey";
    private Messenger mService;
    private boolean bound;
    private Messenger mMessenger = new Messenger(new SettingActivity.IncomingHandler());
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
    /*    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);*/
        Intent s = new Intent(this, SampleService.class);
        Log.i(TAG,"arrivati qui prima del bind");
        bindService(s, mServiceConnection, Context.BIND_AUTO_CREATE);

    }
    @Override
    public void onPause (){
        Log.i(TAG, "onPause");
        if (bound) {
            unregisterSetting();
            unbindService(mServiceConnection);
            bound = false;
        }
        super.onPause();
    }
    private void registerSetting() {
        Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
        registration.arg1 = SampleService.SETTING_ACTIVITY;
        registration.replyTo = mMessenger;

        try {
            mService.send(registration);
        } catch (RemoteException re) {
            /* Service has crashed, display an error */
            Log.e(TAG, "Unable to send client registration to service");
        }
        Message registrationRequest = Message.obtain(null, MessageTypes.REGISTRATION_REQUEST);
        registrationRequest.replyTo = mMessenger;

        try {
            mService.send(registrationRequest);
        } catch (RemoteException re) {
            /* Service has crashed. Nothing to do here */
        }
        Log.i(TAG, "registration command sent");
    }
    private void unregisterSetting() {
        Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
        unregistration.arg1 = SampleService.SETTING_ACTIVITY;
        unregistration.replyTo = mMessenger;

        try {
            mService.send(unregistration);
        } catch (RemoteException re) {
            /* Service crashed. Nothing to do */
        }
    }
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Bound");
            mService = new Messenger(service);
            bound = true;
            registerSetting();
            Button btnReset = (Button) findViewById(R.id.button3);
            btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG,"Reset home/work positions");
                    Intent myInt =  getIntent();
                    String userEmail = myInt.getStringExtra(USER_EMAIL_KEY);
                    CommuteRequest creq = new CommuteRequest(null,null,null,null,userEmail,false);
                    Message msg = Message.obtain(null,MessageTypes.COMMUTE_REQUEST);
                    msg.obj = creq;
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Unbound");
            mService = null;
            bound = false;
        }
    };
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                    Log.w(TAG, "Received an unknown task message");
                    super.handleMessage(msg);
            }
        }
    }
}

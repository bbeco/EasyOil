package com.example.andrea.tabsactionbar;

/**
 * Created by Francesco on 01/03/2017.
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.nearby.messages.internal.MessageType;

import java.util.ArrayList;


public class CommuteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    ArrayList<Marker> oilMarkers;
    private boolean bound;
    private static final String TAG = "CommuteActivity";
    public static final String USER_EMAIL_KEY = "userEmailKey";
    private Messenger mService;
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    Marker home, work;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bound = false;
        oilMarkers = new ArrayList<Marker>();
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();

        ab.setDisplayHomeAsUpEnabled(true);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Intent myInt =  getIntent();
        userEmail = myInt.getStringExtra(USER_EMAIL_KEY);
        Intent s = new Intent(this, SampleService.class);
        Log.i("CommuteActivity","arrivati qui prima del bind");
        bindService(s, mServiceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onPause (){
        Log.i("CommuteActivity", "onPause");
        if (bound) {
            unregisterCommute();
            unbindService(mServiceConnection);
            bound = false;
        }
        super.onPause();
    }
    private void registerCommute() {
        Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
        registration.arg1 = SampleService.COMMUTE_ACTIVITY;
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

    /**
     * Unregister from the service
     */
    private void unregisterCommute() {
        Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
        unregistration.arg1 = SampleService.COMMUTE_ACTIVITY;
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
            registerCommute();
            Log.i(TAG,"mail:"+userEmail);
            CommuteRequest creq = new CommuteRequest(null,null,null,null,userEmail);
            Message msg = Message.obtain(null, MessageTypes.COMMUTE_REQUEST);
            msg.obj = creq;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Unbound");
            mService = null;
            bound = false;
        }
    };
    private double valueNotNull(EditText txt, Marker marker,int begin,int end){
        if(!txt.getText().toString().matches("") ){
            return Double.parseDouble(txt.getText().toString());
        } else {
            return Double.parseDouble(marker.getSnippet().substring(begin,end));
        }
    }
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageTypes.SEARCH_STATION_RESPONSE:
                    SearchOilResponse sor = (SearchOilResponse) msg.obj;
                    Log.i("CommuteHandler","ricevuto ssr");
                    for(int i = 0; i<sor.oils.size();i++){
                        oilMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(sor.oils.get(i).latitude,sor.oils.get(i).longitude))
                                .title("oilMarker"+i)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .snippet("Oil: "+sor.oils.get(i).oil+" Diesel: "+sor.oils.get(i).diesel+" Gpl: "+sor.oils.get(i).gpl)));
                    }
                    break;
                case MessageTypes.COMMUTE_REQUEST:
                    CommuteRequest creq = (CommuteRequest) msg.obj;
                    if(creq.latWork == null && creq.longWork == null && creq.latHome == null && creq.longHome == null){
                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                            Marker home, work = null;
                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                if (home == null){
                                    home = mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title("Home")
                                            .snippet("Home"));
                                } else if(work == null){
                                    work = mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title("Work")
                                            .snippet("Work"));
                                    CommuteRequest comreq = new CommuteRequest(home.getPosition().latitude,home.getPosition().longitude,work.getPosition().latitude,work.getPosition().longitude,userEmail);
                                    Message msg = Message.obtain(null, MessageTypes.COMMUTE_REQUEST);
                                    msg.obj = comreq;
                                    try {
                                        mService.send(msg);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    } else {
                        home = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(creq.latHome,creq.longHome))
                                .title("Home"));
                        work = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(creq.latWork,creq.longWork))
                                .title("Work"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(home.getPosition()));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home.getPosition(),8));
                    }
                    break;
                default:
                    Log.w(TAG, "Received an unknown task message");
                    super.handleMessage(msg);
            }
        }
    }

}


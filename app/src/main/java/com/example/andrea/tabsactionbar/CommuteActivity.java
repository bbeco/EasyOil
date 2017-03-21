package com.example.andrea.tabsactionbar;

/**
 * Created by Francesco on 01/03/2017.
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;


public class CommuteActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    ArrayList<Marker> oilMarkers;
    private boolean bound;
    private static final String TAG = "CommuteActivity";
    private Messenger mService;
    private Messenger mMessenger = new Messenger(new IncomingHandler());
    public Marker home, work;
    private SearchOilResponse sor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bound = false;
        oilMarkers = new ArrayList<>();
        setContentView(R.layout.activity_maps);
	    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
		ActionBar ab = getSupportActionBar();
	    ab.setTitle("Commute");
//      ab.setDisplayHomeAsUpEnabled(true);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.*/

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
        }
        mService = null;
        bound = false;
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
            CommuteRequest creq = new CommuteRequest(null,null,null,null,null,true);
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
                    sor = (SearchOilResponse) msg.obj;
                    Log.i("CommuteHandler","ricevuto ssr");
                    Message msgHttp = Message.obtain(null,SampleService.HTTP_REQUEST);
                    String pathReq = "origin="+home.getPosition().latitude+","+home.getPosition().longitude+"&destination="+work.getPosition().latitude+","+work.getPosition().longitude+"&key="+getString(R.string.google_maps_key);
                    msgHttp.obj = pathReq;
                    try {
                        mService.send(msgHttp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MessageTypes.COMMUTE_REQUEST:
                    CommuteRequest creq = (CommuteRequest) msg.obj;
                    if(creq.latWork == null && creq.longWork == null && creq.latHome == null && creq.longHome == null){
                        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                            @Override
                            public void onMapLongClick(LatLng latLng) {
                                if (home == null){
                                    home = mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title("Home")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                } else if(work == null){
                                    work = mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title("Work")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                    CommuteRequest comreq = new CommuteRequest(home.getPosition().latitude,home.getPosition().longitude,work.getPosition().latitude,work.getPosition().longitude,null,true);
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
                        Toast toast = Toast.makeText(getApplicationContext(),"Long click to set Home and Work positions",Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        if (home == null && work == null) {
                            home = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(creq.latHome, creq.longHome))
                                    .title("Home")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                            work = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(creq.latWork, creq.longWork))
                                    .title("Work")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        }
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(home.getPosition()));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(home.getPosition(),8));
                    }
                    break;
                case SampleService.HTTP_REQUEST:
                    ArrayList<LatLng> points;
                    points = (ArrayList<LatLng>) msg.obj;
                    mMap.addPolyline((new PolylineOptions()).addAll(points)
                            .width(15)
                            .color(Color.RED)
                    );
                    double min = 100;
                    int index = 0;
                    SharedPreferences prf = getSharedPreferences(SettingActivity.PREFERENCE_SETTING,MODE_APPEND);
                    String str = "oil";
                    for(int i = 0; i < sor.oils.size(); i++){
                        double lat1 = sor.oils.get(i).latitude;
                        double lng1 = sor.oils.get(i).longitude;
                        for (int j = 0; j < points.size(); j++){
                            double lat2 = points.get(j).latitude;
                            double lng2 = points.get(j).longitude;
                            if (measure(lat1,lng1,lat2,lng2) < 4){
                                switch(prf.getString(SettingActivity.FUEL_KEY,"1")){
                                    case "1":
                                        if (sor.oils.get(i).oil != 0 && sor.oils.get(i).oil < min){
                                            min = sor.oils.get(i).oil;
                                            index++;
                                        }
                                        break;
                                    case "2":
                                        if (sor.oils.get(i).diesel != 0 && sor.oils.get(i).diesel < min){
                                            min = sor.oils.get(i).diesel;
                                            index++;
                                        }
                                        break;
                                    case "3":
                                        if (sor.oils.get(i).gpl != 0 && sor.oils.get(i).gpl < min){
                                            min = sor.oils.get(i).gpl;
                                            index++;
                                        }
                                }
                                oilMarkers.add(mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(sor.oils.get(i).latitude,sor.oils.get(i).longitude))
                                        .title("oilMarker"+i)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                        .snippet("Oil: "+sor.oils.get(i).oil+" Diesel: "+sor.oils.get(i).diesel+" Gpl: "+sor.oils.get(i).gpl+" ")));
                                break;
                            }
                        }
                    }
                    if(index > 0) {
                        oilMarkers.get(index - 1).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        oilMarkers.get(index-1).setZIndex(2f);
                    }
                    break;

	            case SampleService.ERROR_MESSAGE:
		            Toast toast = Toast.makeText(getApplicationContext(),"Check your internet connection",Toast.LENGTH_SHORT);
		            toast.show();
	            	break;

                default:
                    Log.w(TAG, "Received an unknown task message");
                    super.handleMessage(msg);
            }
        }
    }
    private double measure(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6372.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}


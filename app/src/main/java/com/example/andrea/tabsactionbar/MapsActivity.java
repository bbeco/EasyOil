package com.example.andrea.tabsactionbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.andrea.tabsactionbar.chat.ConversationActivity;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.nearby.messages.internal.MessageType;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationListener listener;
    Marker userMarker;
    ArrayList<Marker> oilMarkers;
    LocationManager locationManager;
    LatLng userLocation;
    private boolean bound;
    private static final String TAG = "MapsActivity";
    private Messenger mService;
    private Messenger mMessenger = new Messenger(new IncomingHandler());

	/* User information */
	private String userFullName;
	private String userEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bound = false;
        oilMarkers = new ArrayList<Marker>();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        setContentView(R.layout.activity_maps);
  /*      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();

        ab.setDisplayHomeAsUpEnabled(true);*/

	     /* Retriving emailAddress passed within intent */
	    Intent mIntent = getIntent();
	    userFullName = mIntent.getStringExtra(ConversationActivity.USER_FULL_NAME_KEY);
	    userEmail = mIntent.getStringExtra(ConversationActivity.USER_EMAIL_KEY);

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
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                userMarker = mMap.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title("Marker in userLocation")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .snippet("You are here"));
                //  userMarker.setPosition(userLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 10));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
                if(locationManager.getLastKnownLocation((LocationManager.NETWORK_PROVIDER))== null) {
                    userLocation = new LatLng(0,0);
                } else {
                    userLocation = new LatLng(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude(), locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude());
                }
                userMarker = mMap.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title("Marker in userLocation")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .snippet("You are here"));
                userMarker.setPosition(userLocation);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 10));
                Intent s = new Intent(this, SampleService.class);
                Log.i("MapsActivity","arrivati qui prima del bind");
                bindService(s, mServiceConnection, Context.BIND_AUTO_CREATE);
            } else {
                Toast toast = Toast.makeText(this,"activate gps",Toast.LENGTH_SHORT);
                toast.show();
                super.finish();
            }
        }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                oilMarkers.add(mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("oilMarker"+oilMarkers.size())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))));
                getAndSendInfo(latLng.latitude,latLng.longitude,null);
            }
        });
    }
    @Override
    public void onPause (){
        Log.i("MapsActivity", "onPause");
        if (bound) {
            unregisterMaps();
            unbindService(mServiceConnection);
            bound = false;
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("MapsTry","permission accepted");
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
                    } else {
                        Toast toast = Toast.makeText(this,"activate gps",Toast.LENGTH_SHORT);
                        toast.show();
                        super.finish();
                    }
                }
            }
        }
    }
    private void registerMaps() {
        Message registration = Message.obtain(null, SampleService.CLIENT_REGISTRATION);
        registration.arg1 = SampleService.MAPS_ACTIVITY;
        registration.replyTo = mMessenger;

        try {
            mService.send(registration);
        } catch (RemoteException re) {
            /* Service has crashed, display an error */
            Log.e(TAG, "Unable to send client registration to service");
        }
        Message serverRegistration = Message.obtain(null, MessageTypes.REGISTRATION_REQUEST);
        serverRegistration.replyTo = mMessenger;
	    serverRegistration.obj = new RegistrationRequest(userEmail, userFullName, 0);
        try {
            mService.send(serverRegistration);
        } catch (RemoteException re) {
            /* Service has crashed, display an error */
            Log.e(TAG, "Unable to ask for server registration");
        }
    }

    /**
     * Unregister from the service
     */
    private void unregisterMaps() {
        Message unregistration = Message.obtain(null, SampleService.CLIENT_UNREGISTRATION);
        unregistration.arg1 = SampleService.MAPS_ACTIVITY;
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
            registerMaps();
            SearchOilRequest req = new SearchOilRequest(userLocation.latitude,userLocation.longitude);
            Message msg = Message.obtain(null,MessageTypes.SEARCH_STATION_REQUEST);
            msg.obj = req;
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

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageTypes.SEARCH_STATION_RESPONSE:
                    SearchOilResponse sor = (SearchOilResponse) msg.obj;
                    Log.i("MapsHandler","ricevuto ssr");

                    mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                        EditText txt1, txt2, txt3;
                        @Override
                        public void onInfoWindowClick(final Marker marker) {
                            getAndSendInfo(marker.getPosition().latitude,marker.getPosition().longitude,marker);
                        }
                    });

                    for(int i = 0; i<sor.oils.size();i++){
                        oilMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(sor.oils.get(i).latitude,sor.oils.get(i).longitude))
                                .title("oilMarker"+i)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .snippet("Oil: "+sor.oils.get(i).oil+" Diesel: "+sor.oils.get(i).diesel+" Gpl: "+sor.oils.get(i).gpl)));
                    }
                    break;
                default:
                    Log.w(TAG, "Received an unknown task message");
                    super.handleMessage(msg);
            }
        }
    }


    private double valueNotNull(EditText txt, Marker marker,int begin,int end){
        if(!txt.getText().toString().matches("") ){
            return Double.parseDouble(txt.getText().toString());
        } else if (marker == null) {
            return 0;
        } else {
            return Double.parseDouble(marker.getSnippet().substring(begin,end));
        }
    }
    private void getAndSendInfo (double latitude, double longitude,final Marker marker){
        final EditText txt1,txt2,txt3;
        final LinearLayout ll = (LinearLayout)findViewById(R.id.textLayout);
        ll.setVisibility(View.VISIBLE);
        txt1 = (EditText)findViewById(R.id.editText1);
        txt2 = (EditText)findViewById(R.id.editText2);
        txt3 = (EditText)findViewById(R.id.editText3);
        Button btn = (Button)findViewById(R.id.button2);
        final double lat = latitude;
        final double lon = longitude;
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                double oil,diesel,gpl;
                oil = valueNotNull(txt1, marker,5,10);
                diesel = valueNotNull(txt2,marker,19,24);
                gpl = valueNotNull(txt3,marker,30,35);
                if (oil == 0 && diesel == 0 && gpl == 0){
                    Toast toast = Toast.makeText(getApplicationContext(),"all fields are zero, creation failed",Toast.LENGTH_SHORT);
                    toast.show();
                    ll.setVisibility(View.GONE);
                    return;
                }
                ModifyRequest mreq = new ModifyRequest(lat,lon,oil,diesel,gpl);
                Message msg = Message.obtain(null, MessageTypes.MODIFY_REQUEST);
                msg.obj = mreq;
                ll.setVisibility(View.GONE);
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });
    }

}


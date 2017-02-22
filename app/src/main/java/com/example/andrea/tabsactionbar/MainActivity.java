package com.example.andrea.tabsactionbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import layout.ChatFragment;
import layout.CommuteFragment;
import layout.Nearby;

import com.example.andrea.tabsactionbar.chat.ConversationActivity;
import com.example.andrea.tabsactionbar.chat.StartConversationActivity;
import com.example.andrea.tabsactionbar.chat.messages.RegistrationRequest;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements Nearby.OnFragmentInteractionListener {
    private final static String TAG = "MainActivity";


    Button btn_map, btn_chat, btn_commute, btn_setting;
    CallbackManager callbackManager;
    LoginButton loginButton;

    private String userName, userEmail;

    private boolean bound = false;
    private Messenger mService = null;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "Bound to service");
            bound = true;
            mService = new Messenger(iBinder);
            registerClient();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
            mService = null;
        }
    };


    //Current tab listener
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);
        loginButton = (LoginButton) findViewById(R.id.login_button1);
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                Log.v("MyLogin","here");
                AccessToken t = loginResult.getAccessToken();

                Log.v("MyLogin",t.getToken());
                GraphRequest request = GraphRequest.newMeRequest(
                        t,
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {
                                try {
                                    userName = object.getString("name");
                                    userEmail = object.getString("email");
                                    Log.v("MyLogin",userName);
                                    Log.v("MyLogin",userEmail);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,link,email");
                request.setParameters(parameters);
                request.executeAsync();

                Intent s = new Intent(getApplicationContext(), SampleService.class);
                Log.i(TAG,"arrivati qui prima del bind");
                bindService(s, mServiceConnection, BIND_AUTO_CREATE);
            }

            @Override
            public void onCancel() {
                Log.v("MyLogin","cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.v("MyLogin","error");
            }
        });
        //updateWithToken(AccessToken.getCurrentAccessToken());


        btn_map = (Button) findViewById(R.id.btn_map);
        btn_chat = (Button) findViewById(R.id.btn_chat);
        btn_commute = (Button) findViewById(R.id.btn_commute);
        btn_setting = (Button) findViewById(R.id.btn_setting);
        btn_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
                    Intent i = new Intent(getApplicationContext(),MapsActivity.class);
                    startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
                    toast.show();
                }

            }
        });
        btn_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
                    Intent i = new Intent(getApplicationContext(), StartConversationActivity.class);
                    startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        btn_commute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
                 //   Intent i = new Intent(getApplicationContext(),MapsActivity.class);
                 //   startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
                 //   Intent i = new Intent(getApplicationContext(),MapsActivity.class);
                 //   startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
        //TODO
    }

    @Override
    public void onDestroy () {
        Log.i(TAG, "onDestroy");
        if (bound) {
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void registerClient() {
        Message registrationRequest = Message.obtain(null, MessageTypes.REGISTRATION_REQUEST);
        RegistrationRequest req = new RegistrationRequest(userEmail, userName, 0);
        Log.e(TAG, req.userId + req.name);
        registrationRequest.obj = req;
        try {
            mService.send(registrationRequest);
        } catch (RemoteException re) {
            /* Service has crashed. Nothing to do here */
        }
        Log.i(TAG, "registration command sent");
    }

}

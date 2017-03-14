package com.example.andrea.tabsactionbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

import layout.Nearby;

public class MainActivity extends AppCompatActivity implements Nearby.OnFragmentInteractionListener {
    private final static String TAG = "MainActivity";


    Button btn_map, btn_chat, btn_commute, btn_setting;
    CallbackManager callbackManager;
    LoginButton loginButton;

    private String userName, userEmail;

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
                Toast toast = Toast.makeText(getApplicationContext(),"logged with facebook",Toast.LENGTH_SHORT);
                toast.show();
                getFbElements();
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
        if( AccessToken.getCurrentAccessToken() == null || AccessToken.getCurrentAccessToken().isExpired()){
            Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
            toast.show();
        } else {
            getFbElements();
        }

        btn_map = (Button) findViewById(R.id.btn_map);
        btn_chat = (Button) findViewById(R.id.btn_chat);
        btn_commute = (Button) findViewById(R.id.btn_commute);
        btn_setting = (Button) findViewById(R.id.btn_setting);
        btn_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()){
                    Intent i = new Intent(getApplicationContext(),MapsActivity.class);
	                if (userName == null || userEmail == null) {
		                Log.e(TAG, "either user's name or email is null");
	                } else {
		                i.putExtra(ConversationActivity.USER_EMAIL_KEY, userEmail);
		                i.putExtra(ConversationActivity.USER_FULL_NAME_KEY, userName);
		                startActivity(i);
	                }
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
	                if (userName == null || userEmail == null) {
		                Log.e(TAG, "either user's name or email is null");
	                } else {
		                i.putExtra(ConversationActivity.USER_EMAIL_KEY, userEmail);
		                i.putExtra(ConversationActivity.USER_FULL_NAME_KEY, userName);
		                startActivity(i);
	                }
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
                       Intent i = new Intent(getApplicationContext(),CommuteActivity.class);
                        Log.i(TAG,"mail:"+userEmail);
                       i.putExtra(CommuteActivity.USER_EMAIL_KEY,userEmail);
                       startActivity(i);
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
                    Intent i = new Intent(getApplicationContext(),SettingActivity.class);
                    i.putExtra(SettingActivity.USER_EMAIL_KEY,userEmail);
                    startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void getFbElements (){
        AccessToken t = AccessToken.getCurrentAccessToken();
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
    }

	@Override
	public void onFragmentInteraction(Uri uri) {

	}
}

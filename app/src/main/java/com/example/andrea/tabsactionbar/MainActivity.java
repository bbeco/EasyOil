package com.example.andrea.tabsactionbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.andrea.tabsactionbar.chat.StartConversationActivity;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;


public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";


    Button btn_map, btn_chat, btn_commute, btn_setting;
    CallbackManager callbackManager;
    LoginButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);

	    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
	    setSupportActionBar(toolbar);
	    ActionBar ab = getSupportActionBar();
	    ab.setDisplayHomeAsUpEnabled(false);


        loginButton = (LoginButton) findViewById(R.id.login_button1);
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast toast = Toast.makeText(getApplicationContext(),"logged with facebook",Toast.LENGTH_SHORT);
                toast.show();
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
                       Intent i = new Intent(getApplicationContext(),CommuteActivity.class);
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
                    startActivity(i);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),"you need to login",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_change_server_address:
				Log.d(TAG, "Setting new server address");

				/* Showing the dialog */
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Server address");


				// Set up the input
				final EditText input = new EditText(this);
				// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
				input.setInputType(InputType.TYPE_CLASS_TEXT);
				builder.setView(input);

				// Set up the buttons
				builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String hostAddress = input.getText().toString();
						Log.d(TAG, "Saving server address: " + hostAddress);
						SharedPreferences savedServerAddress = getSharedPreferences(SampleService.PREF_FILE_NAME, MODE_PRIVATE);
						SharedPreferences.Editor editor = savedServerAddress.edit();
						editor.putString(SampleService.HOST_IP_ADDRESS_KEY, hostAddress);
						editor.apply();
					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

				builder.show();

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}

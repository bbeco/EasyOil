package com.example.andrea.tabsactionbar;

import android.content.Intent;
import android.net.Uri;
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
import android.widget.TextView;

import layout.ChatFragment;
import layout.CommuteFragment;
import layout.Nearby;

public class MainActivity extends AppCompatActivity implements Nearby.OnFragmentInteractionListener {
    private final static String TAG = "MainActivity";


    Button btn_map, btn_chat, btn_commute, btn_setting;
    //Current tab listener
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_map = (Button) findViewById(R.id.btn_map);
        btn_chat = (Button) findViewById(R.id.btn_chat);
        btn_commute = (Button) findViewById(R.id.btn_commute);
        btn_setting = (Button) findViewById(R.id.btn_setting);
        btn_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),MapsActivity.class);
                startActivity(i);
            }
        });
        btn_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //    Intent i = new Intent(getApplicationContext(),MapsActivity.class);
            //    startActivity(i);
            }
        });
        btn_commute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //    Intent i = new Intent(getApplicationContext(),MapsActivity.class);
            //    startActivity(i);
            }
        });
        btn_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //    Intent i = new Intent(getApplicationContext(),MapsActivity.class);
            //    startActivity(i);
            }
        });
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
        //TODO
    }
}

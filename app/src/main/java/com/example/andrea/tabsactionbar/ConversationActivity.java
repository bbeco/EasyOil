package com.example.andrea.tabsactionbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        String array[] = {"ciao", "mondo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                R.layout.list_elem_mine, R.id.message_text, array);
        ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);
    }
}

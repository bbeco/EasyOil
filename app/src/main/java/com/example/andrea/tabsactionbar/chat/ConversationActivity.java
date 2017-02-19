package com.example.andrea.tabsactionbar.chat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.example.andrea.tabsactionbar.R;
import com.example.andrea.tabsactionbar.chat.messages.ChatMessage;

import java.util.ArrayList;

public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = "ConversationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ListView l = (ListView)findViewById(R.id.chat_list);
        l.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        l.setStackFromBottom(true);

        //String array[] = {"ciao", "mondo"};
        ArrayList<ChatMessage> messageList = new ArrayList<ChatMessage>(30);
        for (int i = 0; i < 15; i++) {
            messageList.add(new ChatMessage("Andrea", "Francesco", "ciao " + i, i));
        }
        for (int i = 0; i < 15 ; i++) {
            messageList.add(new ChatMessage("Francesco", "Andrea", "ciao " + (i + 15), i + 15));
        }
        /*ArrayAdapter<ChatMessage> adapter = new ArrayAdapter<ChatMessage>(getApplicationContext(),
                R.layout.list_elem_mine, R.id.message_text, messageList);*/
        ChatListAdapter adapter = new ChatListAdapter(getApplicationContext(), messageList, "Andrea");
        //Log.d(TAG, Integer.toString(adapter.getCount()));
        ListView listView = (ListView) findViewById(R.id.chat_list);
        listView.setAdapter(adapter);
    }
}

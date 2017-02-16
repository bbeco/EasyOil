package com.example.andrea.tabsactionbar;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by andrea on 2/15/17.
 */

public class ChatListAdapter extends BaseAdapter {

    static final int MY_MSG_TYPE = 0;
    static final int OTHER_MESSAGE_TYPE = 1;

    ArrayList<ChatMessage> mList;
    final String myId;
    protected LayoutInflater mInflater;

    /**
     *
     * @param context
     * @param array the array list of ChatMessage elements.
     * @param myId My id. It is used to select the messages sent by me from the incoming ones.
     */
    ChatListAdapter(Context context, ArrayList<ChatMessage> array, String myId) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mList = array;
        this.myId = myId;
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (mList.get(position).sender.equalsIgnoreCase(myId)) {
            return MY_MSG_TYPE;
        } else {
            return OTHER_MESSAGE_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int type = getItemViewType(position);
        //Log.d("CustomAdapter", "Item " + position + " type : " + type);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (type) {
                case MY_MSG_TYPE:
                    convertView = mInflater.inflate(R.layout.list_elem_mine, parent, false);
                    holder.text = (TextView) convertView.findViewById(R.id.message_text);
                    break;

                case OTHER_MESSAGE_TYPE:
                    convertView = mInflater.inflate(R.layout.list_elem_other, parent, false);
                    holder.text = (TextView) convertView.findViewById(R.id.message_text);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.text.setText(mList.get(position).payload);
        //Log.d("CustomAdapter", mList.get(position).payload);
        return convertView;
    }

    public static class ViewHolder {
        public TextView text;
    }
}

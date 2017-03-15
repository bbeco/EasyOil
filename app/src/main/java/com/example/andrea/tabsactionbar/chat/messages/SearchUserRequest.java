package com.example.andrea.tabsactionbar.chat.messages;

import com.example.andrea.tabsactionbar.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andrea on 2/17/17.
 */

public class SearchUserRequest {
    private final int type = MessageTypes.SEARCH_USER_REQUEST;
    public String user;
	public String sender;
    public long ts = 0;

    public SearchUserRequest(String sender, String user, long ts) {
	    this.sender = sender;
        this.user = user;
        this.ts = ts;
    }

    public SearchUserRequest (String s) throws JSONException {
        JSONObject obj = new JSONObject(s);
        this.user = obj.getString("user");
	    this.sender = obj.getString("sender");
        this.ts = Long.parseLong(obj.getString("ts"),10);
    }

    public String toJSONString() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", Integer.toString(this.type));
        obj.put("user", this.user);
	    obj.put("sender", this.sender);
        obj.put("ts", Long.toString(this.ts));
        return obj.toString();
    }

    public String toString() {
        return "{ type: \""  + type + "\", user: \"" + user + "\", ts: \"" + ts + "\" }";
    }
}

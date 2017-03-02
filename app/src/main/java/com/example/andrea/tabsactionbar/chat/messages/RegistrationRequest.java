package com.example.andrea.tabsactionbar.chat.messages;

import com.example.andrea.tabsactionbar.MessageTypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RegistrationRequest {
	private final int type = MessageTypes.REGISTRATION_REQUEST;
	
	public String userId, name;
	public long ts;
	
	public RegistrationRequest(String id, String fullname, long ts) {
		userId = id;
		name = fullname;
		this.ts = ts;
	}
	
	public RegistrationRequest(String s) throws JSONException {
		JSONObject obj = new JSONObject(s);
		userId = obj.getString("userId");
		name = obj.getString("name");
		ts = Long.parseLong(obj.getString("ts"), 10);
	}

	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(type));
		obj.put("userId", userId);
		obj.put("name", name);
		obj.put("ts", Long.toString(ts));
		return obj.toString();
	}
}

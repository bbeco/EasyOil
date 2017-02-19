package com.example.andrea.tabsactionbar.chat;

import com.example.andrea.tabsactionbar.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchUserRequest {
	
	private final int type = MessageTypes.SEARCH_USER_REQUEST;
	public String name;
	
	public SearchUserRequest (){};
	
	public SearchUserRequest (String s) throws JSONException {
		JSONObject obj = new JSONObject(s);
		this.name = obj.getString("name");
	}
	
	public String toJSONString() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("type", Integer.toString(this.type));
		obj.put("name", this.name);
		return obj.toString();
	}

}

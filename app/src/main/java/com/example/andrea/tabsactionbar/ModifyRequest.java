package com.example.andrea.tabsactionbar;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Francesco on 14/02/2017.
 */

public class ModifyRequest {

    private final int type = MessageTypes.MODIFY_REQUEST;
    public double latitude, longitude, oil, diesel, gpl;
	public int id;

    public ModifyRequest (int id, double latitude, double longitude, double oil, double diesel, double gpl) {
	    this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.oil = oil;
        this.diesel = diesel;
        this.gpl = gpl;
    }
    public ModifyRequest (String s) throws JSONException {
        JSONObject obj = new JSONObject(s);
	    this.id = Integer.parseInt(obj.getString("id"));
        this.latitude = Double.parseDouble(obj.getString("latitude"));
        this.longitude = Double.parseDouble(obj.getString("longitude"));
        this.oil = Double.parseDouble(obj.getString("oil"));
        this.diesel = Double.parseDouble(obj.getString("diesel"));
        this.gpl = Double.parseDouble(obj.getString("gpl"));
    }
    public String toJSONString() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", Integer.toString(type));
	    obj.put("id", Integer.toString(id));
        obj.put("latitude", Double.toString(latitude));
        obj.put("longitude", Double.toString(longitude));
        obj.put("oil", Double.toString(oil));
        obj.put("diesel", Double.toString(diesel));
        obj.put("gpl", Double.toString(gpl));
        return obj.toString();
    }
}
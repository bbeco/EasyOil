package com.example.andrea.tabsactionbar;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Francesco on 01/03/2017.
 */

public class CommuteRequest {
    private final int type = MessageTypes.COMMUTE_REQUEST;
    public Double latHome, longHome, latWork,longWork;
    String email;
    boolean valid;

    public CommuteRequest (Double latHome, Double longHome, Double latWork, Double longWork, String email,boolean valid) {
        this.latHome = latHome;
        this.longHome = longHome;
        this.latWork = latWork;
        this.longWork = longWork;
        this.email = email;
        this.valid = valid;
    }
    public CommuteRequest (String s) throws JSONException {
        JSONObject obj = new JSONObject(s);
        this.latHome = obj.getString("latHome").matches("")?null:Double.parseDouble(obj.getString("latHome"));
        this.longHome = obj.getString("longHome").matches("")?null:Double.parseDouble(obj.getString("longHome"));
        this.latWork = obj.getString("latWork").matches("")?null:Double.parseDouble(obj.getString("latWork"));
        this.longWork = obj.getString("longWork").matches("")?null:Double.parseDouble(obj.getString("longWork"));
        this.email = obj.getString("email");
        this.valid = obj.getBoolean("valid");
    }

    public String toJSONString() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", Integer.toString(type));
        obj.put("latHome",latHome==null?"":Double.toString(latHome));
        obj.put("longHome",longHome==null?"":Double.toString(longHome));
        obj.put("latWork",latWork==null?"":Double.toString(latWork));
        obj.put("longWork",longWork==null?"":Double.toString(longWork));
        obj.put("email",email);
        obj.put("valid",valid);
        return obj.toString();
    }
}

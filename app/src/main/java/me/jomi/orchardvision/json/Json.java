package me.jomi.orchardvision.json;

import org.json.JSONObject;

public class Json extends AbstractJson<String, JSONObject> {
    public Json(JSONObject json) {
        super(json);
    }
    public Json(String json) {
        this((JSONObject) get(json, JSONObject::new));
    }


    @Override
    public int length() {
        return json.length();
    }

    @Override public int       getInt    (String key) { return get(key, json::getInt); }
    @Override public long      getLong   (String key) { return get(key, json::getLong); }
    @Override public double    getDouble (String key) { return get(key, json::getDouble); }
    @Override public boolean   getBoolean(String key) { return get(key, json::getBoolean); }
    @Override public String    getString (String key) { return get(key, json::getString); }
    @Override public Json      getJson   (String key) { return new Json     (get(key, json::getJSONObject)); }
    @Override public JsonArray getArray  (String key) { return new JsonArray(get(key, json::getJSONArray)); }
}

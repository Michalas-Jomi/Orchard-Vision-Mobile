package me.jomi.orchardvision.json;

import org.json.JSONArray;

public class JsonArray extends AbstractJson<Integer, JSONArray> {
    public JsonArray(JSONArray json) {
        super(json);
    }
    public JsonArray(String json) {
        this((JSONArray) get(json, JSONArray::new));
    }


    @Override
    public int length() {
        return json.length();
    }

    @Override public int       getInt    (Integer index) { return get(index, json::getInt); }
    @Override public long      getLong   (Integer index) { return get(index, json::getLong); }
    @Override public double    getDouble (Integer index) { return get(index, json::getDouble); }
    @Override public boolean   getBoolean(Integer index) { return get(index, json::getBoolean); }
    @Override public String    getString (Integer index) { return get(index, json::getString); }
    @Override public Json      getJson   (Integer index) { return new Json     (get(index, json::getJSONObject)); }
    @Override public JsonArray getArray  (Integer index) { return new JsonArray(get(index, json::getJSONArray)); }
}

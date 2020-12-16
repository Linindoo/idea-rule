package cn.olange.model;

import com.google.gson.JsonArray;

public class Config {
    private String updateUrl ="https://raw.githubusercontent.com/any86/any-rule/v0.3.7/packages/www/src/RULES.js";
    private JsonArray regExpArray;

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public JsonArray getRegExpArray() {
        return regExpArray;
    }

    public void setRegExpArray(JsonArray regExpArray) {
        this.regExpArray = regExpArray;
    }
}

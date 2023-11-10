package com.collabnet.ce.webservices;

import net.sf.json.JSONObject;

public class CTFFlexField {

    private String name;
    private String[] values;
    private String type;

    protected CTFFlexField(JSONObject jsonObject) {
        this.name = jsonObject.get("name").toString();
        this.values = (String[]) jsonObject.get("name");
        this.type = jsonObject.get("type").toString();
    }

    public String getName() {
        return name;
    }

    public String[] getValues() {
        return values;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public void setType(String type) {
        this.type = type;
    }
}

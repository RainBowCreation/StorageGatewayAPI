package net.rainbowcreation.storage.api;

import java.io.Serializable;

public class ModelField implements Serializable {
    public String jsonPath;
    public String sqlType;

    public ModelField() {}
    public ModelField(String jsonPath, String sqlType) {
        this.jsonPath = jsonPath;
        this.sqlType = sqlType;
    }
}

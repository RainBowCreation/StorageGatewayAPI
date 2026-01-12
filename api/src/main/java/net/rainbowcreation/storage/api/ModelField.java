package net.rainbowcreation.storage.api;

import java.io.Serializable;

public class ModelField implements Serializable {
    private static final long serialVersionUID = 1L;

    public String jsonPath;
    public String sqlType;
    public boolean isIndexed;

    public ModelField() {}

    public ModelField(String jsonPath, String sqlType, boolean isIndexed) {
        this.jsonPath = jsonPath;
        this.sqlType = sqlType;
        this.isIndexed = isIndexed;
    }

    public ModelField(String jsonPath, String sqlType) {
        this(jsonPath, sqlType, false);
    }
}
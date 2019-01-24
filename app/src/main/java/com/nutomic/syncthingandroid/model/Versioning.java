package com.nutomic.syncthingandroid.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Versioning implements Serializable {
    public String type;
    public Map<String, String> params = new HashMap<>();
}

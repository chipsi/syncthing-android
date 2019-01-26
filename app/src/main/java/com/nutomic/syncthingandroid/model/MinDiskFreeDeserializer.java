package com.nutomic.syncthingandroid.model;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class MinDiskFreeDeserializer implements JsonDeserializer<MinDiskFree> {

    private static final String TAG = "MinDiskFreeDeserializer";

    @Override
    public MinDiskFree deserialize(final JsonElement json, final Type typeOfT,
            final JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        MinDiskFree minDiskFree = new MinDiskFree();
        minDiskFree.unit = getFieldAsString(jsonObject, "unit");
        minDiskFree.value = getFieldAsFloat(jsonObject, "value");
        Log.v(TAG, "TEST unit " + minDiskFree.unit);
        Log.v(TAG, "TEST value " + minDiskFree.value);
        return minDiskFree;
    }

    private String getFieldAsString(JsonObject jsonObject, String serializedName) {
        if (jsonObject.get(serializedName) == null) {
            Log.v(TAG, "getFieldAsString: " + serializedName + " == null");
            return null;
        }
        return jsonObject.get(serializedName).getAsString();
    }

    private float getFieldAsFloat(JsonObject jsonObject, String serializedName) {
        if (jsonObject.get(serializedName) == null) {
            Log.v(TAG, "getFieldAsFloat: " + serializedName + " == null");
            return 0;
        }
        return jsonObject.get(serializedName).getAsFloat();
    }
}

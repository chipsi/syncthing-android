package com.nutomic.syncthingandroid.model;

import android.util.Log;

import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class MinDiskFreeSerializer implements JsonSerializer<MinDiskFree> {

    private static final String TAG = "MinDiskFreeSerializer";

    @Override
    public JsonElement serialize(final MinDiskFree minDiskFree, Type typeOfSrc,
            final JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("unit", minDiskFree.unit);
        jsonObject.addProperty("value", minDiskFree.value);
        Log.v(TAG, "TEST SERIALIZE " + minDiskFree.unit);
        Log.v(TAG, "TEST SERIALIZE " + minDiskFree.value);
        return jsonObject;
    }
}

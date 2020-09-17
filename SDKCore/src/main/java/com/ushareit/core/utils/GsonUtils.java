package com.ushareit.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.ushareit.core.Logger;

import android.text.TextUtils;

public class GsonUtils {

    private static final String TAG = "GsonUtils";

    public static <T> T createModel(JSONObject jsonObj, Class<T> cls) {
        if (jsonObj == null || TextUtils.isEmpty(jsonObj.toString()))
            return null;

        try {
            Gson gson;
            gson = new Gson();
            T model = gson.fromJson(jsonObj.toString(), cls);
            return model;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d(TAG, "createModel error : " + e.getLocalizedMessage());
            return null;
        }
    }

    public static <T> T createModel(String jsonStr, Class<T> cls) {
        if (TextUtils.isEmpty(jsonStr))
            return null;

        try {
            Gson gson;
            gson = new Gson();
            T model = gson.fromJson(jsonStr, cls);
            return model;
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d(TAG, "createModel error : " + e.getLocalizedMessage());
            return null;
        }
    }

    public static <T> List<T> createModels(JSONArray jsonArr, Class<T> cls) {
        List<T> result = new ArrayList<>();
        if(jsonArr == null)
            return result;

        try {
            for (int i = 0; i < jsonArr.length(); i++) {
                try {
                    if (jsonArr.getString(i) == null){
                        continue;
                    }
                    T temp = createModel(jsonArr.getString(i), cls);
                    if (temp != null) {
                        result.add(temp);
                    }
                } catch (Exception e) {
                    Logger.d(TAG, "createModels error : " + e.getLocalizedMessage());
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, "createModel error : " + e.getLocalizedMessage());
        }
        return result;
    }

    public static <T> List<T> createModels(String jsonStr, Class<T> cls) {
        if (TextUtils.isEmpty(jsonStr))
            return new ArrayList<>();
        try {
            return createModels(new JSONArray(jsonStr), cls);
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static <T> String models2Json(List<T> models) {
        try {
            Gson gson = new Gson();
            return gson.toJson(models);
        } catch (Exception e) {
            return "";
        }
    }

}

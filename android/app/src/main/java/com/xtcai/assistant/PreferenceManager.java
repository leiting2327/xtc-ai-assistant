package com.xtcai.assistant;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class PreferenceManager {
    private static final String PREF_NAME = "xtc_ai_config";
    private static final String KEY_SETTINGS = "settings";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getSettingsJson() {
        String defaultSettings = "{" +
                "\"primaryApi\":{\"name\":\"MNN\",\"baseUrl\":\"http://127.0.0.1:8080/v1\",\"apiKey\":\"\",\"model\":\"qwen3.5-2b-mnn\"}," +
                "\"backupApi\":{\"name\":\"DeepSeek\",\"baseUrl\":\"https://api.openai.com/v1\",\"apiKey\":\"__DEEPSEEK_API_KEY__\",\"model\":\"nvidia/nemotron-3.5-lightning:free\"}," +
                "\"contactName\":\"宝贝\"," +
                "\"autoReply\":true," +
                "\"systemPrompt\":\"你是一个友好的AI助手，帮助回答手表佩戴者的问题。回答要简洁、适合儿童理解。\"" +
                "}";
        return prefs.getString(KEY_SETTINGS, defaultSettings);
    }

    public void saveSettingsJson(String json) {
        prefs.edit().putString(KEY_SETTINGS, json).apply();
    }

    public JSONObject getSettings() {
        try {
            return new JSONObject(getSettingsJson());
        } catch (JSONException e) {
            try {
                return new JSONObject(getSettingsJson());
            } catch (JSONException ex) {
                return new JSONObject();
            }
        }
    }
}

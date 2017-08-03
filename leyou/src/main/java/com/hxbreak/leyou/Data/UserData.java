package com.hxbreak.leyou.Data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by HxBreak on 2017/8/3.
 */

public class UserData {
    private Context context;
    private String userKey = "";
    private float userMoney = 0;

    public UserData(Context context) {
        this.context = context;
        initData();
    }
    private void initData(){
        SharedPreferences sharedPreferences = context.getSharedPreferences("userdata", Context.MODE_PRIVATE);
        userKey = sharedPreferences.getString("userKey", "");
        userMoney = sharedPreferences.getFloat("userMoney", 0);
    }
    private void saveData(){
        SharedPreferences sharedPreferences = context.getSharedPreferences("userdata", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userKey", userKey);
        editor.putFloat("userMoney", userMoney);
        editor.commit();
    }

    public String getUserKey() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("userdata", Context.MODE_PRIVATE);
        return sharedPreferences.getString("userKey", "");
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
        saveData();
    }

    public float getUserMoney() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("userdata", Context.MODE_PRIVATE);
        return sharedPreferences.getFloat("userMoney", 0);
    }

    public void setUserMoney(float userMoney) {
        this.userMoney = userMoney;
        saveData();
    }
}

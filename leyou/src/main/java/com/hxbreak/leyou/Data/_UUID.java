package com.hxbreak.leyou.Data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by HxBreak on 2017/7/20.
 */

public class _UUID {
    private Context context;
    private String UUID;
    private String CUID;

    public _UUID(Context context) {
        this.context = context;
    }

    public String getUUID(){
        SharedPreferences sharedPreferences = context.getSharedPreferences("UUID", 0);
        if((UUID = sharedPreferences.getString("_uuid", "")).equals("")){
            UUID = java.util.UUID.randomUUID().toString();
            sharedPreferences.edit().putString("_uuid", UUID).commit();
        }
        return UUID;
    }
    public String remix(String imei, String androidid){
        SharedPreferences sharedPreferences = context.getSharedPreferences("UUID", 0);
        if((CUID = sharedPreferences.getString("_cuid", "")).equals("")){
            CUID = CreateMD5.getMd5(imei + androidid + getUUID());
            sharedPreferences.edit().putString("_cuid", CUID).commit();
        }
        return CUID;
    }
}

package com.hxbreak.leyou.Data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

/**
 * Created by HxBreak on 2017/8/3.
 */

public class UserData {
    private Context context;
    private String userKey = "";
    private String userIp = "";
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
        editor.putString("userIp", userIp);
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
    public String getUserIp() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("userdata", Context.MODE_PRIVATE);
        String ip = sharedPreferences.getString("userIp", "");
        if (ip.equals("")){
            ip = getRandomIp();
            setUserIp(ip);
        }
        return ip;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
        saveData();
    }
    public static String getRandomIp(){
        //ip范围
        int[][] range = {{607649792,608174079},//36.56.0.0-36.63.255.255
                {1038614528,1039007743},//61.232.0.0-61.237.255.255
                {1783627776,1784676351},//106.80.0.0-106.95.255.255
                {2035023872,2035154943},//121.76.0.0-121.77.255.255
                {2078801920,2079064063},//123.232.0.0-123.235.255.255
                {-1950089216,-1948778497},//139.196.0.0-139.215.255.255
                {-1425539072,-1425014785},//171.8.0.0-171.15.255.255
                {-1236271104,-1235419137},//182.80.0.0-182.92.255.255
                {-770113536,-768606209},//210.25.0.0-210.47.255.255
                {-569376768,-564133889}, //222.16.0.0-222.95.255.255
        };
        Random rdint = new Random();
        int index = rdint.nextInt(10);
        String ip = num2ip(range[index][0]+new Random().nextInt(range[index][1]-range[index][0]));
        return ip;
    }
    /*
         * 将十进制转换成ip地址
         */
    public static String num2ip(int ip) {
        int [] b=new int[4] ;
        String x = "";

        b[0] = (int)((ip >> 24) & 0xff);
        b[1] = (int)((ip >> 16) & 0xff);
        b[2] = (int)((ip >> 8) & 0xff);
        b[3] = (int)(ip & 0xff);
        x=Integer.toString(b[0])+"."+Integer.toString(b[1])+"."+Integer.toString(b[2])+"."+Integer.toString(b[3]);

        return x;
    }
}

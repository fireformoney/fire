package com.hxbreak.utils;

import com.hxbreak.constant.Config;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;

/**
 * Created by HxBreak on 2017/9/5.
 */

public class PhoneInfoCreator {
    private static final String pi_from_client = "server";
    private static final String pi_channel_id = "20019a";
    private static final String pi_app_id = "b1019a";
    private static String pi_page = "0";
    private static String pi_size = "20";
    private String none = "0000000000000000";
    public String imei = none, androidid = none, imsi = none, mcc = "310", mnc = "260", la = "-1", ci = "-1";
    public String macAddress = "";
    public String ovr = "", os_level = "", svr = "4640014", net_type = "";
    public String dpi = "";

    public HashMap makeDownloadListHash(){
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("from_client", pi_from_client);
        hashMap.put("channel_id", pi_channel_id);
        hashMap.put("app_id", pi_app_id);
        hashMap.put("page", pi_page);
        hashMap.put("size", pi_size);
        hashMap.put("timestamp", String.valueOf((int)(System.currentTimeMillis() / 1000)));
        return hashMap;
    }
    public HashMap updatetime(HashMap<String, String> hashMap){
        hashMap.put("timestamp", String.valueOf((int)(System.currentTimeMillis() / 1000)));
        return hashMap;
    }
    public List<Map.Entry<String, String>> sort(HashMap<String, String> hashMap){
        List<Map.Entry<String, String>> hashMap2 = new ArrayList<Map.Entry<String, String>>(hashMap.entrySet());
        Collections.sort(hashMap2, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return hashMap2;
    }
    public HttpUrl.Builder buildUrl(Iterator iterator, HttpUrl.Builder builder){
        while(iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>)iterator.next();
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        return builder;
    }
    public String buildString(Iterator iterator){
        StringBuffer stringBuffer = new StringBuffer();
        while(iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>)iterator.next();
            stringBuffer.append(entry.getKey());
            stringBuffer.append("=");
            stringBuffer.append(entry.getValue());
            if (iterator.hasNext()) {
                stringBuffer.append("&");
            }
        }
        return stringBuffer.toString();
    }
    public String BRAND = "", MODEL = "";
    public String pt_x = "", pt_y = "";
    public HashMap makeReportHash(String uuid, String ip){
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("cuid", CreateMD5.getMd5(imei + androidid + uuid).toUpperCase());
        hashMap.put("ovr", ovr);
        hashMap.put("os_level", os_level);
        hashMap.put("device", URLEncoder.encode(BRAND.replace(" ", "") + "_" + MODEL.replace(" ", "")));
        hashMap.put("channel_id", pi_channel_id);
        hashMap.put("app_id", pi_app_id);
        hashMap.put("svr", svr);
        hashMap.put("net_type", net_type);
        hashMap.put("resolution", String.format("%s_%s", pt_x, pt_y));
        hashMap.put("info_ma", macAddress);
        hashMap.put("info_ms", imsi);
        hashMap.put("client_id", imei);
        hashMap.put("dpi", dpi);
        hashMap.put("client_ip", ip);
        hashMap.put("mcc", mcc);
        hashMap.put("mno", mnc);
        hashMap.put("info_la", la);
        hashMap.put("info_ci", ci);
        hashMap.put("os_id", androidid);
        hashMap.put("bssid", macAddress);
        hashMap.put("nonce", String.valueOf((int)(System.currentTimeMillis() / 1000)));
        hashMap.put("pkg", "com.hxbreak.leyou");

        return hashMap;
    }
    public String createReportSign(HashMap<String, String> hashMap, String data){
        HashMap<String, String> signHashMap = new HashMap<>();
        signHashMap.put("channel_id", hashMap.get("channel_id"));
        signHashMap.put("app_id", hashMap.get("app_id"));
        signHashMap.put("client_id", hashMap.get("client_id"));
        signHashMap.put("client_ip", hashMap.get("client_ip"));
        signHashMap.put("device", hashMap.get("device"));
        signHashMap.put("net_type", hashMap.get("net_type"));
        signHashMap.put("nonce", hashMap.get("nonce"));
        signHashMap.put("os_level", hashMap.get("os_level"));
        signHashMap.put("ovr", hashMap.get("ovr"));
        signHashMap.put("pkg", hashMap.get("pkg"));
        signHashMap.put("svr", hashMap.get("svr"));
        signHashMap.put("reportData", URLEncoder.encode(data));
        List<Map.Entry<String, String>> sortSign = this.sort(signHashMap);
        String str = this.buildString(sortSign.iterator()) +  hashMap.get("client_id") + hashMap.get("pkg") + Config.APP_SECRET;
        return CreateMD5.getMd5(this.buildString(sortSign.iterator()) + hashMap.get("client_id") + hashMap.get("pkg") + Config.APP_SECRET).toLowerCase();
    }
}

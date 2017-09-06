package com.hxbreak.utils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hxbreak.Bean.AppListResult;
import com.hxbreak.Bean.Result;
import com.hxbreak.constant.Config;
import com.hxbreak.listener.OnListLoadFinished;
import com.hxbreak.listener.OnReportFinished;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by HxBreak on 2017/9/5.
 */

public class TaskDispatcher {
    private OkHttpClient okHttpClient = null;
    private PhoneInfoCreator phoneInfoCreator;

    public TaskDispatcher(){
        this.okHttpClient = new OkHttpClient();
        phoneInfoCreator = new PhoneInfoCreator();
    }
    /**
     * 获取应用列表
     * @param OnListLoadFinished  回调事件
     */
    public void FetchList(final OnListLoadFinished OnListLoadFinished){
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(Config.SERVERAPPLIST).newBuilder();
        HashMap hashMap = phoneInfoCreator.makeDownloadListHash();
        List<Map.Entry<String, String>> map = phoneInfoCreator.sort(hashMap);
        String param = phoneInfoCreator.buildString(map.iterator()) + Config.APP_SECRET;
        String encrypt = CreateMD5.getMd5(param);
        phoneInfoCreator.buildUrl(map.iterator(), httpUrlBuilder).addQueryParameter("sign", encrypt);
        okHttpClient.newCall(new Request.Builder().url(httpUrlBuilder.build()).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                OnListLoadFinished.onFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    Gson gson = new Gson();
                    try{
                        String temp = response.body().string();
                        AppListResult.Content appListResult = gson.fromJson(temp, AppListResult.Content.class);
                        OnListLoadFinished.onSuccess(appListResult);
                    }catch (Exception e){
                        OnListLoadFinished.onFailed();
                    }
                }else{
                    OnListLoadFinished.onFailed();
                }
            }
        });
    }
    public void DownloadReport(String packagename, String uuid, String ip, final OnReportFinished onReportFinished){
        Gson gson = new Gson();
        String reportData = gson.toJson(new pack(packagename)).toLowerCase();
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(Config.SERVERREPORT).newBuilder();
        HashMap<String, String> hashMap = phoneInfoCreator.makeReportHash(uuid, ip);
        List<Map.Entry<String, String>> hashMaps = phoneInfoCreator.sort(hashMap);
        httpUrlBuilder = phoneInfoCreator.buildUrl(hashMaps.iterator(), httpUrlBuilder);
        String sign = phoneInfoCreator.createReportSign(hashMap, gson.toJson(new pack(packagename)).toLowerCase());
        RequestBody requestBody = RequestBody.create(Config.JSON, gson.toJson(new data(packagename, sign)));
        Request request = new Request.Builder().url(httpUrlBuilder.build()).post(requestBody).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onReportFinished.onFailed(-1);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    String str = response.body().string();
                    Gson gson = new Gson();
                    Result.InnerResult result = gson.fromJson(str, Result.InnerResult.class);
                    if(result.result == 0){
                        Result result1 = gson.fromJson(str, Result.class);
                        onReportFinished.onSuccess(result1.content.extraData);
                    }else{
                        onReportFinished.onFailed(result.result);
                    }
                }else{
                    onReportFinished.onFailed(-2);
                }
            }
        });
    }

    public PhoneInfoCreator getPhoneInfoCreator() {
        return phoneInfoCreator;
    }

    public void setPhoneInfoCreator(PhoneInfoCreator phoneInfoCreator) {
        this.phoneInfoCreator = phoneInfoCreator;
    }

    private class pack{
        @SerializedName("package")
        public String Package;
        public pack(String p){
            this.Package = p;
        }
    }
    private class data{
        /**
         * {"reportData":{"package":"com.lehai.ui"},"sign":"95042303416b6381598753ee8944fcdf","reportType":0}
         */
        public inner reportData;
        public String sign;
        public int reportType = 0;
        private class inner{
            @SerializedName("package")
            public String Package;
            public inner(String i){
                this.Package = i;
            }
        }

        public data(String aPackage, String sign) {
            this.reportData = new inner(aPackage);
            this.sign = sign;
        }
    }
}

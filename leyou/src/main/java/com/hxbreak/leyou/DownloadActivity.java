package com.hxbreak.leyou;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Settings;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hxbreak.leyou.Adapter.AppListAdapter;
import com.hxbreak.leyou.Bean.AppListResult;
import com.hxbreak.leyou.Data.CreateMD5;
import com.hxbreak.leyou.Data._UUID;
import com.hxbreak.leyou.Task.DownloadTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DownloadActivity extends BaseActivity implements Callback, AppListAdapter.OnItemClick, DownloadTask.DownloadListener{

    private static final String APP_SECRET = "testappsecret";
    private final String applisturl = "http://package.mhacn.net/api/v2/apps/list";
    private final String appdownloadreport = "http://package.mhacn.net/api/delay/report/download/start";
    private final String CHANNEL_ID = "20020a";
    private final String APP_ID = "b1020a";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private OkHttpClient okHttpClient;
    private AppListResult appListResult;
    private AppListAdapter appListAdapter;
    private BroadcastReceiver broadcastReceiver;

    private HashMap<Integer, DownloadTask> hashMap = new HashMap<>();

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    initList(); break;
                case 2:
                    Toast.makeText(DownloadActivity.this, "列表加载失败", Toast.LENGTH_LONG).show();break;
                case 100:
                    appListAdapter.updateItemDownloadProgess(msg.arg1, msg.arg2);break;
                case 101:
                    Toast.makeText(DownloadActivity.this, "下载任务出错", Toast.LENGTH_LONG).show();break;
                case 200:
                    appListAdapter.listAdd(msg.getData().getString("package", ""));break;
                case 201:
                    appListAdapter.listRemove(msg.getData().getString("package", ""));break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        okHttpClient = new OkHttpClient();

        toolbar = (Toolbar) findViewById(R.id.hb_toolbar);
        recyclerView = (RecyclerView)findViewById(R.id.hb_app_recylerview);
        progressBar = (ProgressBar)findViewById(R.id.hx_progressBar);
        initToolbar();
        dowloadAppList();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        broadcastReceiver = new PackageListener(handler);
        registerReceiver(broadcastReceiver, intentFilter);
    }
    private void initToolbar(){
        setSupportActionBar(toolbar);
        toolbar.setTitle("游戏试玩");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    public List<String> getInstalledAppList(){
        List<String> list = new ArrayList<>();
        for (PackageInfo pinfo : getPackageManager().getInstalledPackages(0)){
            list.add(pinfo.packageName);
        }
        return list;
    }

    @Override
    protected void onDestroy() {
//        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    /**
     * 展示列表内容
     */
    private void initList(){
        progressBar.setVisibility(View.GONE);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(OrientationHelper.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        appListAdapter = new AppListAdapter(this, appListResult.content.list, this, new File(getFilesDir().toString()).listFiles(), getInstalledAppList());
        recyclerView.setAdapter(appListAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 下载app列表
     */
    private void dowloadAppList(){
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("from_client", "server");
        hashMap.put("channel_id", CHANNEL_ID);
        hashMap.put("app_id", APP_ID);
        hashMap.put("pn", "1");
        hashMap.put("rn", "10");
        hashMap.put("timestamp", String.valueOf((int)(System.currentTimeMillis() / 1000)));
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(applisturl).newBuilder();
//        Iterator iterator = hashMap.entrySet().iterator();
        StringBuilder sign = new StringBuilder();
        //排序
        List<Map.Entry<String, String>> hashMap2 = new ArrayList<Map.Entry<String, String>>(hashMap.entrySet());
        Collections.sort(hashMap2, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        Iterator iterator = hashMap2.iterator();
        while(iterator.hasNext()){
            Map.Entry<String, String> entry = (Map.Entry<String, String>)iterator.next();
            httpUrlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            sign.append(entry.getKey());
            sign.append("=");
            sign.append(entry.getValue());
            if (iterator.hasNext()) {
                sign.append("&");
            }
        }

        sign.append(APP_SECRET);
        String final_sign = CreateMD5.getMd5(sign.toString().toLowerCase());
        httpUrlBuilder.addQueryParameter("sign", final_sign);

        Request request = new Request.Builder().url(httpUrlBuilder.build()).build();
        okHttpClient.newCall(request).enqueue(this);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        handler.sendEmptyMessage(2);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if(response.code() == 200){
            Gson gson = new Gson();
            try{
                appListResult = gson.fromJson(new StringReader(response.body().string()), AppListResult.class);
                handler.sendEmptyMessage(1);//数据下载完毕
            }catch (Exception e){
                handler.sendEmptyMessage(2);
            }
        }else{
            handler.sendEmptyMessage(2);
        }
    }

    @Override
    public void OnClick(View view, int position) {
        switch (view.getId()){
            case R.id.hb_btn_download:
                switch (appListAdapter.getItemStatus(position)){
                    case 0:
                        startDownloadPackage(appListResult.content.list[position].apk_url, position);
                        appListAdapter.setItemStatus(position, 1, true);
                        break;
                    case 1:
                        hashMap.get(position).requestShutdown();
                        appListAdapter.setItemStatus(position, 2, true);
                        break;
                    case 2:
                        startDownloadPackage(appListResult.content.list[position].apk_url, position);
                        appListAdapter.setItemStatus(position, 1, true);
                        break;
                    case 3:
                        requestInstallPackage(String.format("/%s.apk", appListAdapter.requestPackageName(position)));break;
                    case 4:
                        requestLaunchPackage(appListAdapter.requestPackageName(position));break;
                }
                break;
        }
    }

    /**
     * 发送请求，是否允许下载app
     * @param url
     * @param packagename
     * @param apkSize
     */
    public void requestDownloadApk(String url, String packagename, long apkSize){
        Toast.makeText(this, url + " " + apkSize, Toast.LENGTH_LONG).show();
        Gson gson = new Gson();
        String reportData = gson.toJson(new pack(packagename)).toLowerCase();
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String none = "0000000000000000";
        String imei = none, androidid = none, imsi = none, mcc = "", mnc = "", la = "", ci = "";

        androidid = Settings.Secure.getString(getContentResolver(), Settings.System.ANDROID_ID);
        if(tm != null){
            imei = tm.getDeviceId();
            imsi = tm.getSubscriberId();
            mcc = tm.getNetworkOperator().substring(0, 3);
            mnc = tm.getNetworkOperator().substring(3);
            GsmCellLocation cellLocation = (GsmCellLocation)tm.getCellLocation();
            la = String.valueOf(cellLocation.getLac());
            ci = String.valueOf(cellLocation.getCid());
        }
        Point pt = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(pt);
        String macAddress = "", ip = "0.0.0.0";
        WifiManager wifiMgr = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
        if (null != info) {
            macAddress = info.getMacAddress();
            ip = Integer.toString(info.getIpAddress());
        }

        //get 方式参数传入
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(appdownloadreport).newBuilder();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("cuid", new _UUID(this).remix(imei, androidid).toUpperCase());
        hashMap.put("ovr", Build.VERSION.SDK);
        hashMap.put("os_level", String.valueOf(Build.VERSION.SDK_INT));
        hashMap.put("device", Build.MODEL);
        hashMap.put("channel_id", CHANNEL_ID);
        hashMap.put("app_id", APP_ID);
        hashMap.put("svr", "4640014");
        hashMap.put("net_type", String.valueOf(getNetype(this)));
        hashMap.put("resolution", String.format("%s_%s", pt.x, pt.y));
        hashMap.put("info_ma", macAddress);
        hashMap.put("info_ms", imsi);
        hashMap.put("client_id", imei);
        hashMap.put("dpi", String.valueOf(getResources().getDisplayMetrics().densityDpi));
        hashMap.put("client_ip", ip);
        hashMap.put("mcc", mcc);
        hashMap.put("mno", mnc);
        hashMap.put("info_la", la);
        hashMap.put("info_ci", ci);
        hashMap.put("os_id", androidid);
        hashMap.put("bssid", macAddress);
        hashMap.put("nonce", String.valueOf((int)(System.currentTimeMillis() / 1000)));
        hashMap.put("pkg", "com.huanju.sdk");
        hashMap.put("reportData", URLEncoder.encode(reportData));
        //参数排序
        List<Map.Entry<String, String>> hashMaps2 = new ArrayList<Map.Entry<String, String>>(hashMap.entrySet());
        Collections.sort(hashMaps2, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> t0, Map.Entry<String, String> t1) {
                return t0.getKey().compareTo(t1.getKey());
            }
        });
        Iterator iterator = hashMaps2.iterator();
        while(iterator.hasNext()){
            Map.Entry<String, String> map = (Map.Entry<String, String>)iterator.next();
            httpUrlBuilder.addQueryParameter(map.getKey(), map.getValue());
        }


        //构建sign
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
        signHashMap.put("reportData", URLEncoder.encode(reportData));
        //加密字段排序
        List<Map.Entry<String, String>> sortSign = new ArrayList<Map.Entry<String, String>>(signHashMap.entrySet());
        Collections.sort(sortSign, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> t0, Map.Entry<String, String> t1) {
                return t0.getKey().compareTo(t1.getKey());
            }
        });
        //字符串拼接
        StringBuilder signBuilder = new StringBuilder();
        Iterator signIterator = sortSign.iterator();
        while(signIterator.hasNext()){
            Map.Entry<String, String> map = (Map.Entry<String, String>)signIterator.next();
            signBuilder.append(map.getKey());
            signBuilder.append("=");
            signBuilder.append(map.getValue());
            if(signIterator.hasNext()) {
                signBuilder.append("&");
            }else{
                signBuilder.append(hashMap.get("client_id"));
                signBuilder.append(hashMap.get("pkg"));
                signBuilder.append(APP_SECRET);
            }
        }
        String sign = CreateMD5.getMd5(signBuilder.toString()).toLowerCase();

        RequestBody requestBody = new FormBody.Builder()
                .addEncoded("Data", gson.toJson(new data(packagename, sign))).build();
        Log.e("HxBreak", gson.toJson(new data(packagename, sign)));
        Request request = new Request.Builder().url(httpUrlBuilder.build()).post(requestBody).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    String str = response.body().string();
                    Log.e("HxBreak", str != null ? str : "NullPointerString");
                }
            }
        });
    }
    /**
     * 开始下载应用包
     * @param url
     * @param position
     */
    public void startDownloadPackage(String url, int position){
        try {
            File file = new File(this.getFilesDir(), "/appcache");
            if (!file.exists()){
                file.mkdir();
            }
//            File targetFile = new File(file, String.format("/%s.apk", appListAdapter.requestPackageName(position)));
            File targetFile = new File(this.getFilesDir(), String.format("/%s.apk", appListAdapter.requestPackageName(position)));
            FileOutputStream fos = new FileOutputStream(targetFile, true);
            DownloadTask downloadTask = new DownloadTask(this,
                    url, position, (int)targetFile.length(), this, fos);
            hashMap.put(position, downloadTask);
            downloadTask.requestDownload();
            Message msg = new Message();
            msg.what = 100;
            msg.arg1 = position;
            msg.arg2 = 0;
            handler.sendMessage(msg);
            Toast.makeText(this, "下载任务即将开始", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "创建下载任务时发生意外 " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
    public boolean requestLaunchPackage(String IPackage){
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(IPackage);
            startActivity(intent);
            return true;
        }catch (Exception e){
            return false;
        }
    }
    public void requestInstallPackage(String path){
        String DataType = "application/vnd.android.package-archive";
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        File file = new File(getFilesDir(), path);
        try{
            Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
//            Runtime.getRuntime().exec("chmod 777 " + new File(getFilesDir(), "/appcache").getAbsolutePath());
        }catch (Exception e){
            Log.e("HxBreak", "chmod failed with error:" + e.toString());
        }
        intent.setDataAndType(Uri.fromFile(file.getAbsoluteFile()), DataType);
        Log.e("HxBreak", file.getAbsoluteFile().toString());
        startActivity(intent);
    }
    /**
     * 下载进度更新
     * @param buffered
     * @param id
     */
    @Override
    public void onUpdate(int buffered, int id) {
        Message msg = new Message();
        msg.what = 100;
        msg.arg1 = id;
        msg.arg2 = buffered;
        handler.sendMessage(msg);
    }

    private class pack{
        @SerializedName("package")
        public String Package;

        public pack(String p){
            this.Package = p;
        }
    }
    private class data{
        @SerializedName("package")
        public String Package;
        public String sign;
        public int reportType = 0;

        public data(String aPackage, String sign) {
            Package = aPackage;
            this.sign = sign;
        }
    }
    public static int getNetype(Context context)
    {
        int netType = -1;
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if(networkInfo==null)
        {
            return netType;
        }
        int nType = networkInfo.getType();
        if(nType==ConnectivityManager.TYPE_MOBILE)
        {
            if(networkInfo.getExtraInfo().toLowerCase().equals("cmnet"))
            {
                netType = 3;
            }
            else
            {
                netType = 2;
            }
        }
        else if(nType==ConnectivityManager.TYPE_WIFI)
        {
            netType = 1;
        }
        return netType;
    }
    public class PackageListener extends BroadcastReceiver
    {
        private Handler handler;

        public PackageListener(Handler handler) {
            this.handler = handler;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = new Message();
            Log.e("HxBreak", "package active");
            if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)){
                Bundle bundle = new Bundle();
                bundle.putString("package", intent.getDataString());
                msg.what = 200;
                msg.setData(bundle);
                handler.sendMessage(msg);
            }else if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
                Bundle bundle = new Bundle();
                bundle.putString("package", intent.getDataString());
                msg.what = 201;
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }
    }
}

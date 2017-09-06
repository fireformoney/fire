package com.hxbreak.leyou;

import android.Manifest;
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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hxbreak.Bean.AppInfo;
import com.hxbreak.leyou.Adapter.AppListAdapter;
import com.hxbreak.leyou.Bean.AppListResult;
import com.hxbreak.leyou.Bean.Result;
import com.hxbreak.leyou.Data.CreateMD5;
import com.hxbreak.leyou.Data.UserData;
import com.hxbreak.leyou.Data._UUID;
import com.hxbreak.leyou.Provider.MyFileProvider;
import com.hxbreak.leyou.Task.DownloadTask;
import com.hxbreak.listener.OnListLoadFinished;
import com.hxbreak.listener.OnReportFinished;
import com.hxbreak.utils.PhoneInfoCreator;
import com.hxbreak.utils.TaskDispatcher;

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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class AppDownloadActivity extends BaseActivity implements Callback, AppListAdapter.OnItemClick, DownloadTask.DownloadListener{

    private final String CHANNEL_ID = "20019a";
    private final String APP_ID = "b1019a";
    private static final String APP_SECRET = "D9IIkKvFYA8q8f0gsHxSpccWyOKT4dAp";
    private final String applisturl = "http://112.126.66.190/apps.php";
    private final String appdownloadreport = "http://package.mhacn.com/api/delay/report/download/start";
    private final String FileStorePath = "/appcache";
    private final String TAG = "HxBreak";
    private final String FILEPROVIDER = "com.hxbreak.leyou.fileprovider";
    public static final MediaType JSON= MediaType.parse("application/json; charset=utf-8");


    private UserData mUserData;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private OkHttpClient okHttpClient;
    private com.hxbreak.Bean.AppListResult.Content appListResult;
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
                    Toast.makeText(AppDownloadActivity.this, "列表加载失败", Toast.LENGTH_LONG).show();break;
                case 3:
                    startDownloadPackage(appListResult.list[msg.arg1].apk_url, msg.arg1);
                    break;
                case 4:
                    Toast.makeText(AppDownloadActivity.this, String.format("任务上报失败 %d", msg.arg1), Toast.LENGTH_SHORT).show();
                    break;
                case 90:
                    //Setup apk size
                    appListAdapter.setItemSize(msg.arg1, msg.arg2);
                    break;
                case 100:
                    appListAdapter.updateItemDownloadProgess(msg.arg1, msg.arg2);
                    if(appListAdapter.shouldLaunchInstall(msg.arg1)){
                        requestInstallPackage(String.format("/%s.apk", appListAdapter.requestPackageName(msg.arg1)));
                    }
                    break;
                case 101:
                    Toast.makeText(AppDownloadActivity.this, "下载任务出错", Toast.LENGTH_LONG).show();break;
                case 200:
                    Toast.makeText(getApplicationContext(), "奖励已经获得", Toast.LENGTH_SHORT).show();
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
        mUserData = new UserData(this);
        toolbar = (Toolbar) findViewById(R.id.hb_toolbar);
        recyclerView = (RecyclerView)findViewById(R.id.hb_app_recylerview);
        progressBar = (ProgressBar)findViewById(R.id.hx_progressBar);
        initToolbar();
        sdk_downloadAppList();

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
        unregisterReceiver(broadcastReceiver);
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
        recyclerView.setItemAnimator(null);
        appListAdapter = new AppListAdapter(this, appListResult.list, this, new File(getFilesDir(), FileStorePath).listFiles(), getInstalledAppList());
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
     * New API
     */
    private void sdk_downloadAppList(){
        TaskDispatcher taskDispatcher = new TaskDispatcher();
        taskDispatcher.FetchList(new OnListLoadFinished() {
            @Override
            public void onFailed() {
                handler.sendEmptyMessage(2);
            }

            @Override
            public void onSuccess(com.hxbreak.Bean.AppListResult.Content content) {
                appListResult = content;
                handler.sendEmptyMessage(1);
            }
        });
    }
    /**
     * 下载app列表
     * old API
     */
    private void dowloadAppList(){
        HashMap<String, String> hashMap = new HashMap<>();
    //20020a&app_id=b1020a
        hashMap.put("from_client", "server");
        hashMap.put("channel_id", "20020a");
        hashMap.put("app_id", "b1020a");
        hashMap.put("page", String.valueOf((int)(Math.random() * 10)));
        hashMap.put("size", "10");
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
            sign.append(entry.getKey());
            sign.append("=");
            sign.append(entry.getValue());
            if (iterator.hasNext()) {
                sign.append("&");
            }
        }

        sign.append(APP_SECRET);
        String final_sign = CreateMD5.getMd5(sign.toString()).toLowerCase();
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
                String temp = response.body().string();
                Log.e(TAG, temp);
                appListResult = gson.fromJson(temp, com.hxbreak.Bean.AppListResult.Content.class);
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
                        sdk_requestDownloadApk(appListResult.list[position].apk_url, appListResult.list[position].Package, appListResult.list[position].apk_size, position);
//                        AppDownloadActivityPermissionsDispatcher.requestDownloadApkWithCheck(this, appListResult.list[position].apk_url, appListResult.list[position].Package, appListResult.list[position].apk_size, position);
//                        requestDownloadApk(appListResult.content.list[position].apk_url, appListResult.content.list[position].Package, appListResult.content.list[position].apk_size, position);
//                        startDownloadPackage(appListResult.content.list[position].apk_url, position);
                        break;
                    case 1:
                        hashMap.get(position).requestShutdown();
                        appListAdapter.setItemStatus(position, 2, true);
                        break;
                    case 2:
                        startDownloadPackage(appListResult.list[position].apk_url, position);
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
     * New API
     * @param url //bypass
     * @param packagename //Must NotNull
     * @param apkSize //bypass
     * @param id //bypass
     */
    public void sdk_requestDownloadApk(String url, String packagename, long apkSize, final int id){
        TaskDispatcher taskDispatcher = new TaskDispatcher();
        PhoneInfoCreator phoneInfoCreator = new PhoneInfoCreator();
        phoneInfoCreator.imei = "000000000000000";
        phoneInfoCreator.BRAND = "generic";
        phoneInfoCreator.MODEL = "CustomPhone-4.4.4-API19-768x1280";
        phoneInfoCreator.dpi = "320";
        phoneInfoCreator.imsi = "310260000000000";
        phoneInfoCreator.androidid = "3e273bee7e3dfd77";
        phoneInfoCreator.ci = "0";
        phoneInfoCreator.la = "0";
        phoneInfoCreator.macAddress = "08:00:27:ba:bb:96";
        phoneInfoCreator.mcc = "310";
        phoneInfoCreator.mnc = "260";
        phoneInfoCreator.os_level = "19";
        phoneInfoCreator.ovr = "19";
        phoneInfoCreator.net_type = "1";
        phoneInfoCreator.pt_x = "768";
        phoneInfoCreator.pt_y = "1280";
        taskDispatcher.setPhoneInfoCreator(phoneInfoCreator);
        taskDispatcher.DownloadReport(packagename,new _UUID(this).getUUID(), mUserData.getUserIp(),  new OnReportFinished() {
            @Override
            public void onFailed(int code) {
                Log.e(TAG, "Fuck You");
            }

            @Override
            public void onSuccess(AppInfo appInfo) {
                /**
                 "apkMd5":"5fe689db9e89f0816b48c5bbb5a6a394",
                 "apkUrl":"http://e.gdown.baidu.com/data/wisegame/9e89f0816b48c5bb/yuwan_695.apk",
                 "apkSize":28902051,
                 "versionCode":695,
                 "package":"cn.longmaster.pengpeng"
                 */
                //Notice ###################### it 's "Package" not "package_name"
                //Here 's AppInfo Not Not Not normal AppInfo Class, just jump to AppInfo.java find field under extra
                Log.e(TAG, String.format("pkg: %s Reported", appInfo.Package));
            }

        });
    }

        /**
         * 发送请求，是否允许下载app
         * @param url
         * @param packagename
         * @param apkSize
         */
    @NeedsPermission({Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void requestDownloadApk(String url, String packagename, long apkSize, final int id){
        Gson gson = new Gson();
        String reportData = gson.toJson(new pack(packagename)).toLowerCase();
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        String none = "0000000000000000";
        String imei = none, androidid = none, imsi = none, mcc = "", mnc = "", la = "", ci = "";

        androidid = Settings.Secure.getString(getContentResolver(), Settings.System.ANDROID_ID);
        if(tm != null){
            imei = tm.getDeviceId();
            imsi = tm.getSubscriberId();
            try {
                mcc = tm.getNetworkOperator().substring(0, 3);
                mnc = tm.getNetworkOperator().substring(3);
            }catch (Exception e){
                mcc = "310";
                mnc = "260";
            }
            CellLocation cellLocation0 = tm.getCellLocation();
            if(cellLocation0 instanceof GsmCellLocation){
                GsmCellLocation cellLocation = (GsmCellLocation)cellLocation0;
                la = String.valueOf(cellLocation.getLac());
                ci = String.valueOf(cellLocation.getCid());
            }else if(cellLocation0 instanceof CdmaCellLocation){
                CdmaCellLocation cellLocation = (CdmaCellLocation)cellLocation0;
                la = String.valueOf(cellLocation.getNetworkId());
                ci = String.valueOf(cellLocation.getBaseStationId());
            }else{
                la = "-1";
                ci = "-1";
            }
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
        hashMap.put("device", URLEncoder.encode(Build.BRAND.replace(" ", "") + "_" + Build.MODEL.replace(" ", "")));

        hashMap.put("channel_id", CHANNEL_ID);
        hashMap.put("app_id", APP_ID);
        hashMap.put("svr", "4640014");
        hashMap.put("net_type", String.valueOf(getNetype(this)));
        hashMap.put("resolution", String.format("%s_%s", pt.x, pt.y));
        hashMap.put("info_ma", macAddress);
        hashMap.put("info_ms", imsi);
        hashMap.put("client_id", imei);
        hashMap.put("dpi", String.valueOf(getResources().getDisplayMetrics().densityDpi));
        hashMap.put("client_ip", mUserData.getUserIp());
        hashMap.put("mcc", mcc);
        hashMap.put("mno", mnc);
        hashMap.put("info_la", la);
        hashMap.put("info_ci", ci);
        hashMap.put("os_id", androidid);
        hashMap.put("bssid", macAddress);
        hashMap.put("nonce", String.valueOf((int)(System.currentTimeMillis() / 1000)));
        hashMap.put("pkg", "com.hxbreak.leyou");
//        hashMap.put("reportData", reportData);
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
        Log.e(TAG, String.format("httpurl : %s", httpUrlBuilder.toString()));

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
        Log.e(TAG, String.format("sign raw : %s", signBuilder.toString()));
        String sign = CreateMD5.getMd5(signBuilder.toString()).toLowerCase();
        /**
         * {"reportData":{"package":"com.lehai.ui"},"sign":"95042303416b6381598753ee8944fcdf","reportType":0}
         */
//        RequestBody requestBody = new FormBody.Builder()
//                .addEncoded("", gson.toJson(new data(packagename, sign))).build();
        RequestBody requestBody = RequestBody.create(JSON, gson.toJson(new data(packagename, sign)));
        Log.e("HxBreak", gson.toJson(new data(packagename, sign)));
        Request request = new Request.Builder().url(httpUrlBuilder.build()).post(requestBody).build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg = new Message();
                msg.what = 4;
                msg.arg1 = 201;
                handler.sendMessage(msg);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Message msg = new Message();
                if(response.code() == 200){
                    String str = response.body().string();
                    Gson gson = new Gson();
                    Result result = gson.fromJson(str, Result.class);
                    if(result.result == 0){
                        msg.what = 3;
                        msg.arg1 = id;
                    }else{
                        msg.what = 4;
                        msg.arg1 = result.result;
                    }
                }else{
                    msg.what = 4;
                    msg.arg1 = 201;
                }
                handler.sendMessage(msg);
            }
        });
    }

    /**
     *
     * @param request
     */
    @OnShowRationale({Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void onShowTip2User(PermissionRequest request){
        request.proceed();
    }
    /**
     * 开始下载应用包
     * @param url
     * @param position
     */
    public void startDownloadPackage(String url, int position){
        boolean hasWork = true;
        try {
            File file = new File(this.getFilesDir(), FileStorePath);
            if (!file.exists()){
                file.mkdir();
            }
//            File targetFile = new File(file, String.format("/%s.apk", appListAdapter.requestPackageName(position)));
            File targetFile = new File(this.getFilesDir(), String.format("%s/%s.apk",FileStorePath, appListAdapter.requestPackageName(position)));
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
            hasWork = false;
        }
        if(hasWork){
            appListAdapter.setItemStatus(position, 1, true);
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

    /**
     * 兼容Android 7.0及以上版本
     * @param path
     */
    public void requestInstallPackage(String path){
        boolean passed = false;
        File file = new File(getFilesDir(), FileStorePath + path);
        String DataType = "application/vnd.android.package-archive";
        Log.e("HxBreak", String.format("file:%s-%s", String.valueOf(file.exists()), file.getAbsolutePath()));
        try{
            Intent intent = new Intent();
            Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
            Runtime.getRuntime().exec("chmod 777 " + file.getParent());
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), DataType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.e(TAG, String.format("Intent Test1: %s", intent.toString()));
            startActivity(intent);
            passed = true;
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }
        if (!passed){
            try {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MyFileProvider myFileProvider = new MyFileProvider();
                intent.setDataAndType(myFileProvider.getUriForFile(this, FILEPROVIDER, file), DataType);
                Log.e(TAG, String.format("Intent Test2: %s", intent.toString()));
                startActivity(intent);
                passed = true;
            }catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }
        if(!passed){
            Toast.makeText(this, "安装程序无法正常启动", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onInit(long length, int id) {
        Message msg = new Message();
        msg.what = 90;
        msg.arg1 = id;
        msg.arg2 = (int)length;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppDownloadActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
            Log.e("HxBreak", "package active :" + intent.getDataString() );
            if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)){
                Bundle bundle = new Bundle();
                bundle.putString("package", intent.getDataString().substring(8));
                msg.what = 200;
                msg.setData(bundle);
                if(appListAdapter.hasPackageInList(intent.getDataString().substring(8))){
                    handler.sendMessage(msg);
                    int x = (int)(Math.random() * 10) + 15;
                    mUserData.setUserMoney(mUserData.getUserMoney() + (float) (x / 100.0));
                }
            }else if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
                Bundle bundle = new Bundle();
                bundle.putString("package", intent.getDataString().substring(8));
                msg.what = 201;
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        }
    }
}

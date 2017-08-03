package com.hxbreak.leyou;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hxbreak.leyou.Adapter.AppListAdapter;
import com.hxbreak.leyou.Bean.AppInfo;
import com.hxbreak.leyou.Bean.AppListResult;
import com.hxbreak.leyou.Data.CreateMD5;
import com.hxbreak.leyou.Data.UserData;
import com.hxbreak.leyou.Provider.MyFileProvider;
import com.hxbreak.leyou.Task.DownloadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by HxBreak on 2017/7/28.
 */

public class GameDownloadActivity extends BaseActivity implements Callback, AppListAdapter.OnItemClick, DownloadTask.DownloadListener{
    private final String applisturl = "http://112.126.66.190/games.php";
    private final String CHANNEL_ID = "20020a";
    private final String APP_ID = "b1020a";
    private final String FileStorePath = "/gamecache";
    private final String TAG = "HxBreak";
    private final String FILEPROVIDER = "com.hxbreak.leyou.fileprovider";

    private UserData mUserData;
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
                    Toast.makeText(GameDownloadActivity.this, "列表加载失败", Toast.LENGTH_LONG).show();break;
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
                    Toast.makeText(GameDownloadActivity.this, "下载任务出错", Toast.LENGTH_LONG).show();break;
                case 200:
                    Toast.makeText(getApplicationContext(), "奖励已经添加", Toast.LENGTH_SHORT).show();
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
        dowloadAppList();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        broadcastReceiver = new GameDownloadActivity.PackageListener(handler);
        registerReceiver(broadcastReceiver, intentFilter);
    }
    private void initToolbar(){
        setSupportActionBar(toolbar);
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
        appListAdapter = new AppListAdapter(this, appListResult.content.list, this, new File(getFilesDir(), FileStorePath).listFiles(), getInstalledAppList(), true);
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
//        hashMap.put("from_client", "server");
//        hashMap.put("channel_id", CHANNEL_ID);
//        hashMap.put("app_id", APP_ID);
//        hashMap.put("pn", "1");
//        hashMap.put("rn", "10");
//        hashMap.put("timestamp", String.valueOf((int)(System.currentTimeMillis() / 1000)));
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
                AppInfo[] appInfos = gson.fromJson(new StringReader(response.body().string()), AppInfo[].class);
                appListResult = new AppListResult();
                appListResult.content.list = appInfos;
                appListResult.content.has_more = 0;
                appListResult.content.total_cnt = appInfos.length;
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
     * 开始下载应用包
     * @param url
     * @param position
     */
    public void startDownloadPackage(String url, int position){
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
                    int x = (int)(Math.random() * 100);
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

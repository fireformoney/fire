package com.hxbreak.leyou.Task;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by HxBreak on 2017/7/19.
 */

public class DownloadTask implements Callback {
    private Context context;
    private String url;
    private int downloadId;
    private int continueDownload = 0;
    private DownloadListener downloadListener;
    private FileOutputStream fos;

    private boolean shutdown = false;

    private Call call;

    public DownloadTask(Context context, String url, int downloadId, int continueDownload, DownloadListener downloadListener, FileOutputStream fos) {
        this.context = context;
        this.url = url;
        this.downloadId = downloadId;
        this.continueDownload = continueDownload;
        this.downloadListener = downloadListener;
        this.fos = fos;
    }

    public DownloadTask(Context context, String url, int downloadId, DownloadListener downloadListener, FileOutputStream fos) {
        this.context = context;
        this.url = url;
        this.downloadId = downloadId;
        this.downloadListener = downloadListener;
        this.fos = fos;
    }

    public void requestDownload(){
        Request.Builder request = new Request.Builder().url(url);
        if(continueDownload != 0){
            request.addHeader("Range", String.format("bytes=%d-", continueDownload));
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        call = okHttpClient.newCall(request.build());
        call.enqueue(this);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        downloadListener.onUpdate(-1, downloadId);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (response.code() != 200 || response.code() != 206){
            String contentRange = response.header("Content-Range", "");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            BufferedInputStream bis = new BufferedInputStream(response.body().byteStream());
            byte[] bt = new byte[51200];
            int size = 0, current = 0;
            if(contentRange.equals("")){
                while(!shutdown && (-1 != (size = bis.read(bt, 0, bt.length)))){
                    bos.write(bt, 0, size);
                    current += size;
                    downloadListener.onUpdate(current, downloadId);
                }
            }else{
                int base = Integer.parseInt(contentRange.substring(6, contentRange.indexOf('-')));
                while(!shutdown && ( -1 != (size = bis.read(bt, 0, bt.length)))){
                    bos.write(bt, 0, size);
                    if(current == 0) {
                        current += base;
                    }
                    current += size;
                    downloadListener.onUpdate(current, downloadId);
                }
            }
            bos.close();
            bis.close();
        }else{
            downloadListener.onUpdate(-1, downloadId);
        }
    }
    public void requestShutdown(){
        call.cancel();
        shutdown = true;
    }
    public interface DownloadListener{
        public void onUpdate(int buffered, int id);
    }
}

package com.hxbreak.leyou.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.hxbreak.leyou.Bean.AppInfo;
import com.hxbreak.leyou.R;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Created by HxBreak on 2017/7/17.
 */

public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener{
    private Context context;
    private AppInfo[] appInfos;
    private OnItemClick onItemClick;
    private HashMap<Integer, AppListAdapter.DownlaodInfo> hashMap = new HashMap<>();
    private File[] filesDir;
    private RequestManager requestManager;
    private List<String> list;
    public AppListAdapter(Context context, AppInfo[] appInfos, OnItemClick onItemClick, File[] files, List<String> list) {
        this.context = context;
        this.appInfos = appInfos;
        this.onItemClick = onItemClick;
        this.filesDir = files;
        this.requestManager = Glide.with(context);
        this.list = list;
    }
    public void listRemove(String str){
        this.list.remove(str);
    }
    public void listAdd(String str){
        this.list.add(str);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.hb_app_item, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AppViewHolder appViewHolder = (AppViewHolder)holder;
        appViewHolder.id.setText(String.valueOf(position + 1));
        appViewHolder.appname.setText(appInfos[position].name);
        appViewHolder.download_btn.setOnClickListener(this);
        appViewHolder.download_btn.setTag(position);
        if(this.filesDir != null){
            for (File f : filesDir){
                if(f != null && f.exists()){
                    if(f.getName().equals(appInfos[position].Package + ".apk")){
                        if(hashMap.get(position) == null){
                            if(f.length() >= appInfos[position].apk_size){
                                hashMap.put(position, new DownlaodInfo((int) f.length(), 3));
                            }else {
                                hashMap.put(position, new DownlaodInfo((int) f.length(), 2));
                            }
                        }
                    }
                }
            }
        }
        requestManager.load(appInfos[position].icon_url).into(appViewHolder.appImage);
        for (String pStr : list){
            if(pStr.equalsIgnoreCase(appInfos[position].Package)){
                setItemStatus(position, 4, false);
            }
        }
        AppListAdapter.DownlaodInfo s = hashMap.get(position);
        if(s != null){
            SpannableStringBuilder ssb = new SpannableStringBuilder(String.format("%.2fMB/%.2fMB", s.getBuffered() /1024 /1024.0, appInfos[position].apk_size / 1024 / 1024.0));
            ssb.setSpan(new ForegroundColorSpan(Color.BLUE), 0, String.format("%.2f", s.getBuffered() /1024 /1024.0).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            appViewHolder.downloadProg.setText(ssb);
            appViewHolder.progressBar.setMax((int)appInfos[position].apk_size);
            appViewHolder.progressBar.setProgress(s.getBuffered());
            switch (s.getStatus()){
                //0 未开始下载,  1 下载中, 2 暂停下载中, 3 下载完成 4 安装完成
                case 0:
                    appViewHolder.download_btn.setText("下载");break;
                case 1:
                    appViewHolder.download_btn.setText("暂停");break;
                case 2:
                    appViewHolder.download_btn.setText("继续");break;
                case 3:
                    appViewHolder.download_btn.setText("安装");break;
                case 4:
                    appViewHolder.download_btn.setText("打开");break;
            }
            if(s.getStatus() > 2){
                appViewHolder.appsize.setVisibility(View.VISIBLE);
                appViewHolder.progressView.setVisibility(View.GONE);
                appViewHolder.appsize.setText("下载完成");
            }else{
                appViewHolder.appsize.setVisibility(View.GONE);
                appViewHolder.progressView.setVisibility(View.VISIBLE);
            }
        }else{
            appViewHolder.download_btn.setText("下载");
            appViewHolder.appsize.setText(appInfos[position].apk_size / 1024 / 1024 + " MB");
            appViewHolder.appsize.setVisibility(View.VISIBLE);
            appViewHolder.progressView.setVisibility(View.GONE);
        }
    }
    public String requestPackageName(int position){
        return appInfos[position].Package;
    }
    public boolean isDownloading(int id){
        return hashMap.get(id) != null;
    }

    public synchronized int getItemStatus(int id){
        AppListAdapter.DownlaodInfo buffered;
        if(null == (buffered = hashMap.get(id))){
            return 0;
        }else{
            return buffered.getStatus();
        }
    }
    public synchronized void setItemStatus(int id, int status, boolean update){
        AppListAdapter.DownlaodInfo di = null;
        if (null != (di = hashMap.get(id))){
            hashMap.put(id, new DownlaodInfo(di.getBuffered(), status));
        }else{
            hashMap.put(id, new DownlaodInfo(0, status));
        }
        if (update){
            notifyItemChanged(id);
        }
        Log.e("HxBreak", String.format("id:%d status:%d", id, status));
    }
    @Override
    public int getItemCount() {
        return appInfos.length;
    }
    @Override
    public void onClick(View view) {
        onItemClick.OnClick(view, (int)view.getTag());
    }

    /**
     * 更新进度条
     * @param id
     * @param buffered
     */
    public void updateItemDownloadProgess(int id, int buffered){
        DownlaodInfo source = hashMap.get(id);
        hashMap.put(id, new DownlaodInfo(buffered, source == null ? 1 : source.getStatus()));
        if (buffered >= appInfos[id].apk_size){
            setItemStatus(id, 3, false);
        }
        notifyItemChanged(id);
        Log.e("HxBreak", String.format("id:%d status:%d", id, 1));
    }
    public class AppViewHolder extends RecyclerView.ViewHolder {
        private TextView id, appname, appsize, downloadProg;
        private Button download_btn;
        private View progressView;
        private ProgressBar progressBar;
        private ImageView appImage;
        public AppViewHolder(View itemView) {
            super(itemView);
            id  = (TextView)itemView.findViewById(R.id.hb_item_id);
            appname  = (TextView)itemView.findViewById(R.id.hb_item_appname);
            appsize  = (TextView)itemView.findViewById(R.id.hb_item_appsize);
            download_btn  = (Button)itemView.findViewById(R.id.hb_btn_download);
            downloadProg = (TextView)itemView.findViewById(R.id.hb_currentspeed);
            progressBar = (ProgressBar)itemView.findViewById(R.id.hb_download_progressBar);
            progressView = itemView.findViewById(R.id.hb_item_progress_group);
            appImage = (ImageView) itemView.findViewById(R.id.hb_item_image);
        }
    }

    public class DownlaodInfo{
        private int buffered;
        private int status;//0 未开始下载,  1 下载中, 2 暂停暂停中, 3 下载完成 4 安装完成

        public DownlaodInfo(int buffered, int status) {
            this.buffered = buffered;
            this.status = status;
        }

        public synchronized int getBuffered() {
            return buffered;
        }

        public synchronized void setBuffered(int buffered) {
            this.buffered = buffered;
        }

        public synchronized int getStatus() {
            return status;
        }

        public synchronized void setStatus(int status) {
            this.status = status;
        }
    }

    public interface OnItemClick{
        public void OnClick(View view, int position);
    }
}

package com.hxbreak.leyou.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hxbreak.leyou.Bean.AppInfo;
import com.hxbreak.leyou.R;

import java.util.HashMap;

/**
 * Created by HxBreak on 2017/7/17.
 */

public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener{
    private Context context;
    private AppInfo[] appInfos;
    private OnItemClick onItemClick;
    private HashMap<Integer, Integer> hashMap = new HashMap<>();
    public AppListAdapter(Context context, AppInfo[] appInfos, OnItemClick onItemClick) {
        this.context = context;
        this.appInfos = appInfos;
        this.onItemClick = onItemClick;
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
        Integer s = hashMap.get(position);
        if(s != null){
            appViewHolder.appsize.setVisibility(View.GONE);
            appViewHolder.progressView.setVisibility(View.VISIBLE);
            SpannableStringBuilder ssb = new SpannableStringBuilder(String.format("%dMB/%dMB", s /1024 /1024, appInfos[position].apk_size / 1024 / 1024));
            ssb.setSpan(new ForegroundColorSpan(Color.BLUE), 0, String.valueOf(s /1024 /1024).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            appViewHolder.downloadProg.setText(ssb);
            appViewHolder.progressBar.setMax((int)appInfos[position].apk_size);
            appViewHolder.progressBar.setProgress(s);
            appViewHolder.download_btn.setText("暂停");
        }else{
            appViewHolder.download_btn.setText("下载");
            appViewHolder.appsize.setText(appInfos[position].apk_size / 1024 / 1024 + " MB");
            appViewHolder.appsize.setVisibility(View.VISIBLE);
            appViewHolder.progressView.setVisibility(View.GONE);
        }
    }
    public boolean isDownloading(int id){
        return hashMap.get(id) != null;
    }
    @Override
    public int getItemCount() {
        return appInfos.length;
    }
    @Override
    public void onClick(View view) {
        onItemClick.OnClick(view, (int)view.getTag());
    }
    public void updateItemDownloadProgess(int id, int buffered){
        hashMap.put(id, buffered);
        notifyItemChanged(id);
    }
    public class AppViewHolder extends RecyclerView.ViewHolder {
        private TextView id, appname, appsize, downloadProg;
        private Button download_btn;
        private View progressView;
        private ProgressBar progressBar;
        public AppViewHolder(View itemView) {
            super(itemView);
            id  = (TextView)itemView.findViewById(R.id.hb_item_id);
            appname  = (TextView)itemView.findViewById(R.id.hb_item_appname);
            appsize  = (TextView)itemView.findViewById(R.id.hb_item_appsize);
            download_btn  = (Button)itemView.findViewById(R.id.hb_btn_download);
            downloadProg = (TextView)itemView.findViewById(R.id.hb_currentspeed);
            progressBar = (ProgressBar)itemView.findViewById(R.id.hb_download_progressBar);
            progressView = itemView.findViewById(R.id.hb_item_progress_group);
        }
    }

    public interface OnItemClick{
        public void OnClick(View view, int position);
    }
}

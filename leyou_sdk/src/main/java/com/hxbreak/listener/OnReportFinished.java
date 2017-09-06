package com.hxbreak.listener;

import com.hxbreak.Bean.AppInfo;

/**
 * Created by HxBreak on 2017/9/5.
 */

public interface OnReportFinished {
    public void onFailed(int code);
    public void onSuccess(AppInfo appInfo);
}

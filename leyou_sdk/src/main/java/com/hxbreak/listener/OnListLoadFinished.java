package com.hxbreak.listener;

import com.hxbreak.Bean.AppListResult;

/**
 * Created by HxBreak on 2017/9/5.
 */

public interface OnListLoadFinished {
    public void onFailed();
    public void onSuccess(AppListResult.Content content);
}

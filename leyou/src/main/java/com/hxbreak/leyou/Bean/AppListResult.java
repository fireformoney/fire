package com.hxbreak.leyou.Bean;

/**
 * Created by HxBreak on 2017/7/17.
 */

public class AppListResult {
    public int result;
    public String msg;
    public Content content;
    public class Content{
        public int total_cnt;
        public int has_more;
        public AppInfo list[];
    }
}

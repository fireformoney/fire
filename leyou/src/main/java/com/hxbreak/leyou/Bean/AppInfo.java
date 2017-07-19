package com.hxbreak.leyou.Bean;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HxBreak on 2017/7/17.
 */

public class AppInfo {
    @SerializedName("package")
    public String Package;
    public String name;
    public String type;
    public String version_name;
    public long version_code;
    public long apk_size;
    public String apk_md5;
    public String apk_url;
}

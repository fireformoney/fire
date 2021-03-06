package com.hxbreak.Bean;

import com.google2hx.gson.annotations.SerializedName;

/**
 * Created by HxBreak on 2017/7/17.
 */

public class AppInfo {
    @SerializedName("package")
    public String Package;
    public String package_name;
    public String name;
    public String type;
    public String version_name;
    public long version_code;
    public long apk_size;
    public String apk_md5;
    public String apk_url;
    public String icon_url;
    //extra
    public String apkMd5;
    public String apkUrl;
    public long apkSize;
    public long versionCode;
}

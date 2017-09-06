package com.hxbreak.Bean;

/**
 * Created by HxBreak on 2017/8/9.
 */

public class Result {
    public int result;
    public innerContent content;
    public class innerContent{
        public AppInfo extraData;
        public int reportType;
    }
    public class InnerResult{
        public int result;
    }
}

package com.greplay.leyou;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.hxbreak.leyou.R;

public class ClassicWebActivity extends AppCompatActivity {
    String clasicUrl = "https://cpu.baidu.com/1001/b3c93215";
    WebView mWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitvity_webclassic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.include);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((TextView)findViewById(R.id.hb_title)).setText("乐悠众包会员版");
        mWebView = ((WebView)findViewById(R.id.web));
        mWebView.loadUrl(clasicUrl);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();break;
        }
        return super.onOptionsItemSelected(item);
    }
}

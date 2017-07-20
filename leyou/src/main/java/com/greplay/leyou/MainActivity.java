package com.greplay.leyou;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.hxbreak.leyou.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.hb_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        ((TextView)findViewById(R.id.hb_title)).setText("乐悠众包会员版");
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

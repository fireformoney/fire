package com.greplay.leyou;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hxbreak.leyou.AppDownloadActivity;
import com.hxbreak.leyou.GameDownloadActivity;
import com.hxbreak.leyou.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.hb_toolbar);
        toolbar.setTitle("");
        ((TextView)findViewById(R.id.hb_title)).setText(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        TextView moneyTextView =(TextView) findViewById(R.id.performoney);
        moneyTextView.setText(Html.fromHtml("<u>"+"提现方式>>"+"</u>"));
        moneyTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ContactActivity.class);
                startActivity(intent);
            }
        });
        Button btnAppin = (Button)findViewById(R.id.app_in);
        btnAppin.setOnClickListener(this);

        Button btnGamein = (Button)findViewById(R.id.game_in);
        btnGamein.setOnClickListener(this);

        Button btnClassicin = (Button)findViewById(R.id.classic_in);
        btnClassicin.setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.app_in:
                Intent intentApp = new Intent(this, AppDownloadActivity.class);
                startActivity(intentApp);
                break;
            case R.id.game_in:
                Intent intentGame = new Intent(this, GameDownloadActivity.class);
                startActivity(intentGame);
                break;
            case R.id.classic_in:
                Intent intentWebClassic = new Intent(this, ClassicWebActivity.class);
                startActivity(intentWebClassic);
                break;
        }

    }
}

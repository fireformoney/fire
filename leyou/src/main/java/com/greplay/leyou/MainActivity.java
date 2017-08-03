package com.greplay.leyou;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hxbreak.leyou.AppDownloadActivity;
import com.hxbreak.leyou.Data.UserData;
import com.hxbreak.leyou.GameDownloadActivity;
import com.hxbreak.leyou.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private UserData mUserData;
    private String mActiveCode[] = {"01001", "01203", "02314", "15203", "10253", "52642"};
    private boolean mTested = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mUserData = new UserData(this);
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
        if(mUserData.getUserKey().equals("")){
            requestInputContent();
            mTested = true;
        }else{
            for(String i : mActiveCode){
                if(i.equals(mUserData.getUserKey())){
                    mTested = true;
                    break;
                }
            }
        }
        if(mTested == false){
            finish();
        }
        updateMoneyUI();
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
    public void updateMoneyUI(){
        TextView yuan = (TextView) findViewById(R.id.yuan);
        yuan.setText(String.format("%.2f元", mUserData.getUserMoney()));
    }
    public void requestInputContent(){
        final EditText editText = new EditText(this);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("请输入邀请码")
                .setIcon(R.mipmap.ic_launcher_round)
                .setView(editText)
                .setCancelable(false)
                .setNegativeButton("取消", null)
                .setPositiveButton("确认", null)
                .create();
        dialog.show();
        dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!editText.getText().toString().equals("")){
                            for (String i : mActiveCode){
                                if(i.equals(editText.getText().toString())){
                                    mUserData.setUserKey(editText.getText().toString());
                                    dialog.dismiss();
                                    return ;
                                }
                            }
                            Toast.makeText(MainActivity.this, "请输入正确的激活码", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMoneyUI();
    }
}

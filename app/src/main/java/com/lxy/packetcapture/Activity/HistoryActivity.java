package com.lxy.packetcapture.Activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.lxy.packetcapture.R;
import com.lxy.packetcapture.base.BaseActivity;
import com.lxy.packetcapture.widget.TitleBar;

public class HistoryActivity extends AppCompatActivity {
    private TitleBar mTitleBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        initView();

    }

    private void initView(){
        mTitleBar = findViewById(R.id.titlebar);
    }
}

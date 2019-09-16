package com.lxy.packetcapture.Activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lxy.packetcapture.R;
import com.lxy.packetcapture.widget.TitleBar;

public class CollectionActivity extends AppCompatActivity implements View.OnClickListener {
    private TitleBar mTitleBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        initView();
    }

    private void initView(){
        mTitleBar = findViewById(R.id.titlebar);
        mTitleBar.setTitleText("历史记录");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.titlebar:
                mTitleBar.setOnviewClick(new TitleBar.OnViewClickListener() {
                    @Override
                    public void onRightImage() {

                    }
                });
        }
    }
}

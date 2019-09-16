package com.lxy.packetcapture;

import android.content.Intent;
import android.net.VpnService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.lxy.packetcapture.Activity.CollectionActivity;
import com.lxy.packetcapture.Activity.HistoryActivity;
import com.lxy.packetcapture.service.LocalVpnService;
import com.lxy.packetcapture.tools.IntentUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,NavigationView.OnNavigationItemSelectedListener{
    private ImageView menu;
    private DrawerLayout drawerLayout;
    private FloatingActionButton floatButton;
    private int flag = 0;
    private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //drawerLayout = findViewById(R.id.draw_layout);
        if(drawerLayout.isDrawerOpen(R.id.navigation_view)){
            drawerLayout.closeDrawer(GravityCompat.START);//关闭侧栏
            //drawerLayout.closeDrawers();
        }
    }

    private void initView(){
        menu = findViewById(R.id.menu);
        drawerLayout = findViewById(R.id.draw_layout);
        floatButton = findViewById(R.id.floatButton);
        mNavigationView = findViewById(R.id.navigation_view);
        menu.setOnClickListener(this);
        floatButton.setOnClickListener(this);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.menu:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.floatButton:
                if(flag == 0){
                    floatButton.setImageResource(R.drawable.capture);
                    flag = 1;
                }
                else if(flag == 1){
                    floatButton.setImageResource(R.drawable.forbidden1);
                    flag = 0;
                }
                openVpn();
                break;
        }
    }



    public void openVpn(){
        Intent intent = VpnService.prepare(this);
        if(intent != null){
            startActivityForResult(intent,1);
        }else {
            onActivityResult(1,RESULT_OK,null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            Intent intent = new Intent(this, LocalVpnService.class);
            startService(intent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.collection:
                IntentUtils.getInstance().goActivity(this, CollectionActivity.class);
                break;
            case R.id.history:
                IntentUtils.getInstance().goActivity(this, HistoryActivity.class);
                break;
            case R.id.manual:
                break;
            case R.id.share:
                break;
            case R.id.about:
                break;
            case R.id.setting:
                break;
                default:
                    break;
        }
        return true;
    }

    /**
     * 解决ativity返回到drawlayout显示侧栏的问题 此方法有问题 在onStart方法中解决
     */
    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(R.id.navigation_view)){
            drawerLayout.closeDrawer(GravityCompat.START);//关闭侧栏
        }
        super.onBackPressed();
    }
}

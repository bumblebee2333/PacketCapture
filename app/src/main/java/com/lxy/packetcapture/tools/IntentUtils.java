package com.lxy.packetcapture.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

/**
 * Intent跳转封装
 * Created by Administrator on 2019/9/9
 */

public class IntentUtils {
    private static final String OPEN_ACTIVITY_KEY = "open_activity";//intent跳转传递传输key
    private static IntentUtils manager;
    private static Intent intent;

    private IntentUtils(){}

    public static IntentUtils getInstance(){
        if(manager == null){
            synchronized (IntentUtils.class){
                if (manager == null){
                    manager = new IntentUtils();
                }
            }
        }
        intent = new Intent();
        return manager;
    }

    /**
     * 获取上一个页面传递过来的参数
     *
     * @param  activity this
     * @param <T>       泛型
     * @return
     */
    public <T> T getParcelableExtra(Activity activity){
        Parcelable parcelable = activity.getIntent().getParcelableExtra(OPEN_ACTIVITY_KEY);
        activity = null;
        return (T) parcelable;
    }

    /**
     * 启动一个Activity
     *
     * @param _this;
     * @param _class
     */
    public void goActivity(Context _this,Class<? extends Activity> _class){
        intent.setClass(_this,_class);
        _this.startActivity(intent);
        _this = null;
    }
}

package com.lxy.packetcapture.widget;

import android.app.Activity;
import android.app.AliasActivity;
import android.content.Context;
import android.content.res.TypedArray;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lxy.packetcapture.R;

/**
 * created by 李昕怡 on 2019/9/9
 */

public class TitleBar extends LinearLayout {
    private ImageView iv_left;
    private TextView tv_left;
    private ImageView iv_right;
    private OnViewClickListener mOnviewClick;

    public TitleBar(Context context) {
        this(context,null);
    }

    public TitleBar(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public TitleBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs,defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs,int defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.layout_titlebar, this);
        iv_left = findViewById(R.id.back);
        tv_left = findViewById(R.id.title_text);
        iv_right = findViewById(R.id.iv_right);

        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.TitleBar,defStyleAttr,0);
        int count = array.getIndexCount();
        for(int i=0;i<count;i++){
            int attr = array.getIndex(i);
            switch (attr){
                case R.styleable.TitleBar_leftText:
                    tv_left.setText(array.getString(attr));
                    break;
                case R.styleable.TitleBar_rightImage:
                    iv_right.setImageResource(array.getResourceId(attr,0));
                    break;
            }
        }
        array.recycle();

        iv_left.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity)getContext()).finish();
            }
        });


        iv_right.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnviewClick.onRightImage();
            }
        });
    }

    public void setOnviewClick(OnViewClickListener viewClick){
        this.mOnviewClick = viewClick;
    }

    public TitleBar setTitleText(String title){
        if(tv_left != null && TextUtils.isEmpty(title)){
            tv_left.setText(title);
        }
        return this;
    }

    public TitleBar setRightImage(int imageId){
        if(iv_right != null){
            iv_right.setImageResource(imageId);
        }
        return this;
    }

    public TitleBar isRightImageVisible(boolean show){
        if(show == true){
            iv_right.setVisibility(VISIBLE);
        }else {
            iv_right.setVisibility(GONE);
        }
        return this;
    }

    public interface OnViewClickListener{
        void onRightImage();
    }
}

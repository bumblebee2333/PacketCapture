package com.lxy.packetcapture.base;

/**
 * author:lxy
 * date:2019/8/8
 * ps:采用MVP架构 这里主要判断在activity加载时 view是否被加载完成
 * 以免出现网络加载较慢等view还未加载出或被销毁等情况
 */


public abstract class BasePresenter<T extends BaseView> {
    protected T mView;

    public void attachView(T view){
        this.mView = view;
    }

    public void detachView(){
        mView = null;
    }

    protected boolean isViewAttached(){
        return mView != null;
    }
}

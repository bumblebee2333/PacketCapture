package com.lxy.packetcapture.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        super(context);
    }
    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //计算父容器的宽高和测量模式
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        //计算在wrap_content下的宽度 高度
        int width = 0;
        int height = 0;

        //每一行的宽度和高度
        int lineWidth = 0;
        int lineHeight = 0;

        int count = getChildCount();

        for(int i=0;i<count;i++){
            View child = getChildAt(i);
            //测量每一个child的宽和高
            measureChild(child,widthMeasureSpec,heightMeasureSpec);
            //获取child的margin
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            //获取child宽高
            int childWith = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            if(lineWidth + childWith > sizeWidth){
                width = Math.max(lineWidth,childWith);
                //叠加当前高度
                height += lineHeight;
                //开启下一行
                lineHeight = childHeight;
                lineWidth = childWith;
            }else {
                lineWidth += childWith;
                lineHeight = Math.max(lineHeight,childHeight);
            }

            //如果是最后一个子view
            if(i == count - 1){
                width = Math.max(lineWidth,childWith);
                height += lineHeight;
            }
        }
        setMeasuredDimension((widthMode == MeasureSpec.EXACTLY)?sizeWidth:width,
                (heightMode == MeasureSpec.EXACTLY)?sizeHeight:height);
    }

    //把每一行的view存起来
    List<List<View>> allViews = new ArrayList<>();
    //存储每一行的最大高度
    List<Integer> mLineHeight = new ArrayList<>();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        allViews.clear();
        mLineHeight.clear();

        //获取父容器的宽度
        int width = getWidth();

        int lineWidth = 0;
        int lineHeight = 0;
        List<View> lineViews = new ArrayList<>();
        int count = getChildCount();

        //先将每一行的view进行计算布局 存到一个List中
        for(int i=0;i<count;i++){
            View child = getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

            if(childWidth + lp.leftMargin + lp.rightMargin + lineWidth > width){
                mLineHeight.add(lineHeight);
                allViews.add(lineViews);
                lineWidth = 0;
                lineViews = new ArrayList<>();
            }

            lineHeight = Math.max(lineHeight,childHeight+lp.topMargin+lp.bottomMargin);
            lineWidth += childWidth + lp.leftMargin+lp.rightMargin;
            lineViews.add(child);
        }
        //存储最后一行
        mLineHeight.add(lineHeight);
        allViews.add(lineViews);

        //根据view的四个角的位置来确定在父容器中摆放的位置
        int left = 0;
        int top = 0;
        //获取view行数
        int lines = allViews.size();
        for(int i=0;i<lines;i++){
            //每一行的所以views
            lineViews = allViews.get(i);
            //每一行的views所对应的高度
            lineHeight = mLineHeight.get(i);

            for(int j=0;j<lineViews.size();j++){
                View child = lineViews.get(j);

                if(child.getVisibility() == GONE){
                    continue;
                }

                MarginLayoutParams lp = (MarginLayoutParams)child.getLayoutParams();

                int childLeft = left + lp.leftMargin;
                int childTop = top + lp.topMargin;
                int childRight = childLeft + child.getMeasuredWidth();
                int childBottom = childTop +  child.getMeasuredHeight();

                child.layout(childLeft,childTop,childRight,childBottom);

                left += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            }
            left = 0;
            top += lineHeight;
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(),attrs);
    }
}

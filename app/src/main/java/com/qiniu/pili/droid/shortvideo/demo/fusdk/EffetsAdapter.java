package com.qiniu.pili.droid.shortvideo.demo.fusdk;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.faceunity.beautycontrolview.entity.Effect;
import com.qiniu.pili.droid.shortvideo.demo.R;

import java.util.List;


/**
 * Created by ly on 18/7/5.
 * 滤镜，mv   adapter,
 */

public class EffetsAdapter extends BaseQuickAdapter<Effect, BaseViewHolder> {
    private Context mContext;
    private ItemTouchListener touchListener;

    public EffetsAdapter(Context context, @Nullable List<Effect> data) {
        super(R.layout.record_item_filter, data);
        this.mContext = context;
    }

    @Override
    protected void convert(final BaseViewHolder helper, Effect item) {
        helper.setText(R.id.record_text, item.description());
        helper.getView(R.id.record_rootView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (touchListener != null) {
                    return touchListener.touch(v, event, helper.getAdapterPosition());
                }

                return false;
            }
        });
        ImageView circleImageView = helper.getView(R.id.record_image);
        circleImageView.setBackgroundResource(item.resId());
    }

    public void setItemTouchListener(ItemTouchListener itemTouchListener) {
        this.touchListener = itemTouchListener;
    }

    public interface ItemTouchListener {
        boolean touch(View v, MotionEvent event, int position);
    }
}

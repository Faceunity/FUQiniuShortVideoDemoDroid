package com.faceunity.wrapper;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by lirui on 2017/4/14.
 */

public class EffectButtonRecyclerViewAdapter extends RecyclerView.Adapter<EffectButtonRecyclerViewAdapter.ItemViewHolder> {

    private String[] BTN_TEXT_ARRAY = {"Animoji", "3D道具", "2D道具", "换脸", "Avatar", "魔幻背景", "手势识别", "滤镜特效", };
    private int[] GROUP_TYPE_ARRAY = {
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_ANIMOJI,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_3D,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_2D,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_CHANGE_FACE,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_AVATAR,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_MAGIC,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_GESTURE,
            EffectRecycleViewAdapter.EFFECT_GROUP_ID_FILTER,};

    private EffectGroupBtnOnClickListener effectGroupBtnOnClickListener;
    private int lastClickPosition = 0;
    private Button lastClickBtn = null;

    private RecyclerView effectGroupButtonRecyclerView;
    private Context context;

    public EffectButtonRecyclerViewAdapter(RecyclerView recyclerView, Context context) {
        effectGroupButtonRecyclerView = recyclerView;
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return BTN_TEXT_ARRAY.length;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
          //      LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //setLayoutParams(params);
        View viewRoot = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyle_button_item, null, true);
        return new ItemViewHolder(viewRoot);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        final int adapterPosition = holder.getAdapterPosition();
        holder.btn.setText(BTN_TEXT_ARRAY[adapterPosition]);
        if (adapterPosition == lastClickPosition) {
            holder.btn.setTextColor(context.getResources().getColor(R.color.egg_yellow));
            lastClickBtn = holder.btn;
        } else holder.btn.setTextColor(context.getResources().getColor(R.color.white));

        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapterPosition >= 0 && adapterPosition < GROUP_TYPE_ARRAY.length) {
                    if (lastClickPosition != adapterPosition) {
                        if (lastClickPosition >= 0 && lastClickBtn != null) {
                            lastClickBtn.setTextColor(context.getResources().getColor(R.color.white));
                        }
                        lastClickPosition = adapterPosition;
                        lastClickBtn = holder.btn;
                        holder.btn.setTextColor(context.getResources().getColor(R.color.egg_yellow));
                    }
                    if (effectGroupBtnOnClickListener != null) {
                        effectGroupBtnOnClickListener.onClick(GROUP_TYPE_ARRAY[holder.getAdapterPosition()]);
                    }
                }
            }
        });
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        Button btn;
        ItemViewHolder(View itemView) {
            super(itemView);
            btn = (Button) itemView;
        }
    }

    public void setEffectGroupBtnOnClickListener(EffectGroupBtnOnClickListener effectGroupBtnOnClickListener) {
        this.effectGroupBtnOnClickListener = effectGroupBtnOnClickListener;
    }

    public interface EffectGroupBtnOnClickListener {
        void onClick(int groupID);
    }
}

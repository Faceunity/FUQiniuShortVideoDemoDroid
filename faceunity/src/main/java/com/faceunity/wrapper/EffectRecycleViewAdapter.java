package com.faceunity.wrapper;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lirui on 2016/10/19.
 */

public class EffectRecycleViewAdapter extends RecyclerView.Adapter<EffectRecycleViewAdapter.ItemViewHolder>{

    public static final String TAG = "EffectRecycleViewAdapter";

    /*first one must be null effect*/
    public static final int[][] EFFECT_ITEM_RES_ARRAY = new int[0][];
//    = {
//            /*animoji*/
//            {R.drawable.ic_delete_all, R.drawable.xiongmao_icon},
//
//            /*3D*/
//            {R.drawable.ic_delete_all, R.drawable.sdx2, R.drawable.itd},
//
//            /*2D*/
//            {R.drawable.ic_delete_all,
//                    R.drawable.caituzi_zh_fu, R.drawable.lhudie_zh_fu},
//
//            /*face change*/
//            {R.drawable.ic_delete_all, R.drawable.afd, R.drawable.baozi},
//
//            /*avatar*/
//            {R.drawable.ic_delete_all, R.drawable.huli_icon},
//
//            /*magic*/
//            {R.drawable.ic_delete_all, R.drawable.hez_ztt_fu, R.drawable.xiandai_ztt_fu},
//
//            /*gesture*/
//            {R.drawable.ic_delete_all,R.drawable.fu_ztt_live520, R.drawable.fu_zh_baoquan},
//
//            /*filter*/
//            {R.drawable.ic_delete_all, R.drawable.gradient, R.drawable.moive},
//    };

    public static final int RECYCLEVIEW_TYPE_EFFECT = 0;
    public static final int RECYCLEVIEW_TYPE_FILTER = 1;
    private int mRecycleViewType;

    private RecyclerView mRecycleView;
    private int mLastClickPosition;
    private EffectItemView lastClickItemView;

    /*this id is corresponding to the array index*/
    public static final int EFFECT_GROUP_ID_FIRST = 0;
    public static final int EFFECT_GROUP_ID_ANIMOJI = 0; //Animoji
    public static final int EFFECT_GROUP_ID_3D = 1; //3D道具
    public static final int EFFECT_GROUP_ID_2D = 2; //2D道具
    public static final int EFFECT_GROUP_ID_CHANGE_FACE = 3; //换脸
    public static final int EFFECT_GROUP_ID_AVATAR = 4; //Avatar
    public static final int EFFECT_GROUP_ID_MAGIC = 5; //魔幻背景
    public static final int EFFECT_GROUP_ID_GESTURE = 6; //手势识别
    public static final int EFFECT_GROUP_ID_FILTER = 7; //滤镜特效

    //public static final int EFFECT_GROUP_ID_WANSHENG = 1 + 1;

    public static final int GROUP_ID_NONE = 0;

    private int mEffectGroupId = 0;
    private boolean isCurrentGroupHasItemSelected = true;
    private int lastHasItemSelectedGroup = 0;

    public EffectRecycleViewAdapter(RecyclerView recyclerView, int recycleViewType, int effectGroupId) {
        super();

        mRecycleView = recyclerView;
        mRecycleViewType = recycleViewType;

        mEffectGroupId = effectGroupId;

        clickDefaultPosition();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(new EffectItemView(parent.getContext(), mRecycleViewType));
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        final int adapterPosition = holder.getAdapterPosition();
        //decide the border by click state
        if ((isCurrentGroupHasItemSelected || lastHasItemSelectedGroup == mEffectGroupId)
                && mLastClickPosition == adapterPosition) {
            holder.mItemView.setBackgroundSelected();
            lastClickItemView = holder.mItemView;
            mLastClickPosition = adapterPosition;
        } else {
            holder.mItemView.setBackgroundUnSelected();
        }
        //deal the resource image to present
        if (mRecycleViewType == RECYCLEVIEW_TYPE_EFFECT) {
            holder.mItemView.mItemIcon.setImageResource(EFFECT_ITEM_RES_ARRAY[mEffectGroupId][
                    position % EFFECT_ITEM_RES_ARRAY[mEffectGroupId].length]);
        }

        holder.mItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCurrentGroupHasItemSelected = true;
                lastHasItemSelectedGroup = mEffectGroupId;

                if (mOnItemSelectedListener != null) {
                    mOnItemSelectedListener.onItemSelected(adapterPosition, mRecycleViewType, mEffectGroupId);
                }
                if (mLastClickPosition != adapterPosition) {
                    if (lastClickItemView != null) lastClickItemView.setBackgroundUnSelected();
                    lastClickItemView = holder.mItemView;
                }
                mLastClickPosition = adapterPosition;
                holder.mItemView.setBackgroundSelected();
                setClickPosition(adapterPosition);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return EFFECT_ITEM_RES_ARRAY[mEffectGroupId].length;
    }

    /**
     * restore click related state int list
     * @param position
     */
    private void setClickPosition(int position) {
        isCurrentGroupHasItemSelected = true;
        mLastClickPosition = position;
    }

    private void clickDefaultPosition() {
        isCurrentGroupHasItemSelected = true;
        lastHasItemSelectedGroup = mEffectGroupId;
        if (mRecycleViewType == RECYCLEVIEW_TYPE_EFFECT) {
            //default effect select item is 1
            setClickPosition(1);
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(1, mRecycleViewType, mEffectGroupId);
            }
        } else {
            //default filter select item is 0
            setClickPosition(0);
            if (mOnItemSelectedListener != null) {
                mOnItemSelectedListener.onItemSelected(0, mRecycleViewType, mEffectGroupId);
            }
        }

    }

    public void setEffectGroupId(int effectGroupId, boolean clickDefaultPosition) {
        isCurrentGroupHasItemSelected = false;
        mEffectGroupId = effectGroupId;
        if (clickDefaultPosition) clickDefaultPosition();
    }

    public int getEffectGroupId() {
        return mEffectGroupId;
    }

    private OnItemSelectedListener mOnItemSelectedListener;

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        mOnItemSelectedListener = onItemSelectedListener;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(int itemPosition, int recycleViewType, int effectGroupId);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        EffectItemView mItemView;
        ItemViewHolder(View itemView) {
            super(itemView);
            mItemView = (EffectItemView) itemView;
        }
    }
}

package com.faceunity.wrapper;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

/**
 * Created by lirui on 2017/1/20.
 */

public class EffectAndFilterSelectAdapter extends RecyclerView.Adapter<EffectAndFilterSelectAdapter.ItemViewHolder> {

    public static final String[] AVATAR_EFFECT = {"xiongmao2.bundle", "nick.bundle"};

    public static final int[] EFFECT_ITEM_RES_ARRAY = {
            R.mipmap.ic_delete_all, R.drawable.xiongmao_icon, R.drawable.sdx2, R.drawable.itd,
            R.drawable.caituzi_zh_fu, R.drawable.lhudie_zh_fu, R.drawable.afd, R.drawable.baozi,
            R.drawable.huli_icon, R.drawable.hez_ztt_fu, R.drawable.xiandai_ztt_fu,
            R.drawable.fu_ztt_live520, R.drawable.fu_zh_baoquan, R.drawable.gradient, R.drawable.moive
    };
    public static final String[] EFFECT_ITEM_FILE_NAME = {"none", "xiongmao2.bundle", "sdx2.bundle", "itd.bundle",
            "multi_dianji_crt_fu.bundle", "lhudie_zh_fu.bundle",
            "afd.bundle", "baozi.mp3", "nick.bundle",
            "hez_ztt_fu.bundle", "xiandai_ztt_fu.bundle", "fu_ztt_live520.bundle", "fu_zh_baoquan.bundle", "f_gradient.bundle", "f_movie.bundle"};

    public static final int[] FILTER_ITEM_RES_ARRAY = {
            R.mipmap.nature, R.mipmap.delta, R.mipmap.electric, R.mipmap.slowlived, R.mipmap.tokyo, R.mipmap.warm
    };
    public final static String[] FILTERS_NAME = {"origin", "delta", "electric", "slowlived", "tokyo", "warm"};

    public static final int[] BEAUTY_FILTER_ITEM_RES_ARRAY = {
            R.mipmap.origin, R.mipmap.qingxin, R.mipmap.shaonv, R.mipmap.ziran, R.mipmap.hongrun
    };
    public final static String[] BEAUTY_FILTERS_NAME = {"ziran", "danya", "fennen", "qingxin", "hongrun"};
    public final static String[] BEAUTY_FILTERS_NAME_SHOW = {"自然", "淡雅", "粉嫩", "清新", "红润"};

    public static final int RECYCLEVIEW_TYPE_EFFECT = 0;
    public static final int RECYCLEVIEW_TYPE_FILTER = 1;
    public static final int RECYCLEVIEW_TYPE_BEAUTY_FILTER = 2;

    private RecyclerView mOwnerRecyclerView;
    private int mOwnerRecyclerViewType;

    private final int DEFAULT_CLICK_POSITION = 1;
    private EffectAndFilterItemView lastClickItemView = null;
    private int lastClickEffectPosition = DEFAULT_CLICK_POSITION;
    private int lastClickFilterPosition = DEFAULT_CLICK_POSITION;
    private OnItemSelectedListener mOnItemSelectedListener;

    private int[] filterLevels = new int[FILTERS_NAME.length + BEAUTY_FILTERS_NAME.length];

    {
        Arrays.fill(filterLevels, 100);
    }

    public EffectAndFilterSelectAdapter(RecyclerView recyclerView, int recyclerViewType) {
        mOwnerRecyclerView = recyclerView;
        mOwnerRecyclerViewType = recyclerViewType;
    }

    @Override
    public int getItemCount() {
        switch (mOwnerRecyclerViewType) {
            case RECYCLEVIEW_TYPE_EFFECT:
                return EFFECT_ITEM_RES_ARRAY.length;
            case RECYCLEVIEW_TYPE_FILTER:
                return FILTER_ITEM_RES_ARRAY.length;
            case RECYCLEVIEW_TYPE_BEAUTY_FILTER:
                return BEAUTY_FILTER_ITEM_RES_ARRAY.length;
        }
        return 0;
    }

    public String getHintStringByPosition(int position) {
        String res = "";
        if (EFFECT_ITEM_RES_ARRAY[position] == R.mipmap.mood) {
            res = "嘴角向上或嘴角向下";

        } else if (EFFECT_ITEM_RES_ARRAY[position] == R.mipmap.fu_zh_duzui) {
            res = "嘟嘴";

        }
        return res;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        final int adapterPosition = holder.getAdapterPosition();

        int lastClickPosition = -1;
        holder.mItemView.setItemType(mOwnerRecyclerViewType);
        switch (mOwnerRecyclerViewType) {
            case RECYCLEVIEW_TYPE_EFFECT:
                holder.mItemView.setItemIcon(EFFECT_ITEM_RES_ARRAY[adapterPosition % EFFECT_ITEM_RES_ARRAY.length]);
                lastClickPosition = lastClickEffectPosition;
                break;
            case RECYCLEVIEW_TYPE_FILTER:
                holder.mItemView.setItemIcon(FILTER_ITEM_RES_ARRAY[adapterPosition % FILTER_ITEM_RES_ARRAY.length]);
                holder.mItemView.setItemText(FILTERS_NAME[adapterPosition % FILTER_ITEM_RES_ARRAY.length].toUpperCase());
                lastClickPosition = lastClickFilterPosition;
                break;
            case RECYCLEVIEW_TYPE_BEAUTY_FILTER:
                holder.mItemView.setItemIcon(BEAUTY_FILTER_ITEM_RES_ARRAY[adapterPosition % BEAUTY_FILTER_ITEM_RES_ARRAY.length]);
                holder.mItemView.setItemText(BEAUTY_FILTERS_NAME_SHOW[adapterPosition % BEAUTY_FILTER_ITEM_RES_ARRAY.length].toUpperCase());
                lastClickPosition = lastClickFilterPosition - FILTER_ITEM_RES_ARRAY.length;
                break;
        }

        if (adapterPosition == lastClickPosition) {
            holder.mItemView.setSelectedBackground();
            lastClickItemView = holder.mItemView;
        } else holder.mItemView.setUnselectedBackground();

        holder.mItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastClickItemView != null) lastClickItemView.setUnselectedBackground();
                lastClickItemView = holder.mItemView;
                switch (mOwnerRecyclerViewType) {
                    case RECYCLEVIEW_TYPE_EFFECT:
                        lastClickEffectPosition = adapterPosition;
                        break;
                    case RECYCLEVIEW_TYPE_FILTER:
                        lastClickFilterPosition = adapterPosition;
                        break;
                    case RECYCLEVIEW_TYPE_BEAUTY_FILTER:
                        lastClickFilterPosition = adapterPosition + FILTER_ITEM_RES_ARRAY.length;
                        break;
                }
                holder.mItemView.setSelectedBackground();
                setClickPosition(adapterPosition);
            }
        });
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(new EffectAndFilterItemView(parent.getContext()));
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        EffectAndFilterItemView mItemView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mItemView = (EffectAndFilterItemView) itemView;
        }
    }

    private void setClickPosition(int position) {
        if (position < 0) {
            return;
        }
        if (mOnItemSelectedListener != null) {
            switch (mOwnerRecyclerViewType) {
                case RECYCLEVIEW_TYPE_EFFECT:
                    mOnItemSelectedListener.onEffectItemSelected(position);
                    break;
                case RECYCLEVIEW_TYPE_FILTER:
                    mOnItemSelectedListener.onFilterItemSelected(position, filterLevels[position]);
                    break;
                case RECYCLEVIEW_TYPE_BEAUTY_FILTER:
                    mOnItemSelectedListener.onBeautyFilterItemSelected(position, filterLevels[position + FILTER_ITEM_RES_ARRAY.length]);
                    break;
            }
        }
    }

    public interface OnItemSelectedListener {
        void onEffectItemSelected(int itemPosition);

        void onFilterItemSelected(int itemPosition, int filterLevel);

        void onBeautyFilterItemSelected(int itemPosition, int filterLevel);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.mOnItemSelectedListener = onItemSelectedListener;
    }

    public void setOwnerRecyclerViewType(int ownerRecyclerViewType) {
        mOwnerRecyclerViewType = ownerRecyclerViewType;
        notifyDataSetChanged();
    }

    public void setFilterLevels(int filterLevel) {
        filterLevels[lastClickFilterPosition] = filterLevel;
    }
}

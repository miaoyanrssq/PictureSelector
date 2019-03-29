package com.luck.picture.lib.adapter;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.luck.picture.lib.R;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.List;

public class BottomPreviewAdapter extends RecyclerView.Adapter<BottomPreviewAdapter.CustomeHolder> {

    private Context context;
    private List<LocalMedia> imagesList;

    public interface OnItemClcikLitener {
        void OnItemClcik(int position, LocalMedia image);
    }

    public OnItemClcikLitener onItemClcikLitener;

    public void setOnItemClcikLitener(OnItemClcikLitener onItemClcikLitener) {
        this.onItemClcikLitener = onItemClcikLitener;
    }

    public interface OnDataChangeFinishListener {
        void changeFinish();
    }

    public OnDataChangeFinishListener onDataChangeFinishListener;

    public void setOnDataChangeFinishListener(OnDataChangeFinishListener onDataChangeFinishListener) {
        this.onDataChangeFinishListener = onDataChangeFinishListener;
    }

    public BottomPreviewAdapter(Context context, List<LocalMedia> imagesList) {
        this.context = context;
        this.imagesList = imagesList;
    }


    @NonNull
    @Override
    public CustomeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CustomeHolder(LayoutInflater.from(context).inflate(R.layout.bootm_preview_item, parent, false));
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(@NonNull final CustomeHolder holder, int position) {
        imagesList.get(position).setSelectPosition(position);
        Glide.with(context).load(imagesList.get(holder.getAdapterPosition()).getPath())
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE)
                        .centerCrop()
                        .override(800, 800))
                .thumbnail(0.5f)
                .into(holder.imageView);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (LocalMedia image : imagesList) {
                    image.setChecked(false);
                }
                imagesList.get(holder.getAdapterPosition()).setChecked(true);
                if (onItemClcikLitener != null) {
                    onItemClcikLitener.OnItemClcik(holder.getAdapterPosition(),imagesList.get(holder.getAdapterPosition()));
                }
            }
        });
        if (imagesList.get(position).isChecked()) {
            holder.imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.border));
        } else {
            holder.imageView.setBackground(null);
        }
    }

    @Override
    public int getItemCount() {
        return imagesList.size();
    }

    class CustomeHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public CustomeHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bottom_imageview_item);
        }
    }

    public void referesh(List<LocalMedia> newData) {
        this.imagesList = newData;
        notifyDataSetChanged();
    }
}

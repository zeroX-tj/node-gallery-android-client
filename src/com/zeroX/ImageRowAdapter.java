package com.zeroX;

import java.util.List;

import android.graphics.Bitmap;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

public class ImageRowAdapter extends ArrayAdapter<Image> {

    private Activity activity;
    private List<Image> items;
    private Image objBean;
    private int row;
    private DisplayImageOptions defaultOptions;

    public ImageRowAdapter(Activity act, int resource, List<Image> arrayList) {
        super(act, resource, arrayList);
        this.activity = act;
        this.row = resource;
        this.items = arrayList;



    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(row, null);

            holder = new ViewHolder();
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        if ((items == null) || ((position + 1) > items.size()))
            return view;

        objBean = items.get(position);

        holder.pbar = (ProgressBar) view.findViewById(R.id.pbar);
        holder.imgView = (ImageView) view.findViewById(R.id.image);

        if (holder.imgView != null) {
            if (null != objBean.getLink()
                    && objBean.getLink().trim().length() > 0) {
                final ProgressBar pbar = holder.pbar;

                ImageLoader.getInstance().displayImage(objBean.getLink(), holder.imgView, new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
                        pbar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        pbar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        pbar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {
                        pbar.setVisibility(View.INVISIBLE);
                    }
                });

            } else {
                holder.imgView.setImageResource(R.drawable.placeholder);
            }
        }

        return view;
    }

    public class ViewHolder {

        public TextView tvTitle, tvDesc, tvDate;
        private ImageView imgView;
        private ProgressBar pbar;

    }

}


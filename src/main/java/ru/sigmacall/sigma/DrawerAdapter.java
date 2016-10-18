package ru.sigmacall.sigma.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.sigmacall.sigma.R;

public class DrawerAdapter extends ArrayAdapter<DrawerListItem> implements Animation.AnimationListener {
    Context mContext;
    ImageView bonusImage;
    Activity activity;

    public DrawerAdapter(Context context, Activity activity, int resource, List<DrawerListItem> objects) {
        super(context, resource, objects);
        this.mContext = context;
        this.bonusImage = null;
        this.activity = activity;
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    private class ViewHolder {
        ImageView imageView;
        TextView title;
        TextView bonusText;
        ImageView bonusImage;
    }

    Animation animation;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        DrawerListItem rowItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) mContext
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.drawer_list_item, null);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.tvDrawerItem);
            holder.imageView = (ImageView) convertView.findViewById(R.id.ivDrawerItem);

            if (position == 0) {
                holder.bonusText = (TextView)convertView.findViewById(R.id.tvBonus);

                bonusImage = (ImageView) convertView.findViewById(R.id.ivBonus);
                bonusImage.setVisibility(View.VISIBLE);

                animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
                bonusImage.startAnimation(animation);
            }

            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.title.setText(rowItem.getTitle());
        holder.imageView.setImageResource(rowItem.getImageId());


        return convertView;
    }
}

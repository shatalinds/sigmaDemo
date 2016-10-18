package ru.sigmacall.sigma.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import ru.sigmacall.sigma.AppConf;
import ru.sigmacall.sigma.MainActivity;
import ru.sigmacall.sigma.R;
import ru.sigmacall.sigma.db.History;
import ru.sigmacall.sigma.db.HistoryDataSource;
import ru.sigmacall.sigma.tools.DbSearcher;
import ru.sigmacall.sigma.tools.NumFormat;

public class HistoryAdapter extends SimpleCursorAdapter {

    public static final String TAG = "HistoryAdapter: ";

    NumFormat fmt = new NumFormat();
    DbSearcher dbs;
    Context context;

    public HistoryAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c, new String[]{}, new int[]{}, 0);
        dbs = new DbSearcher(context);
        this.context = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_list_item, null);

        TextView tvNum = (TextView) view.findViewById(R.id.tv_history_phone);
        TextView tvArea = (TextView) view.findViewById(R.id.tv_history_country);
        TextView tvTime = (TextView) view.findViewById(R.id.tv_history_time);
        TextView tvPrice = (TextView) view.findViewById(R.id.tv_history_price);
        ImageView ivFlag = (ImageView) view.findViewById(R.id.iv_history_flag);

        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/rur.ttf");
        TextView tvRub = (TextView) view.findViewById(R.id.tv_history_rub);

        tvRub.setTypeface(typeface);

        view.setTag(R.id.tv_history_phone, tvNum);
        view.setTag(R.id.tv_history_country, tvArea);
        view.setTag(R.id.tv_history_time, tvTime);
        view.setTag(R.id.tv_history_price, tvPrice);
        view.setTag(R.id.iv_history_flag, ivFlag);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        History history = HistoryDataSource.cursorToHistory(cursor);
        String phoneNum = history.getPhoneNum();
        ((TextView) view.getTag(R.id.tv_history_phone)).setText(fmt.formatNumber(phoneNum));
        int[] countryCreds = MainActivity.getCountry(context, phoneNum);
        ((TextView) view.getTag(R.id.tv_history_country)).setText(
                context.getString(countryCreds[0])
        );
        ((TextView) view.getTag(R.id.tv_history_price))
                .setText(fmt.formatFloat(history.getPrice()));

        ((ImageView) view.getTag(R.id.iv_history_flag)).setImageResource(countryCreds[1]);

        // Fill name if exists
        String uri = history.getContactUri();
        if (uri != null) {
            ((TextView) view.getTag(R.id.tv_history_phone))
                    .setText(dbs.getContactNameByUri(Uri.parse(uri)));
        }

        String dt = history.getTs();
        ((TextView) view.getTag(R.id.tv_history_time)).setText(AppConf.getTime(dt));
    }
}

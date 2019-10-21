package ml.dukan.stores.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ml.dukan.stores.Models.MainDrawerItem;
import ml.dukan.stores.R;

/**
 * Created by khaled on 10/07/17.
 */
public class MainDrawerAdapter extends ArrayAdapter<MainDrawerItem> {

    LayoutInflater inflater;

    public MainDrawerAdapter(Context context, int resource, List<MainDrawerItem> objects) {
        super(context, resource, objects);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    class ViewHolder {
        TextView title_tv;
        ImageView icon_iv;
    }
    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null){
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.item_main_drawer, parent, false);
            holder.title_tv  = (TextView) convertView.findViewById(R.id.item_main_drawer_title);
            holder.icon_iv = (ImageView) convertView.findViewById(R.id.item_main_drawer_icon);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        MainDrawerItem item = getItem(position);

        holder.title_tv.setText(item.title);
        holder.icon_iv.setImageResource(item.drawable_resource);
        return convertView;
    }
}

package com.noodle.testa3appv2;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class BubbleAdapter extends ArrayAdapter<MessageBubble>
{
    private Activity activity;
    private List<MessageBubble> messages;

    public BubbleAdapter(Activity context, int resource, List<MessageBubble> objects)
    {
        super(context, resource, objects);
        activity = context;
        messages = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        int layoutResource = 0;
        MessageBubble messageBubble = getItem(position);
        int viewType = getItemViewType(position);

        if(messageBubble.isOutgoing())
        {
            layoutResource = R.layout.sent_message;
        }
        else
        {
            layoutResource = R.layout.received_message;
        }

        if(convertView != null)
        {
            holder = (ViewHolder) convertView.getTag();
        }
        else
        {
            convertView = inflater.inflate(layoutResource, parent,false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }

        holder.msg.setText(messageBubble.getMessageBody());

        return convertView;
    }

    @Override
    public int getViewTypeCount()
    {
        return 2;
    }

    //I think this is where which side each thing goes is decided? Will need updating.
    @Override
    public int getItemViewType(int position)
    {
        MessageBubble m = (MessageBubble) getItem(position);

        int ret = (m.isOutgoing()) ? 0 : 1;

        return ret;
    }

    private class ViewHolder
    {
        private TextView msg;//TextView?

        public ViewHolder(View v)
        {
            msg = (TextView) v.findViewById(R.id.txt_msg);
        }
    }
}

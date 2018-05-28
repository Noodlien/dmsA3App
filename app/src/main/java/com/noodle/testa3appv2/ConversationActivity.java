package com.noodle.testa3appv2;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ConversationActivity extends AppCompatActivity {

    private ArrayAdapter arrayAdapter;
    private ArrayList<MessageBubble> testBubbleList = new ArrayList<MessageBubble>();
    private ListView conversation;
    private String conversationId;
    private SmsManager smsManager = SmsManager.getDefault();
    private String phoneNumber;
    private EditText input;
    private static ConversationActivity inst;
    private static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        conversationId = (String) this.getIntent().getExtras().get("threadId");
        phoneNumber = (String) this.getIntent().getExtras().get("phoneNumber");

        conversation = findViewById(R.id.conversation);
        arrayAdapter = new BubbleAdapter(this, R.layout.received_message, testBubbleList);
        conversation.setAdapter(arrayAdapter);

        input = findViewById(R.id.input);

        //refreshConversation(conversationId);
        refreshConversation();
    }

    public void onSendClick(View view)
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            getPermissionToSendSMS();
        }
        else
        {
            smsManager.sendTextMessage(phoneNumber, null, input.getText().toString(), null, null);
            Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
        }

        arrayAdapter.add(new MessageBubble(input.getText().toString(), false));
        arrayAdapter.notifyDataSetChanged();
        conversation.smoothScrollToPosition(arrayAdapter.getCount() - 1);
    }

    //public void refreshConversation(String name)
    public void refreshConversation()
    {
        //String[] args = {name};
        String[] args = {conversationId};
        ContentResolver contentResolver = getContentResolver();
        Cursor inboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, "thread_id = ?", args, Telephony.Sms.Outbox.DATE);
        Cursor outboxCursor = contentResolver.query(Uri.parse("content://sms/sent"), null, "thread_id = ?", args, Telephony.Sms.Outbox.DATE_SENT);

        int indexBodyInbox = inboxCursor.getColumnIndex("body");
        int indexBodyOutbox = outboxCursor.getColumnIndex("body");
        int indexDateInbox = inboxCursor.getColumnIndex("date");
        int indexDateOutbox = outboxCursor.getColumnIndex("date");

        //TODO: This method will crash if either sent/inbox cursor is empty. Fix later.
        boolean inboxExists = inboxCursor.moveToFirst();
        boolean outboxExists = outboxCursor.moveToFirst();

        arrayAdapter.clear();

        while((inboxCursor.getPosition() < inboxCursor.getCount()) && (outboxCursor.getPosition() < outboxCursor.getCount()))
        {
            if(Long.parseLong(inboxCursor.getString(indexDateInbox)) < Long.parseLong(outboxCursor.getString(indexDateOutbox)))
            {
                arrayAdapter.add(new MessageBubble(inboxCursor.getString(indexBodyInbox), false));//Test Bubble Stuff
                inboxCursor.moveToNext();
            }
            else
            {
                arrayAdapter.add(new MessageBubble(outboxCursor.getString(indexBodyOutbox), true));//Test Bubble Stuff
                outboxCursor.moveToNext();
            }
        }

        //TODO: This bit sucks
        Cursor remainder = null;
        int remainderBody = 0;
        boolean remainderOutgoing = false;
        if(inboxCursor.getPosition() == inboxCursor.getCount())
        {
            remainder = outboxCursor;
            remainderBody = indexBodyOutbox;
            remainderOutgoing = true;
        }
        else if(outboxCursor.getPosition() == outboxCursor.getCount())
        {
            remainder = inboxCursor;
            remainderBody = indexBodyInbox;
        }

        if(remainder != null) {
            do {
                {
                    arrayAdapter.add(new MessageBubble(remainder.getString(remainderBody), remainderOutgoing));
                }
            }
            while (remainder.moveToNext());
        }

        conversation.smoothScrollToPosition(arrayAdapter.getCount() - 1);
    }

    public void getPermissionToSendSMS()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS))
            {
                Toast.makeText(this, "Allow permission, dick.", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, MainActivity.REQUEST_CODE);
        }
    }

    public static ConversationActivity instance()
    {
        return inst;
    }

    public void updateConversation(String message, String phoneNumber)
    {
        if(this.phoneNumber.equals(phoneNumber))
        {
            arrayAdapter.add(new MessageBubble(message, false));
            conversation.smoothScrollToPosition(arrayAdapter.getCount() - 1);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        inst = this;
        active = true;
    }

    @Override
    public void onStop()
    {
        super.onStop();
        active = false;
    }

    public static boolean isActive() {
        return active;
    }
}

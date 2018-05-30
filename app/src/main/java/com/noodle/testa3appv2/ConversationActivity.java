package com.noodle.testa3appv2;

import android.Manifest;
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

//This activity displays a particular conversation
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

    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        //Grabs the id of this conversation, and the phone number of the contact involved
        conversationId = (String) this.getIntent().getExtras().get("threadId");
        phoneNumber = (String) this.getIntent().getExtras().get("phoneNumber");

        //Setting up the left-right style chatbox display
        conversation = findViewById(R.id.conversation);
        arrayAdapter = new BubbleAdapter(this, R.layout.received_message, testBubbleList);
        conversation.setAdapter(arrayAdapter);

        //
        latitude = this.getIntent().getDoubleExtra("latitude", latitude);
        longitude = this.getIntent().getDoubleExtra("longitude", longitude);

        latitude += 1;
        longitude += 1;

        input = findViewById(R.id.input);

        refreshConversation();
    }

    public void onLocationClick(View view)
    {
        String message = "Latitude: " + latitude + " Longitude: " + longitude;

        //Check for permission. Ask if we don't have it.
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            getPermissionToSendSMS();
        }
        else
        {
            //Send message
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Location Message Sent", Toast.LENGTH_SHORT).show();
        }

        //Add sent message to display, scroll to bottom.
        arrayAdapter.add(new MessageBubble(message, false));
        arrayAdapter.notifyDataSetChanged();
        conversation.smoothScrollToPosition(arrayAdapter.getCount() - 1);
    }

    //Executes when send it clicked.
    public void onSendClick(View view)
    {
        //Check for permission. Ask if we don't have it.
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            getPermissionToSendSMS();
        }
        else
        {
            //Send message
            smsManager.sendTextMessage(phoneNumber, null, input.getText().toString(), null, null);
            Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
        }

        //Add sent message to display, scroll to bottom.
        arrayAdapter.add(new MessageBubble(input.getText().toString(), false));
        arrayAdapter.notifyDataSetChanged();
        conversation.smoothScrollToPosition(arrayAdapter.getCount() - 1);
    }

    //Grabs all the messages associated with this conversation
    public void refreshConversation()
    {
        String[] args = {conversationId};
        ContentResolver contentResolver = getContentResolver();
        //Gets all incoming messages
        Cursor inboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, "thread_id = ?", args, Telephony.Sms.Outbox.DATE);
        //Gets all outgoing messages
        Cursor outboxCursor = contentResolver.query(Uri.parse("content://sms/sent"), null, "thread_id = ?", args, Telephony.Sms.Outbox.DATE_SENT);

        //Index stuff...
        int indexBodyInbox = inboxCursor.getColumnIndex("body");
        int indexBodyOutbox = outboxCursor.getColumnIndex("body");
        int indexDateInbox = inboxCursor.getColumnIndex("date");
        int indexDateOutbox = outboxCursor.getColumnIndex("date");

        //TODO: This method will crash if either sent/inbox cursor is empty, I think? Fix later.
        boolean inboxExists = inboxCursor.moveToFirst();
        boolean outboxExists = outboxCursor.moveToFirst();

        //Make room for update
        arrayAdapter.clear();

        //Okay, so we've got an array of incoming messages, and an array of outgoing messages. Both are sorted chronologically.
        //So, we use the merge part of a merge-sort to combine them into the arrayAdapter.
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

        //Once one array is empty, tack the other on to the end.
        //This is a shitty implementation
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

        //Scroll down so the new message shows
        conversation.smoothScrollToPosition(arrayAdapter.getCount() - 1);
    }

    //Gets permission to send messages
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

    //Returns this instance. Used by MessageBroadcastReceiver
    public static ConversationActivity instance()
    {
        return inst;
    }

    //Sticks a new message in to the list of messages.
    //Needed because new messages are added to Android's message tables too slowly to just be able
    //to re-query when a new message arrives.
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

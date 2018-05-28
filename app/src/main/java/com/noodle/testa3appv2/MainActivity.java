package com.noodle.testa3appv2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity
{

    public static final int REQUEST_CODE = 1;
    private ArrayAdapter arrayAdapter;
    private ArrayList<Conversation> conversationList = new ArrayList<Conversation>();
    private ListView messages;
    private static MainActivity inst;
    private static boolean active = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, conversationList);
        messages = findViewById(R.id.messages);
        messages.setAdapter(arrayAdapter);

        messages.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long thing)
            {
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                Conversation con = (Conversation) adapter.getItemAtPosition(position);

                intent.putExtra("name", con.getName());
                intent.putExtra("threadId", con.getThread());
                intent.putExtra("phoneNumber", con.getPhoneNumber());
                intent.putExtra("messageBody", con.getMessageBody());

                startActivity(intent);
            }
        });

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            getPermissionToReadSMS();
            getPermissionToReadContacts();
        }
        else
        {
            refreshInbox();
        }
    }

    public void refreshInbox()
    {
        ContentResolver contentResolver = getContentResolver();
        Cursor convoCursor = contentResolver.query(Uri.parse("content://sms/conversations"), null, null, null, null);

        int indexSnippet = convoCursor.getColumnIndex("snippet");
        int indexThreadId = convoCursor.getColumnIndex("thread_id");

        if(!convoCursor.moveToFirst()) return;

        arrayAdapter.clear();

        do
        {
            String args[] = {convoCursor.getString(indexThreadId)};
            String cols[] = {"address", "date"};
            Cursor inboxCursor = contentResolver.query(Uri.parse("content://sms/"), cols, "thread_id = ?", args, null);

            int indexAddress = inboxCursor.getColumnIndex("address");
            int indexDate = inboxCursor.getColumnIndex("date");

            if(!inboxCursor.moveToFirst()) return;

            String phoneNum = inboxCursor.getString(indexAddress);
            String name = getContactName(this, phoneNum);
            String snippet = convoCursor.getString(indexSnippet);
            String date = inboxCursor.getString(indexDate);
            Conversation con = new Conversation(name, phoneNum, snippet, args[0], date);

            arrayAdapter.add(con);
        }
        while(convoCursor.moveToNext());

        arrayAdapter.sort(new ConversationComparator());
    }

    public static String getContactName(Context context, String phoneNumber)
    {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = contentResolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if(cursor == null)
        {
            return phoneNumber;
        }

        String name = phoneNumber;
        if(cursor.moveToFirst())
        {
            name = cursor.getString(cursor.getColumnIndex((ContactsContract.PhoneLookup.DISPLAY_NAME)));
        }

        if(cursor != null && !cursor.isClosed())
        {
            cursor.close();
        }

        return name;
    }

    public void getPermissionToReadSMS()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS))
            {
                Toast.makeText(this, "Allow permission, dick.", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUEST_CODE);
        }
    }

    public void getPermissionToReadContacts()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS))
            {
                Toast.makeText(this, "Allow permission, dick.", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE);
        }
    }

    public void newButtonClicked(View view)
    {
        Intent intent = new Intent(MainActivity.this, CreateConversationActivity.class);

        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {
        if(requestCode == REQUEST_CODE)
        {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
                refreshInbox();
            }
            else
            {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public static MainActivity instance()
    {
        return inst;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        inst = this;
        active = true;
        refreshInbox();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        active = false;
    }

    public static boolean isActive()
    {
        return active;
    }

    private class ConversationComparator implements Comparator<Conversation>
    {
        @Override
        public int compare(Conversation o1, Conversation o2) {
            return (int) (Float.parseFloat(o2.getDate()) - Float.parseFloat(o1.getDate()));
        }
    }

    private class Conversation
    {
        private String name;
        private String phoneNumber;
        private String messageBody;
        private String thread;
        private String date;

        public Conversation(String name, String phoneNumber, String messageBody, String thread, String date)
        {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.messageBody = messageBody;
            this.thread = thread;
            this.date = date;
        }

        public String getThread()
        {
            return thread;
        }

        public String getName() {
            return name;
        }

        public String getMessageBody() {
            return messageBody;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getDate() {
            return date;
        }

        @Override
        public String toString() {
            return (name + ":\n" + messageBody);
        }

    }
}

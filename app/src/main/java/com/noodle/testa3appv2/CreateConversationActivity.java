package com.noodle.testa3appv2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//Makes a new conversation. Messy and hacked together, just ignore it unless you're feeling brave.
public class CreateConversationActivity extends AppCompatActivity
{
    private ListView cc_contacts;
    private ArrayAdapter<ContactItem> arrayAdapter;
    private ArrayList<ContactItem> contactsList = new ArrayList<ContactItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_conversation);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactsList);
        cc_contacts = findViewById(R.id.cc_contacts);
        cc_contacts.setAdapter(arrayAdapter);

        cc_contacts.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long thing)
            {
                Intent intent = new Intent(CreateConversationActivity.this, ConversationActivity.class);

                ContactItem contactItem = (ContactItem) adapter.getItemAtPosition(position);

                String newNumber = contactItem.getNumber().replaceAll("-", "");
                newNumber = newNumber.replaceAll("\\s", "");

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(contactItem.getNumber(), null, "TEST", null, null);

                //Hax, could be fine-tuned
                try
                {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                catch(Exception e)
                {
                    System.out.println("Interrupted");
                }

                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(Telephony.Threads.CONTENT_URI, null, /*Telephony.Threads.RECIPIENT_IDS + " = " + contactItem.getId()*/
                        "address = " + newNumber, null, null);

                cursor.moveToFirst();

                String threadId = cursor.getString(cursor.getColumnIndex("thread_id"));
                intent.putExtra("name", contactItem.getName());
                intent.putExtra("threadId", threadId);
                intent.putExtra("phoneNumber", contactItem.getNumber());
                intent.putExtra("messageBody", "trololol");

                startActivity(intent);
            }
        });

        makeContactList();
    }

    //Gets list of contacts to choose from.
    private void makeContactList()
    {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID}, null, null, null);

        int indexName = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        int indexId = cursor.getColumnIndex(ContactsContract.Contacts._ID);

        if(!cursor.moveToFirst()) return;

        arrayAdapter.clear();

        do
        {
            String name = cursor.getString(indexName);
            String contactId = cursor.getString(indexId);
            String number = "Nope";

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(name));
            Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);

            int indexNumber = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            if(phoneCursor.moveToFirst())
            {
                number = phoneCursor.getString(indexNumber);
            }

            ContactItem contact = new ContactItem(name, number, contactId);
            arrayAdapter.add(contact);
            System.out.println(contact);
        }
        while(cursor.moveToNext());
    }

    public class ContactItem
    {
        private String name;
        private String number;
        private String id;

        public ContactItem(String name, String number, String id)
        {
            this.name = name;
            this.number = number;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }

        @Override
        public String toString()
        {
            return (name + "\n" + number);
        }
    }
}

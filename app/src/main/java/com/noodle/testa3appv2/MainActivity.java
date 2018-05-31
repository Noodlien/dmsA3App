package com.noodle.testa3appv2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Comparator;

//TODO close all cursors when done?
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{

    public static final int REQUEST_CODE = 1;
    private ArrayAdapter arrayAdapter;
    private ArrayList<Conversation> conversationList = new ArrayList<Conversation>();
    private ListView messages;
    private static MainActivity inst;
    private static boolean active = false;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    double latitude;
    double longitude;

    EditText latEdit;
    EditText longEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Stuff to display the list of conversations
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, conversationList);
        messages = findViewById(R.id.messages);
        messages.setAdapter(arrayAdapter);

        //What happens when a conversation is clicked on
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

                // Location
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);

                startActivity(intent);
            }
        });

        //Checks for permissions, asks for them if it doesn't have them.
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            getPermissionToReadSMS();
            getPermissionToReadContacts();
            getPermissionToLocation();
        }
        else
        {
            refreshInbox();
        }

        // Location
        latitude = 0;
        longitude = 0;
        latEdit = findViewById(R.id.latText);
        longEdit = findViewById(R.id.longText);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void mapsBtn(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("latitude", latEdit.getText());
        intent.putExtra("longitude", longEdit.getText());
        startActivity(intent);
    }

    //Re-does the whole conversation display
    public void refreshInbox()
    {
        //Queries and gets a table of all existing conversations
        ContentResolver contentResolver = getContentResolver();
        Cursor convoCursor = contentResolver.query(Uri.parse("content://sms/conversations"), null, null, null, null);

        //Indexes of snippet and thread_id in the table of conversations
        int indexSnippet = convoCursor.getColumnIndex("snippet");
        int indexThreadId = convoCursor.getColumnIndex("thread_id");

        //Checks if table is empty.
        if(!convoCursor.moveToFirst()) return;

        //Clears array adapter, 'cos new stuff going in.
        arrayAdapter.clear();

        //Iterates through each conversation in the Cursor
        do
        {
            //Gets all messages associated with a particular conversation
            String args[] = {convoCursor.getString(indexThreadId)};
            String cols[] = {"address", "date"};
            Cursor inboxCursor = contentResolver.query(Uri.parse("content://sms/"), cols, "thread_id = ?", args, null);

            //Indexes
            int indexAddress = inboxCursor.getColumnIndex("address");
            int indexDate = inboxCursor.getColumnIndex("date");

            //Checks if Cursor of messages is empty
            if(!inboxCursor.moveToFirst()) return;

            //Gets relevant info from message table, makes Conversation object out of it.
            String phoneNum = inboxCursor.getString(indexAddress);
            String name = getContactName(this, phoneNum);
            String snippet = convoCursor.getString(indexSnippet);
            String date = inboxCursor.getString(indexDate);
            Conversation con = new Conversation(name, phoneNum, snippet, args[0], date);

            //Add to list.
            arrayAdapter.add(con);
        }
        while(convoCursor.moveToNext());

        //Sorts conversation by most recent message
        arrayAdapter.sort(new ConversationComparator());
    }

    //Gets the contact name associated with a given phone number
    public static String getContactName(Context context, String phoneNumber)
    {
        //This block returns the Contact with the given number, if it exists.
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = contentResolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        //If no name found, just use the phone number
        if(cursor == null)
        {
            return phoneNumber;
        }

        //Else, grab the name.
        String name = phoneNumber;
        if(cursor.moveToFirst())
        {
            name = cursor.getString(cursor.getColumnIndex((ContactsContract.PhoneLookup.DISPLAY_NAME)));
        }

        //And close the cursor.
        if(cursor != null && !cursor.isClosed())
        {
            cursor.close();
        }

        return name;
    }

    //Gets permission to read SMS messages
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

    public void getPermissionToLocation()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Allow permission for Location", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }
    }

    //Gets permission to read contacts
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

    //Triggers when "new" button clicked, starts up CreateConversationActivity
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

    //Just returns this activity, used by MessageBroadcastReceiver
    public static MainActivity instance()
    {
        return inst;
    }

    //Sets up inst, helps track whether this activity is active
    @Override
    public void onStart()
    {
        super.onStart();
        inst = this;
        active = true;
        refreshInbox();

        //
        mGoogleApiClient.connect();
    }

    //Helps track whether this activity is active
    @Override
    public void onStop()
    {
        super.onStop();
        active = false;

        //
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public static boolean isActive()
    {
        return active;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } startLocationUpdates();
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLocation == null){
            startLocationUpdates();
        }

        if (mLocation != null) {
            latitude = mLocation.getLatitude();
            longitude = mLocation.getLongitude();
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3600)
                .setFastestInterval(60);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    //Allows Conversations to be sorted by date
    private class ConversationComparator implements Comparator<Conversation>
    {
        @Override
        public int compare(Conversation o1, Conversation o2) {
            return (int) (Float.parseFloat(o2.getDate()) - Float.parseFloat(o1.getDate()));
        }
    }

    //Conversation class, just encapsulates useful information
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

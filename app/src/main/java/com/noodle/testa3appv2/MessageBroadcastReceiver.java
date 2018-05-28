package com.noodle.testa3appv2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MessageBroadcastReceiver extends BroadcastReceiver
{
    public static final String SMS_BUNDLE = "pdus";
    private String address;

    public MessageBroadcastReceiver()
    {
        System.out.println("Receiver created");
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Toast.makeText(context, "Message get!", Toast.LENGTH_SHORT).show();


        if(MainActivity.isActive())
        {
            Bundle intentExtras = intent.getExtras();
            if(intentExtras != null)
            {
                MainActivity inst = MainActivity.instance();
                try
                {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                catch(Exception e)
                {
                    System.out.println("Interrupted");
                }
                inst.refreshInbox();//Re-do?
            }
        }
        else if(ConversationActivity.isActive())
        {
            Bundle intentExtras = intent.getExtras();
            if(intentExtras != null)
            {
                Object[] sms = (Object[]) intentExtras.get("pdus");
                String smsMessageStr = "";

                for(int i = 0; i < sms.length; i++)
                {
                    String format = intentExtras.getString("format");
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sms[i], format);

                    String smsBody = smsMessage.getMessageBody().toString();
                    address = smsMessage.getOriginatingAddress();
                    smsMessageStr += smsBody;
                }

                //No longer needed?
                ConversationActivity inst = ConversationActivity.instance();
                try
                {
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                catch(Exception e)
                {
                    System.out.println("Interrupted");
                }
                inst.updateConversation(smsMessageStr, address);
                //inst.refreshConversation();
            }
        }
    }
}

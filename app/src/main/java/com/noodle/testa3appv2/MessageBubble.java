package com.noodle.testa3appv2;

public class MessageBubble
{

    private String messageBody;
    private boolean outgoing;

    public MessageBubble(String messageBody, boolean outgoing)
    {
        this.messageBody = messageBody;
        this.outgoing = outgoing;
    }

    public String getMessageBody()
    {
        return messageBody;
    }

    public boolean isOutgoing()
    {
        return outgoing;
    }
}

package com.noodle.testa3appv2;

/**
 * Also used for the left-right style chat alignment. Don't really know how this works, either.
 * Craig Fraser 15889604
 * Connor Hewett 15903849
 */
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

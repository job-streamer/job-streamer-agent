package net.unit8.job_streamer.agent;

import javax.websocket.MessageHandler;

/**
 * @author kawasima
 */
public abstract class StringMessageHandler implements MessageHandler.Whole<String> {
    public abstract void onMessage(String msg);
}

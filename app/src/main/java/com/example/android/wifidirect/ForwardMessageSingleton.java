package com.example.android.wifidirect;

import java.util.HashMap;

/**
 * Created by keyurpatel on 4/3/16.
 */
public class ForwardMessageSingleton {
    private Message message;

    private static ForwardMessageSingleton forwardMessageSingleton;
    private ForwardMessageSingleton(){};
    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }



    public boolean isDoesMessageNeedToBeForwarded() {
        return doesMessageNeedToBeForwarded;
    }

    public void setDoesMessageNeedToBeForwarded(boolean doesMessageNeedToBeForwarded) {
        this.doesMessageNeedToBeForwarded = doesMessageNeedToBeForwarded;
    }

    private boolean doesMessageNeedToBeForwarded = false;

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    private boolean forward = false;



    public static ForwardMessageSingleton getInstance(){
        if (forwardMessageSingleton == null){
            forwardMessageSingleton = new ForwardMessageSingleton();

        }
        return  forwardMessageSingleton;
    }

}

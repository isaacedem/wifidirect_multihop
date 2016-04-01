package com.example.android.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import java.io.Serializable;

/**
 * Created by keyurpatel on 3/24/16.
 */
public class Message implements Serializable{
    /**
     * MessageTypes allowed:
     * Bonjour
     * Text
     * File
     * PeerList
     * RemovePeer
     */
    private String mMessageType;
    private Object mMesssageData;
    private String mSenderDeviceName;

    Message(String messageType, Object messageData, String senderDeviceName){
        this.mMessageType = messageType;
        this.mMesssageData = messageData;
        this.mSenderDeviceName = senderDeviceName;

    }

    public String getmMessageType() {
        return mMessageType;
    }

    public void setmMessageType(String mMessageType) {
        this.mMessageType = mMessageType;
    }

    public Object getmMesssageData() {
        return mMesssageData;
    }

    public String getmSenderDeviceName() {
        return mSenderDeviceName;
    }

    public void setmSenderDeviceName(String mSenderDeviceName) {
        this.mSenderDeviceName = mSenderDeviceName;
    }

    public void setmMesssageData(Object mMesssageData) {

        this.mMesssageData = mMesssageData;
    }
}

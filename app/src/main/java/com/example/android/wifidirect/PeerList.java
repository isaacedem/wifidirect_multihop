package com.example.android.wifidirect;

import android.util.Log;

import java.io.Serializable;
import java.security.Permission;
import java.util.HashMap;

/**
 * Created by keyurpatel on 3/24/16.
 */
public class PeerList implements Serializable{

//    private HashMap<String, String> deviceMacMap;
//    private HashMap<String, String> macIPMap;
    private static HashMap<String, String> deviceIPMap;

    public String getMyPhoneName() {
        return myPhoneName;
    }

    public void setMyPhoneName(String myPhoneName) {
        this.myPhoneName = myPhoneName;
    }

    private String myPhoneName;

    private static String GOIPAddress;

    private static PeerList peerList = null;

    private PeerList(){};

    public static PeerList getInstance(){
        if (peerList == null){
            peerList = new PeerList();
            init();
        }
        return  peerList;
    }

    public static void init(){
//        deviceMacMap = new HashMap<>();
//        macIPMap = new HashMap<>();
          deviceIPMap = new HashMap<String, String>();
        GOIPAddress = "192.168.49.1";
    }

    public String getGOIPAddress(){return GOIPAddress;}

    public void addDevice(String device, String ipAddress){
        Log.i("PeerList", "addDevice " + device + " " + ipAddress);
        deviceIPMap.put(device, ipAddress);
        Log.i("PeerList", "deviceIPMAPsize: " + deviceIPMap.size());
    }

    public HashMap<String, String> getDevices(){
        return deviceIPMap;
    }

    public void removeDevice(String device){
        if (deviceIPMap.containsKey(device))
            deviceIPMap.remove(device);
    }

    public String getDeviceIP(String device) {
        return  this.deviceIPMap.get(device);
    }

    public void updateAllDevices(HashMap<String, String> devices) {

        deviceIPMap = new HashMap<>();
        deviceIPMap.putAll(devices);
    }

}

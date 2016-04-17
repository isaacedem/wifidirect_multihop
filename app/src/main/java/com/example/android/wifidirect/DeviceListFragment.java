/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements PeerListListener {

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    private static String getDeviceStatus(WifiP2pDevice device) {
        int deviceStatus = device.status;
       // Log.d(WiFiDirectActivity.TAG, "Device " + device.deviceName+ " Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "z";
            default:
                return "Unknown";

        }
    }

    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device));
                }
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */

    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);

        PeerList peerList = PeerList.getInstance();
        peerList.setMyPhoneName(device.deviceName);


        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device));
    }

    private boolean autoConnectFlag = false;

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        PeerList globalPeerList = PeerList.getInstance();

        Log.i("help", "asdf on Peers Avail");
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        peers.clear();
        peers.addAll(peerList.getDeviceList());
        Log.d("DeviceListFragment", "onPeersAvailable peers original " + peers.size());
        //for (WifiP2pDevice item : peers) {
        //    Log.d("DeviceListFragment", "onPeersAvailable peers original " + item.deviceName);
        //}
//        Log.d("DeviceListFragment", "onPeersAvailable peers original " + peers.toString());
//        if (this.device.isGroupOwner()) {
//            Log.d("DeviceListFragment", "onPeersAvailable peerlist devices " + globalPeerList.getDevices().keySet());
//
//
//            Iterator<String> itr = globalPeerList.getDevices().keySet().iterator();
//            while (itr.hasNext()) {
//                String device = itr.next();
//                boolean found = false;
//                for (WifiP2pDevice peer : peers) {
//                    if (device.equals(peer.deviceName))
//                        found = true;
//                }
//                if (!found)
//                    itr.remove();
//            }
//            Log.d("DeviceListFragment", "onPeersAvailable remaining peerlist devices " + globalPeerList.getDevices().keySet());
//            // first time should be 0 so only runs when device leaves
//            if (globalPeerList.getDevices().size() > 0) {
//                DeviceDetailFragment.FileServerAsyncTask.sendUpdatedPeerList();
//            }
//        }


        for (WifiP2pDevice peer : peers) {
            Log.d(WiFiDirectActivity.TAG,  peer.deviceName + " " + peer.isGroupOwner());
        }
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(WiFiDirectActivity.TAG, "No devices found");
            return;
        }

        Message forwardedMessage = ForwardMessageSingleton.getInstance().getMessage();
        if(ForwardMessageSingleton.getInstance().isDoesMessageNeedToBeForwarded()){
            for (WifiP2pDevice peer2 : peers) {
                //Log.d("Forwarded Message", peer2.deviceName + " " + peer2.status+" status");
                if(peer2.deviceName.equals(forwardedMessage.getmRecipientName())&& !(peer2.status == WifiP2pDevice.CONNECTED)){
                    //check device to make sure device is not connected to a group
                    //if connected to a group than do not connect and connect to another GO
                    Log.i("Forwarded Message", "Found Recipent: " + peer2.deviceName + " forwadedname: " + forwardedMessage.getmRecipientName());


                    WifiP2pConfig config = new WifiP2pConfig();
                    config.groupOwnerIntent = 15; //Less probability to become the GO
                    config.deviceAddress = peer2.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                            "Connecting to :" + device.deviceAddress, true, true
                    );
                    ((DeviceActionListener) getActivity()).connect(config);
                    ForwardMessageSingleton.getInstance().setDoesMessageNeedToBeForwarded(false);



                }
            }
        }

    }

    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     *
     */
    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        Log.i("help", "asdf on Init Dis");
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();

        void searchPeers(String name);
    }

}

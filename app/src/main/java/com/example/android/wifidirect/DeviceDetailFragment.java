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

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    protected static WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    private EditText receiverView;
    private Button sendBtn;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.groupOwnerIntent = 15; //Less probability to become the GO
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });
        receiverView = (EditText) mContentView.findViewById(R.id.receiverView);
        sendBtn = (Button) mContentView.findViewById(R.id.sendMessageBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        return mContentView;
    }

    public void sendMessage(){
        Log.i("Inside sendMessage", "Send message");
        final String recevierID = receiverView.getText().toString();

        final PeerList peerList = PeerList.getInstance();
        final Message message = new Message("Text", new String("jfkajglkajgkla"), peerList.getMyPhoneName());

        final String receiverIP = peerList.getDeviceIP(recevierID);
        if(receiverIP == null ){
            //todo later have to check for multi-hop
        } else {
            Log.i("Inside sendMessage", "Send message");
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Socket socket = new Socket();
                        socket.connect((new InetSocketAddress(receiverIP, 8988)), 5000);
                        OutputStream os = socket.getOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(os);
                        oos.writeObject(message);
                        oos.close();
                        os.close();
                        socket.close();
                    } catch (Exception e) {
                        Log.d(WiFiDirectActivity.TAG, "Could not send message to Receiver at: " + receiverIP);
                        //Message failed so send Group Owner saying it is not in the group no more
                        final Message messageToGO = new Message("RemovePeer", new String(recevierID), peerList.getMyPhoneName());
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Socket socket = new Socket();
                                    socket.connect((new InetSocketAddress(peerList.getGOIPAddress(), 8988)), 5000);
                                    OutputStream os = socket.getOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(os);
                                    oos.writeObject(messageToGO);
                                    oos.close();
                                    os.close();
                                    socket.close();
                                } catch (Exception e) {
                                    Log.d(WiFiDirectActivity.TAG, "Client peerlist message: " + e);
                                    //Message failed so send Group Owner saying it is not in the group no more

                                }
                            }
                        });
                        thread.start();

                    }
                }
            });
            thread.start();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

        //open up socket for both Group Owner and Group Member
        createAsyncTask();
        if (info.groupFormed && info.isGroupOwner) {

        } else if (info.groupFormed) {
            // The other device acts as the client.
            // Send bonjour message to GO
            sendBonjourMessage();

            // In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));

        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    private void sendBonjourMessage() {
        PeerList peerList = PeerList.getInstance();
        final Message message = new Message("Bonjour", new String("WASSUP"), peerList.getMyPhoneName());

        Log.d(WiFiDirectActivity.TAG, "Right before thread send bonjour: " + peerList.getMyPhoneName());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(WiFiDirectActivity.TAG, "Inside thread: ");
                    Socket socket = new Socket();
//                        socket.setReuseAddress(true);
                    socket.connect((new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), 8988)), 5000);
                    OutputStream os = socket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(message);
                    oos.close();
                    os.close();
                    socket.close();
                } catch (Exception e) {
                    Log.d(WiFiDirectActivity.TAG, "Client bonjour message: " + e);
                }
            }
        });
        thread.start();
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    public static void createAsyncTask() {
        new FileServerAsyncTask().execute();
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, ArrayList<String>> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        public FileServerAsyncTask() {

        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
//                serverSocket.setReuseAddress(true);
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
//                final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                        + ".jpg");
//
//                File dirs = new File(f.getParent());
//                if (!dirs.exists())
//                    dirs.mkdirs();
//                f.createNewFile();
//
//                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
//                InputStream inputstream = client.getInputStream();
//                copyFile(inputstream, new FileOutputStream(f));
//                serverSocket.close();
//                return f.getAbsolutePath();

//                ArrayList<String> passed = passing[0];

                Log.i("DeviceDetailFragment", "Inside Bonjour");
                ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
                Object object = objectInputStream.readObject();
                serverSocket.close();

                Message message = (Message) object;

                processMessage(message, client);
                ArrayList<String> results = new ArrayList<String>();
                results.add(client.getInetAddress().toString());
                return results;

            } catch (Exception e) {

                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }


        private void processMessage(Message message, Socket client) {
            PeerList peerList = PeerList.getInstance();
            if (message.getmMessageType().equals("Bonjour") && info.isGroupOwner) {
                Log.d(WiFiDirectActivity.TAG, "Client IP address: " + client.getInetAddress());
                Log.d(WiFiDirectActivity.TAG, "Client Name?: " + message.getmSenderDeviceName());
                String device = message.getmSenderDeviceName();
                String ipAddress = client.getInetAddress().toString().substring(1); // remove /
                peerList.addDevice(device, ipAddress);
                sendUpdatedPeerList();
                //save IP to GO Global IP LIST
                //send client updated list
            }
            if (message.getmMessageType().equals("PeerList")) {
                Log.d(WiFiDirectActivity.TAG, "Peerlist received");
                HashMap<String, String> receivedPeerList = (HashMap) message.getmMesssageData();
//                HashMap<String, String> hm = receivedPeerList.getDevices();
//                Log.d("PeerLIs phone name", receivedPeerList.getMyPhoneName()+"");
                Log.d("PeerLIst Data", receivedPeerList.size() + "");
                peerList.updateAllDevices(receivedPeerList);
                Log.d("Client Peerlist", peerList.getDevices().keySet().toString());
                //save IP to GO Global IP LIST
                //send client updated list
            }
            if (message.getmMessageType().equals("RemovePeer")) {

                String deviceName = (String) message.getmMesssageData();
                Log.d(WiFiDirectActivity.TAG, "RemovePeer Message Received for " + deviceName );
                Log.d(WiFiDirectActivity.TAG, "RemovePeer Peerlist " + peerList.getDevices().keySet().toString() );
                Log.d("PeerLIst Data Before", peerList.getDevices().size() + "");
                peerList.removeDevice(deviceName);
                Log.d("PeerLIst Data After", peerList.getDevices().size() + "");
                sendUpdatedPeerList();
            }
            createAsyncTask();

        }

        protected static void sendUpdatedPeerList() {
            PeerList peerList = PeerList.getInstance();
            if (peerList.getDevices().size() > 0) {
                // make sure GO is in peer list for easy access by anyone
                peerList.addDevice(peerList.getMyPhoneName(), info.groupOwnerAddress.toString());

                final HashMap<String, String> devices = peerList.getDevices();
                final HashMap<String, String> updatedPeers = new HashMap<>();
                updatedPeers.putAll(peerList.getDevices());
                Log.d("DeviceDetailFrag", "Updated peers size: " + updatedPeers.size());
                Log.d("DeviceDetailFrag", "Sending peerlist size: " + peerList.getDevices().size());
                for (String device : devices.keySet()) {
                    final String ipAddress = devices.get(device);
                    final Message message = new Message("PeerList", updatedPeers, info.groupOwnerAddress.toString());
                    Log.d(WiFiDirectActivity.TAG, "Right before thread: ");
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(WiFiDirectActivity.TAG, "Sending peerlist thread: " + devices.keySet().toString());
                                Socket socket = new Socket();
//                        socket.setReuseAddress(true);
                                Log.d(WiFiDirectActivity.TAG, "Sending peerlist thread IP Address: " + ipAddress);
                                socket.connect((new InetSocketAddress(ipAddress, 8988)), 5000);
                                OutputStream os = socket.getOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(os);
                                oos.writeObject(message);
                                oos.close();
                                os.close();
                                socket.close();
                            } catch (Exception e) {
                                Log.d(WiFiDirectActivity.TAG, "Client peerlist message: " + e);
                            }
                        }
                    });
                    thread.start();
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(final ArrayList<String> result) {
//            if (result != null) {
////                statusText.setText("File copied - " + result);
////                Intent intent = new Intent();
////                intent.setAction(android.content.Intent.ACTION_VIEW);
////                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
////                context.startActivity(intent);
//                Log.d(WiFiDirectActivity.TAG, "Ip Address is " + result);
//                if (result.get(0).toString().equals("/192.168.49.1")) {
//                    Thread thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                Log.d(WiFiDirectActivity.TAG, "Inside message thread: " + result.get(0).toString());
//                                Socket socket = new Socket();
////                        socket.setReuseAddress(true);
//                                socket.connect((new InetSocketAddress("192.168.49.27", 8988)), 5000);
//                                OutputStream os = socket.getOutputStream();
//                                ObjectOutputStream oos = new ObjectOutputStream(os);
//                                oos.writeObject(new String("Hello World"));
//                                oos.close();
//                                os.close();
//                                socket.close();
//                            } catch (Exception e) {
//                                Log.d(WiFiDirectActivity.TAG, "Client hello world message: " + e);
//
//                                //Client no longer in group
//
//                            }
//                        }
//                    });
//                    thread.start();
//                }
//                Thread thread = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Log.d(WiFiDirectActivity.TAG, "Inside message thread: ");
//                            Socket socket = new Socket();
////                        socket.setReuseAddress(true);
//                            socket.connect((new InetSocketAddress("192.168.49.181", 8988)), 5000);
//                            OutputStream os = socket.getOutputStream();
//                            ObjectOutputStream oos = new ObjectOutputStream(os);
//                            oos.writeObject(new String("Hello World"));
//                            oos.close();
//                            os.close();
//                            socket.close();
//                        } catch (Exception e) {
//                            Log.d(WiFiDirectActivity.TAG, "Client hello world message: " + e);
//                        }
//                    }
//                });
//                thread.start();
//                Toast.makeText(context, result.get(0).toString(), Toast.LENGTH_SHORT).show();
//            }
//
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
//            statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }


}

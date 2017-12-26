package com.example.auser.btapp_1;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.app.PendingIntent.getActivity;
import static java.lang.String.valueOf;

public class EditActivity extends Activity {

    private TextView dataTextView ;
    private Button cleanBtn, sendBtn;
    private EditText sendEdit;
    private BluetoothAdapter btAdapter;
    private Context context;
    private String remoteDeviceInfo;
    private BluetoothChatService mChatService = null;
    private String remoteMacAddress;
    private String mConnectedDeviceName =null;
    private StringBuffer mOutStringBuffer;            // String buffer for outgoing messages

    private static final String TAG = "BT_Edit";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_);

        //  Dispaly BT data
        dataTextView = (TextView) findViewById(R.id.data_textView);
        dataTextView.setText("");
        Log.d(TAG, "Edit_OnCreate_start");

        cleanBtn = (Button)findViewById(R.id.clean_btn);
        cleanBtn.setOnClickListener(new BtnOnClickListener());

        sendBtn = (Button)findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(new BtnOnClickListener());

        sendEdit = (EditText) findViewById(R.id.send_editText);
        sendEdit.setText("");

        sendEdit.setOnEditorActionListener(textEditListen);

        context = this;
        //get data from MainActivity
        Intent intent = getIntent();
        remoteDeviceInfo=intent.getStringExtra("remoteDevice");


        // Initialize the BluetoothChatService to perform bluetooth connection mode
        mChatService = new BluetoothChatService(context , mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        // set BT in Server mode
        Log.d(TAG, "Server mode");
        dataTextView.append("Make BT module in Server mode. \n");
        // Start the Bluetooth chat services
        mChatService.start();

        if(remoteDeviceInfo!=null) {         // set BT in cliet mode
            Log.d(TAG, "Cliet  mode");
            String deviceMsg = remoteDeviceInfo.substring(10) ;
            Log.d(TAG, deviceMsg);
            dataTextView.append("Connecting to remote BT device :  \n" + deviceMsg + "\n\n");

            // Get the device MAC address
            remoteMacAddress = remoteDeviceInfo.substring(remoteDeviceInfo.length() - 17) ;
            Log.d(TAG, remoteMacAddress);
            //  String msg = valueOf(remoteMacAddress.length());
            //  Log.d(TAG, msg);

            // Get the remote BluetoothDevice object
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = btAdapter.getRemoteDevice(remoteMacAddress);
            // Attempt to connect to the device
            mChatService.connect(device, true);
        }


    } // end of onCreate()

    @Override
    public void onStart() {
        super.onStart();
        //       Log.d(TAG,"EditActivity onStart()");

    }

    @Override
    public void onResume() {
        super.onResume();
        //      Log.d(TAG,"EditActivity onResume()");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            Log.d(TAG,"EditActivity onDestry()");
            mChatService.stop();
            mChatService=null;
        }
    }


    // if TextEdit string was ended by enter key then send data out
    private TextView.OnEditorActionListener textEditListen = new TextView.OnEditorActionListener(){
        //  EditorInfo.IME_NULL if being called due to the enter key being pressed
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            Log.d(TAG, "enter key check");
            //    Log.d(TAG, valueOf(actionId));
            if (actionId == EditorInfo.IME_ACTION_DONE ) {
                Log.d(TAG, "enter key pressed");
                String message = textView.getText().toString();
                dataTextView.append(">> " + message + "\n");     //display on TextView
                sendMessage(message);
            }
            return true;
        }
    };

    // Check if any button is  pressed
    private class BtnOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch(view.getId()) {

                case R.id.clean_btn:           // clean display TextView
                    dataTextView.setText("");
                    Toast.makeText(EditActivity.this, "Clean display", Toast.LENGTH_SHORT).show();
                    break;

                case R.id.send_btn:            // press Discoverable button to make BT module discoverable for 180 sec
                    dataTextView.append(">> " + sendEdit.getText() + "\n");   //display on TextView
                    // Send a message using content of the edit text widget
                    String message = sendEdit.getText().toString();
                    sendMessage(message);
                    break;
            }
        }

    }

    // Sends a message to remote BT device.
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        int mState= mChatService.getState();
        Log.d(TAG , "mstate in sendMessage =" + valueOf(mState));
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(context,"Bluetooth device is not connected. " , Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            sendEdit.setText(mOutStringBuffer);
        }
    }



    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //   dataTextView.append(">>  : " + writeMessage + "\n");   //display on TextView
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    dataTextView.append("remote : " + readMessage + "\n");   //display on TextView

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(context, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(context, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };



}

package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import java.net.InetAddress;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private static final int SERVER_PORT = 10000;
    private static final String[] ARRAY_OF_REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
    private static int key = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        //Referred from PA1
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf(Integer.parseInt(portStr) * 2);

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }


        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText editText = (EditText) findViewById(R.id.editText1);

        //Retrieve pointer for send button
        final Button sendButton = (Button) findViewById(R.id.button4);

        //Register actionlistener for send button
        sendButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView textView = (TextView) findViewById(R.id.textView1);
                textView.append("\t" + msg); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        //Referred from OnPTestClickListener.java
        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            ServerSocket serverSocket = serverSockets[0];

            while(true) {
                try {
                    Socket socketForServer = serverSocket.accept();
                    DataInputStream inputMessage = new DataInputStream(socketForServer.getInputStream());
                    String str = inputMessage.readUTF();
                    publishProgress(str);
                    socketForServer.close();
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask socket IOException");
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived + "\t\n");

            //Reference : https://developer.android.com/reference/android/content/Context.html#getContentResolver()
            ContentResolver contentResolver = getContentResolver();

            //Referred from OnPTestClickListener.java
            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
            ContentValues contentValues = new ContentValues();
            contentValues.put("key",Integer.toString(key));
            contentValues.put("value", strReceived);
            contentResolver.insert(mUri, contentValues);
            key++;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                //Multicasting messages on all 5 AVDs
                for (int i=0; i<ARRAY_OF_REMOTE_PORTS.length; i++) {
                    try {
                        String msgToSend = msgs[0];
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(ARRAY_OF_REMOTE_PORTS[i]));
                        DataOutputStream outputMessage = new DataOutputStream(socket.getOutputStream());
                        outputMessage.writeUTF(msgToSend);
                        outputMessage.flush();
                        outputMessage.close();
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask IOException" + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "ClientTask Exception" + e.getMessage());
            }

            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
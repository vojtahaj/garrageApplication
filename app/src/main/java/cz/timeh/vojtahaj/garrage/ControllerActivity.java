package cz.timeh.vojtahaj.garrage;

import androidx.appcompat.app.AppCompatActivity;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import me.aflak.bluetooth.Bluetooth;

import static cz.timeh.vojtahaj.garrage.R.color.colorGreen;
import static cz.timeh.vojtahaj.garrage.R.color.colorOrange;
import static cz.timeh.vojtahaj.garrage.R.color.colorRed;
import static cz.timeh.vojtahaj.garrage.R.color.colorWhite;
import static cz.timeh.vojtahaj.garrage.R.color.switch_thumb_disabled_material_dark;

public class ControllerActivity extends AppCompatActivity implements Bluetooth.CommunicationCallback {

    private Button btnDoor;
    private TextView garrage, connect, garrageLength, beforeGarrage, door;

    private Bluetooth bt;
    private BluetoothDevice bluetoothDevice;

    private boolean doorState;
    private boolean registered = false;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);


        int state = getIntent().getIntExtra("position", 0);
        Log.w("Intent int", String.valueOf(state));

        garrage = findViewById(R.id.textViewGarrage);
        beforeGarrage = findViewById(R.id.textViewBeforeGarrage);
        garrageLength = findViewById(R.id.textViewLengthInGarrage);
        btnDoor = findViewById(R.id.btnGarrage);
        connect = findViewById(R.id.textViewOnOff);
        connect.setBackgroundColor(getResources().getColor(colorRed));

        door = findViewById(R.id.textViewDoor);

        bt = new Bluetooth(this);
        bt.enableBluetooth();
        bt.setCommunicationCallback(this);

        Toast.makeText(ControllerActivity.this, "Connecting", Toast.LENGTH_LONG).show();
        bluetoothDevice = bt.getPairedDevices().get(state);
        bt.connectToDevice(bluetoothDevice);
        Log.i("BT name", bluetoothDevice.getName());
        Log.i("BT address", bluetoothDevice.getAddress());

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered = true;
        btnDoor.setEnabled(false);
        btnDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    bt.send("DOOR\n");

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        registered = false;
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Log.i("BTconnect", device.getName());
        bt.send("A?\n");
        makeToast("Connected");

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnDoor.setEnabled(true);
                connect.setText(R.string.on);
                connect.setBackgroundColor(getResources().getColor(colorGreen));
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Log.i("BTdisconnect", message);
        makeToast("Disconnected");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnDoor.setEnabled(false);
                connect.setText(R.string.off);
                connect.setBackgroundColor(getResources().getColor(colorRed));
            }
        });
        connect();
    }

    private void connect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bt.connectToDevice(bluetoothDevice);
                    }
                }, 4000);
            }
        });
    }

    @Override
    public void onMessage(String message) {
        Log.i("BTonMessage", message);
        parseMessage(message);
    }

    private void parseMessage(String message) {
        TextView textView = null;
        int color = getResources().getColor(R.color.colorWhite);
        String s = message;
        switch (message) {
            case "GFREE":
                textView = garrage;
                s = message;
                color = getResources().getColor(colorGreen);
                break;
            case "BARIER":
                textView = garrage;
                s = getResources().getString(R.string.barrier);
                color = getResources().getColor(colorRed);
                break;
            case "BGFREE":
                textView = beforeGarrage;
                s = getResources().getString(R.string.bgFree);
                break;
            case "STOP":
                textView = garrage;
                s = message;
                color = getResources().getColor(colorRed);
                break;
            case "BGSTOP":
                textView = beforeGarrage;
                color = getResources().getColor(colorRed);
                s = "Překážka před garáží";
                break;
            case "DOC":
                s = getString(R.string.middle);
                color = getResources().getColor(colorOrange);
                textView = door;
                break;
            case "DOPEN":
                s = getString(R.string.open);
                color = getResources().getColor(colorGreen);
                textView = door;
                break;
            case "DCLOSE":
                s = getString(R.string.close);
                color = getResources().getColor(colorRed);
                textView = door;
                break;
            default:
//                color = getResources().getColor(colorGreen);
//                textView = connect;
                Log.w("Complicated MESSAGE", message);
                parseComplicatedMessage(message);
                break;
        }

        if (textView != null)
            display(s, textView, color);
    }

    private void parseComplicatedMessage(String message) {
        int l = message.length();
        String s = message.substring(0, 2);
//        Log.e("message", s);
        switch (s) {
            case "GL":
                String m = message.substring(2, l);
                display("Ke zdi zbývá cm: " + m, garrageLength, getResources().getColor(colorGreen));
                break;
            default:
                Log.e("Other message", message);
                break;
        }
    }

    private void display(final String message, final TextView textView, final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
                textView.setBackgroundColor(color);
            }
        });

    }

    @Override
    public void onError(String message) {
        Log.i("BTERR", message);
    }

    @Override
    public void onConnectError(BluetoothDevice device, String message) {
        Log.i("BTConnectERR", message);
//        makeToast("error trying it again");
        connect();
    }

    private void makeToast(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ControllerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(ControllerActivity.this, MainActivity.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if (registered) {
                            unregisterReceiver(mReceiver);
                            registered = false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if (registered) {
                            unregisterReceiver(mReceiver);
                            registered = false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }

    };

}

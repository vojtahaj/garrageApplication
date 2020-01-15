package cz.timeh.vojtahaj.garrage;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;

public class MainActivity extends AppCompatActivity {

    private Bluetooth bt;
    private ListView lvDevices;
    private Button btnNotFound;
    private List<BluetoothDevice> pairedDevices;
    private boolean registered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        lvDevices = findViewById(R.id.listViewPaired);
        btnNotFound = findViewById(R.id.btnBtNotFound);
        registered = true;

        bt = new Bluetooth(this);
        bt.enableBluetooth();

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, ControllerActivity.class);
                intent.putExtra("position", i);
                if (registered) {
                    unregisterReceiver(mReceiver);
                    registered = false;
                }
                startActivity(intent);
                finish();
            }
        });

        btnNotFound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "click", Toast.LENGTH_LONG).show();
            }
        });
        addDeviceToList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registered) {
            registered = false;
            unregisterReceiver(mReceiver);
        }
    }


    private void addDeviceToList() {
        pairedDevices = bt.getPairedDevices();

        List<String> names = new ArrayList<>();

            for (BluetoothDevice bd : pairedDevices) {
                names.add(bd.getName());

            }
            String[] array = names.toArray(new String[names.size()]);
            ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, array);
            lvDevices.setAdapter(adapter);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lvDevices.setEnabled(true);
                            }
                        });
                        Toast.makeText(MainActivity.this, "Zapinam BT", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addDeviceToList();
                                lvDevices.setEnabled(true);
                            }
                        });
                        break;
                }
            }
        }
    };
}

package com.example.pair_ultra;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

//仍需继续重构
public class DeviceListActivity extends Activity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private ArrayAdapter<String> newDevicesArrayAdapter;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
        setResult(Activity.RESULT_CANCELED);

        //基本变量设定
        int itemLayoutId = R.layout.device_name;
        int pairedListViewId = R.id.paired_devices;
        int newDevicesListViewId = R.id.new_devices;

        // 初始化控件
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(createScanButtonClickListener());

        ArrayAdapter<String> pairedDevicesArrayAdapter = createArrayAdapter(this, itemLayoutId);
        initialListView(pairedListViewId, pairedDevicesArrayAdapter, deviceClickListener);

        newDevicesArrayAdapter = createArrayAdapter(this, itemLayoutId);
        initialListView(newDevicesListViewId, newDevicesArrayAdapter, deviceClickListener);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        appendDevices(pairedDevices, pairedDevicesArrayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void discoverBluetoothDevicesProgress(){
        setProgressBarIndeterminateVisibility(true);
        setTitle("scanning for devices...");

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }

    private AdapterView.OnItemClickListener deviceClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            bluetoothAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length()-17);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    public ArrayAdapter<String> createArrayAdapter(Context context, int itemlayoutId) {
        return new ArrayAdapter<>(context, itemlayoutId);
    }

    public void initialListView(int listViewIdInLayout, ArrayAdapter<String> arrayAdapter, AdapterView.OnItemClickListener itemClickListener){
        ListView newDevicesListView = findViewById(listViewIdInLayout);
        newDevicesListView.setAdapter(arrayAdapter);
        newDevicesListView.setOnItemClickListener(itemClickListener);
    }

    public void appendDevices(Set<BluetoothDevice> pairedDevices, ArrayAdapter<String> arrayAdapter){
        String noDevicesHint = "No devices have been paired";
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                arrayAdapter.add(concatedDeviceMessage(device));
            }
        } else {
            arrayAdapter.add(noDevicesHint);
        }
    }

    public View.OnClickListener createScanButtonClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        };
    }

    public String concatedDeviceMessage(BluetoothDevice device) {
        return device.getName() + "\n" + device.getAddress();
    }

}
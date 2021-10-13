package com.example.pair_ultra;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.security.AccessController;

import static com.example.pair_ultra.GlobalConfig.*;

public class MainActivity extends AppCompatActivity {

    private TextView textConnectStatus;
    private TextView textCommand;
    private RadioGroup signalGroup;
    private RadioGroup sampleRateGroup;
    private Button lockButton;

    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.INTERNET
    };

    private RecordControl recordThing;
    private int persionId=0;

    BluetoothAdapter bluetoothAdapter = null;
    BluetoothServer bluetoothServer = null;
    private StringBuffer outStringBuffer;

    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取权限
        Utils.checkPermissions(this, permissions);
        //初始化所有控件
        initialAllViews();
        initialBluetoothServer();
        initAudioSetting();
        setClickListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_operation, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.connect_bluetooth_device: {
                Intent BluetoothIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(BluetoothIntent, REQUEST_CONNECT_DEVICE);
                return true;
            }
            case R.id.self_discoverable: {
                makeSelfDiscoverable();
                return true;
            }
        }
        return false;
    }



    public void initialAllViews(){
        //绑定控件
        textConnectStatus = (TextView) findViewById(R.id.text_connect_status);
        textCommand = (TextView) findViewById(R.id.text_command);
        recordThing = new RecordControl(this, persionId);
        signalGroup = (RadioGroup) findViewById(R.id.signal_group);
        sampleRateGroup = (RadioGroup) findViewById(R.id.sample_rate_group);
        lockButton = (Button) findViewById(R.id.lock_button);
    }

    public void initAudioSetting(){
        signalGroup.check(R.id.single_ultra_20k);
        sampleRateGroup.check(R.id.sr_44100);
        SAMPLE_RATE_INHZ = 44100;
        SONIC_ID = R.raw.ultrasonic20k;
        isEnableRadioGroups(false);
    }

    public void initialBluetoothServer(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
        if (!bluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (bluetoothServer == null) {
            setupServer();
        }
    }

    public void isEnableSingleRadioGroup(RadioGroup radioGroup, Boolean flag){
        for(int i=0;i<radioGroup.getChildCount();i++){
            radioGroup.getChildAt(i).setEnabled(flag);
        }
        radioGroup.setEnabled(flag);
    }

    public void isEnableRadioGroups(Boolean flag){
        isEnableSingleRadioGroup(signalGroup, flag);
        isEnableSingleRadioGroup(sampleRateGroup, flag);
    }

    private void setupServer() {
        bluetoothServer = new BluetoothServer(this, mHandler);
        outStringBuffer = new StringBuffer();
    }

    private void setStatus(CharSequence subTitle) {
        textConnectStatus.setText(subTitle);
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        bluetoothServer.connect(device, secure);
    }

    private void makeSelfDiscoverable(){
        if (bluetoothAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public String[] unpackReceivedMessage(String message){
        //unpackedData数组中一共有四个元素，第一个元素是开始或者结束等指令，
        // 第二个元素是保存文件缩写名，第三个元素是当前语句的剩余次数，第四个元素是当前人员的id号
        String unpackedData[] = message.split(SPILIT_CHAR);
        return unpackedData;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    //填写创建蓝牙服务器的代码
                    setupServer();
                } else {
                    String bluetoothNotEnabledHint = "Bluetooth was not enabled. Leaving Bluetooth Chat.";
                    Toast.makeText(getApplicationContext(), bluetoothNotEnabledHint, Toast.LENGTH_LONG).show();
                    this.finish();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothServer.STATE_CONNECTED:
                            setStatus("connected");
                            break;
                        case BluetoothServer.STATE_CONNECTING:
                            setStatus("connecting");
                            break;
                        case BluetoothServer.STATE_LISTEN:
                        case BluetoothServer.STATE_NONE:
                            setStatus("disconnected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    String unpackedMessage[] = unpackReceivedMessage(readMessage);
                    textCommand.setText(readMessage);
                    int packedDataLen = unpackedMessage.length;
                    if (packedDataLen == PACKED_DATA_LEN){
                        dealWhenReadMessage(unpackedMessage);
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void dealWhenReadMessage(String[] unpackedMessage) {
        String command = unpackedMessage[0];
        switch (command) {
            case START:
                recordThing.startRecord(unpackedMessage);
                break;
            case END:
                recordThing.stopRecord();
                break;
            case BACK:
                recordThing.backDelete(unpackedMessage);
                break;
        }
    }

    public void setClickListeners(){
        signalGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.single_ultra_20k:
                        SONIC_ID = R.raw.ultrasonic20k;
                        break;
                    case R.id.multi_ultra:
                        SONIC_ID = R.raw.sound500;
                        break;
                }
                System.out.println();
//                Toast.makeText(getApplicationContext(), SAMPLE_RATE_INHZ + " " + SONIC_ID, Toast.LENGTH_SHORT).show();
            }
        });

        sampleRateGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i){
                    case R.id.sr_44100:
                        SAMPLE_RATE_INHZ = 44100;
                        break;
                    case R.id.sr_48000:
                        SAMPLE_RATE_INHZ = 48000;
                        break;
                }
//                Toast.makeText(getApplicationContext(), SAMPLE_RATE_INHZ + " " + SONIC_ID, Toast.LENGTH_SHORT).show();
            }
        });

        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean flag = signalGroup.isEnabled();
                isEnableRadioGroups(!flag);
            }
        });
    }
}
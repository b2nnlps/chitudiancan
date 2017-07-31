package com.chitukeji.diancan;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_open)
    Button btnOpen;
    @BindView(R.id.btn_search)
    Button btnSearch;
    @BindView(R.id.btn_setting)
    Button btnSetting;
    @BindView(R.id.lv_unbound_device)
    ListView lvUnboundDevice;
    @BindView(R.id.lv_bound_device)
    ListView lvBoundDevice;


    private BluetoothAdapter mBluetoothAdapter;


    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 0;
    public static final String DEVICE = "device";

    private ArrayList<BluetoothDevice> unbondDevicesList = new ArrayList<>();
    private ArrayList<BluetoothDevice> bondDevicesList = new ArrayList<>();
    private DeviceReceiver deviceReceiver;
    private ArrayList<String> boundName = new ArrayList<>();
    private ArrayList<String> unboundName = new ArrayList<>();
    private ArrayList<String> connentName = new ArrayList<>();
    private MyBluetoothAdapter boundAdapter;
    private MyBluetoothAdapter unboundAdapter;
    private String defaultDevice = "";
    private int lastDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initView();
        initIntentFilter();

        defaultData();
    }

    private void initView() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        defaultData();
        showBtnText();
        showBoundDevices();

    }

    private void defaultData() {//获取默认设备
        String fileName = "defaultDevice.ng"; //文件名字
        defaultDevice = readFile(fileName);
    }

    public String readFile(String fileName) {
        String res = "";
        try {
            FileInputStream fin = openFileInput(fileName);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);
            res = new String(buffer);
            fin.close();
        } catch (Exception e) {
        }
        return res;
    }


    private void showBoundDevices() {
        Set<BluetoothDevice> bluetoothDeviceSet = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bluetoothDeviceSet) {
            bondDevicesList.add(device);
            if (!defaultDevice.equals("") && device.getAddress().equals(defaultDevice)) {

                btnOpen.setText("默认设备：" + device.getName());
/*
                if (!connentName.contains(device.getAddress())) {
                    connentName.add(device.getAddress());
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, PrintActivity.class);
                    intent.putExtra(DEVICE, device);
                    startActivity(intent);
                }*/
            }

        }
        boundName.addAll(getData(bondDevicesList));
        boundAdapter = new MyBluetoothAdapter(this, boundName);
        lvBoundDevice.setAdapter(boundAdapter);
        lvBoundDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,     //添加LIST的点击事件
                                    int arg2, long arg3) {
                BluetoothDevice device = bondDevicesList.get(arg2);
                if (!connentName.contains(device.getAddress())) {
                    lastDevice = arg2;
                    connentName.add(device.getAddress());
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, PrintActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

                    intent.putExtra(DEVICE, device);
                    startActivity(intent);

                    // bondDevicesList.set(arg2,device.n());

                } else {
                    if (lastDevice == arg2) {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, PrintActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        intent.putExtra(DEVICE, device);
                        startActivity(intent);
                    }
                    ToastUtil.showToast(MainActivity.this, "该打印机连接正常.");
                }
            }
        });


        unboundName.addAll(getData(unbondDevicesList));
        unboundAdapter = new MyBluetoothAdapter(this, unboundName);
        lvUnboundDevice.setAdapter(unboundAdapter);
        lvUnboundDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int arg2, long arg3) {
                try {
                    Method createBondMethod = BluetoothDevice.class
                            .getMethod("createBond");
                    createBondMethod.invoke(unbondDevicesList.get(arg2));
                    bondDevicesList.add(unbondDevicesList.get(arg2));
                    unbondDevicesList.remove(arg2);
                    addBondDevicesToListView();
                    addUnbondDevicesToListView();
                } catch (Exception e) {
                    ToastUtil.showToast(MainActivity.this, "配对失败");

                }

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(deviceReceiver);
    }

    @OnClick({R.id.btn_open, R.id.btn_search, R.id.btn_setting})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_open:
                pressTb();
                break;
            case R.id.btn_search:
                searchDevices();
                break;
            case R.id.btn_setting:
                Toast.makeText(MainActivity.this, "设置完毕后要记得点击保存", Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void showBtnText() {
        if (isOpen()) {
            btnOpen.setText("默认设备：" + defaultDevice);
            btnOpen.setEnabled(false);
            btnSearch.setEnabled(true);
        } else {
            btnOpen.setText("打开蓝牙");
            btnSearch.setEnabled(false);
        }
    }

    /**
     * 判断蓝牙是否打开
     *
     * @return boolean
     */
    public boolean isOpen() {
        return mBluetoothAdapter.isEnabled();

    }

    //点击打开/关闭蓝牙
    private void pressTb() {
        if (isOpen()) {
            mBluetoothAdapter.disable();
        } else {
            openBluetooth();
        }
    }

    //打开蓝牙
    private void openBluetooth() {

        if (mBluetoothAdapter == null) {
            ToastUtil.showToast(this, "设备不支持蓝牙");
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }


    /**
     * 搜索蓝牙设备
     */

    public void searchDevices() {

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        //判断是否有权限
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FINE_LOCATION);
            //判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                ToastUtil.showToast(this, "Android 6.0及以上的设备需要用户授权才能搜索蓝牙设备");
            }

        } else {
            startSearch();
        }
    }


    private void startSearch() {
        bondDevicesList.clear();
        unbondDevicesList.clear();
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // The requested permission is granted.
                    startSearch();
                } else {
                    // The user disallowed the requested permission.
                    ToastUtil.showToast(MainActivity.this, "您拒绝授权搜索蓝牙设备！");
                }
                break;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ENABLE_BT) {

            }
        }
    }


    private void initIntentFilter() {
        deviceReceiver = new DeviceReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(deviceReceiver, intentFilter);

    }

    /**
     * 蓝牙广播接收器
     */
    private class DeviceReceiver extends BroadcastReceiver {
        ProgressDialog progressDialog;

        DeviceReceiver(Context context) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("请稍等...");
            progressDialog.setMessage("搜索蓝牙设备中...");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        addBandDevices(device);
                    } else {
                        addUnbondDevices(device);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    progressDialog.show();

                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                        .equals(action)) {
                    System.out.println("设备搜索完毕");
                    progressDialog.dismiss();

                    addUnbondDevicesToListView();
                    addBondDevicesToListView();

                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        btnSearch.setEnabled(true);
                        btnOpen.setText("关闭蓝牙");
                        lvUnboundDevice.setEnabled(true);
                        lvBoundDevice.setEnabled(true);
                    } else if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                        btnOpen.setText("打开蓝牙");
                        btnSearch.setEnabled(false);
                        lvUnboundDevice.setEnabled(false);
                        lvBoundDevice.setEnabled(false);

                    }
                }

            }
        }
    }


    private ArrayList<String> getData(ArrayList<BluetoothDevice> list) {
        ArrayList<String> data = new ArrayList<>();
        int count = list.size();
        for (int i = 0; i < count; i++) {
            String deviceName = list.get(i).getName();
            data.add(deviceName != null ? deviceName : list.get(i).getAddress());
        }
        return data;
    }


    /**
     * 添加已绑定蓝牙设备到ListView
     */
    private void addBondDevicesToListView() {
        boundName.clear();
        boundName.addAll(getData(bondDevicesList));
        boundAdapter.notifyDataSetChanged();


    }


    /**
     * 添加未绑定蓝牙设备到ListView
     */
    private void addUnbondDevicesToListView() {
        unboundName.clear();
        unboundName.addAll(getData(unbondDevicesList));
        unboundAdapter.notifyDataSetChanged();
    }

    /*
    添加未绑定设备
     */
    private void addUnbondDevices(BluetoothDevice device) {
        if (!unbondDevicesList.contains(device)) {
            unbondDevicesList.add(device);
        }
    }

    /*
    添加绑定设备
     */
    private void addBandDevices(BluetoothDevice device) {
        if (!bondDevicesList.contains(device)) {
            bondDevicesList.add(device);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 创建退出对话框
            AlertDialog isExit = new AlertDialog.Builder(this).create();
            // 设置对话框标题
            isExit.setTitle("系统提示");
            // 设置对话框消息
            isExit.setMessage("要关闭这个连接吗？");
            // 添加选择按钮并注册监听
            isExit.setButton("确定", backListener);
            isExit.setButton2("取消", backListener);
            // 显示对话框
            isExit.show();
        }

        return false;
        //return true;
    }

    DialogInterface.OnClickListener backListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
                    System.exit(0);
                    break;
                case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
                    moveTaskToBack(true);
                    break;
                default:
                    break;
            }
        }
    };

}

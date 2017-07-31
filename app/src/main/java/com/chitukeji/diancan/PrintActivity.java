package com.chitukeji.diancan;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class PrintActivity extends AppCompatActivity {

    @BindView(R.id.tv_printdevice)
    TextView tvPrintdevice;
    @BindView(R.id.tv_connect_state)
    TextView tvConnectState;
    @BindView(R.id.et_print_data)
    EditText etPrintData;
    @BindView(R.id.btn_print)
    Button btnPrint;
    @BindView(R.id.btn_command)
    Button btnCommand;
    @BindView(R.id.btn_connect)
    Button btnConnect;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device;
    private boolean isConnection;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private boolean TCPon = false, autoConnent = true;
    private String myName = "", myAddress = "";
    private String[] userData;
    private long dataCount = 0;
    private SoundPool soundPool;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            String response = (String) msg.obj;
            String[] orderData;
            switch (msg.what) {
                case 0:
                    dataCount++;
                    if (dataCount % 10 == 0) etPrintData.setText("");
                    orderData = response.split("\\|");                    //0是订单号，1是打印的内容
                    etPrintData.append(orderData[1]);
                    if (isConnection)
                        try {
                            outputStream.write(CommandsUtil.BYTE_COMMANDS[4]);//加大字体
                            outputStream.write(CommandsUtil.BYTE_COMMANDS[6]);//加粗打印订单号
                            print("\n" + "#" + orderData[0] + "\n");
                            outputStream.write(CommandsUtil.BYTE_COMMANDS[3]);//变小字体
                            outputStream.write(CommandsUtil.BYTE_COMMANDS[5]);//取消加粗
                            print(orderData[1] + userData[1] + "\n\n\n");
                            MediaPlayer player = MediaPlayer.create(PrintActivity.this, R.raw.dingdong);
                            player.start();
                        } catch (IOException e) {
                            // ToastUtil.showToast(PrintActivity.this,"设置指令失败！");
                        }
                    //在这里进行UI操作，将结果显示到界面上
                    break;
                case 1:
                    etPrintData.setText(response);
                    break;
                default:
                    break;
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        ButterKnife.bind(this);

        initData();
        initView();
        startConnect();

        ServerListener mt = new ServerListener();
        new Thread(mt).start();

    }

    private void initData() {
        Intent intent = getIntent();
        device = intent.getParcelableExtra(MainActivity.DEVICE);
        // if (device == null) return;
        String str = readFile("user.ng");
        if (str.length() > 1) {
            userData = str.split("\\|");
        } else {
            autoConnent = false;
            Toast.makeText(PrintActivity.this, "提示，你还未设置商家信息。", Toast.LENGTH_LONG).show();
            finish();
        }
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

    public void writeFile(String fileName, String writestr) {
        try {
            FileOutputStream fout = openFileOutput(fileName, MODE_PRIVATE);
            byte[] bytes = writestr.getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initView() {
        myName = device.getName() == null ? device.getAddress() : device.getName();
        myAddress = device.getAddress();  //获取设备地址，保持这个玩意，下次直接连接
        tvPrintdevice.setText(myName);
    }

    /**
     * 连接蓝牙设备
     */
    private void startConnect() {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();

            isConnection = bluetoothSocket.isConnected();

            if (bluetoothAdapter.isDiscovering()) {
                System.out.println("关闭适配器！");
                bluetoothAdapter.isDiscovering();
            }
            setConnectResult(isConnection);
        } catch (Exception e) {
            setConnectResult(false);
        }
    }

    private void setConnectResult(boolean result) {
        if (result) writeFile("defaultDevice.ng", myAddress);
        tvConnectState.setText(result ? "连接成功！" : "连接失败！");
        btnConnect.setVisibility(result ? View.GONE : View.VISIBLE);
        btnConnect.setEnabled(!result);
    }

    @OnClick({R.id.btn_print, R.id.btn_command, R.id.btn_connect})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:
                startConnect();
                break;
            case R.id.btn_command:
                selectCommand();
                break;
            case R.id.btn_print:
                print(etPrintData.getText().toString());
                break;


        }
    }

    /**
     * 选择指令
     */
    public void selectCommand() {

        new AlertDialog.Builder(this)
                .setTitle("请选择指令")
                .setItems(CommandsUtil.ITEMS, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            outputStream.write(CommandsUtil.BYTE_COMMANDS[which]);
                        } catch (IOException e) {
                            ToastUtil.showToast(PrintActivity.this, "设置指令失败！");
                        }
                    }
                })
                .create()
                .show();
    }

    /**
     * 打印数据
     */
    public void print(String sendData) {

        if (TextUtils.isEmpty(sendData)) {
            ToastUtil.showToast(PrintActivity.this, "请输入打印内容！");
            return;
        }
        if (isConnection) {
            System.out.println("开始打印！！");
            try {
                sendData = sendData;
                byte[] data = sendData.getBytes("gbk");
                outputStream.write(data, 0, data.length);
                outputStream.flush();
            } catch (IOException e) {
                ToastUtil.showToast(PrintActivity.this, "发送失败！");
                tvConnectState.setText("连接丢失！");
                btnConnect.setVisibility(View.VISIBLE);
                btnConnect.setEnabled(true);
            }
        } else {
            ToastUtil.showToast(PrintActivity.this, "设备未连接，请重新连接！");
            tvConnectState.setText("连接丢失！");
            btnConnect.setVisibility(View.VISIBLE);
            btnConnect.setEnabled(true);

        }
    }


    public Socket mySocket;

    class ServerListener implements Runnable {
        public void run() {
            while (autoConnent) {  //自动重连
                if (!TCPon)
                    try {
                        mySocket = new Socket("121.42.24.85", 45612);
                        DataInputStream input = new DataInputStream(mySocket.getInputStream());
                        DataOutputStream ouput = new DataOutputStream(mySocket.getOutputStream());

                        String str1 = userData[0];
                        byte[] a = str1.getBytes();
                        ouput.write(a, 0, str1.length());
                        ouput.flush();

                        TCPon = true;
                        byte[] b = new byte[10000];
                        while (true) {
                            int length = input.read(b);
                            String Msg = new String(b, 0, length, "gb2312");

                            if (Msg.equals("Alive")) {
                                ouput.write("Alive".getBytes(), 0, 5);
                                ouput.flush();
                            } else {
                                ouput.write("OK".getBytes(), 0, 2);
                                ouput.flush();

                                Message message = new Message();
                                message.what = 0;
                                //将服务器返回的结果存放到Message中
                                message.obj = Msg;
                                handler.sendMessage(message);
                            }
                        }

                    } catch (Exception ex) {
                        TCPon = false;
                        Message message = new Message();
                        message.what = 1;
                        //将服务器返回的结果存放到Message中
                        message.obj = ex.toString();
                        handler.sendMessage(message);
                    }

                try {
                    Thread.sleep(3000);  //3s延迟重连
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
/*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
*/

    /**
     * 断开蓝牙设备连接
     */
    public void disconnect() {
        System.out.println("断开蓝牙设备连接");
        try {
            bluetoothSocket.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
       /*if (keyCode == KeyEvent.KEYCODE_BACK )
        {
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
        }*/
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
        }
        return super.onKeyDown(keyCode, event);
        //return true;
    }

    DialogInterface.OnClickListener backListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
                  /*  try{
                        autoConnent=false;
                        mySocket.close();
                    }catch (IOException e){

                    }*/
                    finish();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
                    break;
                default:
                    break;
            }
        }
    };

}

package com.chitukeji.diancan;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/2/12.
 */

public class SettingActivity extends AppCompatActivity {

    @BindView(R.id.btn_save)
    Button btn_save;
    @BindView(R.id.text_user)
    EditText text_user;
    @BindView(R.id.text_info)
    EditText text_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);

        initData();

    }

    private void initData() {        //读取保存的信息
        String str = readFile("user.ng");
        if (str.length() > 1) {
            String[] temp = str.split("\\|");
            text_user.setText(temp[0]);
            text_info.setText(temp[1]);
        }
    }

    @OnClick({R.id.btn_save})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                if (text_user.getText().toString().length() == 0 || text_info.getText().toString().length() == 0) {
                    Toast.makeText(SettingActivity.this, "两项都必须要填写", Toast.LENGTH_LONG).show();
                    break;
                }
                Toast.makeText(SettingActivity.this, "保存成功！", Toast.LENGTH_LONG).show();
                String str = text_user.getText().toString() + "|" + text_info.getText().toString();
                writeFile("user.ng", str);
                finish();
                break;

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
}

package com.beijingepidial.ccpvirus;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Date;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;

import org.apache.commons.lang3.StringUtils;



public class LoginActivity extends AppCompatActivity {

    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Context context = LoginActivity.this.getApplicationContext();
        SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
        final  SharedPreferences.Editor editor = sp.edit();
        ((RadioGroup)findViewById(R.id.rgscan)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rbdown:
                        editor.putString(Variables.SCANTYPE, "down");
                        break;
                    case R.id.rbtube:
                        editor.putString(Variables.SCANTYPE, "tube");
                        break;
                }
                editor.commit();
            }
        });
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(((EditText) findViewById(R.id.etPlatebarcode)).getText())) {
                    Toast.makeText(LoginActivity.this, "Please enter plate barcode!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (((Spinner) findViewById(R.id.spSars)).getSelectedItemPosition() == 0) {
                    Toast.makeText(LoginActivity.this, "Please select assay!", Toast.LENGTH_LONG).show();
                    return;
                }
                String barcode = ((EditText) findViewById(R.id.etPlatebarcode)).getText().toString();
                editor.putString(Variables.PLATECODE, barcode);
                editor.commit();

                startActivity(new Intent(LoginActivity.this, PlateActivity.class));
            }
        });
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });
        findViewById(R.id.btnScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.CAMERA}, Variables.REQ_PERM_CAMERA);
                    return;
                }
                // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
                if (ActivityCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Variables.REQ_PERM_EXTERNAL_STORAGE);
                    return;
                }
                // 二维码扫码
                Intent intent = new Intent(LoginActivity.this, CaptureActivity.class);
                startActivityForResult(intent, Variables.REQ_QR_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String barcode = bundle.getString(Variables.INTENT_EXTRA_KEY_QR_SCAN);
            ((EditText)findViewById(R.id.etPlatebarcode)).setText(barcode);
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

}

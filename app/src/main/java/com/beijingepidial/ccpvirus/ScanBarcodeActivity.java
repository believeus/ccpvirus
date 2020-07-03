package com.beijingepidial.ccpvirus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import com.beijingepidial.entity.Well;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.activity.CaptureActivity;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ScanBarcodeActivity extends AppCompatActivity {
    private Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Toast.makeText(ScanBarcodeActivity.this,"Well barcode does not exist! ",Toast.LENGTH_LONG).show();
            return false;
        }
    });
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_barcode);
        findViewById(R.id.btnScanwell).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 申请相机权限
                if (ActivityCompat.checkSelfPermission(ScanBarcodeActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(ScanBarcodeActivity.this, new String[]{Manifest.permission.CAMERA}, Variables.REQ_PERM_CAMERA);
                    return;
                }
                // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
                if (ActivityCompat.checkSelfPermission(ScanBarcodeActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(ScanBarcodeActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Variables.REQ_PERM_EXTERNAL_STORAGE);
                    return;
                }
                // 二维码扫码
                Intent intent = new Intent(ScanBarcodeActivity.this, CaptureActivity.class);
                startActivityForResult(intent, Variables.REQ_QR_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Bundle bundle = data.getExtras();
            final String barcode = bundle.getString(Variables.INTENT_EXTRA_KEY_QR_SCAN);
            switch (requestCode) {
                case Variables.REQ_QR_CODE:

                    new AsyncTask() {
                        @Override
                        protected Object doInBackground(Object[] objects) {
                            try {
                                OkHttpClient client = new OkHttpClient.Builder()
                                        .connectTimeout(30, TimeUnit.SECONDS)//设置连接超时时间
                                        .readTimeout(20, TimeUnit.SECONDS)//设置读取超时时间
                                        .build();
                                String v = client.newCall(new Request.Builder().url(Variables.host + "plate/findwell.jhtml").post(new FormBody.Builder().add("barcode", barcode).build()).build()).execute().body().string();
                                if (StringUtils.isEmpty(v)){
                                    handler.sendEmptyMessage(0);
                                    return null;
                                }
                                Well well = new Gson().fromJson(v, new TypeToken<Well>() {}.getType());
                                Intent intent = new Intent(ScanBarcodeActivity.this, SendPDFActivity.class);
                                intent.putExtra("well", well);
                                startActivity(intent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();


                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

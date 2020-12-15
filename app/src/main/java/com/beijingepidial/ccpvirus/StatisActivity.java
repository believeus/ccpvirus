package com.beijingepidial.ccpvirus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.beijingepidial.entity.PDF;
import com.beijingepidial.entity.Well;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import www.thl.com.ui.BarChartView;
import www.thl.com.ui.LineChartView;

public class StatisActivity extends AppCompatActivity {
    private BarChartView mBarChartView;
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)//设置连接超时时间
            .readTimeout(20, TimeUnit.SECONDS)//设置读取超时时间
            .build();
    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(final Message msg) {
            new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    try {
                        List<String> xnames = new ArrayList<>();
                        xnames.add("negative");
                        xnames.add("positive");
                        Request request = new Request.Builder().url(Variables.host + "patient/statis.jhtml").build();
                        String v = client.newCall(request).execute().body().string();
                        if (StringUtils.isEmpty(v)) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(StatisActivity.this);
                            dialog.setTitle("Message");
                            dialog.setMessage("No record");
                            dialog.show();
                        } else {
                            List<List<Float>> datas = new ArrayList<>();
                            List<Float> v1 = new ArrayList<>();
                            String[] m=v.split("@");
                            float negative=Integer.parseInt(m[0]);
                            float positive=Integer.parseInt(m[1]);
                            v1.add(negative);
                            v1.add(positive);
                            datas.add(v1);
                            mBarChartView.initData(datas, xnames);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statis);
        mBarChartView = (BarChartView) findViewById(R.id.mBarChartView);
        mBarChartView.setTextColor(Color.parseColor("#bfbccf"));
        mBarChartView.setCanTouch(true);
        mBarChartView.setAnimation(true);
        handler.sendEmptyMessage(0);

    }

}

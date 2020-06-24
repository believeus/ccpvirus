package com.beijingepidial.ccpvirus;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Date;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.text.SimpleDateFormat;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
//        Spinner spinner = findViewById(R.id.spSars);
//        spinner.setSelection(1,true);
        //时间显示
        EditText time1 = findViewById(R.id.etDate);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd  h:mm:ss a");
        Date date = new Date(System.currentTimeMillis());
        time1.setText(simpleDateFormat.format(date));
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, PlateActivity.class));
            }
        });
        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });
    }

    private class PhotoEntity {
    }


}

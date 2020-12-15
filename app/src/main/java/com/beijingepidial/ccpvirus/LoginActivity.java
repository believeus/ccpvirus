package com.beijingepidial.ccpvirus;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        //时间显示
        EditText time1 = findViewById(R.id.etDate);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd  h:mm:ss a");
        Date date = new Date(System.currentTimeMillis());
        time1.setText(simpleDateFormat.format(date));
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(((EditText) findViewById(R.id.etName)).getText())) {
                    Toast.makeText(LoginActivity.this, "Please enter username!", Toast.LENGTH_LONG).show();
                    return;
                }
                if (((Spinner) findViewById(R.id.spSars)).getSelectedItemPosition() == 0) {
                    Toast.makeText(LoginActivity.this, "Please select assay!", Toast.LENGTH_LONG).show();
                    return;
                }

                String vname = ((EditText) findViewById(R.id.etName)).getText().toString();

                Context context = LoginActivity.this.getApplicationContext();
                SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();

                editor.putString(Variables.SESSIONUSER, vname);
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
    }


}

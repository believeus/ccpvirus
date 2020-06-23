package com.beijingepidial.ccpvirus;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import java.util.Date;
import android.widget.EditText;
import android.widget.Spinner;
import java.text.SimpleDateFormat;

public class LoginActivity extends AppCompatActivity {
    private Spinner mSp_game_name;

    @SuppressLint("WrongViewCast")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        mSp_game_name = findViewById(R.id.spSars);
        //Spinner默认null值
        mSp_game_name.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean b = true;
                if (b) {
                    // 主要功能代码；
                    view.setVisibility(View.INVISIBLE);
                } else {
                    //获取到对象
                    PhotoEntity photoEntity = (PhotoEntity) mSp_game_name.getSelectedItem();
                }
                b = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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

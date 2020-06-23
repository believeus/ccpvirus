package com.beijingepidial.ccpvirus;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.beijingepidial.entity.Well;

import java.io.Serializable;
import java.util.Calendar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class SendPDFActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        Well well = (Well)getIntent().getSerializableExtra("well");
        Button btn=(Button)findViewById(R.id.btnColor);
        btn.setText(well.name);
        btn.setTextColor(Color.parseColor("#FFFFFF"));
        btn.getBackground().setColorFilter(Color.parseColor(well.color), PorterDuff.Mode.SRC_IN);



    }

}

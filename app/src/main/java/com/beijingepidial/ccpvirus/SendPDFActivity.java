package com.beijingepidial.ccpvirus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SendPDFActivity extends AppCompatActivity {
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
            .readTimeout(20, TimeUnit.SECONDS)//设置读取超时时间
            .build();
    Properties properties = new Properties();
    private static final int SUCCESS = 0;
    private static final int ERRORPDF = 1;
    private static final int ERROREMAIL = 2;
    private static final int INTDATA = 3;
    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(final Message msg) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(SendPDFActivity.this);
            dialog.setTitle("Message");
            switch (msg.what) {
                case INTDATA:
                    final Well well = (Well) msg.obj;
                    new AsyncTask() {
                        @Override
                        protected Object doInBackground(Object[] objects) {
                            try {
                                FormBody form = new FormBody.Builder().add("wellbarcode", well.barcode).add("wellname", well.name).add("platebarcode", well.parent).build();
                                Request.Builder request = new Request.Builder().url(Variables.host + "patient/result.jhtml");
                                String v = client.newCall(request.post(form).build()).execute().body().string();
                                PDF pdf = new Gson().fromJson(v, PDF.class);
                                Context context = SendPDFActivity.this.getApplicationContext();
                                SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
                                String operator = sp.getString(Variables.SESSIONUSER, "");
                                ((EditText) findViewById(R.id.etOperator)).setText(operator);
                                ((EditText) findViewById(R.id.etWbarcode)).setText(pdf.barcode);
                                ((EditText) findViewById(R.id.etPbarcode)).setText(pdf.parent);
                                ((EditText) findViewById(R.id.etPatient)).setText(pdf.patientname);
                                ((EditText) findViewById(R.id.etEmail)).setText(pdf.email);
                                ((EditText) findViewById(R.id.etNote)).setText(pdf.note);
                                RadioGroup rg = ((RadioGroup) findViewById(R.id.rg));
                                RadioButton radio = (RadioButton) rg.getChildAt(pdf.positive);
                                radio.setChecked(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                    break;
                case SUCCESS:
                    dialog.setMessage("Successfully sent pdf to the email");
                    dialog.show();
                    break;
                case ERRORPDF:
                    dialog.setMessage("Create PDF error");
                    dialog.show();
                    break;
                case ERROREMAIL:
                    dialog.setMessage("Send Email error");
                    dialog.show();
                    break;
            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        final Well well = (Well) getIntent().getSerializableExtra("well");
        Message msg = new Message();
        msg.what = INTDATA;
        msg.obj = well;
        handler.sendMessage(msg);
        Button btn = (Button) findViewById(R.id.btnColor);
        btn.setText(well.name);
        btn.setTextColor(Color.parseColor("#FFFFFF"));
        btn.getBackground().setColorFilter(Color.parseColor(well.color), PorterDuff.Mode.SRC_IN);
        findViewById(R.id.btnYes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.btnYes).setEnabled(false);
                final ProgressDialog dialog = new ProgressDialog(SendPDFActivity.this);
                dialog.setTitle("Message");
                dialog.setMessage("Loading...");
                dialog.setCancelable(false);
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        try {
                            Context context = SendPDFActivity.this.getApplicationContext();
                            SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
                            String operator = sp.getString(Variables.SESSIONUSER, "");
                            Editable editable = ((EditText) findViewById(R.id.etNote)).getText();
                            PDF pdf = new PDF();
                            RadioGroup rg = ((RadioGroup) findViewById(R.id.rg));
                            int checkId = rg.getCheckedRadioButtonId();
                            RadioButton rb = (RadioButton) rg.findViewById(checkId);
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
                            pdf.patientname = ((TextView) findViewById(R.id.etPatient)).getText().toString();
                            pdf.email = ((TextView) findViewById(R.id.etEmail)).getText().toString();
                            pdf.barcode = well.barcode;
                            pdf.color = well.color;
                            pdf.parent = well.parent;
                            pdf.createTime = format.format(new Date());
                            pdf.note = (editable == null) ? "" : editable.toString();
                            pdf.positive = rb.getText().toString().equals("Positive") ? (byte) 1 : (byte) 0;
                            pdf.operator = operator;
                            String v = client.newCall(new Request.Builder().url(Variables.host + "patient/build/pdf.jhtml").post(new FormBody.Builder().add("data", new Gson().toJson(pdf)).build()).build()).execute().body().string();
                            if (v.equals("success")) {
                                dialog.dismiss();
                                findViewById(R.id.btnSave).setEnabled(true);
                            }
                            handler.sendEmptyMessage(v.equals("success") ? SUCCESS : v.equals("error-pdf") ? ERRORPDF : ERROREMAIL);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute();
            }
        });
    }

}

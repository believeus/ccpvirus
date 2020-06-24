package com.beijingepidial.ccpvirus;

import android.app.AlertDialog;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SendPDFActivity extends AppCompatActivity {
    private static final int SUCCESS = 0;
    private static final int ERRORPDF = 1;
    private static final int ERROREMAIL = 2;
    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(SendPDFActivity.this);
            dialog.setTitle("Message");
            switch (msg.what) {
                case SUCCESS:
                    dialog.setMessage("Successfully sent pdf to the specified user email");
                    break;
                case ERRORPDF:
                    dialog.setMessage("Create PDF error");
                    break;
                case ERROREMAIL:
                    dialog.setMessage("Send Email error");
                    break;
            }
            dialog.show();
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        final Well well = (Well) getIntent().getSerializableExtra("well");
        Button btn = (Button) findViewById(R.id.btnColor);
        btn.setText(well.name);
        btn.setTextColor(Color.parseColor("#FFFFFF"));
        btn.getBackground().setColorFilter(Color.parseColor(well.color), PorterDuff.Mode.SRC_IN);
        findViewById(R.id.btnYes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        try {
                            Editable editable = ((EditText) findViewById(R.id.etNote)).getText();
                            PDF pdf = new PDF();
                            RadioGroup rg = ((RadioGroup) findViewById(R.id.rg));
                            int checkId = rg.getCheckedRadioButtonId();
                            RadioButton rb = (RadioButton) rg.findViewById(checkId);
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
                            pdf.name = ((TextView) findViewById(R.id.etName)).getText().toString();
                            pdf.email = ((TextView) findViewById(R.id.etEmail)).getText().toString();
                            pdf.barcode = well.barcode;
                            pdf.color = well.color;
                            pdf.parent=well.parent;
                            pdf.createTime = format.format(new Date());
                            pdf.note = (editable == null) ? "" : editable.toString();
                            pdf.positive = rb.getText().toString().equals("Positive") ? (byte) 1 : (byte) 0;
                            System.out.println(new Gson().toJson(pdf));
                            OkHttpClient client = new OkHttpClient.Builder()
                                    .connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
                                    .readTimeout(20, TimeUnit.SECONDS)//设置读取超时时间
                                    .build();
                            Properties properties = new Properties();
                            properties.load(SendPDFActivity.this.getAssets().open("project.properties"));
                            String url = properties.getProperty("url");
                            String v = client.newCall(new Request.Builder().url(url + "patient/build/pdf.jhtml").post(new FormBody.Builder().add("data", new Gson().toJson(pdf)).build()).build()).execute().body().string();
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

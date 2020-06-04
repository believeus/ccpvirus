package com.beijingepidial.ccpvirus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beijingepidial.entity.Well;
import com.google.gson.Gson;
import com.google.zxing.activity.CaptureActivity;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlateActivity extends AppCompatActivity {
    // request参数
    private final int REQ_QR_CODE = 11002; // // 打开扫描界面请求码
    private final int REQ_PERM_CAMERA = 11003; // 打开摄像头
    private final int REQ_PERM_EXTERNAL_STORAGE = 11004; // 读写文件
    private final String INTENT_EXTRA_KEY_QR_SCAN = "qr_scan_result";
    private final int NAME = 0;
    private final int BARCODE = 1;
    private final int ROW = 3;
    private final int COL = 4;
    private Map<Integer, Well> wells = new HashMap<Integer, Well>();
    private Button[][] box = new Button[12][8];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plate);
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              new AsyncTask<String, Void, String>(){
                  @Override
                  protected String doInBackground(String... strings) {
                      try{
                          Properties properties = new Properties();
                          properties.load(PlateActivity.this.getAssets().open("project.properties"));
                          String url=properties.getProperty("url");
                          String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                          if (StringUtils.isEmpty(barcode)) {
                              Toast.makeText(PlateActivity.this, "Please scan the plate barcode", Toast.LENGTH_LONG).show();
                              return "ERROR";
                          }
                          Gson gson = new Gson();
                          String data = gson.toJson(wells.values());
                          MediaType mediaType = MediaType.get("application/json; charset=utf-8");
                          OkHttpClient client = new OkHttpClient();
                          RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", data).build();
                          Request request = new Request.Builder().url(url+"plate/save.jhtml").post(body).build();
                          Response response = client.newCall(request).execute();//发送请求
                          if (!response.isSuccessful()) {
                          }
                      }catch (Exception e){
                          e.printStackTrace();
                      }
                      return "SUCCESS";
                  }

                  @Override
                  protected void onPostExecute(String s) {
                      super.onPostExecute(s);
                  }
              }.execute();

            }
        });
        findViewById(R.id.btnPlateScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 申请相机权限
                if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.CAMERA}, REQ_PERM_CAMERA);
                    return;
                }
                // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
                if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM_EXTERNAL_STORAGE);
                    return;
                }
                // 二维码扫码
                Intent intent = new Intent(PlateActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQ_QR_CODE);
            }
        });
        GridLayout gridlayout = (GridLayout) findViewById(R.id.gridlayout);
        String[] colname = new String[]{"H", "G", "F", "E", "D", "C", "B", "A"};
        //12行8列
        for (int row = 0; row < gridlayout.getRowCount(); row++) { //12行
            for (int col = 0; col < gridlayout.getColumnCount(); col++) { //8列
                final Button btn = new Button(this);
                String name = colname[col] + (row + 1);
                btn.setText(name);
                final int id = View.generateViewId();
                btn.setId(id);
                btn.setTag(R.id.NAME, name);
                btn.setTag(R.id.ROW, row);
                btn.setTag(R.id.COL, col);
                btn.setRotation(90);
                btn.setBackground(ContextCompat.getDrawable(this, R.drawable.well_shape));
                btn.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        if (btn.getTag(R.id.BARCODE) == null) return;
                        menu.add(0, 1, 0, "BARCODE:" + btn.getTag(R.id.BARCODE).toString());
                        String scantime = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date(Long.valueOf(btn.getTag(R.id.scantime).toString()).longValue()));
                        menu.add(0, 1, 1, "SCANDATE:" + scantime);

                    }
                });
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 申请相机权限
                        if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            // 申请权限
                            ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.CAMERA}, REQ_PERM_CAMERA);
                            return;
                        }
                        // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
                        if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            // 申请权限
                            ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM_EXTERNAL_STORAGE);
                            return;
                        }
                        // 二维码扫码
                        Intent intent = new Intent(PlateActivity.this, CaptureActivity.class);
                        startActivityForResult(intent, id);
                    }
                });
                GridLayout.Spec rowSpec = GridLayout.spec(row);     //设置它的行和列
                GridLayout.Spec columnSpec = GridLayout.spec(col);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                params.setMargins(15, 5, 5, 10);
                params.width = 115;
                params.height = 115;
                params.setGravity(Gravity.CENTER);
                gridlayout.addView(btn, params);

            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(INTENT_EXTRA_KEY_QR_SCAN);
            switch (requestCode) {
                case REQ_QR_CODE:
                    ((EditText) findViewById(R.id.etbarcode)).setText(scanResult);
                    break;
                default:
                    Button btn = (Button) findViewById(requestCode);
                    btn.setBackground(ContextCompat.getDrawable(this, R.drawable.well_scan_shape));
                    btn.setTag(R.id.BARCODE, scanResult);
                    Well well = new Well();
                    well.id = requestCode;
                    well.name = btn.getTag(R.id.NAME).toString();
                    well.row = Integer.valueOf(btn.getTag(R.id.ROW).toString()).intValue();
                    well.col = Integer.valueOf(btn.getTag(R.id.COL).toString()).intValue();
                    well.scantime = System.currentTimeMillis();
                    btn.setTag(R.id.scantime, well.scantime);
                    well.barcode = scanResult;
                    if (wells.get(well.id) != null) {
                        wells.remove(well.id);
                    }
                    wells.put(well.id, well);
                    Toast.makeText(PlateActivity.this, String.valueOf(requestCode), Toast.LENGTH_LONG).show();
                    break;

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
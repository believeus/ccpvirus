package com.beijingepidial.ccpvirus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import com.beijingepidial.entity.Well;
import com.google.gson.Gson;
import com.google.zxing.activity.CaptureActivity;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlateActivity extends AppCompatActivity {
    // request参数
    private Stack<String> stack = new Stack<String>();
    private Map<RectF, Button> rects = new HashMap<RectF, Button>();
    private Map<String, Well> wells = new HashMap<String, Well>();
    private Map<String, Button> mcbox = new HashMap<String, Button>();
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 0:
                        if (stack.size() != 2) return false;
                        String v1 = stack.pop();
                        String v2 = stack.pop();
                        String[] va = v1.split("@");
                        String[] vb = v2.split("@");
                        char taa = va[0].charAt(0);//字符
                        char tba = vb[0].charAt(0);//字符
                        int nab = Integer.valueOf(va[1]);//数字
                        int nbb = Integer.valueOf(vb[1]);//数字
                        if (nab == nbb) {
                            for (int i = 0; i < Math.abs(taa - tba); i++) {
                                int v = taa > tba ? tba : taa;
                                String vi = String.valueOf(((char) (v + i))) + nbb;
                                Button btn = mcbox.get(vi);
                                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_dash));
                            }
                        }
                        Toast.makeText(PlateActivity.this, v1 + ":" + v2, Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        JSONObject jsonObj = new JSONObject(new JSONObject(msg.obj.toString()).getString("data"));
                        Iterator<String> keys = jsonObj.keys();
                        while (keys.hasNext()) {
                            String name = keys.next();
                            JSONObject plate = jsonObj.getJSONObject(name);
                            Button btn = mcbox.get(name);
                            Well well = new Well();
                            well.name = name;
                            well.barcode = plate.getString("barcode");
                            well.scantime = plate.getLong("scantime");
                            well.color = plate.getString("color");
                            wells.put(name, well);
                            btn.setTag(R.id.barcode, well.barcode);
                            btn.setTag(R.id.scantime, well.scantime);
                            btn.setTag(R.id.color, well.color);
                            //有barcode没有颜色
                            if (StringUtils.isNotEmpty(well.barcode) && StringUtils.isEmpty(well.color)) {
                                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_red));
                                //有颜色没有barcode,显示成正方形
                            } else if (StringUtils.isEmpty(well.barcode) && StringUtils.isNotEmpty(well.color)) {
                                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_rect_white));
                                btn.setBackgroundColor(Color.parseColor(well.color));
                                //有颜色有barcode,显示成圆形
                            } else if (StringUtils.isNotEmpty(well.barcode) && StringUtils.isNotEmpty(well.color)) {
                                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_white));
                                Drawable drw = btn.getBackground();
                                drw.setColorFilter(Color.parseColor(well.color), PorterDuff.Mode.SRC_IN);

                            }
                            btn.setTextColor(getResources().getColor(R.color.well_font_white));
                        }
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plate);
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(((EditText) findViewById(R.id.etbarcode)).getText().toString())) {
                    Toast toast = Toast.makeText(PlateActivity.this, "Please scan the Plate barcode!", Toast.LENGTH_LONG);
                    Display display = getWindowManager().getDefaultDisplay();
                    // 获取屏幕高度
                    int height = display.getHeight();
                    toast.setGravity(Gravity.TOP, 0, height / 4);
                    toast.show();
                    return;
                }
                Intent intent = new Intent(PlateActivity.this, CamaraActivity.class);
                intent.putExtra("barcode", ((EditText) findViewById(R.id.etbarcode)).getText().toString());
                startActivityForResult(intent, Variables.SCAN_PLATE_WELL);
            }
        });
        findViewById(R.id.btnPlateScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 申请相机权限
                if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.CAMERA}, Variables.REQ_PERM_CAMERA);
                    return;
                }
                // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
                if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请权限
                    ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Variables.REQ_PERM_EXTERNAL_STORAGE);
                    return;
                }
                // 二维码扫码
                Intent intent = new Intent(PlateActivity.this, CaptureActivity.class);
                startActivityForResult(intent, Variables.REQ_QR_CODE);
            }
        });
        GridLayout gridlayout = (GridLayout) findViewById(R.id.gridlayout);
        final List<String> vname = Arrays.asList(new String[]{"H", "G", "F", "E", "D", "C", "B", "A"});
        //12行8列
        for (int i = 0; i < gridlayout.getRowCount(); i++) { //12行
            for (int j = 0; j < gridlayout.getColumnCount(); j++) { //8列
                final int r = i + 1;
                final int c = j;
                final Button btn = new Button(PlateActivity.this);
                String name = vname.get(c) + r;
                btn.setText(name);
                final int id = View.generateViewId();
                btn.setId(id);
                btn.setTag(R.id.name, name);
                btn.setTextSize(10);
                btn.setRotation(90);
                btn.setTextColor(getResources().getColor(R.color.well_font_color));
                btn.setBackgroundColor(R.drawable.well_setting);
                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well));
                btn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                stack.push(String.valueOf(vname.get(c) + "@" + r));
                                handler.sendEmptyMessage(0);
                                break;
                            case MotionEvent.ACTION_UP:
                                if (!stack.isEmpty()) stack.pop();
                                break;
                        }
                        return false;
                    }
                });
                btn.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        if (StringUtils.isEmpty(((EditText) findViewById(R.id.etbarcode)).getText().toString())) {
                            Toast toast = Toast.makeText(PlateActivity.this, "Please scan the Plate barcode!", Toast.LENGTH_LONG);
                            Display display = getWindowManager().getDefaultDisplay();
                            // 获取屏幕高度
                            int height = display.getHeight();
                            toast.setGravity(Gravity.TOP, 0, height / 4);
                            toast.show();
                            return;
                        }
                        menu.add(0, 1, 0, "Scan barcode").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (StringUtils.isEmpty(((EditText) findViewById(R.id.etbarcode)).getText().toString())) {
                                    Toast toast = Toast.makeText(PlateActivity.this, "Please scan the Plate barcode!", Toast.LENGTH_LONG);
                                    Display display = getWindowManager().getDefaultDisplay();
                                    // 获取屏幕高度
                                    int height = display.getHeight();
                                    toast.setGravity(Gravity.TOP, 0, height / 4);
                                    toast.show();
                                    return true;
                                }
                                // 申请相机权限
                                if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    // 申请权限
                                    ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.CAMERA}, Variables.REQ_PERM_CAMERA);
                                    return true;
                                }
                                // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
                                if (ActivityCompat.checkSelfPermission(PlateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    // 申请权限
                                    ActivityCompat.requestPermissions(PlateActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Variables.REQ_PERM_EXTERNAL_STORAGE);
                                    return true;
                                }
                                // 二维码扫码
                                Intent intent = new Intent(PlateActivity.this, CaptureActivity.class);
                                startActivityForResult(intent, id);
                                return true;
                            }
                        });
                        if (btn.getTag(R.id.barcode) != null) {
                            menu.add(0, 1, 1, "Scan color").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    String name = String.valueOf(btn.getText());
                                    Intent intent = new Intent(PlateActivity.this, ScanwellActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putString("color", btn.getTag(R.id.color).toString());
                                    bundle.putString("barcode", btn.getTag(R.id.barcode).toString());
                                    bundle.putString("name", name);
                                    intent.putExtras(bundle);
                                    startActivityForResult(intent, Variables.SCAN_PLATE_WELL);
                                    return false;
                                }
                            });
                            menu.add(0, 1, 3, "Barcode:" + btn.getTag(R.id.barcode).toString());
                            String scantime = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date(Long.valueOf(btn.getTag(R.id.scantime).toString()).longValue()));
                            menu.add(0, 1, 4, "Scan time:" + scantime);
                        }
                    }
                });

                GridLayout.Spec rowSpec = GridLayout.spec(i);     //设置它的行和列
                GridLayout.Spec columnSpec = GridLayout.spec(j);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                params.setMargins(10, 10, 10, 10);

                params.width = 100;
                params.height = 100;
                params.setGravity(Gravity.CENTER);
                gridlayout.addView(btn, params);
                mcbox.put(name, btn);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return true;
    }

    private void scan(String barcode) {
        ((EditText) findViewById(R.id.etbarcode)).setText(barcode);
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    Properties properties = new Properties();
                    properties.load(PlateActivity.this.getAssets().open("project.properties"));
                    String url = properties.getProperty("url");
                    String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = new FormBody.Builder().add("barcode", barcode).build();
                    Request request = new Request.Builder().url(url + "plate/findData.jhtml").post(body).build();
                    Response response = client.newCall(request).execute();//发送请求
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = response.body().string();
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            final String barcode = bundle.getString(Variables.INTENT_EXTRA_KEY_QR_SCAN);
            if (requestCode == Variables.REQ_QR_CODE || requestCode == Variables.SCAN_PLATE_WELL)
                scan(barcode);
            else {
                new AsyncTask<String, Void, String>() {
                    Handler handler = new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            Well well = (Well) msg.obj;
                            Button btn = (Button) findViewById(requestCode);
                            String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                            if (StringUtils.isNotEmpty(well.color) && StringUtils.isNotEmpty(well.barcode)) {
                                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_white));
                                Drawable drw = btn.getBackground();
                                drw.setColorFilter(Color.parseColor(well.color), PorterDuff.Mode.SRC_IN);
                            } else {
                                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_red));
                            }
                            btn.setTextColor(getResources().getColor(R.color.well_font_white));
                            btn.setTag(R.id.barcode, barcode);
                            btn.setTag(R.id.scantime, well.scantime);
                            btn.setTag(R.id.color, well.color);
                            return false;
                        }
                    });

                    @Override
                    protected String doInBackground(String... strings) {
                        try {
                            if (StringUtils.isEmpty(barcode)) {
                                Toast.makeText(PlateActivity.this, "Please scan the plate barcode", Toast.LENGTH_LONG).show();
                                return "ERROR";
                            }
                            OkHttpClient client = new OkHttpClient();
                            Properties properties = new Properties();
                            properties.load(PlateActivity.this.getAssets().open("project.properties"));
                            String url = properties.getProperty("url");
                            String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                            String v = client.newCall(new Request.Builder().url(url + "plate/findData.jhtml").post(new FormBody.Builder().add("barcode", barcode).build()).build()).execute().body().string();
                            JSONObject bv = StringUtils.isNotEmpty(v) ? new JSONObject(new JSONObject(v).getString("data")) : null;

                            Button btn = (Button) findViewById(requestCode);
                            Well well = new Well();
                            well.name = btn.getTag(R.id.name).toString();
                            well.scantime = System.currentTimeMillis();
                            well.barcode = barcode;
                            if (bv != null && bv.has(String.valueOf(btn.getTag(R.id.name)))) {
                                JSONObject oo = new JSONObject(bv.getString(String.valueOf(btn.getTag(R.id.name))));
                                if (StringUtils.isNotEmpty(oo.getString("color"))) {
                                    well.color = oo.getString("color");
                                } else well.color = "";
                            } else well.color = "";

                            wells.put(well.name, well);
                            Gson gson = new Gson();
                            String data = gson.toJson(wells);
                            RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", data).build();
                            Request request = new Request.Builder().url(url + "plate/save.jhtml").post(body).build();
                            client.newCall(request).execute();//发送请求
                            Message message = new Message();
                            message.obj = well;
                            handler.sendMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return "SUCCESS";
                    }
                }.execute();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
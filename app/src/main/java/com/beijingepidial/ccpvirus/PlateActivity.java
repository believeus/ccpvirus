package com.beijingepidial.ccpvirus;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import com.beijingepidial.entity.Plate;
import com.beijingepidial.entity.Well;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.Stack;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.innovattic.rangeseekbar.RangeSeekBar;

public class PlateActivity extends AppCompatActivity {
    private Map<String, Well> w96 = new HashMap<String, Well>();
    private static final int MULCHECK = 0;
    private static final int RELOAD = -1;
    private static final int SCANPLATE = 1;
    private static final int SGLCHECK = 2;
    private static final int RMCOLOR = 3;
    private static final int CFIlTER = 4;
    private String body;
    private Map<String, Button> checkbtn = new HashMap<String, Button>();
    private Stack<String> stack = new Stack<String>();
    private Map<String, Well> wells = new HashMap<String, Well>();
    private Map<String, Button> initbox = new HashMap<String, Button>();
    private RangeSeekBar rangeseekbar;
    private Handler handler = new Handler(new Handler.Callback() {
        private void loadColor(Button mbv) {
            String barcode = mbv.getTag(R.id.barcode).toString();
            String color = mbv.getTag(R.id.color).toString();
            mbv.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_dash));
            boolean isCheck = Boolean.valueOf(mbv.getTag(R.id.isCheck).toString());
            boolean y = checkbtn.isEmpty() ? false : true;
            findViewById(R.id.btnNext).setVisibility(y ? View.VISIBLE : View.GONE);
            if (StringUtils.equals(color, "@")) {
                mbv.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_rect_white));
                mbv.getBackground().setColorFilter(Color.parseColor("#efefef"), PorterDuff.Mode.SRC_IN);
                mbv.setTextColor(Color.parseColor("#efefef"));
                return;
            }
            if (isCheck) {
                mbv.setTextColor(Color.parseColor("#FFFFFF"));
                mbv.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_circle_dash));
            } else if (StringUtils.isEmpty(barcode)) {
                //没barcode有颜色
                if (StringUtils.isNotEmpty(color)) {
                    mbv.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well_rect_white));
                    mbv.getBackground().setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN);
                    //没barcode没颜色
                } else {
                    //没选中
                    mbv.setBackgroundColor(R.drawable.well_setting);
                    mbv.setTextColor(Color.parseColor("#4D4D4D"));
                    mbv.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well));
                }
            } else {
                mbv.setBackground(ContextCompat.getDrawable(PlateActivity.this, (StringUtils.isNotEmpty(color) ? R.drawable.well_circle_white : R.drawable.well_circle_red)));
                mbv.getBackground().setColorFilter(Color.parseColor(StringUtils.isNotEmpty(color) ? color : "#D81B60"), PorterDuff.Mode.SRC_IN);
            }
        }

        private void loadData(String body, int min, int max) throws Exception {
            Context context = PlateActivity.this.getApplicationContext();
            SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
            String operator = sp.getString(Variables.SESSIONUSER, "");
            JSONObject jsonObj = new JSONObject(new JSONObject(body).getString("data"));
            Iterator<String> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                JSONObject pv = jsonObj.getJSONObject(name);
                Button btn = initbox.get(name);
                Well well = new Well();
                well.name = name;
                well.barcode = pv.getString("barcode");
                well.scantime = pv.getLong("scantime");
                well.color = pv.getString("color");
                well.operator = operator;
                float[] v = new float[3];
                if (StringUtils.isNotEmpty(well.color)) {
                    if (StringUtils.isNotEmpty(well.barcode)) {
                        w96.put(name, well);
                    }
                    Color.colorToHSV(Color.parseColor(well.color), v);
                    if (!(v[0] > min && v[0] < max))
                        well.color = "@";
                } else {
                    well.color = pv.getString("color");
                }
                wells.put(name, well);
                btn.setTag(R.id.barcode, well.barcode);
                btn.setTag(R.id.scantime, well.scantime);
                btn.setTag(R.id.color, well.color);
                btn.setTag(R.id.isCheck, false);
                btn.setTextColor(Color.parseColor(StringUtils.isEmpty(well.color) ? "#4D4D4D" : "#FFFFFF"));
                loadColor(btn);
            }
        }

        @Override
        public boolean handleMessage(final Message msg) {
            try {
                switch (msg.what) {
                    case CFIlTER:
                        int min = msg.arg1;
                        int max = msg.arg2;
                        loadData(PlateActivity.this.body, min, max);
                        break;
                    case RMCOLOR:
                        final String vn = msg.obj.toString();
                        new AsyncTask() {
                            @Override
                            protected Object doInBackground(Object[] objects) {
                                try {
                                    String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                                    OkHttpClient client = new OkHttpClient();
                                    String v = client.newCall(new Request.Builder().url(Variables.host + "plate/findData.jhtml").post(new FormBody.Builder().add("barcode", barcode).build()).build()).execute().body().string();
                                    JSONObject bv = StringUtils.isNotEmpty(v) ? new JSONObject(new JSONObject(v).getString("data")) : null;
                                    if (bv != null) {
                                        JSONObject oo = new JSONObject(bv.getString(vn));
                                        oo.put("color", "");
                                        bv.put(vn, oo);
                                        RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", bv.toString()).build();
                                        Request request = new Request.Builder().url(Variables.host + "plate/rmcolor.jhtml").post(body).build();
                                        client.newCall(request).execute();//发送请求
                                        load(barcode);
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                return null;
                            }
                        }.execute();
                        break;
                    case RELOAD:
                        //还原单选的按钮
                        for (Iterator<Button> it = checkbtn.values().iterator(); it.hasNext(); ) {
                            Button bn = it.next();
                            bn.setBackgroundColor(R.drawable.well_setting);
                            bn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well));
                            bn.setTextColor(Color.parseColor("#4D4D4D"));
                            it.remove();
                        }
                        //重新加载数据
                        rangeseekbar.setMinThumbValue(0);
                        rangeseekbar.setMaxThumbValue(360);
                        loadData(PlateActivity.this.body, 0, 360);
                        findViewById(R.id.flayout).setVisibility(View.VISIBLE);
                        findViewById(R.id.tvColor).setVisibility(View.VISIBLE);
                        break;
                    //多选
                    case MULCHECK:
                        if (stack.size() != 2) break;
                        if (alert("Please scan the Plate barcode!")) break;
                        String v1 = stack.pop();
                        String v2 = stack.pop();
                        String[] va = v1.split("@");
                        String[] vb = v2.split("@");
                        char taa = va[0].charAt(0);//字符
                        char tba = vb[0].charAt(0);//字符
                        int nab = Integer.valueOf(va[1]);//数字
                        int nbb = Integer.valueOf(vb[1]);//数字
                        if (Math.abs(nab - nbb) > 5) {
                            Toast.makeText(PlateActivity.this, "Begin:" + v1 + " End:" + v2 + "\nChoose no more than 6 columns.", Toast.LENGTH_SHORT).show();
                            findViewById(R.id.btnNext).setVisibility(View.GONE);
                        } else {
                            //框选两个手指选择的行与列
                            for (int i = 0; i < (nab > nbb ? Math.abs(nab - nbb + 1) : Math.abs(nab - nbb - 1)); i++) {
                                for (int j = 0; j < (taa < tba ? Math.abs(taa - tba - 1) : Math.abs(taa - tba + 1)); j++) {
                                    int v = taa > tba ? tba : taa;//B
                                    String vi = String.valueOf(((char) (v + j))) + (nab > nbb ? (nbb + i) : (nbb - i));
                                    Button mbv = initbox.get(vi);
                                    mbv.setTag(R.id.isCheck, true);
                                    checkbtn.put(vi, mbv);
                                    loadColor(mbv);
                                }
                            }
                        }
                        findViewById(R.id.flayout).setVisibility(View.GONE);
                        findViewById(R.id.tvColor).setVisibility(View.GONE);
                        break;
                    case SCANPLATE:
                        int _min = rangeseekbar.getMinThumbValue();
                        int _max = rangeseekbar.getMaxThumbValue();
                        if (StringUtils.isEmpty(msg.obj.toString())) {
                            findViewById(R.id.flayout).setVisibility(View.GONE);
                            findViewById(R.id.tvColor).setVisibility(View.GONE);
                            final AlertDialog.Builder dialog = new AlertDialog.Builder(PlateActivity.this);
                            dialog.setTitle("Message");
                            dialog.setMessage("Barcode does not exist!\nCreate?");
                            dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                }

                            });
                            dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    new AsyncTask() {
                                        @Override
                                        protected Object doInBackground(Object[] objects) {
                                            try {
                                                String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                                                OkHttpClient client = new OkHttpClient();
                                                RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", "{}").build();
                                                Request request = new Request.Builder().url(Variables.host + "plate/create.jhtml").post(body).build();
                                                client.newCall(request).execute();//发送请求
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            return null;
                                        }
                                    }.execute();
                                }
                            });
                            dialog.show();
                        } else {
                            loadData(msg.obj.toString(), _min, _max);
                            findViewById(R.id.flayout).setVisibility(View.VISIBLE);
                            findViewById(R.id.tvColor).setVisibility(View.VISIBLE);
                        }
                        break;
                    //单选
                    case SGLCHECK:
                        String vt = msg.obj.toString().replace("@", "");
                        Button bv = initbox.get(vt);
                        boolean isCheck = Boolean.valueOf(bv.getTag(R.id.isCheck).toString()) ? false : true;
                        if (isCheck) checkbtn.put(vt, bv);
                        else checkbtn.remove(vt);
                        bv.setTag(R.id.isCheck, isCheck);
                        if (isValid(checkbtn)) loadColor(bv);
                        findViewById(R.id.flayout).setVisibility(checkbtn.isEmpty() ? View.VISIBLE : View.GONE);
                        findViewById(R.id.tvColor).setVisibility(checkbtn.isEmpty() ? View.VISIBLE : View.GONE);
                        findViewById(R.id.btnNext).setVisibility(!checkbtn.isEmpty() ? View.VISIBLE : View.GONE);
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    });

    private boolean isValid(Map<String, Button> maps) {
        if (maps.size() > 1) {
            //从小到大排序
            List<Object> keys = Arrays.asList(maps.keySet().toArray());
            Collections.sort(keys, new Comparator<Object>() {
                @Override
                public int compare(Object w1, Object w2) {
                    char m1 = w1.toString().replaceAll("[1-9]{1,2}", "").charAt(0);
                    char m2 = w2.toString().replaceAll("[1-9]{1,2}", "").charAt(0);
                    int v1 = Integer.parseInt(w1.toString().replaceAll("[A-H]", ""));
                    int v2 = Integer.parseInt(w2.toString().replaceAll("[A-H]", ""));
                    if (m1 == m2) return v1 - v2;
                    if (v1 == v2) return m1 - m2;
                    return v1 - v2;
                }
            });
            int begin = Integer.parseInt(keys.get(0).toString().replaceAll("[A-H]", ""));
            int end = Integer.parseInt(keys.get(keys.size() - 1).toString().replaceAll("[A-H]", ""));
            if (Math.abs(end - begin) > 5) {
                Toast.makeText(PlateActivity.this, "Begin:" + keys.get(0).toString() + " End:" + keys.get(keys.size() - 1).toString() + "\nChoose no more than 6 columns.", Toast.LENGTH_SHORT).show();
                maps.remove(keys.get(keys.size() - 1).toString());
                findViewById(R.id.btnNext).setVisibility(View.GONE);
                return false;
            }
        }
        return true;
    }

    private boolean alert(String msg) {
        if (StringUtils.isEmpty(((EditText) findViewById(R.id.etbarcode)).getText().toString())) {
            Toast toast = Toast.makeText(PlateActivity.this, msg, Toast.LENGTH_LONG);
            Display display = getWindowManager().getDefaultDisplay();
            // 获取屏幕高度
            int height = display.getHeight();
            toast.setGravity(Gravity.TOP, 0, height / 4);
            toast.show();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plate);
        rangeseekbar = (RangeSeekBar) findViewById(R.id.cfilter);
        rangeseekbar.setSeekBarChangeListener(new RangeSeekBar.SeekBarChangeListener() {
            private int min;
            private int max;

            @Override
            public void onStartedSeeking() {

            }

            @Override
            public void onStoppedSeeking() {

            }

            @Override
            public void onValueChanged(int min, int max) {
                Message msg = new Message();
                msg.what = CFIlTER;
                msg.arg1 = min;
                msg.arg2 = max;
                handler.sendMessage(msg);
            }
        });
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alert("Please scan the Plate barcode!")) return;
                Map<String, List<Well>> m = new HashMap<String, List<Well>>();
                List<Well> wells = new ArrayList<Well>();
                Intent intent = new Intent(PlateActivity.this, ScanwellActivity.class);
                Bundle bundle = new Bundle();
                for (Iterator<Button> it = checkbtn.values().iterator(); it.hasNext(); ) {
                    Button bv = it.next();
                    Well w = new Well();
                    w.name = String.valueOf(bv.getText().toString());
                    w.color = StringUtils.isEmpty(bv.getTag(R.id.color).toString()) ? "#FFFFFF" : bv.getTag(R.id.color).toString();
                    w.barcode = bv.getTag(R.id.barcode).toString();
                    w.parent = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                    wells.add(w);
                }
                Gson gson = new Gson();
                bundle.putString("data", gson.toJson(wells));
                String bc = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                bundle.putString("barcode", bc);
                intent.putExtras(bundle);
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
                final String name = vname.get(c) + r;
                final int id = View.generateViewId();
                btn.setId(id);
                btn.setText(name);
                btn.setTag(R.id.isCheck, false);
                btn.setTag(R.id.name, name);
                btn.setTag(R.id.barcode, "");
                btn.setTag(R.id.color, "");
                btn.setTextSize(10);
                btn.setRotation(90);
                btn.setTextColor(Color.parseColor("#4D4D4D"));
                btn.setBackgroundColor(R.drawable.well_setting);
                btn.setBackground(ContextCompat.getDrawable(PlateActivity.this, R.drawable.well));
                btn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                stack.push(String.valueOf(vname.get(c) + "@" + r));
                                handler.sendEmptyMessage(MULCHECK);
                                break;
                            case MotionEvent.ACTION_UP:
                                if (!stack.isEmpty()) {
                                    Message m = new Message();
                                    m.what = SGLCHECK;
                                    m.obj = stack.pop();
                                    handler.sendMessage(m);
                                }
                                break;
                        }
                        return false;
                    }
                });
                btn.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        if (alert("Please scan the Plate barcode!")) return;
                        menu.add(0, 1, 0, "Scan barcode").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (alert("Please scan the Plate barcode!")) return false;
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
                        if (StringUtils.isNotEmpty(btn.getTag(R.id.barcode).toString())) {
                            menu.add(0, 1, 2, "Barcode:" + btn.getTag(R.id.barcode).toString());
                            String scantime = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date(Long.valueOf(btn.getTag(R.id.scantime).toString()).longValue()));
                            menu.add(0, 1, 3, "Scan time:" + scantime);
                        }
                        if (StringUtils.isNotEmpty(btn.getTag(R.id.color).toString())) {
                            menu.add(0, 1, 4, "Delete color").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    Message msg = new Message();
                                    msg.what = RMCOLOR;
                                    msg.obj = name;
                                    handler.sendMessage(msg);
                                    return false;
                                }
                            });

                        }
                        if (StringUtils.isNotEmpty(btn.getTag(R.id.color).toString())) {
                            if (StringUtils.isNotEmpty(btn.getTag(R.id.barcode).toString())) {
                                menu.add(0, 1, 5, "Send PDF").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        Context context = PlateActivity.this.getApplicationContext();
                                        SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
                                        String operator = sp.getString(Variables.SESSIONUSER, "");
                                        Intent intent = new Intent(PlateActivity.this, SendPDFActivity.class);
                                        Well well = new Well();
                                        well.color = btn.getTag(R.id.color).toString();
                                        well.barcode = btn.getTag(R.id.barcode).toString();
                                        well.name = btn.getTag(R.id.name).toString();
                                        well.parent = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                                        well.operator = operator;
                                        intent.putExtra("well", well);
                                        startActivity(intent);
                                        return false;
                                    }
                                });
                            }
                        }
                        menu.add(0, 1, 6, "Reload").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                handler.sendEmptyMessage(RELOAD);
                                return false;
                            }
                        });
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
                initbox.put(name, btn);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return true;
    }

    private void load(String barcode) {
        final ProgressDialog dialog = new ProgressDialog(PlateActivity.this);
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
        ((EditText) findViewById(R.id.etbarcode)).setText(barcode);
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = new FormBody.Builder().add("barcode", barcode).build();
                    Request request = new Request.Builder().url(Variables.host + "plate/findData.jhtml").post(body).build();
                    final Response response = client.newCall(request).execute();//发送请求
                    if (response.isSuccessful()) dialog.dismiss();
                    Message msg = new Message();
                    msg.what = SCANPLATE;
                    msg.obj = PlateActivity.this.body = response.body().string();
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
            final Bundle bundle = data.getExtras();
            final String barcode = bundle.getString(Variables.INTENT_EXTRA_KEY_QR_SCAN);
            switch (requestCode) {
                case Variables.REQ_QR_CODE:
                case Variables.SCAN_PLATE_WELL:
                    load(barcode);
                    break;
                default:
                    new AsyncTask<String, Void, String>() {
                        Handler handler = new Handler(new Handler.Callback() {
                            @Override
                            public boolean handleMessage(Message msg) {
                                Well well = (Well) msg.obj;
                                Button btn = (Button) findViewById(requestCode);
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
                                if (alert("Please scan the Plate barcode!")) return "NULL";
                                OkHttpClient client = new OkHttpClient();
                                String barcode = ((EditText) findViewById(R.id.etbarcode)).getText().toString();
                                String v = client.newCall(new Request.Builder().url(Variables.host + "plate/findData.jhtml").post(new FormBody.Builder().add("barcode", barcode).build()).build()).execute().body().string();
                                Plate plate = new Gson().fromJson(v, new TypeToken<Plate>() {}.getType());
                                JSONObject bv = StringUtils.isNotEmpty(v) ? new JSONObject(new JSONObject(v).getString("data")) : null;
                                Button btn = (Button) findViewById(requestCode);
                                Context context = PlateActivity.this.getApplicationContext();
                                SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
                                String operator = sp.getString(Variables.SESSIONUSER, "");
                                Well well = new Well();
                                well.name = btn.getTag(R.id.name).toString();
                                well.scantime = System.currentTimeMillis();
                                well.barcode = bundle.getString(Variables.INTENT_EXTRA_KEY_QR_SCAN);
                                well.operator = operator;
                                well.parent=plate.barcode;
                                if (bv != null && bv.has(String.valueOf(btn.getTag(R.id.name)))) {
                                    JSONObject oo = new JSONObject(bv.getString(String.valueOf(btn.getTag(R.id.name))));
                                    if (StringUtils.isNotEmpty(oo.getString("color"))) {
                                        well.color = oo.getString("color");
                                    } else well.color = "";
                                } else well.color = "";

                                Gson gson = new Gson();
                                String data = gson.toJson(well);
                                RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", data).build();
                                Request request = new Request.Builder().url(Variables.host + "barcode/update.jhtml").post(body).build();
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
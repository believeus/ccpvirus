package com.beijingepidial.ccpvirus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beijingepidial.entity.Circle;
import com.beijingepidial.entity.Well;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.innovattic.rangeseekbar.RangeSeekBar;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ScanwellActivity extends AppCompatActivity implements SensorEventListener {
    private List<Well> wells;
    private Map<String, Button> initbox = new HashMap<String, Button>();
    private Map<String, Button> checkbtn = new HashMap<String, Button>();
    private static final int MULCHECK = 0;
    private static final int RELOAD = -1;
    private static final int SCANPLATE = 1;
    private static final int SGLCHECK = 2;
    private SurfaceView svBeforeColor;
    private SurfaceView svAfterColor;
    private StringBuilder sb = new StringBuilder();
    private int radiusmin = 10;
    private int radiusmax = 25;
    private boolean isClone;
    private List<Mat> fmasks = new ArrayList<Mat>();
    private List<Mat> smasks = new ArrayList<Mat>();
    private List<List<Scalar>> scalars = new ArrayList<List<Scalar>>();
    private List<Circle> circles = new ArrayList<Circle>();
    private boolean stop;
    private boolean iterate;
    private Mat imat;
    private JavaCameraView javaCameraView;

    private Mat image;
    private Mat gary;
    private Mat edges;
    private Mat hierarchy;

    private boolean takePhoto;
    private boolean isReTake;
    private boolean isScan;
    private MediaPlayer mediaPlayer;
    //Begin:传感器
    private SensorManager sensorManager;
    private Sensor acc_sensor;
    private Sensor mag_sensor;
    private float[] accValues = new float[3];
    private float[] magValues = new float[3];
    // 旋转矩阵，用来保存磁场和加速度的数据
    private float r[] = new float[9];
    // 模拟方向传感器的数据（原始数据为弧度）
    private float values[] = new float[3];

    private LevelView levelView;
    private TextView tvHorz;
    private TextView tvVert;
    private static final int IDENTIFY = 0;
    private static final int CATCHCOLOR = 1;
    private static final int STOP = 3;
    private Stack<String> stack = new Stack<String>();
    private Handler handle = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SGLCHECK:
                    String vt = msg.obj.toString();
                    Button bv = initbox.get(vt);
                    boolean isCheck = Boolean.valueOf(bv.getTag(R.id.isCheck).toString()) ? false : true;
                    if (isCheck) checkbtn.put(vt, bv);
                    else checkbtn.remove(vt);
                    bv.setTag(R.id.isCheck, isCheck);
                    String color = bv.getTag(R.id.color).toString();
                    bv.setBackground(ContextCompat.getDrawable(ScanwellActivity.this, !isCheck ? R.drawable.well_circle_white : R.drawable.well_circle_dash));
                    bv.getBackground().setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_IN);
                    break;
                case IDENTIFY:
                    String text = String.valueOf(msg.obj);
                    ((TextView) findViewById(R.id.tvmsg)).setText(text);
                    break;
                case CATCHCOLOR:
                    findViewById(R.id.llColor).setVisibility(View.VISIBLE);
                    Map<String, Integer> ybox = new HashMap<String, Integer>();
                    Map<String, Integer> xbox = new HashMap<String, Integer>();
                    Map<String, Integer> rowm = new HashMap<String, Integer>();
                    Map<String, Integer> colm = new HashMap<String, Integer>();
                    SurfaceHolder holder = svAfterColor.getHolder();
                    Canvas canvas = holder.lockCanvas();
                    Paint paint = new Paint();
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Utils.px2dp(1));
                    paint.setTextSize(Utils.px2dp(120));
                    paint.setColor(Color.parseColor("#FFFFFF"));//白色
                    int w = svAfterColor.getWidth();
                    int h = svAfterColor.getHeight();
                    String[] rowname = {"A", "B", "C", "D", "E", "F", "G", "H"};
                    String[] colname = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
                    int xDelta = w / 12;
                    int yDelta = h / 8;
                    Circle _c1 = circles.get(0);
                    Circle _c2 = circles.get(circles.size() - 1);
                    char c1 = _c1.name.replaceAll("[1-9]{1,2}", "").charAt(0);
                    char c2 = _c2.name.replaceAll("[1-9]{1,2}", "").charAt(0);
                    int v1 = Integer.parseInt(_c1.name.replaceAll("[A-H]", ""));
                    int v2 = Integer.parseInt(_c2.name.replaceAll("[A-H]", ""));
                    GridLayout layout = (GridLayout) findViewById(R.id.gridlayout);
                    layout.setRowCount(v2 - v1 + 1);
                    layout.setColumnCount(8);
                    for (int i = 0; i < rowname.length; i++) {
                        int y = Utils.px2dp((i * (yDelta + 100)) + 245);
                        canvas.drawText(rowname[i], Utils.px2dp(30), y, paint);
                        ybox.put(rowname[i], y);
                        colm.put(rowname[i], 7 - i);
                    }
                    for (int i = v1 - 1, j = 0; i < v2; i++, j++) {
                        int x = Utils.px2dp((j * (xDelta + 150)) + 200);
                        canvas.drawText(colname[i], x, Utils.px2dp(150), paint);
                        xbox.put(colname[i], x);
                        rowm.put(colname[i], j);
                    }
                    paint.setStrokeWidth(Utils.px2dp(50));
                    for (Iterator<Circle> it = circles.iterator(); it.hasNext(); ) {
                        final Circle cl = it.next();
                        char c = cl.name.replaceAll("[1-9]{1,2}", "").charAt(0);
                        int v = Integer.parseInt(cl.name.replaceAll("[A-H]", ""));
                        int y = ybox.get(String.valueOf(c));
                        int x = xbox.get(String.valueOf(v));
                        double[] rgb = image.get(cl.y, cl.x);
                        String cval = String.format("#%02x%02x%02x", (int) rgb[0], (int) rgb[1], (int) rgb[2]);
                        paint.setColor(Color.parseColor(cval));
                        canvas.drawCircle(x + Utils.px2dp(30), y - Utils.px2dp(30), 10, paint);
                        int i = rowm.get(String.valueOf(v));
                        int j = colm.get(String.valueOf(c));
                        GridLayout.Spec rowSpec = GridLayout.spec(i);     //设置它的行和列
                        GridLayout.Spec columnSpec = GridLayout.spec(j);
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
                        params.setMargins(10, 10, 10, 10);
                        Button btn = new Button(ScanwellActivity.this);
                        btn.setText(cl.name);
                        btn.setId(View.generateViewId());
                        btn.setTag(R.id.isCheck, false);
                        btn.setTag(R.id.name, cl.name);
                        btn.setTag(R.id.color, cval);
                        btn.setTextSize(10);
                        btn.setRotation(90);
                        btn.setTextColor(Color.parseColor("#FFFFFF"));
                        btn.setBackground(ContextCompat.getDrawable(ScanwellActivity.this, R.drawable.well_circle_white));
                        btn.getBackground().setColorFilter(Color.parseColor(cval), PorterDuff.Mode.SRC_IN);
                        btn.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        stack.push(cl.name);
                                        handle.sendEmptyMessage(MULCHECK);
                                        break;
                                    case MotionEvent.ACTION_UP:
                                        if (!stack.isEmpty()) {
                                            Message m = new Message();
                                            m.what = SGLCHECK;
                                            m.obj = stack.pop();
                                            handle.sendMessage(m);
                                        }
                                        break;
                                }
                                return false;
                            }
                        });
                        initbox.put(cl.name, btn);
                        params.width = 100;
                        params.height = 100;
                        params.setGravity(Gravity.CENTER);
                        layout.addView(btn, params);
                    }
                    holder.unlockCanvasAndPost(canvas);


                    break;
                case STOP:
                    findViewById(R.id.btnCatchColor).setEnabled(true);
                    findViewById(R.id.btnReTake).setEnabled(true);
                    findViewById(R.id.btnPause).setEnabled(false);
                    findViewById(R.id.tvmsg).setVisibility(View.GONE);
                    break;
            }

            return false;
        }
    });

    //End:传感器
    public ScanwellActivity() {
        List<Scalar> obj = new ArrayList<Scalar>();
        obj.add(0, new Scalar(0, 43, 46));
        obj.add(1, new Scalar(180, 255, 255));
        scalars.add(0, obj);
        isScan = true;
        this.isClone = true;
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    javaCameraView.enableView();
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 禁用横屏
        //拍照时调用声音
        final AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        if (volume != 0) {
            Uri uri = Uri.parse("file:///system/media/audio/ui/camera_click.ogg");
            mediaPlayer = MediaPlayer.create(this, uri);
        }
        //保持螢幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.scan_well);
        ((EditText) findViewById(R.id.etScansize)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                stop = true;
                ((Button) findViewById(R.id.btnPause)).setText("start");
                return false;
            }
        });
        svBeforeColor = (SurfaceView) findViewById(R.id.svBeforeCol);
        svAfterColor = (SurfaceView) findViewById(R.id.svAfterCol);
        Bundle bundle = getIntent().getExtras();
        wells = new Gson().fromJson(bundle.getString("data"), new TypeToken<ArrayList<Well>>() {
        }.getType());
        ((TextView) findViewById(R.id.tvPicksize)).setText(String.valueOf(wells.size()));
        ((EditText) findViewById(R.id.etScansize)).setText(String.valueOf(wells.size()));
        SurfaceHolder holder = svBeforeColor.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas canvas = holder.lockCanvas();
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Utils.px2dp(1));
                paint.setTextSize(Utils.px2dp(120));
                paint.setColor(Color.parseColor("#FFFFFF"));//白色
                int w = svBeforeColor.getWidth();
                int h = svBeforeColor.getHeight();
                //从小到大排序
                Collections.sort(wells, new Comparator<Well>() {
                    @Override
                    public int compare(Well w1, Well w2) {
                        char m1 = w1.name.replaceAll("[1-9]{1,2}", "").charAt(0);
                        char m2 = w2.name.replaceAll("[1-9]{1,2}", "").charAt(0);
                        int v1 = Integer.parseInt(w1.name.replaceAll("[A-H]", ""));
                        int v2 = Integer.parseInt(w2.name.replaceAll("[A-H]", ""));
                        if (m1 == m2) return v1 - v2;
                        if (v1 == v2) return m1 - m2;
                        return v1 - v2;
                    }
                });
                int begin = Integer.parseInt(wells.get(0).name.replaceAll("[A-H]", ""));
                int end = Integer.parseInt(wells.get(wells.size() - 1).name.replaceAll("[A-H]", ""));
                String[] rowname = {"A", "B", "C", "D", "E", "F", "G", "H"};
                String[] colname = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
                int xDelta = w / 12;
                int yDelta = h / 8;
                Map<String, Integer> ybox = new HashMap<String, Integer>();
                Map<String, Integer> xbox = new HashMap<String, Integer>();
                for (int i = 0; i < rowname.length; i++) {
                    int y = Utils.px2dp((i * (yDelta + 100)) + 245);
                    ybox.put(rowname[i], y);
                    canvas.drawText(rowname[i], Utils.px2dp(30), y, paint);
                }
                for (int i = begin - 1, j = 0; i < end; i++, j++) {
                    int x = Utils.px2dp((j * (xDelta + 150)) + 200);
                    xbox.put(colname[i], x);
                    canvas.drawText(colname[i], x, Utils.px2dp(150), paint);
                }
                paint.setStrokeWidth(Utils.px2dp(50));
                for (Iterator<Well> it = wells.iterator(); it.hasNext(); ) {
                    Well w1 = it.next();
                    char c = w1.name.replaceAll("[1-9]{1,2}", "").charAt(0);
                    int v = Integer.parseInt(w1.name.replaceAll("[A-H]", ""));
                    int y = ybox.get(String.valueOf(c));
                    int x = xbox.get(String.valueOf(v));
                    paint.setColor(Color.parseColor(StringUtils.isEmpty(w1.color) ? "#FFFFFF" : w1.color));
                    canvas.drawCircle(x + Utils.px2dp(30), y - Utils.px2dp(30), 10, paint);
                }
                holder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        Canvas canvas = holder.lockCanvas();

        //Begin:传感器
        levelView = (LevelView) findViewById(R.id.gv_hv);
        tvVert = (TextView) findViewById(R.id.tvv_vertical);
        tvHorz = (TextView) findViewById(R.id.tvv_horz);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //End:传感器
        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setZOrderOnTop(true);
        javaCameraView.setZOrderMediaOverlay(true);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            private List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            @Override
            public void onCameraViewStarted(int width, int height) {
                ScanwellActivity.this.gary = new Mat();
                ScanwellActivity.this.edges = new Mat();
                ScanwellActivity.this.hierarchy = new Mat();

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                //区间移动时,stop为false,禁止是为true
                if (stop) return imat;
                int size = Integer.valueOf(((EditText) findViewById(R.id.etScansize)).getText().toString());
                image = inputFrame.rgba().clone();
                Mat imgClone = image.clone();
                if (isScan) {
                    if (takePhoto) {
                        circles.clear();
                        contours.clear();
                        Mat imatClone = imat.clone();
                        Imgproc.cvtColor(imat, imat, Imgproc.COLOR_RGB2GRAY);
                        Imgproc.GaussianBlur(imat, imat, new Size(11, 11), 0);
                        Imgproc.Canny(imat, imat, 20, 160, 3, false);
                        //RETR_EXTERNAL只检测外围轮廓
                        Imgproc.findContours(imat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                        for (int i = 0, flen = contours.size(); i < flen; i++) {
                            Imgproc.drawContours(imat, contours, i, new Scalar(0, 255, 0), 1);
                            MatOfPoint2f point2f = new MatOfPoint2f(contours.get(i).toArray());
                            Point center = new Point();
                            float[] radius = new float[1];
                            //获取点集最小圆
                            Imgproc.minEnclosingCircle(point2f, center, radius);
                            int r = (int) radius[0];
                            if (r < radiusmin || r > radiusmax) {
                                continue;
                            }
                            sb.append(r).append(":");
                            Circle circle = new Circle();
                            circle.id = i;
                            circle.x = ((int) center.x);
                            circle.y = ((int) center.y);
                            circle.radius = r;
                            circles.add(circle);
                        }
                        if (circles.size() != size) {
                            Message message = new Message();
                            message.what = IDENTIFY;
                            message.obj = "Recognizing size:" + (circles.size() + 1) + "\nRadius:" + sb.toString();
                            handle.sendMessage(message);
                            sb.delete(0, sb.length());
                            iterate = true;
                            takePhoto = false;
                            return imatClone;
                        }

                        handle.sendEmptyMessage(STOP);
                        //分组
                        List<List<Well>> lbox = Utils.groupBy(wells, new Comparator<Well>() {
                            @Override
                            public int compare(Well _c1, Well _c2) {
                                int v1 = Integer.parseInt(_c1.name.replaceAll("[A-H]", ""));
                                int v2 = Integer.parseInt(_c2.name.replaceAll("[A-H]", ""));
                                return v1 - v2;
                            }
                        });

                        //排序分组
                        Collections.sort(circles, new Comparator<Circle>() {
                            @Override
                            public int compare(Circle o1, Circle o2) {
                                return o1.x - o2.x;
                            }
                        });
                        List<List<Circle>> cbox = new ArrayList<>();
                        for (int i = 0; i < lbox.size(); i++) {
                            int begin = (i == 0 ? 0 : lbox.get(i - 1).size());
                            int end = begin + lbox.get(i).size();
                            List<Circle> cs = circles.subList(begin, end);
                            Collections.sort(cs, new Comparator<Circle>() {
                                @Override
                                public int compare(Circle o1, Circle o2) {
                                    return o1.y - o2.y;
                                }
                            });
                            cbox.add(cs);
                        }

                        for (int i = 0; i < cbox.size(); i++) {
                            List<Circle> circles = cbox.get(i);
                            for (int j = 0; j < circles.size(); j++) {
                                Circle c = circles.get(j);
                                c.name = lbox.get(i).get(j).name;
                                Imgproc.circle(imgClone, new Point(c.x, c.y), c.radius, new Scalar(255, 0, 0), 2, 8);//绘制圆
                                Imgproc.putText(imgClone, c.name, new Point(c.x, c.y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 3);
                            }
                        }
                        stop = true;
                        iterate = false;
                        takePhoto = false;
                        imat = imgClone;
                        return imat;
                    }
                    //颜色提取
                    Mat hmat = new Mat();
                    Imgproc.cvtColor(imgClone, hmat, Imgproc.COLOR_RGB2HSV);
                    for (int i = 0, len = scalars.size(); i < len; i++) {
                        List<Scalar> scalar = scalars.get(i);
                        fmasks.add(i, new Mat());
                        Core.inRange(hmat, scalar.get(0), scalar.get(1), fmasks.get(i));
                        if (i > 0) {
                            smasks.add((i - 1), new Mat());
                            if (i == 1) {
                                Core.bitwise_or(fmasks.get(i - 1), fmasks.get(i), smasks.get(i - 1));
                            } else if (i > 1) {
                                Core.bitwise_or(smasks.get(i - 2), fmasks.get(i), smasks.get(i - 1));
                            }
                        }
                    }
                    int fLen = fmasks.size();
                    int sLen = smasks.size();
                    Core.bitwise_and(imgClone, imgClone, hmat, fLen == 1 ? fmasks.get(fLen - 1) : smasks.get(sLen - 1));
                    imat = hmat.clone();
                    smasks.clear();
                    fmasks.clear();
                    if (iterate) takePhoto = true;
                    return hmat;
                }
                return imgClone;
            }

        });
        //拍照
        levelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (stop){Toast.makeText(ScanwellActivity.this,"Please click the start button",Toast.LENGTH_SHORT).show(); return;}
                    takePhoto = true;
                    isClone = true;
                    isReTake = false;
                    findViewById(R.id.etScansize).setEnabled(false);
                    findViewById(R.id.btnCatchColor).setEnabled(true);
                    findViewById(R.id.btnReTake).setEnabled(true);
                    findViewById(R.id.tvmsg).setVisibility(View.VISIBLE);
                    //播放相机拍照的声音
                    if (mediaPlayer != null)
                        mediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.btnPause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (StringUtils.isEmpty(((EditText) findViewById(R.id.etScansize)).getText().toString())) {
                    if (stop) {
                        Toast.makeText(ScanwellActivity.this, "Please enter the scan size!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                stop = stop ? false : true;
                findViewById(R.id.etScansize).setEnabled(stop ? true : false);
                String vb = ((Button) findViewById(R.id.btnPause)).getText().toString();
                ((Button) findViewById(R.id.btnPause)).setText(vb.equals("start") ? "pause" : "start");
            }
        });

        findViewById(R.id.btnCatchColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.btnSave).setVisibility(View.VISIBLE);
                findViewById(R.id.tvTip).setVisibility(View.VISIBLE);
                Message msg = new Message();
                msg.what = CATCHCOLOR;
                msg.obj = new Gson().toJson(circles);
                handle.sendMessage(msg);

            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        try {
                            Bundle bundle = getIntent().getExtras();
                            String barcode = bundle.getString("barcode");
                            OkHttpClient client = new OkHttpClient();
                            String v = client.newCall(new Request.Builder().url(Variables.host + "plate/findData.jhtml").post(new FormBody.Builder().add("barcode", barcode).build()).build()).execute().body().string();
                            JSONObject bv = StringUtils.isNotEmpty(v) ? new JSONObject(new JSONObject(v).getString("data")) : null;
                            Context context = ScanwellActivity.this.getApplicationContext();
                            SharedPreferences sp = context.getSharedPreferences(Variables.APPNAME, Activity.MODE_PRIVATE);
                            String operator = sp.getString(Variables.SESSIONUSER, "");
                            if (bv != null) {
                                Collection<Button> values = checkbtn.isEmpty() ? initbox.values() : checkbtn.values();
                                for (Iterator<Button> it = values.iterator(); it.hasNext(); ) {
                                    Button btn = it.next();
                                    String n = btn.getTag(R.id.name).toString();
                                    String c = btn.getTag(R.id.color).toString();
                                    if (bv.has(n)) {
                                        JSONObject oo = new JSONObject(bv.getString(n));
                                        oo.put("color", c);
                                        oo.put("operator",operator);
                                        bv.put(n, oo);
                                    } else {
                                        Well w = new Well();
                                        w.name = n;
                                        w.color = c;
                                        w.barcode = "";
                                        w.operator=operator;
                                        JSONObject oo = new JSONObject(new Gson().toJson(w));
                                        bv.put(n, oo);
                                    }
                                }
                                RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", bv.toString()).build();
                                Request request = new Request.Builder().url(Variables.host + "plate/savewell.jhtml").post(body).build();
                                client.newCall(request).execute();//发送请求
                                Bundle vbund = new Bundle();
                                vbund.putString(Variables.INTENT_EXTRA_KEY_QR_SCAN, barcode);
                                setResult(RESULT_OK, new Intent().putExtras(vbund));
                                finish();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute();


            }
        });

        findViewById(R.id.btnReTake).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.tvTip).setVisibility(View.GONE);
                findViewById(R.id.btnCatchColor).setEnabled(false);
                findViewById(R.id.btnReTake).setEnabled(false);
                findViewById(R.id.btnSave).setVisibility(View.GONE);
                isReTake = true;
                stop = false;
            }
        });
        ((RangeSeekBar) findViewById(R.id.sbarRadius)).setSeekBarChangeListener(new RangeSeekBar.SeekBarChangeListener() {
            private int min = 10;
            private int max = 25;

            @Override
            public void onStartedSeeking() {

            }

            @Override
            public void onStoppedSeeking() {
                ScanwellActivity.this.radiusmin = min;
                ScanwellActivity.this.radiusmax = max;
            }

            @Override
            public void onValueChanged(int min, int max) {
                this.min = min;
                this.max = max;
                ((TextView) findViewById(R.id.tvradius)).setText("Circle radius:" + min + "~" + max);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        // 取消方向传感器的监听
        sensorManager.unregisterListener(this);
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Begin:传感器
        acc_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        // 给传感器注册监听：
        sensorManager.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mag_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //End:传感器
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // 获取手机触发event的传感器的类型
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                accValues = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magValues = event.values.clone();
                break;
        }
        SensorManager.getRotationMatrix(r, null, accValues, magValues);
        SensorManager.getOrientation(r, values);
        // 获取　沿着Z轴转过的角度
        float azimuth = values[0];
        // 获取　沿着X轴倾斜时　与Y轴的夹角
        float pitchAngle = values[1];
        // 获取　沿着Y轴的滚动时　与X轴的角度
        //此处与官方文档描述不一致，所在加了符号（https://developer.android.google.cn/reference/android/hardware/SensorManager.html#getOrientation(float[], float[])）
        float rollAngle = -values[2];
        onAngleChanged(rollAngle, pitchAngle, azimuth);

    }

    /**
     * 角度变更后显示到界面
     *
     * @param rollAngle
     * @param pitchAngle
     * @param azimuth
     */
    private void onAngleChanged(float rollAngle, float pitchAngle, float azimuth) {
        levelView.setAngle(rollAngle, pitchAngle);
        tvHorz.setText(String.valueOf((int) Math.toDegrees(rollAngle)) + "°");
        tvVert.setText(String.valueOf((int) Math.toDegrees(pitchAngle)) + "°");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
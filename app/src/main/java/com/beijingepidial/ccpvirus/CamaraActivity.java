package com.beijingepidial.ccpvirus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.beijingepidial.entity.Circle;
import com.beijingepidial.entity.GridCol;
import com.beijingepidial.entity.GridRow;
import com.beijingepidial.entity.RGB;
import com.beijingepidial.entity.Well;
import com.google.gson.Gson;
import com.innovattic.rangeseekbar.RangeSeekBar;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CamaraActivity extends AppCompatActivity implements SensorEventListener {
    private boolean isClone;
    private Map<String, HashMap<String, String>> mrc = new HashMap<String, HashMap<String, String>>();
    private List<Mat> fmasks = new ArrayList<Mat>();
    private List<Mat> smasks = new ArrayList<Mat>();
    private List<List<Scalar>> scalars = new ArrayList<List<Scalar>>();
    private List<Circle> circles = new ArrayList<Circle>();
    private boolean stop;
    private boolean iterate;
    private Mat imat;
    private Scalar scalar = new Scalar(0, 0, 0);
    private JavaCameraView javaCameraView;
    private RGB[][] rgbs;
    private Circle[][] clbox;
    private GridCol gridCols[];
    private GridRow gridRows[];
    private SurfaceView svColorPlate;

    private Mat image;
    private Mat gary;
    private Mat edges;
    private Mat hierarchy;
    //圆半径
    private int radius;
    private int rx;
    private int ry;
    //左上角点x坐标点
    private int lx;
    //左上角点y坐标点
    private int ly;
    private int col;
    private int row;
    private int xArea;
    private int yArea;
    private int width;
    private int height;
    private boolean init;
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
    private Handler handle = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                findViewById(R.id.lysetting).setVisibility(View.VISIBLE);
            }
            String text = String.valueOf(msg.obj);
            ((TextView) findViewById(R.id.tvmsg)).setText(text);
            return false;
        }
    });

    //End:传感器
    public CamaraActivity() {
        List<Scalar> obj = new ArrayList<Scalar>();
        obj.add(0, new Scalar(0, 43, 46));
        obj.add(1, new Scalar(180, 255, 255));
        scalars.add(0, obj);
        isScan = true;
        rgbs = new RGB[8][12];
        this.clbox = new Circle[8][12];
        this.gridCols = new GridCol[12];
        this.gridRows = new GridRow[8];
        this.init = true;
        this.isClone = true;
        this.lx = 50;
        this.ly = 50;
        this.rx = 50;
        this.ry = 50;
        this.col = gridCols.length;
        this.row = gridRows.length;
        for (int r = 0; r < row; r++) {
            for (int c = 0; c < col; c++) {
                rgbs[r][c] = new RGB();
            }
        }
        //初始化96圓
        for (int r = 0; r < row; r++) {
            for (int c = 0; c < col; c++) {
                clbox[r][c] = new Circle();
            }
        }
        //初始化12列
        for (int i = 0; i < 12; i++) {
            gridCols[i] = new GridCol(String.valueOf(i + 1));
        }
        //初始化8行
        for (int i = 0; i < 8; i++) {
            gridRows[i] = new GridRow(Character.toString((char) (65 + i)));
        }
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
        AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        if (volume != 0) {
            Uri uri = Uri.parse("file:///system/media/audio/ui/camera_click.ogg");
            mediaPlayer = MediaPlayer.create(this, uri);
        }
        //保持螢幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camara);
        //Begin:传感器
        levelView = (LevelView) findViewById(R.id.gv_hv);
        tvVert = (TextView) findViewById(R.id.tvv_vertical);
        tvHorz = (TextView) findViewById(R.id.tvv_horz);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //End:传感器
        svColorPlate = (SurfaceView) findViewById(R.id.svColorPlate);
        svColorPlate.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas canvas = holder.lockCanvas();
                Paint paint = new Paint();
                paint.setTextSize(40);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(5);
                canvas.drawText("Dear User ^-^:", 60, 100, paint);
                canvas.drawText("Click 'Catch' Button to get color", 60, 150, paint);
                canvas.drawText("The color will be displayed here", 60, 200, paint);
                holder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setZOrderOnTop(true);
        javaCameraView.setZOrderMediaOverlay(true);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            private List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            @Override
            public void onCameraViewStarted(int width, int height) {
                CamaraActivity.this.gary = new Mat();
                CamaraActivity.this.edges = new Mat();
                CamaraActivity.this.hierarchy = new Mat();

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                //区间移动时,stop为false,禁止是为true
                if (stop) return imat;
                //w=1080
                final int w = CamaraActivity.this.width = inputFrame.rgba().cols();
                //h=1440
                final int h = CamaraActivity.this.height = inputFrame.rgba().rows();
                if (init)
                    CamaraActivity.this.xArea = (w - rx - lx) / col;
                if (init)
                    CamaraActivity.this.yArea = (h - ry - ly) / row;
                init = false;
                image = inputFrame.rgba().clone();
                Mat imgClone = image.clone();
                if (isScan) {
                    //边界检测
                    if (takePhoto) {
                        circles.clear();
                        contours.clear();
                        mrc.clear();
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
                            if (r <= 6 || r > 30) {
                                continue;
                            }
                            Circle circle = new Circle();
                            circle.id = i;
                            circle.x = ((int) center.x);
                            circle.y = ((int) center.y);
                            circle.radius = r;
                            circles.add(circle);

                        }
                        int nsize = Integer.valueOf(((EditText) findViewById(R.id.edtGridsize)).getText().toString());
                        if (circles.size() != nsize) {
                            Message msg = new Message();
                            msg.obj = "Recognized SIZE：" + circles.size();
                            handle.sendMessage(msg);
                            iterate = true;
                            takePhoto = false;
                            return imatClone;
                        }
                        Message msg = new Message();
                        msg.obj = "Recognized Num：" + circles.size();
                        handle.sendMessage(msg);
                        for (int i = 0; i < circles.size(); i++) {
                            Circle c = circles.get(i);
                            Imgproc.circle(imgClone, new Point(c.x, c.y), c.radius, new Scalar(255, 0, 0), 2, 8);//绘制圆
                            Imgproc.putText(imgClone, String.valueOf(i), new Point(c.x, c.y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 3);
                        }

                        Collections.sort(circles, new Comparator<Circle>() {
                            @Override
                            public int compare(Circle o1, Circle o2) {
                                return o1.x - o2.x;
                            }
                        });
                        Circle[][] cbox = new Circle[8][circles.size() / 8];
                        //每一列取8个
                        for (int i = 0, len = circles.size() / 8; i < len; i++) {
                            List<Circle> cs = circles.subList(i * 8, 7 + (i * 8) + 1);
                            Collections.sort(cs, new Comparator<Circle>() {
                                @Override
                                public int compare(Circle o1, Circle o2) {
                                    return o1.y - o2.y;
                                }
                            });
                            for (int j = 0; j < 8; j++) {
                                cbox[j][i] = cs.get(j);
                            }
                        }
                        try {
                            SurfaceHolder holder = svColorPlate.getHolder();
                            Canvas canvas = holder.lockCanvas();
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            Paint whiteP = new Paint();
                            whiteP.setStyle(Paint.Style.STROKE);
                            whiteP.setTextSize(40);
                            int xArea = svColorPlate.getWidth() / 12;
                            int yArea = svColorPlate.getHeight() / 8;
                            whiteP.setColor(Color.WHITE);
                            String[] rowname = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
                            for (int r = 0; r < cbox.length; r++) {//8行
                                canvas.drawText(gridRows[r].getName(), 10, ((xArea - 32) * r) + 75, whiteP);
                                for (int c = 0; c < cbox[r].length; c++) {//12列
                                    canvas.drawText(gridCols[c].getName(), 60 + ((yArea + 23) * c), 40, whiteP);
                                    Circle cl = cbox[r][c];
                                    double[] color = image.get(cl.y, cl.x);
                                    RGB rgb = rgbs[r][c];
                                    rgb.setRed(color[0]);
                                    rgb.setGreen(color[1]);
                                    rgb.setBlue(color[2]);
                                    rgb.setAlpha(color[3]);
                                    String hex = String.format("#%02x%02x%02x", (int) color[0], (int) color[1], (int) color[2]);
                                    String name = rowname[r] + (c + 1);
                                    HashMap<String, String> mxc = new HashMap<>();
                                    mxc.put(name, hex);
                                    mrc.put(name, mxc);
                                    Paint paint = new Paint();
                                    paint.setStyle(Paint.Style.STROKE);
                                    paint.setStrokeWidth(15);
                                    paint.setARGB((int) color[3], (int) rgb.getRed(), (int) rgb.getGreen(), (int) rgb.getBlue());
                                    canvas.drawCircle(50 + ((xArea - 5) * c) + 25, 30 + ((yArea - 5) * r) + 35, 10, paint);
                                }
                            }
                            holder.unlockCanvasAndPost(canvas);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        clbox = cbox;
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
                        //先把图片转成hsv模式，然后再判断
                        //inRange 这个方法，判断输入的mat每个像素是否在范围内，如果在就返回白色，不在返回黑色，最后会输出一个黑白的mat图片。
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
                    //绘制一个上下左右居中的矩形
                    Imgproc.rectangle(hmat, new Point(lx, ly), new Point(w - rx, (h / 2) - ry), new Scalar(0, 139, 139), 5);
                    for (int r = 0; r < row; r++) {
                        gridRows[r].setX(lx - 30);
                        gridRows[r].setY(ly + ((((yArea - 8) * r)) / 2));
                        Imgproc.putText(hmat, gridRows[r].getName(), new Point(gridRows[r].getX() + gridRows[r].getxDelta(), gridRows[r].getY() + gridRows[r].getyDelta()), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        for (int c = 0; c < col; c++) {
                            gridCols[c].setX(lx + (xArea * c));
                            gridCols[c].setY(ly);
                            Imgproc.putText(hmat, gridCols[c].getName(), new Point(gridCols[c].getX() + gridCols[c].getxDelta(), gridCols[c].getY() + gridCols[c].getyDelta()), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        }
                    }
                    smasks.clear();
                    fmasks.clear();
                    if (iterate) takePhoto = true;
                    return hmat;
                } else {
                    for (int r = 0; r < row; r++) {
                        gridRows[r].setX(lx - 30);
                        gridRows[r].setY(ly + ((((yArea - 8) * r)) / 2));
                        Imgproc.putText(imgClone, gridRows[r].getName(), new Point(gridRows[r].getX() + gridRows[r].getxDelta(), gridRows[r].getY() + gridRows[r].getyDelta()), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        for (int c = 0; c < col; c++) {
                            gridCols[c].setX(lx + (xArea * c));
                            gridCols[c].setY(ly);
                            Imgproc.putText(imgClone, gridCols[c].getName(), new Point(gridCols[c].getX() + gridCols[c].getxDelta(), gridCols[c].getY() + gridCols[c].getyDelta()), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        }
                    }
                    //绘制一个上下左右居中的矩形
                    Imgproc.rectangle(imgClone, new Point(lx, ly), new Point(w - rx, (h / 2) - ry), new Scalar(0, 139, 139), 5);
                    return imgClone;
                }
            }

        });
        //拍照
        levelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handle.sendEmptyMessage(1);
                    takePhoto = true;
                    isClone = true;
                    isReTake = false;
                    findViewById(R.id.btnCatchColor).setEnabled(true);
                    findViewById(R.id.btnReTake).setEnabled(true);
                    findViewById(R.id.LayoutColorPal).setVisibility(View.VISIBLE);
                    SurfaceHolder holder = svColorPlate.getHolder();
                    Canvas canvas = holder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Paint paint = new Paint();
                    paint.setTextSize(40);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(5);
                    canvas.drawText("Dear User ^-^:", 60, 100, paint);
                    canvas.drawText("Click 'Catch' Button to get color", 60, 150, paint);
                    canvas.drawText("The color will be displayed here", 60, 200, paint);
                    holder.unlockCanvasAndPost(canvas);
                    //播放相机拍照的声音
                    if (mediaPlayer != null)
                        mediaPlayer.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask() {

                    @Override
                    protected Object doInBackground(Object[] objects) {
                        try {
                            Properties properties = new Properties();
                            properties.load(CamaraActivity.this.getAssets().open("project.properties"));
                            String url = properties.getProperty("url");
                            OkHttpClient client = new OkHttpClient();
                            String barcode = getIntent().getStringExtra("barcode");
                            RequestBody body = new FormBody.Builder().add("barcode", barcode).build();
                            Request request = new Request.Builder().url(url + "plate/findData.jhtml").post(body).build();
                            Response response = client.newCall(request).execute();//发送请求
                            JSONObject jsonObj = new JSONObject(new JSONObject(response.body().string()).getString("data"));
                            Iterator<String> keys = jsonObj.keys();
                            while (keys.hasNext()) {
                                String name = keys.next();//数据库中的key
                                HashMap<String, String> mxc = mrc.get(name);//扫描的key
                                if (mxc != null) {
                                    String color = mxc.get(name);
                                    jsonObj.getJSONObject(name).put("color", color);
                                    mxc.remove(name);
                                }
                            }
                            Iterator<HashMap<String, String>> iterator = mrc.values().iterator();
                            while (iterator.hasNext()) {
                                HashMap<String, String> mnb = iterator.next();
                                for (Iterator it = mnb.keySet().iterator(); it.hasNext(); ) {
                                    String name = (String) it.next();
                                    Well well = new Well();
                                    well.name = name;
                                    well.color = mnb.get(name);
                                    well.barcode = "";
                                    well.scantime = 0;
                                    jsonObj.put(name, new JSONObject(new Gson().toJson(well).toString()));
                                }
                            }

                            client.newCall(new Request.Builder().url(url + "plate/save.jhtml").post(new FormBody.Builder().add("barcode", barcode).add("data", jsonObj.toString()).build()).build()).
                                    execute();//发送请求
                            Bundle bundle = new Bundle();
                            bundle.putString(Variables.INTENT_EXTRA_KEY_QR_SCAN, barcode);
                            setResult(RESULT_OK, new Intent().putExtras(bundle));
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                }.execute();

            }
        });
        findViewById(R.id.btnCatchColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mrc.clear();
                    SurfaceHolder holder = svColorPlate.getHolder();
                    Canvas canvas = holder.lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Paint whiteP = new Paint();
                    whiteP.setStyle(Paint.Style.STROKE);
                    whiteP.setTextSize(40);
                    int xArea = svColorPlate.getWidth() / 12;
                    int yArea = svColorPlate.getHeight() / 8;
                    whiteP.setColor(Color.WHITE);
                    String[] rowname = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
                    for (int r = 0; r < clbox.length; r++) {
                        canvas.drawText(gridRows[r].getName(), 10, ((xArea - 32) * r) + 75, whiteP);
                        for (int c = 0; c < clbox[r].length; c++) {
                            canvas.drawText(gridCols[c].getName(), 60 + ((yArea + 23) * c), 40, whiteP);
                            Circle cl = clbox[r][c];
                            int x = cl.x;
                            int xDelta = cl.xDelta;
                            int y = cl.y;
                            int yDelta = cl.yDelta;
                            double[] color = image.get(isScan ? y : y + yDelta, isScan ? x : x + xDelta);
                            RGB rgb = rgbs[r][c];
                            rgb.setRed(color[0]);
                            rgb.setGreen(color[1]);
                            rgb.setBlue(color[2]);
                            rgb.setAlpha(color[3]);
                            String hex = String.format("#%02x%02x%02x", (int) color[0], (int) color[1], (int) color[2]);
                            String name = rowname[r] + (c + 1);
                            HashMap<String, String> mcx = new HashMap<>();
                            mcx.put(name, hex);
                            mrc.put(name, mcx);
                            Paint paint = new Paint();
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(15);
                            paint.setARGB((int) color[3], (int) rgb.getRed(), (int) rgb.getGreen(), (int) rgb.getBlue());
                            canvas.drawCircle(50 + ((xArea - 5) * c) + 25, 30 + ((yArea - 5) * r) + 35, 10, paint);
                        }
                    }
                    holder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btnReTake).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isReTake = true;
                stop = false;
                findViewById(R.id.lysetting).setVisibility(View.GONE);
                findViewById(R.id.btnCatchColor).setEnabled(false);
                findViewById(R.id.btnReTake).setEnabled(false);
                SurfaceHolder holder = svColorPlate.getHolder();
                Canvas canvas = holder.lockCanvas();
                Paint paint = new Paint();
                paint.setTextSize(40);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(5);
                canvas.drawText("Dear User ^-^:", 60, 100, paint);
                canvas.drawText("Click 'Catch' Button to get color", 60, 150, paint);
                canvas.drawText("The color will be displayed here", 60, 200, paint);
                holder.unlockCanvasAndPost(canvas);
                findViewById(R.id.LayoutColorPal).setVisibility(View.GONE);
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
package com.beijingepidial.ccpvirus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ScanwellActivity extends AppCompatActivity implements SensorEventListener {
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
    private static final int BTNCOLOR = 1;
    private static final int STOP = 3;
    private Handler handle = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case IDENTIFY:
                    String text = String.valueOf(msg.obj);
                    ((TextView) findViewById(R.id.tvmsg)).setText(text);
                    break;
                case BTNCOLOR:
                   /* Button btn = (Button) findViewById(R.id.btnAfterCol);
                    btn.setBackgroundColor(Color.parseColor(String.valueOf(msg.obj)));
                    btn.setTextColor(Color.parseColor("#FFFFFF"));*/
                    break;
                case STOP:
                    findViewById(R.id.btnCatchColor).setEnabled(true);
                    findViewById(R.id.btnReTake).setEnabled(true);
                    findViewById(R.id.btnpause).setEnabled(false);
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
        svBeforeColor = (SurfaceView) findViewById(R.id.svBeforeCol);
        svAfterColor = (SurfaceView) findViewById(R.id.svAfterCol);

        SurfaceHolder holder = svBeforeColor.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Canvas canvas = holder.lockCanvas();
                Bundle bundle = getIntent().getExtras();
                List<Well> wells = new Gson().fromJson(bundle.getString("data"), new TypeToken<ArrayList<Well>>() {
                }.getType());
                for (Iterator<Well> it = wells.iterator(); it.hasNext(); ) {
                    Well wl = it.next();
                    Paint paint = new Paint();
                    paint.setStrokeWidth(15);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setTextSize(200 / wells.size());
                    if (wells.size() == 1) {
                        paint.setColor(StringUtils.isEmpty(wl.color) ? Color.parseColor("#FFFFFF") : Color.parseColor(wl.color));
                        int w = svBeforeColor.getWidth();
                        int h = svBeforeColor.getHeight();
                        // 将坐标原点移到控件中心
                        canvas.translate(w / 2, h / 2);
                        // 绘制居中文字
                        float textWidth = paint.measureText(wl.name);
                        // 文字baseline在y轴方向的位置
                        float baseLineY = Math.abs(paint.ascent() + paint.descent()) / 2;
                        canvas.drawText(wl.name, -textWidth / 2, baseLineY, paint);
                        canvas.drawText(wl.name, ((w / 2)), h / 2, paint);
                    }
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        Canvas canvas = holder.lockCanvas();


       /* if (wells.size() == 1) {
            findViewById(R.id.btnColorBoard).setVisibility(View.VISIBLE);
            Well well = wells.get(0);
            String name = well.name;
            String color = well.color;
            Button btnBeforeCol = (Button) findViewById(R.id.btnBeforeCol);
            btnBeforeCol.setText(name);
            btnBeforeCol.setTextColor(StringUtils.isEmpty(color) ? Color.parseColor("#FFFFFF") : Color.parseColor("#000000"));
            btnBeforeCol.setBackgroundColor(StringUtils.isEmpty(color) ? Color.parseColor("#FFFFFF") : Color.parseColor(color));
            Button btnAfterCol = (Button) findViewById(R.id.btnAfterCol);
            btnAfterCol.setText(name);
            btnAfterCol.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }*/
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
                //w=1080
                final int w = inputFrame.rgba().cols();
                //h=1440
                final int h = inputFrame.rgba().rows();
                image = inputFrame.rgba().clone();
                Mat imgClone = image.clone();
                if (isScan) {
                    //边界检测
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
                        if (circles.size() != 1) {
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
                        for (int i = 0; i < circles.size(); i++) {
                            Circle c = circles.get(i);
                            Imgproc.circle(imgClone, new Point(c.x, c.y), c.radius, new Scalar(255, 0, 0), 2, 8);//绘制圆
                            Imgproc.putText(imgClone, String.valueOf(i), new Point(c.x, c.y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 3);
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
                    takePhoto = true;
                    isClone = true;
                    isReTake = false;
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
        findViewById(R.id.btnpause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = stop ? false : true;
                String vb = ((Button) findViewById(R.id.btnpause)).getText().toString();
                ((Button) findViewById(R.id.btnpause)).setText(vb.equals("start") ? "pause" : "start");
            }
        });
        findViewById(R.id.btnCatchColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.btnSave).setVisibility(View.VISIBLE);
                findViewById(R.id.tvmsg).setVisibility(View.VISIBLE);
                Circle cl = circles.get(0);
                double[] rgb = image.get(cl.y, cl.x);
                String color = String.format("#%02x%02x%02x", (int) rgb[0], (int) rgb[1], (int) rgb[2]);
                Message msg = new Message();
                msg.what = BTNCOLOR;
                msg.obj = color;
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
                            Circle cl = circles.get(0);
                            double[] rgb = image.get(cl.y, cl.x);
                            String color = String.format("#%02x%02x%02x", (int) rgb[0], (int) rgb[1], (int) rgb[2]);
                            Bundle bundle = getIntent().getExtras();
                            String barcode = bundle.getString("barcode");
                            String name = bundle.getString("name");
                            OkHttpClient client = new OkHttpClient();
                            Properties properties = new Properties();
                            properties.load(ScanwellActivity.this.getAssets().open("project.properties"));
                            String url = properties.getProperty("url");
                            String v = client.newCall(new Request.Builder().url(url + "plate/findData.jhtml").post(new FormBody.Builder().add("barcode", barcode).build()).build()).execute().body().string();
                            JSONObject bv = StringUtils.isNotEmpty(v) ? new JSONObject(new JSONObject(v).getString("data")) : null;
                            if (bv != null && bv.has(name)) {
                                JSONObject oo = new JSONObject(bv.getString(name));
                                oo.put("color", color);
                                bv.put(name, oo);
                                RequestBody body = new FormBody.Builder().add("barcode", barcode).add("data", bv.toString()).build();
                                Request request = new Request.Builder().url(url + "plate/save.jhtml").post(body).build();
                                client.newCall(request).execute();//发送请求
                                Bundle vbund = new Bundle();
                                vbund.putString(Variables.INTENT_EXTRA_KEY_QR_SCAN, barcode);
                                setResult(RESULT_OK, new Intent().putExtras(vbund));
                                finish();
                            }

                        } catch (Exception e) {

                        }
                        return null;
                    }
                }.execute();


            }
        });
        findViewById(R.id.btnReTake).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.btnCatchColor).setEnabled(false);
                findViewById(R.id.btnReTake).setEnabled(false);
                findViewById(R.id.btnSave).setVisibility(View.GONE);
                /*Button btn = (Button) findViewById(R.id.btnAfterCol);
                btn.setBackgroundColor(Color.parseColor("#FFFFFF"));
                btn.setTextColor(Color.parseColor("#000000"));*/
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
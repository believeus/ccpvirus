package com.beijingepidial.ccpvirus;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.beijingepidial.entity.Circle;
import com.beijingepidial.entity.GridCol;
import com.beijingepidial.entity.GridRow;
import com.beijingepidial.entity.RGB;
import com.innovattic.rangeseekbar.RangeSeekBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private  List<Mat> masks = new ArrayList<Mat>();
    private List<List<Scalar>> scalars = new ArrayList<List<Scalar>>();
    private boolean stop;
    private Mat imat;
    private Scalar scalar = new Scalar(0, 0, 0);
    private JavaCameraView javaCameraView;
    private RGB[][] rgbs;
    private Circle[][] clbox;
    private GridCol gridCols[];
    private GridRow gridRows[];
    private SurfaceView svColorPlate;
    private EditText edtRadius;
    private EditText edtRow;
    private EditText edtCol;
    private EditText edtColDelta;
    private EditText edtRowDelta;
    private Spinner spColNumLeftRight;
    private Spinner spRowNumUpDown;
    private Spinner spRowNumLeftRight;
    private Spinner spColNumUpDown;
    private Mat image;
    private Mat gary;
    private Mat edges;
    private Mat hierarchy;
    //圆半径
    private int radius;
    //圆与圆X轴之间的距离
    private int xDelta;
    //圆与圆y轴之间的距离
    private int yDelta;
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
    private boolean isClone;
    private boolean isTakePhoto;
    private boolean isReTake;
    private boolean isAutoPick;
    private Spinner spCellRow;
    private Spinner spCellCol;
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

    //End:传感器
    public MainActivity() {
        rgbs = new RGB[8][12];
        this.clbox = new Circle[8][12];
        this.gridCols = new GridCol[12];
        this.gridRows = new GridRow[8];
        this.init = true;
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
        setContentView(R.layout.activity_main);
        //Begin:传感器
        levelView = (LevelView) findViewById(R.id.gv_hv);
        tvVert = (TextView) findViewById(R.id.tvv_vertical);
        tvHorz = (TextView) findViewById(R.id.tvv_horz);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //End:传感器
        svColorPlate = (SurfaceView) findViewById(R.id.svColorPlate);
        edtRadius = (EditText) findViewById(R.id.edtRadius);
        edtRow = (EditText) findViewById(R.id.edtRow);
        edtColDelta = (EditText) findViewById(R.id.edtColDelta);
        edtRowDelta = (EditText) findViewById(R.id.edtRowDelta);
        spRowNumUpDown = (Spinner) findViewById(R.id.spRowNumUpDown);
        spRowNumLeftRight = (Spinner) findViewById(R.id.spRowNumLeftRight);
        spColNumUpDown = (Spinner) findViewById(R.id.spColNumUpDown);
        spColNumLeftRight = (Spinner) findViewById(R.id.spColNumLeftRight);
        spCellRow = (Spinner) findViewById(R.id.spCellRow);
        spCellCol = (Spinner) findViewById(R.id.spCellCol);
        final ArrayAdapter rowAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Arrays.asList(new String[]{"A", "B", "C", "D", "E", "F", "G", "H"}));
        rowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter colAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Arrays.asList(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"}));
        colAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //第四步：将适配器添加到下拉列表上
        spRowNumUpDown.setAdapter(rowAdapter);
        spRowNumLeftRight.setAdapter(rowAdapter);
        spColNumLeftRight.setAdapter(colAdapter);
        spColNumUpDown.setAdapter(colAdapter);
        spCellRow.setAdapter(rowAdapter);
        spCellCol.setAdapter(colAdapter);
        edtCol = (EditText) findViewById(R.id.edtCol);

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
            private List<MatOfPoint> list = new ArrayList<MatOfPoint>();

            @Override
            public void onCameraViewStarted(int width, int height) {
                MainActivity.this.gary = new Mat();
                MainActivity.this.edges = new Mat();
                MainActivity.this.hierarchy = new Mat();
                MainActivity.this.radius = Integer.parseInt(edtRadius.getText().toString());
                MainActivity.this.xDelta = Integer.parseInt(edtColDelta.getText().toString());
                MainActivity.this.yDelta = Integer.parseInt(edtRowDelta.getText().toString());

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                //区间移动时,stop为false,禁止是为true
                if (stop) return imat;
                //w=1080
                final int w = MainActivity.this.width = inputFrame.rgba().cols();
                //h=1440
                final int h = MainActivity.this.height = inputFrame.rgba().rows();
                if (init)
                    MainActivity.this.xArea = (w - rx - lx) / col;
                if (init)
                    MainActivity.this.yArea = (h - ry - ly) / row;
                init = false;
                if (isClone) {
                    image = inputFrame.rgba().clone();
                    isClone = false;
                }
                if (!isReTake) {
                    if (isTakePhoto) {
                        Mat imgClone = image.clone();
                        if (isAutoPick) {
                            masks.clear();
                            Mat hmat = new Mat();
                            Imgproc.cvtColor(imgClone, hmat, Imgproc.COLOR_RGB2HSV);
                            for (int i = 0; i < scalars.size(); i++) {
                                List<Scalar> scalar = scalars.get(i);
                                Mat mask = new Mat();
                                Core.inRange(hmat, scalar.get(0), scalar.get(1), mask);
                                masks.add(i, mask);
                                if (i > 0) {
                                    Core.bitwise_or(masks.get(i - 1),  masks.get(i), mask);
                                }
                            }
                            Core.bitwise_and(imgClone, imgClone, hmat, masks.get(masks.size()-1));
                            imat = hmat;
                            stop = true;
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
                                    Circle cl = clbox[r][c];
                                    cl.setX(lx + (xArea * c));
                                    cl.setY(ly + (((yArea - 10) * r) / 2) - 10);
                                    double[] color = image.get(cl.getY() + cl.getyDelta(), cl.getX() + cl.getxDelta());
                                    Imgproc.circle(imgClone, new Point(cl.getX() + cl.getxDelta(), cl.getY() + cl.getyDelta()), radius, scalar, 2, Core.LINE_AA);
                                }
                            }
                        }
                        //绘制一个上下左右居中的矩形
                        Imgproc.rectangle(imgClone, new Point(lx, ly), new Point(w - rx, (h / 2) - ry), new Scalar(0, 139, 139), 5);
                        return imgClone;
                    }
                }
                final Mat frame = inputFrame.rgba();
                Imgproc.cvtColor(frame, gary, Imgproc.COLOR_RGB2GRAY);
                //Imgproc.Canny(gary, edges, 50, 500, 3, false);
                list.clear();
                //绘制一个上下左右居中的矩形
                Imgproc.rectangle(frame, new Point(lx, ly), new Point(w - rx, (h / 2) - ry), new Scalar(0, 139, 139), 5);
                //Imgproc.findContours(edges, list, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                for (int r = 0; r < row; r++) {
                    gridRows[r].setX(lx - 30);
                    gridRows[r].setY(ly + ((((yArea - 8) * r)) / 2));
                    Imgproc.putText(frame, gridRows[r].getName(), new Point(gridRows[r].getX() + gridRows[r].getxDelta(), gridRows[r].getY() + gridRows[r].getyDelta()), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                    for (int c = 0; c < col; c++) {
                        gridCols[c].setX(lx + (xArea * c));
                        gridCols[c].setY(ly);
                        Imgproc.putText(frame, gridCols[c].getName(), new Point(gridCols[c].getX() + gridCols[c].getxDelta(), gridCols[c].getY() + gridCols[c].getyDelta()), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        Circle cl = clbox[r][c];
                        cl.setX(lx + (xArea * c));
                        cl.setY(ly + (((yArea - 10) * r) / 2) - 10);
                        Imgproc.circle(frame, new Point(cl.getX() + cl.getxDelta(), cl.getY() + cl.getyDelta()), radius, scalar, 2, Core.LINE_AA);
                    }
                }
                //5 绘制轮廓
               /* for (int i = 0, len = list.size(); i < len; i++) {
                    Imgproc.drawContours(frame, list, i, new Scalar(0, 255, 0), 1);
                }*/

                return frame;
            }
        });
        //拍照
        levelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    isTakePhoto = true;
                    isClone = true;
                    isReTake = false;
                    ((CheckBox) findViewById(R.id.ckAuto)).setEnabled(true);
                    ((CheckBox) findViewById(R.id.ckManual)).setEnabled(true);
                    findViewById(R.id.btnCatchColor).setEnabled(true);
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
        findViewById(R.id.btnWidthMax).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                rx -= 1;
                                lx -= 1;
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnWidthMin).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                rx += 1;
                                lx += 1;
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnHeightMax).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ly -= 1;
                                ry -= 1;
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnHeightMin).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ly += 1;
                                ry += 1;
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnRight).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                lx += 1;
                                rx -= 1;

                            }
                        }, 0, 5);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnLeft).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                lx -= 1;
                                rx += 1;

                            }
                        }, 0, 5);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnUp).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ly -= 1;
                                ry += 1;
                            }
                        }, 0, 5);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnDown).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ly += 1;
                                ry -= 1;
                            }
                        }, 0, 5);
                        break;
                    default:
                        timer.cancel();
                        break;

                }
                return true;
            }
        });
        findViewById(R.id.btnCicleMin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radius == 0) return;
                radius--;
                edtRadius.setText(String.valueOf(radius));
            }
        });
        findViewById(R.id.btnCicleMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radius++;
                edtRadius.setText(String.valueOf(radius));
            }
        });
        findViewById(R.id.btnRowMin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                if (row > 1) row--;
                edtRow.setText(Character.toString((char) (64 + row)));
            }
        });
        findViewById(R.id.btnRowMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                if (row < 8) row++;
                edtRow.setText(Character.toString((char) (64 + row)));
            }
        });
        findViewById(R.id.btnMixColDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xDelta--;
                MainActivity.this.xArea = ((width - ry - ly) / col);
                MainActivity.this.xArea += xDelta;
                edtColDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMaxColDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xDelta++;
                MainActivity.this.xArea = ((width - ry - ly) / col);
                MainActivity.this.xArea += xDelta;
                edtColDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMixRowDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yDelta--;
                MainActivity.this.yArea = (height - ry - ly) / row;
                MainActivity.this.yArea += yDelta;
                edtRowDelta.setText(String.valueOf(yDelta));
            }
        });
        findViewById(R.id.btnMaxRowDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yDelta++;
                MainActivity.this.yArea = (height - ry - ly) / row;
                MainActivity.this.yArea += yDelta;
                edtRowDelta.setText(String.valueOf(yDelta));
            }
        });
        findViewById(R.id.btnRowDown).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                                int row = ((int) spRowNumUpDown.getSelectedItem().toString().charAt(0)) - 64 - 1;
                                for (int c = 0; c < col; c++) {
                                    clbox[row][c].yDeltaAdd();
                                }
                                gridRows[row].yDeltaAdd();
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;

                }

                return true;
            }

        });
        findViewById(R.id.btnRowUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                int row = ((int) spRowNumUpDown.getSelectedItem().toString().charAt(0)) - 64 - 1;
                for (int c = 0; c < col; c++) {
                    clbox[row][c].yDeltaLow();
                }
                gridRows[row].yDeltaLow();
            }
        });
        findViewById(R.id.btnRowLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int row = spRowNumLeftRight.getSelectedItem().toString().charAt(0) - 64 - 1;
                for (int c = 0; c < col; c++) {
                    clbox[row][c].xDeltaLow();
                }
            }
        });
        findViewById(R.id.btnRowRight).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                                int row = ((int) spRowNumLeftRight.getSelectedItem().toString().charAt(0)) - 64 - 1;
                                for (int c = 0; c < col; c++) {
                                    clbox[row][c].xDeltaAdd();
                                }
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;

                }

                return true;
            }
        });
        findViewById(R.id.btnColLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int col = Integer.parseInt(spColNumLeftRight.getSelectedItem().toString()) - 1;
                for (int r = 0; r < row; r++) {
                    clbox[r][col].xDeltaLow();
                }
                gridCols[col].xDeltaLow();
            }
        });
        findViewById(R.id.btnColRight).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                int col = Integer.valueOf(spColNumLeftRight.getSelectedItem().toString()).intValue() - 1;
                                for (int r = 0; r < row; r++) {
                                    clbox[r][col].xDeltaAdd();
                                }
                                gridCols[col].xDeltaAdd();
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;

                }
                return true;
            }
        });
        findViewById(R.id.btnColDown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int col = Integer.parseInt(spColNumUpDown.getSelectedItem().toString()) - 1;
                for (int r = 0; r < row; r++) {
                    clbox[r][col].yDeltaAdd();
                }
            }
        });
        findViewById(R.id.btnColUp).setOnTouchListener(new View.OnTouchListener() {
            private Timer timer;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                int col = Integer.parseInt(spColNumUpDown.getSelectedItem().toString()) - 1;
                                for (int r = 0; r < row; r++) {
                                    clbox[r][col].yDeltaLow();
                                }
                            }
                        }, 0, 2);
                        break;
                    default:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnColMin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (col > 1) col--;
                edtCol.setText(String.valueOf(col));
            }
        });
        findViewById(R.id.btnColMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (col < 12) col++;
                edtCol.setText(String.valueOf(col));
            }
        });

        findViewById(R.id.btnCatchColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    for (int r = 0; r < row; r++) {
                        canvas.drawText(gridRows[r].getName(), 10, ((xArea - 32) * r) + 75, whiteP);
                        for (int c = 0; c < col; c++) {
                            canvas.drawText(gridCols[c].getName(), 60 + ((yArea + 23) * c), 40, whiteP);
                            Circle cl = clbox[r][c];
                            int x = cl.getX();
                            int xDelta = cl.getxDelta();
                            int y = cl.getY();
                            int yDelta = cl.getyDelta();
                            double[] color = image.get(y + yDelta, x + xDelta);
                            RGB rgb = rgbs[r][c];
                            rgb.setRed(color[0]);
                            rgb.setGreen(color[1]);
                            rgb.setBlue(color[2]);
                            rgb.setAlpha(color[3]);
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
                ((CheckBox) findViewById(R.id.ckAuto)).setEnabled(false);
                ((CheckBox) findViewById(R.id.ckManual)).setEnabled(false);
                findViewById(R.id.btnCatchColor).setEnabled(false);
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
        findViewById(R.id.btnCellLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int r = (spCellRow.getSelectedItem().toString().charAt(0)) - 64 - 1;
                int c = Integer.valueOf(spCellCol.getSelectedItem().toString()).intValue() - 1;
                clbox[r][c].xDeltaLow();
            }
        });
        findViewById(R.id.btnCellRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int r = (spCellRow.getSelectedItem().toString().charAt(0)) - 64 - 1;
                int c = Integer.valueOf(spCellCol.getSelectedItem().toString()).intValue() - 1;
                clbox[r][c].xDeltaAdd();
            }
        });
        findViewById(R.id.btnCellUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int r = (spCellRow.getSelectedItem().toString().charAt(0)) - 64 - 1;
                int c = Integer.valueOf(spCellCol.getSelectedItem().toString()).intValue() - 1;
                clbox[r][c].yDeltaLow();
            }
        });
        findViewById(R.id.btnCellDown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int r = (spCellRow.getSelectedItem().toString().charAt(0)) - 64 - 1;
                int c = Integer.valueOf(spCellCol.getSelectedItem().toString()).intValue() - 1;
                clbox[r][c].yDeltaAdd();
            }
        });
        ((CheckBox) findViewById(R.id.ckAuto)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) isAutoPick = isChecked;
            }
        });

        ((RangeSeekBar) findViewById(R.id.skA)).setSeekBarChangeListener(new RangeSeekBar.SeekBarChangeListener() {
            private List<Scalar> obj = new ArrayList<Scalar>();

            {
                obj.add(0, new Scalar(5, 43, 46));
                obj.add(1, new Scalar(20, 255, 255));
                scalars.add(0,obj);
            }

            private int min;
            private int max;

            @Override
            public void onStartedSeeking() {
                stop = false;
            }

            @Override
            public void onStoppedSeeking() {
                obj.clear();
                obj.add(0, new Scalar(min, 43, 46));
                obj.add(1, new Scalar(max, 255, 255));
            }

            @Override
            public void onValueChanged(int min, int max) {
                this.max = max;
                this.min = min;
            }
        });
        ((RangeSeekBar) findViewById(R.id.skB)).setSeekBarChangeListener(new RangeSeekBar.SeekBarChangeListener() {
            private List<Scalar> obj = new ArrayList<Scalar>();

            {
                obj.add(0, new Scalar(156, 43, 46));
                obj.add(1, new Scalar(180, 255, 255));
                scalars.add(1,obj);
            }

            private int min;
            private int max;

            @Override
            public void onStartedSeeking() {
                stop = false;
            }

            @Override
            public void onStoppedSeeking() {
                obj.clear();
                obj.add(0, new Scalar(min, 43, 46));
                obj.add(1, new Scalar(max, 255, 255));
            }

            @Override
            public void onValueChanged(int min, int max) {
                this.max = max;
                this.min = min;
            }
        });
        ((RangeSeekBar) findViewById(R.id.skC)).setSeekBarChangeListener(new RangeSeekBar.SeekBarChangeListener() {
            private List<Scalar> obj = new ArrayList<Scalar>();

            {
                obj.add(0, new Scalar(20, 43, 46));
                obj.add(1, new Scalar(30, 255, 255));
                scalars.add(2,obj);
            }

            private int min;
            private int max;

            @Override
            public void onStartedSeeking() {
                stop = false;
            }

            @Override
            public void onStoppedSeeking() {
                obj.clear();
                obj.add(0, new Scalar(min, 43, 46));
                obj.add(1, new Scalar(max, 255, 255));
            }

            @Override
            public void onValueChanged(int min, int max) {
                this.max = max;
                this.min = min;
            }
        });
        ((RangeSeekBar) findViewById(R.id.skD)).setSeekBarChangeListener(new RangeSeekBar.SeekBarChangeListener() {
            private List<Scalar> obj = new ArrayList<Scalar>();

            {
                obj.add(0, new Scalar(75, 43, 46));
                obj.add(1, new Scalar(115, 255, 255));
                scalars.add(3,obj);
            }

            private int min;
            private int max;

            @Override
            public void onStartedSeeking() {
                stop = false;
            }

            @Override
            public void onStoppedSeeking() {
                obj.clear();
                obj.add(0, new Scalar(min, 43, 46));
                obj.add(1, new Scalar(max, 255, 255));
            }

            @Override
            public void onValueChanged(int min, int max) {
                this.max = max;
                this.min = min;
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
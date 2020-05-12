package com.beijingepidial.ccpvirus;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.beijingepidial.entity.Circle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private JavaCameraView javaCameraView;
    private Circle[][] circlebox;
    private EditText edtRadius;
    private EditText edtRow;
    private EditText edtColDelta;
    private EditText edtRowDelta;
    private EditText edtRowNum;
    private EditText edtColNum;
    private Mat gary;
    private Mat edges;
    private Mat hierarchy;
    private int width;
    private int height;
    //圆半径
    private int radius;
    //圆与圆X轴之间的距离
    private int xDelta;
    private boolean isXDelta;
    private boolean isYDelta;
    //圆与圆y轴之间的距离
    private int yDelta;
    private int rx;
    private int ry;
    //左上角点x坐标点
    private int lx;
    //左上角点y坐标点
    private int ly;
    private String[] colVal;
    private String[] rowVal;
    private int col;
    private int row;
    private boolean isCapture;

    public MainActivity() {
        this.circlebox = new Circle[8][12];
        this.lx = 50;
        this.ly = 50;
        this.rx = 50;
        this.ry = 50;
        this.isCapture = false;
        this.rowVal = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
        this.colVal = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        this.col = colVal.length;
        this.row = rowVal.length;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //保持螢幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        edtRadius = (EditText) findViewById(R.id.edtRadius);
        edtRow = (EditText) findViewById(R.id.edtRow);
        edtColDelta = (EditText) findViewById(R.id.edtColDelta);
        edtRowDelta = (EditText) findViewById(R.id.edtRowDelta);
        edtRowNum = (EditText) findViewById(R.id.edtRowNum);
        edtColNum=(EditText)findViewById(R.id.edtColNun);
        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
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
                //初始化96圓
                for (int r = 0; r < row; r++) {
                    for (int c = 0; c < col; c++) {
                        circlebox[r][c] = new Circle(0, 0);
                    }
                }
            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                final Mat frame = inputFrame.rgba();
                final int w = frame.width();
                final int h = frame.height();
                Imgproc.cvtColor(frame, gary, Imgproc.COLOR_RGB2GRAY);
                Imgproc.Canny(gary, edges, 50, 500, 3, false);
                list.clear();
                Imgproc.findContours(edges, list, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                int xArea = (((w - rx - lx)) / col);
                int yArea = (h - ry - ly) / row;
                for (int r = 0; r < row; r++) {
                    Imgproc.putText(frame, rowVal[r], new Point(lx - 30, ly + (yArea * r) + 50), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                    for (int c = 0; c < col; c++) {
                        Imgproc.putText(frame, colVal[c], new Point(lx + (xArea * c) + 20, ly - 10), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        circlebox[r][c].setX(lx + (xArea * c));
                        circlebox[r][c].setY(ly + (yArea * r));
                        Imgproc.circle(frame, new Point(circlebox[r][c].getX(), circlebox[r][c].getY()), radius, new Scalar(0, 139, 139), 2, Core.LINE_AA);
                    }
                }
                //绘制一个上下左右居中的矩形
                Imgproc.rectangle(frame, new Point(lx, ly), new Point(w - rx, h - ry), new Scalar(0, 139, 139), 5);
                //5 绘制轮廓
                for (int i = 0, len = list.size(); i < len; i++) {
                    Imgproc.drawContours(frame, list, i, new Scalar(0, 255, 0), 1);
                }
                //拍照截图
                if (isCapture) {
                    isCapture = false;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Mat src = inputFrame.rgba().clone();
                                Rect rect = new Rect(lx, ly, w - 2 * lx, h - 2 * ly);
                                Mat mat = new Mat();
                                Imgproc.cvtColor(src.submat(rect), mat, Imgproc.COLOR_BGR2RGBA);
                                double[] rgb = mat.get(100, 100);
                                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                                String filename = "xxx.png";
                                File file = new File(path, filename);
                                Imgcodecs.imwrite(file.toString(), new Mat(100, 100, CvType.CV_8UC3, new Scalar(rgb[0], rgb[1], rgb[2])));
                                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.toString(), null);
                                Uri uri = Uri.fromFile(file);
                                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                                Toast.makeText(MainActivity.this, "take phone success", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                }
                return frame;
            }
        });
       /* findViewById(R.id.btnTakePhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               try{
                    isCapture=true;
                   //此种方式andorid版opencv用imwrite把照片保存到本地时，解决颜色不符问题
                   //Highgui.imwrite(file.toString(),mat);
                   //Bitmap mBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                            *//*File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            String filename="xxx.png";
                            File file=new File(path,filename);
                            Imgcodecs.imwrite(file.toString(),mat);*//*



         *//*  Utils.matToBitmap(mat, mBitmap);
                    OutputStream fileout = new FileOutputStream(new File(path,filename).toString());
                    mBitmap.compress(Bitmap.CompressFormat.PNG,100,fileout);
                    fileout.flush();
                    fileout.close();*//*

         *//* MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.toString(), null);
                    Uri uri = Uri.fromFile(file);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));*//*


               }catch (Exception e){
                   e.printStackTrace();
               }
            }
        });*/
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
                        }, 0, 20);

                        break;
                    case MotionEvent.ACTION_UP:
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
                        timer.cancel();
                        break;
                }
                return true;
            }
        });
        findViewById(R.id.btnup).setOnTouchListener(new View.OnTouchListener() {
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
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
                        }, 0, 20);
                        break;
                    case MotionEvent.ACTION_UP:
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
                row--;
                if (row < 0) return;
                edtRow.setText(String.valueOf(row));
            }
        });
        findViewById(R.id.btnRowMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                row++;
                if (row > 8) return;
                edtRow.setText(String.valueOf(row));
            }
        });
        findViewById(R.id.btnMixEdtColDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isXDelta = true;
                xDelta--;
                edtColDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMaxEdtColDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isXDelta = true;
                xDelta++;
                edtColDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMixEdtRowDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isYDelta = true;
                yDelta--;
                edtRowDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMaxEdtRowDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isYDelta = true;
                yDelta++;
                edtRowDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnRowRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.btnRowLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.btnColLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = Integer.parseInt(edtColNum.getText().toString());
                c--;
                for (int r = 0; r < row; r++) {
                       int xDelta=circlebox[r][c].getxDelta();
                       xDelta--;
                       circlebox[r][c].setxDelta(xDelta);
                    }
            }
        });
        findViewById(R.id.btnColRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c = Integer.parseInt(edtColNum.getText().toString());
                c--;
                for (int r = 0; r < row; r++) {
                    int xDelta=circlebox[r][c].getxDelta();
                    xDelta++;
                    circlebox[r][c].setxDelta(xDelta);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


}
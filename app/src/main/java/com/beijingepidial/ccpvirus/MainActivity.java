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
    private EditText edtRadius;
    private EditText edtRow;
    private Mat gary;
    private Mat edges;
    private Mat hierarchy;
    private int width;
    private int height;
    //圆半径
    private int radius;
    //圆与圆X轴之间的距离
    private int xDelta;
    //圆与圆y轴之间的距离
    private int yDelta;
    private int rx = 50;
    private int ry = 50;
    //左上角点x坐标点
    private int lx = 50;
    //左上角点y坐标点
    private int ly = 50;
    private String[] colVal;
    private String[] rowVal;
    private int col;
    private int row;
    private boolean isCapture;
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
        edtRadius=(EditText) findViewById(R.id.edtRadius);
        edtRow=(EditText)findViewById(R.id.edtRow);
        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
           private List<MatOfPoint> list = new ArrayList<MatOfPoint>();
            @Override
            public void onCameraViewStarted(int width, int height) {
                radius=Integer.parseInt(edtRadius.getText().toString());
                xDelta=50;
                yDelta=50;
                isCapture=false;
                colVal =new String[]{"A","B","C","D","E","F","G","H"};
                rowVal =new String[]{"1","2","3","4","5","6","7","8","9","10","11","12"};
                col=colVal.length;
                row=rowVal.length;
                gary = new Mat();
                edges = new Mat();
                hierarchy = new Mat();
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
                //绘制上下左右居中矩形框
                for (int i = 0; i< row; i++){
                    int xDelta = (((w - rx - lx))/row);
                    int yDelta = (h - ry - ly)/col;
                    Imgproc.putText(frame, rowVal[i],new Point(lx+(xDelta*i)+15,ly-10),Core.FONT_HERSHEY_SIMPLEX,1, new Scalar(0, 139, 139),5);
                    if (i< col) {
                        Imgproc.putText(frame, colVal[i], new Point(lx - 30, ly + (yDelta * i)+40), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        for (int j = 0; j< row; j++) {
                            Imgproc.circle(frame, new Point(lx + (xDelta * j) + 30, ly + (yDelta * i) + 40), radius, new Scalar(0, 139, 139), 2,Core.LINE_AA);
                        }
                    }
                }
                //绘制一个上下左右居中的矩形
                Imgproc.rectangle(frame, new Point(lx, ly), new Point(w - rx, h - ry), new Scalar(0, 139, 139), 5);
                //5 绘制轮廓
                for (int i = 0, len = list.size(); i < len; i++) {
                   Imgproc.drawContours(frame, list, i, new Scalar(0, 255, 0), 1);
                }
                //拍照截图
                if (isCapture){
                    isCapture=false;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                Mat src=inputFrame.rgba().clone();
                                Rect rect = new Rect(lx, ly, w - 2*lx, h - 2*ly);
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
                            }catch (Exception e){
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
                if (radius==0)return;
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
                if (row<0)return;
                edtRow.setText(String.valueOf(row));
            }
        });
        findViewById(R.id.btnRowMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                row++;
                if (row>12)return;
                edtRow.setText(String.valueOf(row));
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
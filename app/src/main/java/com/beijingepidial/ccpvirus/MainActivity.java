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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private JavaCameraView javaCameraView;
    private Circle[][] clbox;
    private EditText edtRadius;
    private EditText edtRow;
    private EditText edtCol;
    private EditText edtColDelta;
    private EditText edtRowDelta;
    private Spinner spColNum;
    private Spinner spRowNum;
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
    private String[] colVal;
    private String[] rowVal;
    private int col;
    private int row;
    private int xArea;
    private int yArea;
    private int width;
    private int height;
    private boolean init;
    private boolean isclone;
    private boolean isTakePhoto;
    private boolean isCaptureColor;

    public MainActivity() {
        this.clbox = new Circle[8][12];
        this.init=true;
        this.lx = 50;
        this.ly = 50;
        this.rx = 50;
        this.ry = 50;
        this.isCaptureColor = false;
        this.isTakePhoto=false;
        this.rowVal = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
        this.colVal = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        this.col = colVal.length;
        this.row = rowVal.length;
        //初始化96圓
        for (int r = 0; r < row; r++) {for (int c = 0; c < col; c++) {clbox[r][c] = new Circle();} }
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
        spRowNum = (Spinner) findViewById(R.id.spRowNum);
        spColNum=(Spinner) findViewById(R.id.spColNum);
        ArrayAdapter rowAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Arrays.asList(rowVal));
        rowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter colAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Arrays.asList(colVal));
        colAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //第四步：将适配器添加到下拉列表上
        spRowNum.setAdapter(rowAdapter);
        spColNum.setAdapter(colAdapter);
        edtCol=(EditText)findViewById(R.id.edtCol);
        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            private List<MatOfPoint> list = new ArrayList<MatOfPoint>();

            @Override
            public void onCameraViewStarted(int width, int height) {
                MainActivity.this.gary=new Mat();
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
                final int w=MainActivity.this.width= inputFrame.rgba().width();
                final int h=MainActivity.this.height= 750;
                if (init)
                    MainActivity.this.xArea = (w - rx - lx) / col;
                if (init)
                    MainActivity.this.yArea = (h - ry - ly) / row;
                init=false;
                if (isclone){image=inputFrame.rgba().clone();isclone=false;}
                if (isTakePhoto) {
                    Mat mat=image.clone();
                    for (int r = 0; r < row; r++) {
                        Imgproc.putText(mat, rowVal[r], new Point(lx - 30, ly + (yArea * r)+40), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        for (int c = 0; c < col; c++) {
                            Imgproc.putText(mat, colVal[c], new Point(lx + (xArea * c) + 20, ly - 10), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                            Circle cl= clbox[r][c];
                            cl.setX(lx + (xArea * c));
                            cl.setY(ly + (yArea * r));
                            Imgproc.circle(mat, new Point(cl.getX()+cl.getxDelta(), cl.getY()+cl.getyDelta()), radius, new Scalar(0, 139, 139), 2, Core.LINE_AA);
                        }
                    }
                    //绘制一个上下左右居中的矩形
                    Imgproc.rectangle(mat, new Point(lx, ly), new Point(w - rx, h - ry), new Scalar(0, 139, 139), 5);
                    return mat;
                }
                final Mat frame  = inputFrame.rgba();
                Imgproc.cvtColor(frame, gary, Imgproc.COLOR_RGB2GRAY);
                Imgproc.Canny(gary, edges, 50, 500, 3, false);
                list.clear();
                //绘制一个上下左右居中的矩形
                Imgproc.rectangle(frame, new Point(lx, ly), new Point(w - rx, h - ry), new Scalar(0, 139, 139), 5);
                Imgproc.findContours(edges, list, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                for (int r = 0; r < row; r++) {
                    Imgproc.putText(frame, rowVal[r], new Point(lx - 30, ly + (yArea * r)+40), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                    for (int c = 0; c < col; c++) {
                        Imgproc.putText(frame, colVal[c], new Point(lx + (xArea * c) + 20, ly - 10), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 139, 139), 5);
                        Circle cl= clbox[r][c];
                        cl.setX(lx + (xArea * c));
                        cl.setY(ly + (yArea * r));
                        Imgproc.circle(frame, new Point(cl.getX()+cl.getxDelta(), cl.getY()+cl.getyDelta()), radius, new Scalar(0, 139, 139), 2, Core.LINE_AA);
                    }
                }
                //5 绘制轮廓
                for (int i = 0, len = list.size(); i < len; i++) {
                    Imgproc.drawContours(frame, list, i, new Scalar(0, 255, 0), 1);
                }

                return frame;
            }
        });
        findViewById(R.id.btnCatchPicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTakePhoto=true;
                isclone=true;
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
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                if (row > 1)row--;
                edtRow.setText(Character.toString((char)(64+row)));
            }
        });
        findViewById(R.id.btnRowMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                if (row<8)row++;
                edtRow.setText(Character.toString((char) (64 + row)));
            }
        });
        findViewById(R.id.btnMixColDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xDelta--;
                MainActivity.this.xArea = ((width - ry - ly) / col);
                MainActivity.this.xArea+=xDelta;
                edtColDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMaxColDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xDelta++;
                MainActivity.this.xArea = ((width - ry - ly) / col);
                MainActivity.this.xArea+=xDelta;
                edtColDelta.setText(String.valueOf(xDelta));
            }
        });
        findViewById(R.id.btnMixRowDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yDelta--;
                MainActivity.this.yArea = (height - ry - ly) / row;
                MainActivity.this.yArea+=yDelta;
                edtRowDelta.setText(String.valueOf(yDelta));
            }
        });
        findViewById(R.id.btnMaxRowDelta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yDelta++;
                MainActivity.this.yArea = (height - ry - ly) / row;
                MainActivity.this.yArea+=yDelta;
                edtRowDelta.setText(String.valueOf(yDelta));
            }
        });
        findViewById(R.id.btnRowDown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                int row=((int)spRowNum.getSelectedItem().toString().charAt(0))-64-1;
                for (int c = 0; c < col; c++) {
                    clbox[row][c].yDeltaAdd();
                }
            }
        });
        findViewById(R.id.btnRowUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //A==65 B=66 C=67 D=68 E=69 F=70 G=71 H=72
                int row=((int)spRowNum.getSelectedItem().toString().charAt(0))-64-1;
                for (int c = 0; c < col; c++) {
                    clbox[row][c].yDeltaLow();
                }
            }
        });
        findViewById(R.id.btnColLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int col = Integer.parseInt(spColNum.getSelectedItem().toString())-1;
                for (int r = 0; r < row; r++) {
                       clbox[r][col].xDeltaLow();
                }
            }
        });
        findViewById(R.id.btnColRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int col=Integer.valueOf(spColNum.getSelectedItem().toString()).intValue()-1;
                for (int r = 0; r < row; r++) {
                     clbox[r][col].xDeltaAdd();
                }
            }
        });
        findViewById(R.id.btnColMin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (col>1)col--;
                edtCol.setText(String.valueOf(col));
            }
        });
        findViewById(R.id.btnColMax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (col<12)col++;
                edtCol.setText(String.valueOf(col));
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
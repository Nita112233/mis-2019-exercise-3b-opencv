package com.example.misexercise3nita;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import static java.lang.System.out;

// Mainly using sample template provided in the course. https://github.com/mmbuw-courses/mis-2019-exercise-3b-opencv/blob/master/app/src/main/java/mis/example/misopencv/MainActivity.java

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase openCvCameraView; // Connect OpenCV with device's camera
    private int mCameraId = 0;
    private CascadeClassifier noseClassifier;
    private int noseSize;
    Mat mRgba, mRgbaF, mRgbaT, gray;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //using a Haar Cascade training data to recognize nose
                    String noseAsset = initAssetFile("haarcascade_mcs_nose.xml");
                    try{
                        noseClassifier =  new CascadeClassifier(noseAsset);
                    } catch (Exception e) {
                        Log.i("OpenCVActivity", "Error loading nose classifier", e);
                    }
                    noseClassifier.load(noseAsset);
                    if(!noseClassifier.load(noseAsset)){
                        out.println("Error loading nose assets\n");
                        return;
                    };
                    openCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        // before opening the CameraBridge, we need the Camera Permission on newer Android versions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0x123);
        } else {
            openCvCameraView = (JavaCameraView) findViewById(R.id.opencvView);
            openCvCameraView.setVisibility(SurfaceView.VISIBLE);
            openCvCameraView.setCameraIndex(-1); // Set to open the back camera first. Front camera index = 1. Default camera index = -1.
            openCvCameraView.setCvCameraViewListener(this);
        }
    }

    // switch camera button
    // https://stackoverflow.com/questions/16273370/opencvandroidsdk-switching-between-front-camera-and-back-camera-at-run-time
    public void swapCamera(View view) {
        mCameraId = mCameraId^1;
        openCvCameraView.disableView();
        openCvCameraView.setCameraIndex(mCameraId);
        openCvCameraView.enableView();
    }

    @Override
    public void onPause(){
        super.onPause();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    //https://blog.codeonion.com/2016/04/09/show-camera-on-android-app-using-opencv-for-android/
    @Override
    public void onCameraViewStarted(int width, int height) {
        // Receive Image Data when the camera preview starts on the screen
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        noseSize = (int)(height/10); // set to recognize nose that fill up 10% of the screen
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        gray = inputFrame.gray();

        // Change camera orientation to portrait. Works on back camera, but not on the front camera.
        // https://blog.codeonion.com/2016/04/09/show-camera-on-android-app-using-opencv-for-android/
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2RGB);

        MatOfRect noses = new MatOfRect();

        // Use the classifier to detect noses
        // https://docs.opencv.org/3.4/d1/de5/classcv_1_1CascadeClassifier.html#aaf8181cb63968136476ec4204ffca498
        noseClassifier.detectMultiScale(
                gray,
                noses,
                1.1,
                2, // set to 2 to reduce false negative https://stackoverflow.com/questions/22249579/opencv-detectmultiscale-minneighbors-parameter
                0,
                new Size(noseSize, noseSize),
                new Size());
        Rect[] nosesArray = noses.toArray();
        // if nose found, create red circle for “clown nose effect”
        // https://stackoverflow.com/questions/44447432/detect-face-and-apply-a-mask-on-it-with-opencv
        // https://docs.opencv.org/2.4/doc/tutorials/objdetect/cascade_classifier/cascade_classifier.html?highlight=haarcascade
        for(int i = 0; i < nosesArray.length; i++) {
            // finding center of nose https://stackoverflow.com/questions/34790480/opencv-find-centre-of-face-in-facedetction
            Point centerNose = new Point(
                    (nosesArray[i].tl().x + nosesArray[i].br().x)/2,
                    (nosesArray[i].tl().y + nosesArray[i].br().y)/2);
            // finding proportional radius for clown nose
            int radius = (int)(nosesArray[i].br().x - nosesArray[i].tl().x)/3;
            Imgproc.circle( // draw red circle https://docs.opencv.org/3.0-beta/modules/imgproc/doc/drawing_functions.html#circle
                    mRgba,
                    centerNose, //center of nose
                    radius, // circle radius
                    new Scalar(255, 0, 0), // color = red
                    -1 // fill circle
            );
        }
        return mRgba;
    }

    public String initAssetFile(String filename)  {
        File file = new File(getFilesDir(), filename);
        if (!file.exists()) try {
            InputStream is = getAssets().open(filename);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data); os.write(data); is.close(); os.close();
        } catch (IOException e) { e.printStackTrace(); }
        return file.getAbsolutePath();
    }
}

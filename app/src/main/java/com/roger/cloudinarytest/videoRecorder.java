package com.roger.cloudinarytest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class videoRecorder extends AppCompatActivity {

    /** Code that is used for the response of permission granting alerts */
    private static final int REQUEST_CAMERA_PERMISSION_CALLBACK = 0x1234;

    /** Android permissions that will be asked to the user in order to record (> Android Lolipop 5.0) */
    private static final String[] RECORD_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int MAX_VIDEO_SIZE = 10_485_760; //10 MiB = 10 * 1024 * 1024
    private static final int MAX_RECORD_TIME = 60_000; //60 milisec
    private static final int ORIENTATION_DEGREES = 90; //portrait
    private static final String VIDEO_EXTENSION = ".mp4";
    private static final String VIDEO_NAME = "video";

    private Camera mCamera;
    private MyCameraSurfaceView myCameraSurfaceView;
    private MediaRecorder mediaRecorder;
    private File outputVideo;

    FloatingActionButton recordButton;
    boolean recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recording = false;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_recorder);

        if(hasPermission()) {
            setupCamera();
        } else {
            //will not finish activity until user explicitly denies permissions
        }

    }

    FloatingActionButton.OnClickListener recordButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(recording){
                // stop recording and release camera
                mediaRecorder.stop();  // stop the recording
                releaseMediaRecorder(); // release the MediaRecorder object
                recordButton.setImageResource(R.drawable.ic_record);
                Toast.makeText(videoRecorder.this, "grabado", Toast.LENGTH_SHORT).show();

                Intent preview = new Intent(getApplicationContext(), videoPreview.class);
                preview.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                preview.putExtra("URL", outputVideo.getPath());
                startActivity(preview);

                finish();

            }else{

                //Release Camera before MediaRecorder start
                releaseCamera();

                if(!prepareMediaRecorder()){
                    Toast.makeText(videoRecorder.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }
                recordButton.setImageResource(R.drawable.ic_stop);
                mediaRecorder.start();
                recording = true;

            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    /** Determines if the user has granted a group of permissions to the app */
    public static boolean hasPermissions(Context context, String ... permissions) {

        for(String p : permissions) {
            if(ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                Log.w("PERMISSIONS", "No Pemission for " + p);
                return false;
            }

            Log.d("PERMISSIONS", "Has permission :" + p);
        }




        return true;
    }

    /** Returns true if the user has permission to use the Camera,
     * If the user has no permission, then the permission prompt will be shown to the user.
     * If this is the second or more time the user has not accepted the permission, an explanation
     * will be shown to the user on why he needs the permission
     *
     * @return true if the user has the permission, false if not
     */
    private boolean hasPermission() {
        // Check for camera permission
        if (!(videoRecorder.hasPermissions(this, RECORD_PERMISSIONS))) {

            // No explanation needed for Camera Recording, we can request directly the permission.
            ActivityCompat.requestPermissions(this,
                    RECORD_PERMISSIONS,
                    REQUEST_CAMERA_PERMISSION_CALLBACK);

            return false; //no permission, will be requested

        } else {
            return true;  //has permission
        }

    }


    /** Called when the user accepts or denies a permission from the prompt alert
     *
     * In this screen, user should accept all the following: CAMERA
     *
     * Once the permissions are granted, then the video recording functionality can be set up.
     *
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CALLBACK: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // camera task you need to do.
                    setupCamera();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request
        }
    }

    /**
     * Setups the activity for recording. This should only be called once
     * the user has granted camera and storage permissions.
     */
    private void setupCamera(){

        //Get Camera for preview
        mCamera = getCameraInstance();
        if(mCamera == null){
            Toast.makeText(videoRecorder.this, "Fail to get Camera", Toast.LENGTH_LONG).show();
            return;
        }

        mCamera.setDisplayOrientation(90);
        myCameraSurfaceView = new MyCameraSurfaceView(this, mCamera);
        FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.videoview);
        myCameraPreview.addView(myCameraSurfaceView);

        recordButton = (FloatingActionButton)findViewById(R.id.fab);
        recordButton.setImageResource(R.drawable.ic_record);
        recordButton.setOnClickListener(recordButtonListener);
    }




    private Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){

        }
        return c;
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            mCamera.lock();
        }
    }

    private boolean prepareMediaRecorder() {
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(ORIENTATION_DEGREES);
        mediaRecorder = new MediaRecorder();

        mCamera.unlock();

        mediaRecorder.setCamera(mCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        try{
            outputVideo = File.createTempFile(VIDEO_NAME, VIDEO_EXTENSION, getExternalCacheDir());
        }catch (IOException ioe){

        }
        mediaRecorder.setOutputFile(outputVideo.getAbsolutePath());
        mediaRecorder.setMaxDuration(MAX_RECORD_TIME);
        mediaRecorder.setMaxFileSize(MAX_VIDEO_SIZE);

        mediaRecorder.setOrientationHint(ORIENTATION_DEGREES);
        mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public MyCameraSurfaceView(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            mHolder = getHolder();
            mHolder.addCallback(this);

            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int weight,
                                   int height) {

            if (mHolder.getSurface() == null){
                return;
            }

            try {
                mCamera.stopPreview();
            } catch (Exception e){
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setDisplayOrientation(ORIENTATION_DEGREES);
                mCamera.startPreview();

            } catch (Exception e){
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub

        }
    }
}
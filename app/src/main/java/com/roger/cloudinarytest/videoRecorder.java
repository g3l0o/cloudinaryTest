package com.roger.cloudinarytest;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class videoRecorder extends AppCompatActivity {

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


        //Get Camera for preview
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        if(mCamera == null){
            Toast.makeText(videoRecorder.this, "Fail to get Camera", Toast.LENGTH_LONG).show();
        }

        myCameraSurfaceView = new MyCameraSurfaceView(this, mCamera);
        FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.videoview);
        myCameraPreview.addView(myCameraSurfaceView);

        recordButton = (FloatingActionButton)findViewById(R.id.fab);
        recordButton.setImageResource(R.drawable.ic_record);
        recordButton.setOnClickListener(recordButtonListener);

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

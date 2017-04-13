package com.roger.cloudinarytest;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_VIDEO_CAPTURE = 1;
    static final int REQUEST_VIDEO_GALLERY = 2;
    static final int MAX_VIDEO_SIZE = 10_485_760; // 10 * 1024 * 1024 = 10 MiB

    Button button_camera;
    Button button_gallery;

    VideoView mVideo;
    MediaController mediaControls;

    Cloudinary cloudinary;

    Map uploadResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cloudinary = StartDataBase();

        button_camera = (Button) findViewById(R.id.buttonCamera);
        button_gallery = (Button) findViewById(R.id.buttonGallery);
        mVideo = (VideoView) findViewById(R.id.videoView);

        button_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent recordVideoIntent = new Intent(getApplicationContext(), videoRecorder.class);
                startActivityForResult(recordVideoIntent, REQUEST_VIDEO_CAPTURE);

            }
        });

        button_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryVideoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryVideoIntent.setType("video/*");
                startActivityForResult(Intent.createChooser(galleryVideoIntent, "Select Video"), REQUEST_VIDEO_GALLERY);
            }
        });

        if(mediaControls == null){
            mediaControls = new MediaController(MainActivity.this);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK){
            Toast.makeText(getApplicationContext(), intent.getStringExtra("URL"), Toast.LENGTH_SHORT).show();
            playVideo(intent.getStringExtra("URL"));

            AsyncTask uploader = new ASTVideo();
            String uri = intent.getStringExtra("URL");
            uploader.execute(uri);
        }


        if(requestCode == REQUEST_VIDEO_GALLERY && resultCode == RESULT_OK){

            Intent preview = new Intent(getApplicationContext(), videoPreview.class);
            preview.putExtra("URL", intent.getData().toString());
            startActivityForResult(preview, REQUEST_VIDEO_CAPTURE);
        }
    }

    public void playVideo(String uri){
        try{
            mVideo.setMediaController(mediaControls);
            mVideo.setVideoURI(android.net.Uri.parse(uri));
            mVideo.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void uploadImage(final InputStream inputStream) {

        final Map<String, String> options = new HashMap<>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    cloudinary.uploader().upload(inputStream, options);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(runnable).start();
    }

    public void uploadVideo(final InputStream inputStream) {

        final Map<String, String> options = new HashMap<>();
        options.put("resource_type", "video");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    cloudinary.uploader().upload(inputStream, options);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(runnable).start();
    }

    protected Cloudinary StartDataBase(){
        Map config = new HashMap();
        config.put("cloud_name", "djhlybxq8");
        config.put("api_key", "982818278736173");
        config.put("api_secret", "D-aspNby4D5SkWW6QkjFWYj4mag");
        return new Cloudinary(config);
    }

    private class ASTVideo extends AsyncTask{

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, "Subiendo Video", "Estamos subiendo tu video");
            Log.wtf("video", "acá ando");
        }

        @Override
        protected Object doInBackground(Object[] params) {

            publishProgress("Cargando");

            final Map<String, String> options = new HashMap<>();
            options.put("resource_type", "video");

            try {
                InputStream is = getContentResolver().openInputStream(android.net.Uri.parse(params[0].toString()));
                uploadResult = cloudinary.uploader().upload(is, options);

            } catch (OutOfMemoryError ooe){
                Toast.makeText(MainActivity.this, "El video es muy largo", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            progressDialog.dismiss();
            Log.d("map video", uploadResult.get("url").toString());
            playVideo(uploadResult.get("url").toString());
        }


    }
}

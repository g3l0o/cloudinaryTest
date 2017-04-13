package com.roger.cloudinarytest;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class videoPreview extends AppCompatActivity {

    private String videoURL;

    VideoView mVideo;
    MediaController mediaControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        videoURL=  getIntent().getStringExtra("URL");

        mVideo = (VideoView) findViewById(R.id.previewView);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent output = new Intent();
                output.putExtra("URL", getIntent().getStringExtra("URL"));
                setResult(RESULT_OK, output);
                finish();
            }
        });

        if(mediaControls == null){
            mediaControls = new MediaController(videoPreview.this);
        }

        playVideo(videoURL);
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
}

package com.learning;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.samples.DefaultMp4SampleList;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class RecordingActivity extends AppCompatActivity implements Observer {

    RecyclerView video_list;
    Button stop_recording;
    ImageButton start_recording;

    int hasWritePermission, hasReadPermission, hasRecordPermission;
    ArrayList<String> permission = new ArrayList();

    MediaRecorder mediaRecorder;
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection = null;
    MediaProjectionCallBack mediaProjectionCallBack;

    VirtualDisplay display;

    CountDownTimer timer;
    int seconds, minutes;

    int screenDpi, displayWidth, displayHeight;
    String bitRate = "6144";    //6MB (define the clearty)
    String frameRate = "24";

    NotificationManager notificationManager;
    Notification notification;
    RemoteViews notificationView;

    Boolean isRecording = false;
    private ArrayList<String> sources = new ArrayList<>();
    private String filePath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 100 && resultCode != RESULT_OK) {
            mediaRecorder.reset();
            timer.cancel();
            start_recording.setVisibility(View.VISIBLE);
            stop_recording.setVisibility(View.INVISIBLE);
            return;
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallBack, null);
        display = createVirtualDisplay();
        mediaRecorder.start();

        isRecording = true;
        setNotification("Record..");

        startTimer();

        start_recording.setVisibility(View.INVISIBLE);
        stop_recording.setVisibility(View.VISIBLE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        video_list = findViewById(R.id.video_list);
        stop_recording = findViewById(R.id.stop);
        start_recording = findViewById(R.id.record);

        video_list.setLayoutManager(new LinearLayoutManager(this));

        //observer
        AppController.getObserver().addObserver(this);

        //permission checking and requesting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            hasRecordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

            if (hasRecordPermission != PackageManager.PERMISSION_GRANTED) {
                permission.add(Manifest.permission.RECORD_AUDIO);
            }
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                permission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if (!permission.isEmpty()) {
                ActivityCompat.requestPermissions(this, permission.toArray(new String[permission.size()]), 1000);
            } else {

            }

        }

        start_recording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seconds = 0;
                minutes = 0;

                startRecording();
            }
        });

        stop_recording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
                loadAdapter();
            }
        });

        //recording video
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjectionCallBack = new MediaProjectionCallBack();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenDpi = metrics.densityDpi;

        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        displayHeight = point.y;
        displayWidth = point.x;

        loadAdapter();

    }

    public void setNotification(String message) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new Notification(R.mipmap.ic_launcher, null, System.currentTimeMillis());
        notificationView = new RemoteViews(getPackageName(), R.layout.notify_layout);
        notificationView.setTextViewText(R.id.notify_status, message);

        PendingIntent recordIntent, stopIntent, closeIntent;
        if (isRecording) {
            notificationView.setImageViewResource(R.id.play, android.R.drawable.ic_media_play);
        } else {
            notificationView.setImageViewResource(R.id.stop, android.R.drawable.ic_media_pause);
        }

        recordIntent = PendingIntent.getBroadcast(AppController.getInstance(), 0, new Intent("PLAY"), PendingIntent.FLAG_UPDATE_CURRENT);
        stopIntent = PendingIntent.getBroadcast(AppController.getInstance(), 0, new Intent("STOP"), PendingIntent.FLAG_UPDATE_CURRENT);
        closeIntent = PendingIntent.getBroadcast(AppController.getInstance(), 0, new Intent("CLOSE"), PendingIntent.FLAG_UPDATE_CURRENT);

        notificationView.setOnClickPendingIntent(R.id.play, recordIntent);
        notificationView.setOnClickPendingIntent(R.id.stop, stopIntent);
        notificationView.setOnClickPendingIntent(R.id.close, closeIntent);

        notification.contentView = notificationView;
        notificationManager.notify(0, notification);
    }

    public void startRecording() {

        initRecorder();
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 100);
            return;
        }
        start_recording.setVisibility(View.INVISIBLE);
        stop_recording.setVisibility(View.VISIBLE);
        display = createVirtualDisplay();
        mediaRecorder.start();

        isRecording = true;
        setNotification("Recording...");

        startTimer();

    }

    public void startTimer() {
        timer = new CountDownTimer(1000 * 60 * 60, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String time = "00:00";
                seconds += 1;

                if (seconds == 60) {
                    seconds = 0;
                    minutes += 1;
                }

                if (seconds < 10) {
                    time = "0" + seconds;
                } else {
                    time = seconds + "";
                }

                if (minutes < 10) {
                    time = "0" + minutes + ":" + time;
                } else {
                    time = minutes + ":" + time;
                }


                stop_recording.setText(time);
            }

            @Override
            public void onFinish() {
                stopRecording();
                loadAdapter();
            }
        }.start();
    }

    public void initRecorder() {

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/ScreenRecorder");
        if (!file.exists()) file.mkdir();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
        mediaRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
        mediaRecorder.setVideoSize(displayWidth, displayHeight);

        filePath = file.getPath() + "/" + getFileName();

        mediaRecorder.setOutputFile(filePath);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            System.out.println("Exception : " + e);
            toast("Unable to record screen.");
            finish();
        }

    }

    private String getFileName() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        return simpleDateFormat.format(new Date()) + ".mp4";
    }

    public void stopRecording() {

        //mediaRecorder.stop();
        timer.cancel();

        isRecording = false;

        mediaRecorder.reset();

        if (display == null) {
            return;
        }

        if (sources.size()>1){
            mergeRecording(sources,Environment.getExternalStorageDirectory().getPath()+"/ScreenRecorder/"+getFileName());
        }

        display.release();

        start_recording.setVisibility(View.VISIBLE);
        stop_recording.setVisibility(View.INVISIBLE);

    }

    public VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("ScreenRecorder", displayWidth, displayHeight, screenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    toast("Permission Denied");
                    finish();
                }
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        String action = AppController.getObserver().getAction();
        if (action.equalsIgnoreCase("CLOSE")) {
            toast("Recording cancle!!");
            finish();
        } else if (action.equalsIgnoreCase("STOP")) {
            stopRecording();
            loadAdapter();
        } else {
            if (isRecording) {
                notificationView.setImageViewResource(R.id.play, android.R.drawable.ic_media_play);
                notificationView.setTextViewText(R.id.notify_status, "Recording Pause...");
                isRecording = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder.pause();
                    timer.cancel();
                } else{
                    sources.add(filePath);
                    stopRecording();
                }

            } else {
                notificationView.setImageViewResource(R.id.stop, android.R.drawable.ic_media_pause);
                notificationView.setTextViewText(R.id.notify_status, "Recording...");
                isRecording = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder.resume();
                    startTimer();
                }else{
                    startRecording();
                }

            }
            //startRecording();
        }
    }

    class MediaProjectionCallBack extends MediaProjection.Callback {

        @Override
        public void onStop() {
            mediaProjectionManager = null;
            stopRecording();
            loadAdapter();
            //super.onStop();
        }
    }

    public ArrayList<File> getAllFiles() {
        ArrayList<File> files = new ArrayList<>();
        File directory = new File(Environment.getExternalStorageDirectory().getPath() + "/ScreenRecorder");
        File[] allFiles = directory.listFiles();
        if (allFiles != null && allFiles.length > 1) {
            Arrays.sort(allFiles, new Comparator<File>() {
                @Override
                public int compare(File t1, File t2) {
                    long m1 = t1.lastModified();
                    long m2 = t2.lastModified();
                    return ((m2 < m1) ? -1 : ((m1 > m2) ? 1 : 0) * (-1));
                }
            });
            for (int i = 0; i < allFiles.length; i++) {
                if (allFiles[i].getName().endsWith(".mp4")) {
                    files.add(allFiles[i]);
                }
            }
        }
        return files;
    }

    public void loadAdapter() {
        VideoAdapter videoAdapter = new VideoAdapter(getAllFiles(),RecordingActivity.this);
        video_list.setAdapter(videoAdapter);
    }

    public void toast(String msg) {
        Toast.makeText(getApplicationContext(), "" + msg, Toast.LENGTH_SHORT).show();
    }

    public Boolean mergeRecording(ArrayList<String> sources, String target) {
        try {


            List<Movie> myRecordings = new ArrayList<>();
            for (String s : sources) {
                myRecordings.add(MovieCreator.build(s));
            }
            List<Track> videos = new LinkedList<>();
            for (Movie m : myRecordings) {
                for (Track t:m.getTracks()){
                    videos.add(t);
                }
            }

            Movie movie = new Movie();
            if (videos.isEmpty()){
                movie.addTrack(new AppendTrack(videos.toArray(new Track[videos.size()])));
            }

            Container c = new DefaultMp4Builder().build(movie);
            FileChannel f = new RandomAccessFile(String.format(target),"rw").getChannel();
            c.writeContainer(f);
            return true;

        } catch (Exception e) {
            System.out.println("Exception : " + e);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

}

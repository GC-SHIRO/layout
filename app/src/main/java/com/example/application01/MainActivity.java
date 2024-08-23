package com.example.application01;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.media.MediaPlayer;
import android.os.Environment;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import java.io.File;
import java.io.IOException;
import android.Manifest;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity {
    private boolean isPlaying = false;
    private static final int REQUEST_CODE = 200;
    private MediaRecorder mediaRecorder;
    private String filename = null;//录音文件地址
    private  long pressStartTime;
    private  long playtime;
    private  static  final long SHORT_PRESS_THRESHOLD = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });

        getWindow().getDecorView().post(() -> hideSystemBars());
        //请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
        }


        //播放器
        MediaPlayer mp = MediaPlayer.create(this,R.raw.mp3toplay);
        Button buttonsp = findViewById(R.id.buttonplaying);
        buttonsp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPlaying){
                    mp.pause();
                    mp.seekTo(0);
                    isPlaying = false;
                    buttonsp.setText("Start Playing");
                }else{
                    mp.start();
                    buttonsp.setText("Stop Playing");
                    isPlaying=true;
                }

            }
        });
        //停止播放事件
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                isPlaying = false;
                buttonsp.setText("Start Playing");
            }
        });


        //录音
        Button recordButton = findViewById(R.id.recordButton);
        TextView text  = findViewById(R.id.textView);
        // 设置文件路径
        File tempDir = getTempDirectory();
        filename = tempDir.getAbsolutePath() +"/Recording";

        MediaPlayer mprec = MediaPlayer.create(this,R.raw.record_over);

        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录按下的时间
                        pressStartTime = System.currentTimeMillis();
                        // 开始录音
                        startRecording();
                        text.setText("I'm hearing,sir");
                        recordButton.setScaleX((float) 0.90);
                        recordButton.setScaleY((float) 0.90);
                        recordButton.setText("Stop Record");
                        return false;

                    case MotionEvent.ACTION_UP:
                        // 计算按下的时间
                        long pressDuration = System.currentTimeMillis() - pressStartTime;

                        if (pressDuration < SHORT_PRESS_THRESHOLD) {
                            // 短按处理
                            showToast("Pressed Time Too Short:"+pressDuration+"ms");
                            // 如果短按，停止录音
                            stopRecording();
                            recordButton.setScaleX((float) 1.00);
                            recordButton.setScaleY((float) 1.00);
                            text.setText("Hello World!");
                            recordButton.setText("Start Record");
                            v.performClick();
                            return false;
                        } else {
                            // 长按处理
                            stopRecording();
                            recordButton.setScaleX((float) 1.00);
                            recordButton.setScaleY((float) 1.00);
                            text.setText("Hello World!");
                            convertToWav(filename+".3gp",filename+".wav");
                            showToast("Save Succesed,Duration:"+pressDuration+"ms");
                            mprec.start();
                            recordButton.setText("Start Record");
                            return false;
                        }

                    default:
                        return false;
                }
            }
        });

    }

    //转换wav
    private void convertToWav(String inputFilePath,String outputFilePath){
        String[] cmd = {"-i",inputFilePath,outputFilePath};
        FFmpeg.executeAsync(cmd,(executionId, returnCode) -> {
            if(returnCode == Config.RETURN_CODE_SUCCESS){
                Log.e("FFmpeg", "Conversion to WAV successed");

            }else {
                Log.e("FFmpeg", "Conversion to WAV failed");
            }
        });
    }

    //创建Temp文件夹
    private File getTempDirectory(){
        File tempDir;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            tempDir = new File(getExternalFilesDir(null),"Temp");
        }else{
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Temp";
            tempDir = new File(path);
        }

        if(!tempDir.exists()){
            tempDir.mkdirs();
        }
        return  tempDir;
    }

    //开始录音
    private void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(filename+".3gp");
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Log.d("TAG", "Recording started");
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            } catch (RuntimeException e) {
                Log.e("TAG", "start() failed");
            }
        }
    }
    //结束录音
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                Log.e("TAG", "stop() failed");
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                Log.d("TAG", "Recording stopped");
            }
        }
    }
    //展示提示信息
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //隐藏bar
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30 及以上：使用 WindowInsetsController
            getWindow().setDecorFitsSystemWindows(false);  // 使内容可以延伸到系统栏区域
            View decorView = getWindow().getDecorView();
            WindowInsetsController insetsController = decorView.getWindowInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // API 30 以下：使用 SYSTEM_UI_FLAG
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }


}
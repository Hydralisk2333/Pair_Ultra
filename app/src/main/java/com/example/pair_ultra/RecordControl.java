package com.example.pair_ultra;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


import static com.example.pair_ultra.GlobalConfig.*;


public class RecordControl {

    private Context context;
    private String saveFileName;
    private AudioRecord audioRecord;
    private AudioTrack playTrack;

    private int personId;
    private int sonicId;

    private boolean isRecording;
    private boolean isTrackPlay;

    String TAG = "no name";

    public RecordControl(Context context, int personId){
        this.context = context;
        this.personId = personId;
        sonicId = SONIC_ID;
    }

    public void setSaveFileName(String[] packedMessage) {
        String name = packedMessage[1] + "_" + packedMessage[2];
        personId = Integer.parseInt(packedMessage[3]);
        saveFileName = personId + "/" + name;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void startRecord(String[] packedMessage){
        setSaveFileName(packedMessage);
        playInModeStream();
        startWaveRecord();
    }

    public void stopRecord(){
        stopWaveRecord();
        stopPlay();
        pcm2wavFile();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void startWaveRecord(){
        final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLE_RATE_INHZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

        final byte data[] = new byte[minBufferSize];
        // 下面是存储文件的方法

        String pcmFileName = saveFileName + ".pcm";
        System.out.println(pcmFileName);

        final File file = new File(context.getExternalFilesDir(""), pcmFileName);
//        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "dir1/11.txt");

        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        if (file.exists()) {
            file.delete();
        }

        audioRecord.startRecording();
        isRecording = true;

        new Thread(new Runnable() {
            @Override
            public void run() {

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (null != os) {
                    while (isRecording) {
                        int read = audioRecord.read(data, 0, minBufferSize);
                        // 如果读取音频数据没有出现错误，就将数据写入到文件
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        Log.i(TAG, "run: close file output stream !");
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void pcm2wavFile(){
        String pcmFileName = saveFileName + ".pcm";
        String wavFileName = saveFileName + ".wav";
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        File pcmFile = new File(context.getExternalFilesDir(""), pcmFileName);
        File wavFile = new File(context.getExternalFilesDir(""), wavFileName);
        if (!wavFile.mkdirs()) {
            Log.e("file create error", "wavFile Directory not created");
        }
        if (wavFile.exists()) {
            wavFile.delete();
        }
        pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());
        if(pcmFile.exists()){
            pcmFile.delete();
        }
    }

    public void stopWaveRecord(){
        isRecording = false;
        // 释放资源
        if (null != audioRecord) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            //recordingThread = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void playInModeStream() {
        /*
         * SAMPLE_RATE_INHZ 对应pcm音频的采样率
         * channelConfig 对应pcm音频的声道
         * AUDIO_FORMAT 对应pcm音频的格式
         * */

        isTrackPlay = true;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        final int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_INHZ, channelConfig, AUDIO_FORMAT);
        playTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder().setSampleRate(SAMPLE_RATE_INHZ)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(channelConfig)
                        .build(),
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        playTrack.setVolume(1f);
        playTrack.play();

        try {

            // sonicid
            final InputStream fileInputStream = context.getResources().openRawResource(sonicId);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int innerCount = 0;
                        byte[] tempBuffer = new byte[minBufferSize];
                        if (fileInputStream.available() > 0){
                            byte[] headBuffer = new byte[44];
                            int readCount = fileInputStream.read(headBuffer);
                            Log.d("start count", ""+readCount);
                        }
                        while (fileInputStream.available() > 0) {
                            innerCount++;
//                            Log.d("whileCount", innerCount+"");
                            int readCount = fileInputStream.read(tempBuffer);

                            if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                                    readCount == AudioTrack.ERROR_BAD_VALUE) {
                                continue;
                            }

                            if (playTrack == null || playTrack.getState() == AudioTrack.STATE_UNINITIALIZED){
                                Log.d("tryBreak", "break");
                                break;
                            }

                            synchronized (this) {
                                if (!isTrackPlay){
                                    break;
                                }
                            }

                            if (readCount != 0 && readCount != -1) {
                                Log.d("SonicPlay", readCount+"");
                                playTrack.write(tempBuffer, 0, readCount);
                            }
                        }
                        Log.d(TAG, "Stopping");
                        playTrack.stop();
                        Log.d(TAG, "Releasing");
                        playTrack.release();
                        Log.d(TAG, "Nulling");
                    } catch (IOException e) {
                        Log.d("tryError", "playError");
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stopPlay() {
        if (playTrack != null) {
            synchronized (this) {
                if (isTrackPlay){
                    isTrackPlay = false;
                }
            }
        }
    }

    public void backDelete(String[] packedMessage){
        setSaveFileName(packedMessage);
        String deleteFileName = saveFileName + ".wav";
        final File file = new File(context.getExternalFilesDir(""), deleteFileName);
        if (file.exists()){
            file.delete();
        }
    }



}

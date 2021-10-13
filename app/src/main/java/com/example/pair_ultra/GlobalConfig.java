package com.example.pair_ultra;

import android.media.AudioFormat;


public class GlobalConfig {

    /**
     * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
     */
    public static int SAMPLE_RATE_INHZ = 44100;

    public static int SONIC_ID = R.raw.ultrasonic20k;
    /**
     * 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
     */
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
     */
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static final int MAX_COUNT = 30;
    public static final int PACKED_DATA_LEN = 4;
    public static final String START = "start";
    public static final String END = "end";
    public static final String BACK = "back";
    public static final String AHEAD = "ahead";
    public static final String PERSON_ID = "personId";
    public static final String SPILIT_CHAR = "-";
}

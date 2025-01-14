package gr.auth.ee.mug.datacollectionapp.audiocapture;

import android.media.MediaRecorder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioCaptureManager {
    private MediaRecorder recorder;

    /**
     * Class constructor, creates the recorder
     */
    public AudioCaptureManager() {
        recorder = new MediaRecorder();
    }

    /**
     * Creates the audio file, sets the recorders parameters and starts the recording
     * @param externalFilesDir The directory where the app saves its files
     * @return The absolute file path of the file that's created
     */
    public String start(String externalFilesDir) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String file = sdf.format(new Date()) + ".amr";
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        String fullFilePath = externalFilesDir + "/" + file;
        recorder.setOutputFile(fullFilePath);
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();
        return fullFilePath;
    }

    /**
     * Stops the audio recording and clears the recorder
     */
    public void stop() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }
}

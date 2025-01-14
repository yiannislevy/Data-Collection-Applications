package gr.auth.ee.mug.datacollectionapp.mandocapture;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MandoCaptureManager {

    MandometerController mandometerController;
    Context mContext;
    private String externalFilesDir;
    private int notAvailable;

    /**
     * Class constructor, initializes the parameters and connects to the mandometer
     * @param context provided by activity/fragment/application to be used for creating mandometer
     * @param bluetoothDevice the bluetooth device that represents the mandometer
     */
    public MandoCaptureManager(@NonNull Context context, BluetoothDevice bluetoothDevice) {
        this.mContext = context;
        mandometerController = new MandometerController(context.getSharedPreferences("mandometer", Context.MODE_PRIVATE), context);
        mandometerController.setMandometerDev(bluetoothDevice);
        this.notAvailable = mandometerController.connectToMandometer();
    }

    /**
     * Getter for notAvailable property
     * @return 1 if mandometer is not available for connection, 0 otherwise
     */
    public int notAvailable() {
        return this.notAvailable;
    }

    /**
     * Creates the meal and starts weight monitoring
     * @param externalFilesDir The directory where the app saves its files
     */
    public void start(String externalFilesDir) {
        mandometerController.createMeal();
        mandometerController.startRecMeal();
        this.externalFilesDir = externalFilesDir;
    }

    /**
     * Stops the recording and calls function to save weights
     * @return The absolute file path of the file that's created
     */
    public String stop() {
        mandometerController.stopRecMandometer();
        mandometerController.setSkip();  // i.e., finishMeal()
        int[] weights = mandometerController.getWeights();
        mandometerController.disconnectFromMandometer();
        return writeWeights(weights);
    }

    /**
     * Sets the tare after the user selects it
     */
    public void setMandoTare() {
        mandometerController.setTare();
    }

    /**
     * Saves the weights captured from the mandometer to a file at the phones storage directory
     * @param weights the array that contains the weights captured by the mandometer
     */
    public String writeWeights(int[] weights) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fullFileName = externalFilesDir + "/" + "weights_" + sdf.format(new Date()) + ".txt";
        File weightFile = new File(fullFileName);
        try {
            weightFile.createNewFile();
            try {
                FileWriter writer = new FileWriter(weightFile);
                for (int weight : weights) writer.write(weight + "\n");
                writer.close();
            } catch (IOException e) {
                Toast.makeText(mContext, "Problem writing to weights file.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } catch (IOException e) {
            Toast.makeText(mContext, "Problem creating weights file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return fullFileName;
    }
}

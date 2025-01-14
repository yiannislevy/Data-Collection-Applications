package gr.auth.ee.mug.datacollectionapp.mandocapture;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Mandometer API for Android
 * <p>
 * Requires "android.permission.BLUETOOTH" permission in AndroidManifest.xml
 *
 * @author Vasileios Papapanagiotou [email: vassilis@mug.ee.auth.gr]
 * @version 1.0
 */
@SuppressLint("MissingPermission")
public class MandometerAPI {

    /**
     * Message attribute 'what' is equal to msgID_Weight when Weight Data are returned
     */
    public static final int msgID_Weight = 1;
    /**
     * Message attribute 'what' is equal to msgID_Tare when the current Tare Value is returned
     */
    public static final int msgID_Tare = 2;
    /**
     * Message attribute 'what' is equal to msgID_TechnicalInfo when Technical Info is returned
     */
    public static final int msgID_TechnicalInfo = 3;
    /**
     * Message attribute 'what' is equal to msgID_Calibration when in Calibration mode
     */
    public static final int msgID_Calibration = 4;
    /**
     * Message attribute 'what' is equal to msgID_Error if error occurs
     */
    public static final int msgID_Error = 9;
    private static final int m5listener_buffer_size = 32;
    private static final byte LF = (byte) 10;
    private static final byte CR = (byte) 13;
    private static final byte Hyphen = (byte) 45;
    private final BluetoothDevice device;
    private final Handler handler;
    private final UUID m5uuid;
    private BluetoothSocket socket;
    private InputStream inStream;
    private OutputStream outStream;
    private m5listener m5listenerThread;
    private Context context;

    /**
     * @param adevice  The BluetoothDevice of the Mandometer
     * @param ahandler An android handler to pass messages; the handleMessage function should be implemented by the
     *                    function that created the Mandometer5API object
     */
    public MandometerAPI(BluetoothDevice adevice, Handler ahandler, Context mContext) throws SecurityException {
        device = adevice;
        handler = ahandler;
        m5uuid = adevice.getUuids()[0].getUuid();
        this.context = mContext;
    }

    /**
     * Connect to the Mandometer device
     *
     * @param securely If true, a secure channel is created
     * @return 0 if no error, a positive number indicating the error occured otherwise
     * @throws IOException
     */
    public int Connect(boolean securely) throws IOException, SecurityException {
        int e = 0;

        if (securely) {
            socket = device.createRfcommSocketToServiceRecord(m5uuid);
        } else {
            socket = device.createInsecureRfcommSocketToServiceRecord(m5uuid);
        }

        socket.connect();

        inStream = socket.getInputStream();
        outStream = socket.getOutputStream();

		if (socket == null) {e += 1;}
		if (inStream == null) {e += 2;}
		if (outStream == null) {e += 4;}

        m5listenerThread = new m5listener();
        m5listenerThread.setContext(context);

        if (e == 0) {
            m5listenerThread.start();
        }

        return e;
    }

    /**
     * Disconnect from the Mandometer
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void Disconnect() throws IOException, InterruptedException {
        m5listenerThread.uNeed2Stop();
        socket.close();
        inStream = null;
        outStream = null;
        m5listenerThread.join();
    }

    private void processReceived(String str) throws Exception{
        Message msg = Message.obtain();
        if(str.substring(0, 2).equals("Ch"))
            throw new Exception("Chck received");
        int cmd = Integer.parseInt(str.substring(0, 2));

        if (cmd == 61) {
            msg.what = msgID_TechnicalInfo;
            msgTechnicalInfo payload = new msgTechnicalInfo();
            payload.SoftwareVersion = Float.parseFloat(str.substring(3, 13));
            payload.BootcodeVersion = Float.parseFloat(str.substring(14, 21));
            msg.obj = payload;

        } else if (cmd == 51) {
            msg.what = msgID_Weight;
            msgWeight payload = new msgWeight();
            payload.StabilityStatus = Integer.parseInt(str.substring(3, 4)) == 1;
            payload.Reserved = Integer.parseInt(str.substring(4, 5)) == 1;
            payload.TareStatus = Integer.parseInt(str.substring(5, 6)) == 1;
            payload.Weight = Integer.parseInt(str.substring(8, 12));
            if (str.charAt(7) == Hyphen) {
                payload.Weight *= -1;
            }
            payload.ChargeCurrentMode = str.charAt(13);
            payload.BatteryVoltage = Integer.parseInt(str.substring(14, 18));
            msg.obj = payload;

        } else if (cmd == 58) {
            msg.what = msgID_Tare;
            msgTare payload = new msgTare();
            payload.TareValue = Integer.parseInt(str.substring(3, 7));
            msg.obj = payload;

        } else if (cmd == 32) {
            msg.what = msgID_Calibration;
            msgCalibration payload = new msgCalibration();
            payload.msgString = "Calibration mode started: Make sure nothing is on the scale and all feet are "
					+ "touching a surface.";
            payload.phase = 1;
            payload.InternalCount = -1;
            payload.mVolts = -1;
            payload.Weight = -1;
            msg.obj = payload;

        } else if (cmd == 33) {
            msg.what = msgID_Calibration;
            msgCalibration payload = new msgCalibration();
            payload.msgString = "Calibration mode: Zeroing is completed.";
            payload.phase = 2;
            payload.InternalCount = Integer.parseInt(str.substring(3, 10));
            payload.mVolts = Float.parseFloat(str.substring(11, 17));
            payload.Weight = -1;
            msg.obj = payload;

        } else if (cmd == 35) {
            msg.what = msgID_Calibration;
            msgCalibration payload = new msgCalibration();
            payload.msgString = "Calibration mode: Calibrate using weight.";
            payload.phase = 3;
            payload.InternalCount = -1;
            payload.mVolts = -1;
            payload.Weight = -1;
            msg.obj = payload;

        } else if (cmd == 37) {
            msg.what = msgID_Calibration;
            msgCalibration payload = new msgCalibration();
            payload.msgString = "Calibration mode: Place the specified weight on the Mandometer.";
            payload.phase = 4;
            payload.InternalCount = -1;
            payload.mVolts = -1;
            payload.Weight = Integer.parseInt(str.substring(3, 7));
            msg.obj = payload;

        } else if (cmd == 36) {
            msg.what = msgID_Calibration;
            msgCalibration payload = new msgCalibration();
            payload.msgString = "Calibration mode completed.";
            payload.phase = 5;
            payload.InternalCount = Integer.parseInt(str.substring(3, 10));
            payload.mVolts = Float.parseFloat(str.substring(11, 17));
            payload.Weight = -1;
            msg.obj = payload;

        } else {
            msg.what = msgID_Error;
            msgError payload = new msgError();
            payload.msgString = str.substring(3);
            msg.obj = payload;
        }

        handler.sendMessage(msg);
    }

    /**
     * Starts the calibration sequence
     *
     * @throws IOException
     */
    public void Calibrate() throws IOException {
        byte[] buffer = prepareSend("30");
        outStream.write(buffer);
    }

    /**
     * Requests a single weight measurement from the Mandometer
     *
     * @throws IOException
     */
    public void getWeightSingle() throws IOException {
        byte[] buffer = prepareSend("50");
        outStream.write(buffer);
    }

    /**
     * Starts a continuous stream of weight measurements. Subsequent calls to this method have no effect
     * <p>
     * Call stopWeightStream() to stop the Mandometer from sending additional measurements
     *
     * @throws IOException
     */
    public void startWeightStream() throws IOException {
        byte[] buffer = prepareSend("57");
        outStream.write(buffer);
    }

    /**
     * Stops the continuous stream of weight measurements initiated by startWeightStream(). Subsequent calls to this
     * method have no effect
     *
     * @throws IOException
     */
    public void stopWeightStream() throws IOException {
        byte[] buffer = prepareSend("56");
        outStream.write(buffer);
    }

    /**
     * Sets current weight to zero
     *
     * @throws IOException
     */
    public void setZero() throws IOException {
        byte[] buffer = prepareSend("54");
        outStream.write(buffer);
    }

    /**
     * Tares the current weight
     *
     * @throws IOException
     */
    public void setTare() throws IOException {
        byte[] buffer = prepareSend("55");
        outStream.write(buffer);
    }

    /**
     * Requests the current tare value
     *
     * @throws IOException
     */
    public void getTare() throws IOException {
        byte[] buffer = prepareSend("58");
        outStream.write(buffer);
    }

    /**
     * Cancels the current tare value
     *
     * @throws IOException
     */
    public void cancelTare() throws IOException {
        byte[] buffer = prepareSend("59");
        outStream.write(buffer);
    }

    /**
     * Requests the Technical Info of the Mandometer
     *
     * @throws IOException
     */
    public void getTechnicalParametres() throws IOException {
        byte[] buffer = prepareSend("60");
        outStream.write(buffer);
    }

    /**
     * Resets the Mandometer. Note that this causes the Mandometer to disconnect. Always call Disconnect immediately
	 * after reseting to avoid exceptions
     *
     * @throws IOException
     */
    public void Reset() throws IOException {
        byte[] buffer = prepareSend("62");
        outStream.write(buffer);
    }

    /**
     * Change the bluetooth name of the mandometer. The new name should be 1-6 characters long.
     *
     * @param newName The new name of the device. Note that if newName="123456", then the bluetooth name will be set
     *                   to "MANDO-123456"
     * @throws IOException
     */
    public void ChangeBluetoothName(String newName) throws IOException {
        String cmd = "69," + newName.subSequence(0, 6);
        byte[] buffer = prepareSend(cmd);
        outStream.write(buffer);
    }

    private byte[] prepareSend(String cmd) throws UnsupportedEncodingException {
        String str = "[" + cmd + "]" + checkSum(cmd) + "\r\n";
        return str.getBytes(StandardCharsets.US_ASCII);
    }

    private String checkSum(String n) throws UnsupportedEncodingException {
        byte[] b = n.getBytes(StandardCharsets.US_ASCII);

        int s = 0;
        for (int i = 0; i < b.length; i++) {
            s += b[i];
        }

        s += 91 + 93;
        s = s % 256;
        s = 256 - s;

        String str = Integer.toString(s);

		if (str.length() == 1) {str = "00" + str;}
		if (str.length() == 2) {str = "0" + str;}

        return str;
    }

    private class m5listener extends Thread {
        private boolean stop = false;

        private Context context;

        protected void uNeed2Stop() {
            stop = true;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            byte currByte = 0;
            byte prevByte = 0;
            byte[] buffer = new byte[m5listener_buffer_size];
            int p = 0;

            while (true) {
                try {
                    currByte = (byte) inStream.read();
                } catch (IOException e) {
                    if (stop) {
                        break;
                    } else {
                        e.printStackTrace();
                    }
                }

                buffer[p] = currByte;
                p++;

                if ((currByte == CR && prevByte == LF) || (currByte == LF && prevByte == CR)) {
                    assert p > 7;
                    Log.v("lemug", new String(buffer));
                    try {
                        processReceived(new String(Arrays.copyOfRange(buffer, 1, p - 6)));
                    } catch (Exception e) {
                        ContextCompat.getMainExecutor(context).execute(() -> {
                            Toast.makeText(context, "Chck error. Please refer to the instruction manual.", Toast.LENGTH_SHORT).show();
                        });
                        System.exit(0);
                    }
                    p = 0;
                    currByte = 0;
                    prevByte = 0;
                } else {
                    prevByte = currByte;
                }

                assert p < m5listener_buffer_size;
            }
        }
    }

    /**
     * Message returned to handler during calibration mode
     */
    public class msgCalibration {
        public String msgString;
        public int phase;
        public int InternalCount;
        public float mVolts;
        public int Weight;
    }

    /**
     * Message returned to handler when Weight Data are requested, either explicitly or during automatic transmission mode
     */
    public class msgWeight {
        public boolean StabilityStatus;
        public boolean Reserved;
        public boolean TareStatus;
        public int Weight;
        public char ChargeCurrentMode;
        public int BatteryVoltage;
    }

    /**
     * Message returned to handler when Tare Value is requested
     */
    public class msgTare {
        public int TareValue;
    }

    /**
     * Message returned to handler when Technical Info are requested
     */
    public class msgTechnicalInfo {
        public float SoftwareVersion;
        public float BootcodeVersion;
    }

    /**
     * Message returned to handler when error has occurred
     */
    public class msgError {
        public String msgString;
    }

}

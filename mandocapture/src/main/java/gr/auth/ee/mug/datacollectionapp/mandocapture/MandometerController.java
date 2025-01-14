package gr.auth.ee.mug.datacollectionapp.mandocapture;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class MandometerController {

    private final SharedPreferences pref;

    private BluetoothDevice Mandometer;
    private MandometerAPI MandoAPI;
    private boolean eating;
    private int curWeight;
    private int[] curMeal;
    private int curMealLen;
    private final Object signal = new Object();

    private boolean inMeal = false;
    private MealDataModel theMeal;
    private String theMealUniqueID;
    private mealManager theMealManager;

    private boolean stUniqueID;
    private boolean stHasChewingSensor;
    private boolean stHasMandometerSensor;
    private boolean stIsConfirmedSnack;
    private boolean stIsRegisteredMeal;
    private boolean stMealStartTime;
    private boolean stMealEndTime;
    private boolean stMealType;
    private boolean stSatietyBeforeMeal;
    private boolean stSatietyAfterMeal;
    private boolean stLikedFood;
    private boolean stFoodComponents;
    private boolean stDrinkComponents;
    private boolean stRecDone;
    private Object theMealLock;
    private Context context;

    public int[] getWeights() {
        final int[] x = new int[curMealLen];
        for (int i = 0; i < x.length; i++) {
            x[i] = curMeal[i];
        }

        return x;
    }

    /**
     *
     */
    private final Handler MandoHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MandometerAPI.msgID_Calibration) {
                Log.v("lemug", "MandoHandler: msgID_Calibration");
            } else if (msg.what == MandometerAPI.msgID_Error) {
                Log.v("lemug", "MandoHandler: msgID_Error");
            } else if (msg.what == MandometerAPI.msgID_Tare) {
                Log.v("lemug", "MandoHandler: msgID_Tare");
            } else if (msg.what == MandometerAPI.msgID_TechnicalInfo) {
                Log.v("lemug", "MandoHandler: msgID_TechnicalInfo");
            } else if (msg.what == MandometerAPI.msgID_Weight) {
                Log.v("lemug", "MandoHandler: msgID_Weight");
                MandometerAPI.msgWeight tempmsg = (MandometerAPI.msgWeight) msg.obj;
                if (eating) {
                    curMeal[curMealLen] = tempmsg.Weight;
                    curMealLen++;
                } else {
                    curWeight = tempmsg.Weight;
                    signal.notify();
                }
            }
        }
    };

    /**
     * @param preferences
     */
    public MandometerController(SharedPreferences preferences, Context context) {
        pref = preferences;
        this.context = context;
    }

    /**
     * @param dev
     */
    @SuppressLint("MissingPermission")
    public void setMandometerDev(BluetoothDevice dev) throws SecurityException {
        Mandometer = dev;
        MandoAPI = new MandometerAPI(Mandometer, MandoHandler, context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("MMuuid", dev.getUuids()[0].toString());
        editor.commit();
    }

    /**
     * @return
     */
    public int connectToMandometer() {
        eating = false;
        int r = 0;
        try {
            MandoAPI.Connect(true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            r = 1;
            return r;
        }
        try {
            MandoAPI.stopWeightStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            r = 2;
        }
        Log.v("lemug", "Mandometer5Controller: connected");
        return r;
    }

    /**
     * @return
     */
    public int disconnectFromMandometer() {
        int r = 0;
        try {
            MandoAPI.Disconnect();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            r = 1;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            r = 2;
        }
        Log.v("lemug", "Mandometer5Controller: disconnected");
        return r;
    }

    /**
     *
     */
    public void createMeal() {
        if (inMeal) {
            Log.v("lemug", "Currently inMeal. Cannot execute createMeal()");
            return;
        }

        theMeal = new MealDataModel();
        theMealUniqueID = "";
        inMeal = true;
        setAllParamsStatus(false);
        stRecDone = false;
        theMealManager = new mealManager();
        theMealManager.start();
    }

    private void setAllParamsStatus(boolean b) {
        stUniqueID = b;
        stHasChewingSensor = b;
        stHasMandometerSensor = b;
        stIsConfirmedSnack = b;
        stIsRegisteredMeal = b;
        stMealStartTime = b;
        stMealEndTime = b;
        stMealType = b;
        stSatietyBeforeMeal = b;
        stSatietyAfterMeal = b;
        stLikedFood = b;
        stFoodComponents = b;
        stDrinkComponents = b;
    }

    /**
     * Start recording a meal (the meal weights)
     *
     * @return
     */
    public int startRecMeal() {
		if (eating) {return 1;}

        curMeal = new int[2 * 60 * 60]; //  Assuming a maximum meal duration of 2 hours
        curMealLen = 0;
        eating = true;
        try {
            MandoAPI.startWeightStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Stop recording the meal
     *
     * @return
     */
    public int stopRecMandometer() {

        if (stRecDone) {
            Log.v("lemug", "stRecDone is already true");
            return 1;
        }

        try {
            MandoAPI.stopWeightStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stRecDone = true;
        synchronized (theMealLock) {theMealLock.notify();}


        return 0;
    }

    /**
     * @return
     */
    private double[] getRecMandometer() {
        double[] buffer = new double[curMealLen];
        for (int i = 0; i < curMealLen; i++) {
            buffer[i] = curMeal[i];
        }
        return buffer;
    }

    public void setMealUniqueID(String s) {
        theMealUniqueID = s;
        if (!stUniqueID) {
            stUniqueID = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // Meal parameters' setters

    public void setHasChewingSensor(Boolean b) {
        theMeal.setHasChewingSensor(b);
        if (!stHasChewingSensor) {
            stHasChewingSensor = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setHasMandometerSensor(Boolean b) {
        theMeal.setHasMandometerSensor(b);
        if (!stHasMandometerSensor) {
            stHasMandometerSensor = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setIsConfirmedSnack(Boolean b) {
        theMeal.setIsConfirmedSnack(b);
        if (!stIsConfirmedSnack) {
            stIsConfirmedSnack = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setIsRegisteredMeal(Boolean b) {
        theMeal.setIsRegisteredMeal(b);
        if (!stIsRegisteredMeal) {
            stIsRegisteredMeal = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setMealStartTime(String s) {
        theMeal.setMealStartTime(s);
        if (!stMealStartTime) {
            stMealStartTime = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setMealEndTime(String s) {
        theMeal.setMealEndTime(s);
        if (!stMealEndTime) {
            stMealEndTime = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setMealType(String s) {
        theMeal.setMealType(s);
        if (!stMealType) {
            stMealType = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setSatietyBeforeMeal(Integer i) {
        theMeal.setSatietyBeforeMeal(i);
        if (!stSatietyBeforeMeal) {
            stSatietyBeforeMeal = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setSatietyAfterMeal(Integer i) {
        theMeal.setSatietyAfterMeal(i);
        if (!stSatietyAfterMeal) {
            stSatietyAfterMeal = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setLikedFood(Boolean b) {
        theMeal.setLikedFood(b);
        if (!stLikedFood) {
            stLikedFood = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setFoodComponents(MealDataModel.FoodComponents a) {
        theMeal.setFoodComponents(a);
        if (!stFoodComponents) {
            stFoodComponents = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setDrinkComponents(MealDataModel.DrinkComponents a) {
        theMeal.setDrinkComponents(a);
        if (!stDrinkComponents) {
            stDrinkComponents = true;
            synchronized (theMealLock) {theMealLock.notify();}
        }
    }

    public void setSkip() {
        setAllParamsStatus(true);
        synchronized (theMealLock) {theMealLock.notify();}
    }

    public void setTare() {
        try {
            MandoAPI.setTare();
        } catch (IOException e) {
            Log.e("MandoController", "IO exception at setTare");
        }
    }

    /**
     *
     */
    private class mealManager extends Thread {

        boolean locked;

        public mealManager() {
            theMealLock = new Object();
            locked = true;
        }

        @Override
        public void run() {

            synchronized (theMealLock) {

                while (locked) {
                    try {
                        theMealLock.wait();
                    } catch (InterruptedException e) {
                        // Nothing should happen here
                    }

                    locked = !(stUniqueID
                            & stHasChewingSensor
                            & stHasMandometerSensor
                            & stIsConfirmedSnack
                            & stIsRegisteredMeal
                            & stMealStartTime
                            & stMealEndTime
                            & stMealType
                            & stSatietyBeforeMeal
                            & stSatietyAfterMeal
                            & stLikedFood
                            & stFoodComponents
                            & stDrinkComponents
                            & stRecDone);
                }
            }


            double[] rawdata = getRecMandometer();

            // Clean up
            theMeal = null;
            theMealLock = null;
            theMealUniqueID = "";
            setAllParamsStatus(false);
            stRecDone = false;
        }
    }
}

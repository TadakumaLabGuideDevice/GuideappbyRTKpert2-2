/*加速度・地磁気センサによる割り込み関係*/

package com.example.guideappbyrtk;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class sensorChangeEvent implements SensorEventListener{
    private float[] accV = new float[3];
    private float[] magV = new float[3];
    private float[] R1 = new float[16];
    private float[] R2 = new float[16];
    private float[] I = new float[16];
    private float[] v = new float[3];
    private float[] gravity = new float[3];
    float Deg;
    double accX, accY, accZ;
    double magX, magY, magZ;
    static String magAngle;

    public void onSensorChanged(SensorEvent e) {
        // Isolate the force of gravity with the low-pass filter.
        float alpha = 0.8F;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * e.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * e.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * e.values[2];

        switch (e.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:  //加速度センサ
                accV = e.values.clone();
                //accX = e.values[0] - gravity[0];
                //accY = e.values[1] - gravity[1];
                //accZ = e.values[2] - gravity[2];
                accX = e.values[0];
                accY = e.values[1];
                accZ = e.values[2];
                break;

                case Sensor.TYPE_MAGNETIC_FIELD:  //地磁気センサ
                magV = e.values.clone();
                magX = e.values[0];
                magY = e.values[1];
                magZ = e.values[2];
                break;
        }

        if(magV != null && accV != null) {  //センサから利用者の方向を計測
            SensorManager.getRotationMatrix(R1, I, accV, magV);
            SensorManager.remapCoordinateSystem(R1, SensorManager.AXIS_X, SensorManager.AXIS_Y, R2);
            SensorManager.getOrientation(R2, v);
            int angle = (int)Math.floor(Math.toDegrees(v[0]));
            v[0] = angle;
            Deg = v[0];

            magAngle = ""+ Deg;
            MapsActivity.Mag2.setText(magAngle);
        }
    }
    public void onAccuracyChanged(Sensor s, int accuracy){}
}


package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class CalibrationJsonTranslater {

        public static final String GX_TAG = "gx";
        public static final String GY_TAG = "gy";
        public static final String GZ_TAG = "gz";

        public static final String MX_TAG = "mx";
        public static final String MY_TAG = "my";
        public static final String MZ_TAG = "mz";


        public static String toText(List<CalibrationReading> calibrationReadings)
                throws JSONException {
            return toJson(calibrationReadings).toString(SexyTopo.JSON_INDENT);
        }


        public static List<CalibrationReading> toCalibrationReadings(String string) throws JSONException {
            List<CalibrationReading> calibrationReadings = new ArrayList<>();
            JSONArray array = new JSONArray(string);
            List<JSONObject> calibrationData = IoUtils.toList(array);
            for (JSONObject json : calibrationData) {
                calibrationReadings.add(toCalibrationReading(json));
            }
            return calibrationReadings;
        }


        public static JSONArray toJson(List<CalibrationReading> calibrationReadings)
                throws JSONException {

            JSONArray json = new JSONArray();
            for (CalibrationReading calibrationReading : calibrationReadings) {
                json.put(toJson(calibrationReading));
            }

            return json;
        }



        public static JSONObject toJson(CalibrationReading calibrationReading) throws JSONException {
            JSONObject json = new JSONObject();
            json.put(GX_TAG, calibrationReading.getGx());
            json.put(GY_TAG, calibrationReading.getGy());
            json.put(GZ_TAG, calibrationReading.getGz());
            json.put(MX_TAG, calibrationReading.getMx());
            json.put(MY_TAG, calibrationReading.getMy());
            json.put(MZ_TAG, calibrationReading.getMz());
            return json;
        }

        public static CalibrationReading toCalibrationReading(JSONObject json) throws JSONException {
            int gx = json.getInt(GX_TAG);
            int gy = json.getInt(GY_TAG);
            int gz = json.getInt(GZ_TAG);
            int mx = json.getInt(MX_TAG);
            int my = json.getInt(MY_TAG);
            int mz = json.getInt(MZ_TAG);
            CalibrationReading calibrationReading = new CalibrationReading();
            calibrationReading.updateAccelerationValues(gx, gy, gz);
            calibrationReading.updateMagneticValues(mx, my, mz);
            return calibrationReading;
        }


    }

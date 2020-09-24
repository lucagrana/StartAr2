package com.example.startar2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MyModel {

    private static MyModel instance = null;
    private static ArrayList<Double> latitudini = null;
    private static ArrayList<Double> longitudini = null;
    private static ArrayList<Double> qrLon = null;
    private static ArrayList<Double> qrLat = null;
    private static ArrayList<Integer> qrId = null;
    private static ArrayList<Integer> qrOrientamento = null;

    public static synchronized MyModel getInstance() {
        if (instance == null) {
            instance = new MyModel();
        }
        return instance;

    }

    public static void coordinate(JSONObject punti) throws JSONException {
        JSONArray points = punti.getJSONArray("punti");
        latitudini = new ArrayList<Double>(points.length());
        longitudini = new ArrayList<Double>(points.length());
        for (int i = 0; i < points.length(); ++i) {
            JSONObject puntis = points.getJSONObject(i);
            Double lat = puntis.getDouble("latitudine");
            Double lon = puntis.getDouble("longitudine");
            latitudini.add(lat);
            longitudini.add(lon);
        }
    }

    public static void qrcode(JSONObject qrcodes) throws JSONException {
        JSONArray points = qrcodes.getJSONArray("qrcodes");
        qrLon = new ArrayList<Double>(qrcodes.length());
        qrLat = new ArrayList<Double>(qrcodes.length());
        qrId = new ArrayList<Integer>(qrcodes.length());
        qrOrientamento = new ArrayList<Integer>(qrcodes.length());
        for (int i = 0; i < qrcodes.length(); i++){
            JSONObject qr = points.getJSONObject(i);
            Double lat = qr.getDouble("latitudine");
            Double lon = qr.getDouble("longitudine");
            int id = qr.getInt("id");
            int orientamento = qr.getInt("orientamento");
            qrLon.add(lon);
            qrLat.add(lat);
            qrId.add(id);
            qrOrientamento.add(orientamento);
        }
    }

    public static void setInstance(MyModel instance) {
        MyModel.instance = instance;
    }

    public static ArrayList<Double> getLatitudini() {
        return latitudini;
    }

    public static void setLatitudini(ArrayList<Double> latitudini) {
        MyModel.latitudini = latitudini;
    }

    public static ArrayList<Double> getLongitudini() {
        return longitudini;
    }

    public static ArrayList<Double> getQrLon() {
        return qrLon;
    }

    public static void setQrLon(ArrayList<Double> qrLon) {
        MyModel.qrLon = qrLon;
    }

    public static ArrayList<Double> getQrLat() {
        return qrLat;
    }

    public static void setQrLat(ArrayList<Double> qrLat) {
        MyModel.qrLat = qrLat;
    }

    public static ArrayList<Integer> getQrId() {
        return qrId;
    }

    public static void setQrId(ArrayList<Integer> qrId) {
        MyModel.qrId = qrId;
    }

    public static ArrayList<Integer> getQrOrientamento() {
        return qrOrientamento;
    }

    public static void setQrOrientamento(ArrayList<Integer> qrOrientamento) {
        MyModel.qrOrientamento = qrOrientamento;
    }

    public static void setLongitudini(ArrayList<Double> longitudini) {
        MyModel.longitudini = longitudini;
    }
}

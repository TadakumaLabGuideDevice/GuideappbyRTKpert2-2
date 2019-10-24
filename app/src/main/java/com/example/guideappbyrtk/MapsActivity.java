package com.example.guideappbyrtk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;


//必要なさそうなパッケージ達
/*import java.nio.charset.CoderMalfunctionError;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import android.content.Context;
import android.content.DialogInterface;
import com.google.android.gms.location.LocationServices;
import android.net.Uri;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import android.media.MediaScannerConnection;
import android.text.format.Time;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;*/

//import android.location.LocationListener;
//iplementsされてた理由とは？　いらん可能性大
//Google Maps

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener, LocationListener,GpsActivity.OnLocationResultListener,MockLocationListener {

    //テキスト
    public static TextView bluetoothState;      //bluetoothの状態表示
    public static TextView gpsState;            //GPSの状態
    public TextView current_Lat;                //緯度
    public TextView current_Lng;                //経度
    public TextView statusTx;                   //誘導状況表示
    public static TextView Mag;                 //方位
    public static TextView Mag2;

    //ボタン
    private Button connectBt;
    private Button startBt;
    private Button stopBt;
    private Button voiceBt;

    //GpsActivityのインスタンス生成
    private GpsActivity gpsActivity;
    private LocationManager manager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    Location mockLocation;
    //private Location location;

    //Google Maps関連
    private GoogleMap mMap;
    public String travelMode = "walking";  //default  ここでルート検索の際歩行を優先したルートが表示される
    ArrayList<LatLng> markerPoints;
    public static MarkerOptions options;


    //音声入出力用
    private static final int REQUEST_CODE = 1000;
    private int lang;
    TextToSpeech txtToStop;
    //private TextView spTxtView;


    //加速度・地磁気センサ関連
    public SensorManager sensorManager;
    Sensor s1,s2;
    sensorChangeEvent sensorChangeEvent = new sensorChangeEvent();    //sensorChangeEvenインスタンスを生成



    //Bluetooth Adapter
    private BluetoothAdapter mAdapter;
    private BluetoothDevice mDevice;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  //なんでもいい臭いけど要調査
    private BluetoothSocket mSocket;
    OutputStream mmOutputStream = null;
    InputStream mmInStream = null;
    boolean connectFlg = false ;                  //盲導盤との接続状態
    String output = null;                         //盲導盤への指令　節電用に用いる


    //座標(緯度・経度)
    double currentLat = 37.900401;                 //現在の緯度　GPSによって取得
    double currentLng = 140.103678;                //現在の経度  初期値は大学 6-222室

    double targetLat;                              //目的地の緯度　タッチor音声入力で設定
    double targetLng;                              //目的地の経度

    double startLat;                               //ルート検索のスタート緯度　ルート検索時に用いる
    double startLng;                               //ルート検索のスタート経度

    double[] pathLat = new double[10000];          //ルート検索で取得した経路の緯度
    double[] pathLng = new double [10000];         //ルート検索で取得した経路の経度


    int path_val;                                  //for文用
    private float[] results = new float[3];        //GPSによる2点間の距離，角度
    private int target_deg;                        //角度差
    boolean targetFlg = false;                     //目標位置が決まったか判断
    boolean currentFlg = false;                    //現在位置が決まったか判断


    //タイマー関連
    private Timer mainTimer = new Timer();         //タイマー割り込み用
    private MainTimerTask mainTimerTask = null;    //スケジュールされるタスク
    private Handler timerHandler = new Handler();  //タイマー割り込み用のハンドラ
    double dt = 500;                              //タイマー割り込みの周期[ms]


    //保存用
    int val = 30000;                                 //デバッグ　緯度経度記録用
    double[]array1 = new double[val];
    double[]array2 = new double[val];
    //double time_count = 0;
    //String text;
    int measure_val = 0;
    int hori;


    //不確定要素　必要性に応じてやる　検索中に出てぐるぐるを出すやつ　
    /*public ProgressBar searchTimeBar = new ProgressBar(this);
    searchTimeBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    searchTimeBar.setMessage("ルート検索中...");
    searchTimeBar.hide();*/



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //描画設定
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //テキスト表示設定
        bluetoothState = findViewById(R.id.bluetoothState);
        gpsState = findViewById(R.id.gpsState);
        current_Lat = findViewById(R.id.current_lat);
        current_Lng = findViewById(R.id.current_lng);
        statusTx = findViewById(R.id.status);
        Mag = findViewById(R.id.mag);
        Mag2 = findViewById(R.id.mag2);


        //ボタン初期化
        connectBt = findViewById(R.id.connect_bt);
        connectBt.setOnClickListener(new ClickEvent());

        startBt = findViewById(R.id.start_bt);
        startBt.setOnClickListener(new ClickEvent());

        stopBt = findViewById(R.id.stop_bt);
        stopBt.setOnClickListener(new ClickEvent());

        voiceBt = findViewById(R.id.voice_bt);
        voiceBt.setOnClickListener(new ClickEvent());


        //GoogleMaps設定
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        markerPoints = new ArrayList<>();
        //manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //GPS設定----------------------------------------------------------------------------
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsActivity = new GpsActivity(this ,this);
        //gpsActivity.fusedLocationProviderClient.setMockMode(true);
        gpsActivity.setMockLocation();
        gpsActivity.startLocationUpdates();

        //boolean GPSFlg = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //gpsTx.setText(GPSstatus);


        //音声検索設定
        lang = 0;   //音声入力用　　言語選択 0:日本語、1:英語、2:オフライン、その他:General
        txtToStop = new TextToSpeech(this, this);  //音声出力用


        //Bluetooth設定
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothState.setText(R.string.search_device);
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        for(BluetoothDevice device :devices){
            //string型の固定値の比較.equals(string)
            //ペアリング用　盲導盤の回路に搭載されてるbluetoothモジュール参照
            String DEVICE_NAME = "SBDBT-001bdc057cd3";
            if(device.getName().equals(DEVICE_NAME)){

                bluetoothState.setText(device.getName());
                mDevice = device;
            }
        }
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }


    //アプリ立ち上げ時
    protected void onResume() {
        super.onResume();
        //GPS起動

        //mockLocation = new Location();
        //gpsActivity.fusedLocationProviderClient.setMockMode(true);


        //加速度センサ、地磁気センサ起動
        s1 = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        s2 = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorChangeEvent, s1, SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorChangeEvent, s2, SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onPause() {    //GPSが得られなくなった時の停止用
        super.onPause();

        if (gpsActivity != null) {
            gpsActivity.stopLocationUpdates();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        //自動で山形大工学部6-222付近まで移動
        //CameraPosition cameraPos = new CameraPosition.Builder().target(new LatLng(37.900401, 140.103678)).zoom(18.0F).bearing(0).build();
        mMap = googleMap;
        LatLng firstPosition = new LatLng(37.900401, 140.103678);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 18));

        //パーミッションの確認
        UiSettings uiSettings = mMap.getUiSettings();
        mMap.setMyLocationEnabled(true);                     //現在位置の丸を表示するためのもの
        mMap.setOnMapClickListener(new MapClick());          //マップタッチ時のリスナ呼び出し用
        uiSettings.setZoomControlsEnabled(true);             //ズームとかするためのボタン出すやつ
        uiSettings.setMyLocationButtonEnabled(true);         //現在位置に飛ぶためのボタン表示のためのやつ
    }


    //GPSによる割り込み　ここから
    //@Override
    public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
            return;
        }
        // 緯度・経度を取得
        currentLat = locationResult.getLastLocation().getLatitude();
        currentLng = locationResult.getLastLocation().getLongitude();
        String Lat = "currentLat:" + currentLat;
        String Lng = "currentLng" + currentLng;
        current_Lat.setText(Lat);
        current_Lng.setText(Lng);

    }

    //GPSによって位置情報が変化した際のイベント
    @Override
    public void onLocationChanged(Location location) {

    }


    //GPSのプロバイダ関連のイベント
    //@Override ここの対処必要　LocationListenerのせいで消せない　使わないと思うけど
    public void onProviderDisabled(String provider) {
        gpsState.setText(R.string.disabled_provider);
    }
    public void onProviderEnabled(String provider) {
        gpsState.setText(R.string.enabled_provider);
    }


    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch(status){
            case LocationProvider.AVAILABLE:
                gpsState.setText(R.string.available);
                break;
            case LocationProvider.OUT_OF_SERVICE:
                gpsState.setText(R.string.out_of_service);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                gpsState.setText(R.string.temp_unavailable);
                break;
        }
    }


    //GPSによる割り込み　ここまで


    //音声出力の初期設定(言語，話すスピード等)
    @Override
    public void onInit(int status) {

        if(status == TextToSpeech.SUCCESS) {
            // 音声合成の設定を行う

            float pitch = 1.0f; // 音の高低
            float rate = 1.0f; // 話すスピード
            Locale locale = Locale.JAPANESE; // 対象言語のロケール
            // ※ロケールの一覧表
            //   http://docs.oracle.com/javase/jp/1.5.0/api/java/util/Locale.html

            txtToStop.setPitch(pitch);
            txtToStop.setSpeechRate(rate);
            txtToStop.setLanguage(locale);
        }

    }

    @Override
    public void onMockLocationChanged(Location location) {

    }

    //音声出力の初期設定(言語，話すスピード等)　ここまで



    //マップタッチによる割り込み　ここから
    //タッチ1度目…目的地指定(デバッグ用)
    //タッチ2回目…現在地指定(デバッグ用)
    //タッチ3回目…目的地＆現在地リセット

    class MapClick implements GoogleMap.OnMapClickListener{
        public void onMapClick(LatLng point) {
            if(currentFlg){                              //3回タッチすると目的地再設定 　currentFlgがtrueならこの処理
                markerPoints.clear();
                mMap.clear();
                targetFlg = false;
                currentFlg = false;
                if(null != mainTimer) {
                    mainTimer.cancel();
                    mainTimer = null;
                    mainTimerTask = null;
                    hori = 0;
                }
            }
            else{                                               //
                markerPoints.add(point);
                options = new MarkerOptions();
                options.position(point);
                mMap.addMarker(options);
                if(!targetFlg){ //目的地が未設定の場合
                    targetLat = point.latitude;
                    targetLng = point.longitude;
                    targetFlg = true;
                }
                else { //目的地が設定されている場合
                    startLat = point.latitude;
                    startLng = point.longitude;
                    markerPoints.add(point);
                    currentFlg = true;
                    routeSearch();                          //ルート検索＆表示
                }
            }
        }
    }

    //ルート検索
    private void routeSearch() {
        //searchTimeBar.show();
        LatLng originPoint = new LatLng(startLat, startLng);    //誘導開始位置(現在地)
        LatLng destPoint = new LatLng(targetLat, targetLng);    //誘導終了位置(目的地)
        //googlemapのルート案内に入力
        String url = getDirectionsUrl(originPoint, destPoint);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    //ルート検索時に必要
    private String getDirectionsUrl(LatLng originPoint, LatLng destPoint) {
        String str_originPoint = "origin=" + originPoint.latitude + "," + originPoint.longitude;
        String str_destPoint = "destination=" + destPoint.latitude + "," + destPoint.longitude;
        String sensor = "sensor=false";
        //パラメータ
        String parameters = str_originPoint + "&" + str_destPoint + "&" + sensor + "&language=ja" + "&mode=" + travelMode +"&key=AIzaSyA5Ick4I1SS4hEPS2G3PJjFd2jvu8OC_h4" ;
        //JSON指定
        String output = "json";

        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();

        } catch (Exception e) {
            //Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /*public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }*/

    private class DownloadTask extends AsyncTask<String, Void, String> {
        //非同期で取得
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    //経路の座標取得部分
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                parseJsonpOfDirectionAPI parser = new parseJsonpOfDirectionAPI();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        //ルート検索で得た座標を使って経路表示
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            if (result.size() != 0) {

                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions();

                    List<HashMap<String, String>> path = result.get(i);

                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                        double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));

                        //経路作成の際の各マーカーの座標取得
                        pathLat[j] = lat;
                        pathLng[j] = lng;
                        LatLng position = new LatLng(lat, lng);
                        options.position(position);
                        mMap.addMarker(options);

                        points.add(position);
                        hori = j;
                    }

                    //誘導経路の描画
                    lineOptions.addAll(points);
                    lineOptions.width(8);
                    lineOptions.color(Color.BLUE);
                }

                mMap.addPolyline(lineOptions);
            } else {
                mMap.clear();
                Toast.makeText(MapsActivity.this, "ルート情報を取得できませんでした", Toast.LENGTH_LONG).show();
            }
            //searchDialog.hide();
        }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // 認識結果を ArrayList で取得
            ArrayList<String> candidates = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (candidates.size() > 0) {
                // 認識結果候補で一番有力なものを表示
                String speakResult = candidates.get(0);
                statusTx.setText(speakResult);
                toSpeech("もくてきちを" + speakResult + "にせっていしました");  //音声出力

                //座標を取得
                Geocoder gcoder = new Geocoder(this, Locale.getDefault());
                List<Address> lstAddr;
                try {
                    // 位置情報の取得
                    lstAddr = gcoder.getFromLocationName(candidates.get(0), 1);
                    if (lstAddr.size() > 0) {
                        // 音声入力で得た目的地の緯度、経度取得
                        Address addr = lstAddr.get(0);
                        targetLat = addr.getLatitude();
                        targetLng = addr.getLongitude();
                        Toast.makeText(this, "位置\n緯度:" + targetLat + "\n経度:" + targetLng, Toast.LENGTH_LONG).show();
                        //マーカー表示
                        LatLng voice_point = new LatLng(targetLat, targetLng);
                        markerPoints.add(voice_point);
                        options = new MarkerOptions();
                        options.position(voice_point);
                        mMap.addMarker(options);
                        targetFlg = true;
                    }
                    //この下で緯度経度が取得できなかった時の処理入れるべし

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    //音声出力の設定
    public void toSpeech(String speech){
        if(txtToStop.isSpeaking()){
            txtToStop.stop();
        }
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");
        txtToStop.speak(speech, TextToSpeech.QUEUE_FLUSH, null,"messageID");
    }

    private void shutDown() {
        if (null != txtToStop) {
            // to release the resource of TextToSpeech
            txtToStop.shutdown();
        }
    }
    //VOICEボタンイベントここまで------------------------------------------------------------------------


    //クリックしたときのイベント
    class ClickEvent implements View.OnClickListener {
        public void onClick(View v) {
            if (v.equals(connectBt)) connect();   //bluetooth接続ボタン
            else if (v.equals(startBt)) start();  //誘導開始用ボタン
            else if (v.equals(stopBt)) stop();    //誘導強制終了用ボタン
            else if (v.equals(voiceBt)) voice();  //音声入力用ボタン
        }


        private void connect() {
            if (!connectFlg) {
                try {
                    // 取得したデバイス名を使ってBluetoothでSocket接続
                    mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    mSocket.connect();
                    mmInStream = mSocket.getInputStream();
                    mmOutputStream = mSocket.getOutputStream();
                    bluetoothState.setText(R.string.connected);
                    mmOutputStream.write("START#".getBytes());
                    connectFlg = true;
                } catch (Exception e) {
                    bluetoothState.setText((CharSequence) e);
                    try {
                        mSocket.close();
                    } catch (Exception ee) {
                        connectFlg = false;
                    }
                }
            }
        }


        private void start() {
            mainTimer.cancel();
            mainTimer = null;
            mainTimerTask = null;
            path_val = 0;
            measure_val = 0;
            //計測＆誘導開始
            Toast.makeText(MapsActivity.this, "誘導開始", Toast.LENGTH_SHORT).show();
            mainTimer = new Timer();
            mainTimerTask = new MainTimerTask();
            mainTimer.schedule(mainTimerTask, 0, (int) dt);    //1000[ms]間隔
        }


        private void stop() {
            mainTimer = new Timer();
            mainTimer.cancel();
            mainTimer = null;
            mainTimerTask = null;

            if(path_val > hori){
                currentLat = targetLat;
                currentLng = targetLng;
            }else{
                currentLat = pathLat[path_val];
                currentLng = pathLng[path_val];
            }
        }


        private void voice() {
            if (!targetFlg) {
                // 音声認識が使えるか確認する
                try {
                    // 音声認識の　Intent インスタンス
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                    if (lang == 0)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toString());  // 日本語
                    else if (lang == 1)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());  // 英語
                    else if (lang == 2)
                        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);  // Off line mode
                    else
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "音声を入力");
                    // インテント発行
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    statusTx.setText(R.string.no_activity);
                }
            } else {
                Toast.makeText(MapsActivity.this, "既に目的地が設定されています", Toast.LENGTH_LONG).show();
            }
        }
    }


        //VOICEボタンイベントここから------------------------------------------------------------------------
        //音声入力の結果を受け取るために onActivityResult を設置


        //STARTボタンイベントここから------------------------------------------------------------------------
        //タイマー割り込みのイベント　スタートボタンを押した際に開始される
        public class MainTimerTask extends TimerTask {
            public void run() {
                final String[] txt = new String[1];
                timerHandler.post(new Runnable() {
                    public void run() {
                        float Deg = sensorChangeEvent.Deg;//results[0]…2点間の距離　　results[1]…2点間の角度
                        if (path_val > hori) {                                                                  //ルート検索によって作成した経路を通り終えた際(最後の目的地までの誘導)
                            Location.distanceBetween(currentLat, currentLng, targetLat, targetLng, results);
                            target_deg = (int) results[1] - (int) Deg;                                          //(Googlemap2点間の角度)　-　(地磁気センサ)

                            if (target_deg > 180) {
                                target_deg = target_deg - 360;
                            }
                            else if(target_deg < -180) {
                                target_deg = target_deg + 360;
                            }

                            txt[0] = "目的地まで" + results[0] + "[m]  角度：" + target_deg;
                            String nowDeg = "" + Deg;
                            Mag.setText(nowDeg);
                            statusTx.setText(txt[0]);

                            //現在位置と目標位置との距離が2[m]以下になったら誘導終了
                            if (results[0] < 2.0) {
                                Toast toast = Toast.makeText(getApplicationContext(), "FINISH!!", Toast.LENGTH_SHORT);
                                toast.show();

                                //盲導盤に停止の合図
                                if (connectFlg) {
                                    try {
                                        mmOutputStream.write("7".getBytes());  //123456以外の数字送ると止まる
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //計測終了
                                if (null != mainTimer) {
                                    mainTimer.cancel();
                                    mainTimer = null;
                                    mainTimerTask = null;
                                }
                            }
                            else {
                                if (connectFlg) outputToDevice(target_deg);
                            }
                        }

                        else {  //ルート検索で作成した経路を通過中の際
                            Location.distanceBetween(currentLat, currentLng, pathLat[path_val], pathLng[path_val], results);
                            target_deg = (int) results[1] - (int) Deg;                                                          //(Googlemap2点間の角度)　-　(加速度・地磁気センサ)

                            if (target_deg > 180) {
                                target_deg = target_deg - 360;
                            }

                            else if (target_deg < -180) {
                                target_deg = target_deg + 360;
                            }

                            txt[0] =    "目的地まで" + results[0] + "[m]  角度：" + target_deg;
                            String nowDeg = "" + Deg;
                            Mag.setText(nowDeg);
                            statusTx.setText(txt[0]);

                            //現在位置と目標マーカーとの距離が2[m]以下になったら目標を次のマーカーへ切り替える
                            if (results[0] < 2.0) {
                                path_val++;  //次のマーカーの更新
                            }

                            else {
                                if (connectFlg) outputToDevice(target_deg);
                            }
                        }

                        //保存用
                        array1[measure_val] = currentLat;
                        array2[measure_val] = currentLng;
                        measure_val++;

                        //歩いた軌跡を描画
                        ArrayList<LatLng> current_points;
                        PolylineOptions current_lineOptions = null;

                        for (int i = 0; i < 1; i++) {
                            current_points = new ArrayList<>();
                            current_lineOptions = new PolylineOptions();
                            for (int drow_val = 0; drow_val < measure_val; drow_val++) {
                                double drowLat = array1[drow_val];
                                double drowLng = array2[drow_val];
                                LatLng current_pos = new LatLng(drowLat, drowLng);
                                current_points.add(current_pos);
                            }
                            current_lineOptions.addAll(current_points);
                            current_lineOptions.width(10);
                            current_lineOptions.color(Color.RED);
                        }
                        mMap.addPolyline(current_lineOptions);
                    }

                });
            }
        }


        //盲導盤へ指令を送るイベント
        public void outputToDevice(int deg) {
            String direction = null;

            //角度によって呈示する方向を選択                   呈示する方向
            if (deg < -45) direction = "1";  //左
            else if (-45 <= deg && deg < -10) direction = "2";  //左上
            else if (-10 <= deg && deg <= 10) direction = "3";  //上　
            else if (10 < deg && deg <= 45) direction = "4";  //右上
            else if (45 < deg) direction = "5";  //右

            //節電用　呈示する方向が切り替わった時のみ盲導盤へ指令送信
            if (!direction.equals(output)) {
                output = direction;
                try {
                    mmOutputStream.write(output.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //ヒュベニの公式
   /*public static double deg2rad(double deg){
        return deg * Math.PI / 180.0;
    }

    public static double getDistance(double lat1, double lng1, double lat2, double lng2){
        double my = deg2rad((lat1 + lat2) / 2.0);
        double dy = deg2rad(lat1 - lat2);
        double dx = deg2rad(lng1 - lng2);

        double Rx = 6378137.000;
        double Ry = 6356752.314245;
        double E = Math.sqrt( (Rx * Rx - Ry * Ry) / (Rx * Rx) );

        double sin = Math.sin(my);
        double cos = Math.cos(my);
        double W = Math.sqrt(1.0 - E * E * sin * sin);
        double M = Rx * (1 - E * E) / (W * W * W);
        double N = Rx / W;

        double dym = dy * M;
        double dxncos = dx * N * cos;

        return Math.sqrt(dym * dym + dxncos * dxncos);
    }*/
    //STARTボタンイベントここまで------------------------------------------------------------------------
    //ボタンタッチによる割り込み　ここまで


    //アプリ終了時
    protected void onDestroy() {
        super.onDestroy();
        //GPS終了
        if (manager != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.removeUpdates(this);
        }
        //speech終了
        shutDown();
        //加速度センサ、地磁気センサ終了
        sensorManager.unregisterListener(sensorChangeEvent, s1);
        sensorManager.unregisterListener(sensorChangeEvent, s2);
        //bluetooth接続終了
        try{
            mSocket.close();
        }
        catch(Exception ignored){}
    }
}





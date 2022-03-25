/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package skunkworks.gnsstimeadjuster;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.theta360.pluginapplication.task.SetDateTimeZoneTask;
import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends PluginActivity implements LocationListener {
    private static final String TAG = "GNSS Time Adjuster";

    LocationManager locationManager;

    boolean mUpdateRequest = false ;
    boolean mSatelliteDataStatus = false;
    Date lastRcvSysTime = new Date(0); //init 1970/01/01 00:00:00(GMT)

    //フォーマット作成
    SimpleDateFormat dateTimeZone = new SimpleDateFormat("yyyy:MM:dd HH:mm:ssZ"); //ダメ、+0900のよに、タイムゾーンにコロン入らない

    //周期動作用
    private Handler updateHandler;
    private Timer updateTimer;

    //画面の部品
    Button button;
    TextView viewGnssDataStat;
    TextView viewLat;
    TextView viewLng;
    TextView viewGnssDateTimeZone;
    TextView viewSysDateTimeZone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init View
        viewGnssDataStat = (TextView) findViewById(R.id.gnssStatus);
        viewLat = (TextView) findViewById(R.id.lat);
        viewLng = (TextView) findViewById(R.id.lng);
        viewGnssDateTimeZone = (TextView) findViewById(R.id.gnssUtcTime);
        viewSysDateTimeZone = (TextView) findViewById(R.id.text4);

        //Init Botton
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUpdateRequest) {
                    mUpdateRequest = false;
                    button.setText("Start adjustment");
                } else {
                    mUpdateRequest = true;
                    button.setText("Stop adjustment");
                }
            }
        });

        // Init Timer
        statTimerTask();

        //位置情報取得に関するコード：パーミッションの有無チェック
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,},
                    1000);
        }
        else{
            locationStart();
        }


        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true);
        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                    //No processing
                }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent event) {
                /**
                 * You can control the LED of the camera.
                 * It is possible to change the way of lighting, the cycle of blinking, the color of light emission.
                 * Light emitting color can be changed only LED3.
                 */
                //notificationLedBlink(LedTarget.LED3, LedColor.BLUE, 1000);
            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent event) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //THETA X needs to open WebAPI camera before camera.takePicture
        notificationWebApiCameraOpen();
        // 500ms Wait必要
        Log.d(TAG, "Wait 500ms");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Deal with error.
            e.printStackTrace();
        }

        //Check "Date/time setting" -> "auto"
        int val = android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.AUTO_TIME, 0);
        Log.d(TAG, "Check Date/time settings=" + String.valueOf(val));
        if (val==0) {
            //ダイアログでOK操作したらfinish()する。onPause()呼ばれる。
            DateTimeNotAutoDaialog dialog = new DateTimeNotAutoDaialog();
            dialog.show( getSupportFragmentManager(), "date_time_notset_dialog" );
        }

        if (isApConnected()) {

        }

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        // Do end processing
        //close();

        locationStop();

        //THETA X needs to close WebAPI camera before finishing plugin
        notificationWebApiCameraClose();
        super.onPause();
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged()");

        // 緯度の表示
        double lat = +location.getLatitude();
        String strLat = "Latitude:" + String.format("%.6f", lat);
        Log.d(TAG, strLat);

        // 経度の表示
        double lng = location.getLongitude();
        String strLng = "Longtude:" + String.format("%.6f", lng);
        Log.d(TAG, strLng);

        // 時間
        long utcTime = location.getTime();
        String strUtcTimeCount = "UTC Time:" + String.valueOf(utcTime);
        Date gnssDateTimeZone = new Date(utcTime);
        String strGnssDateTimeZone = dateTimeZone.format(gnssDateTimeZone);
        Log.d(TAG, strGnssDateTimeZone);

        //System Time
        Date sysDateTimeZone = new Date();
        lastRcvSysTime = sysDateTimeZone;
        String strSysDateTimeZone = dateTimeZone.format(lastRcvSysTime);
        Log.d(TAG, "lastRcvSysTime =" + strSysDateTimeZone);

        //画面表示
        mSatelliteDataStatus = true;
        viewGnssDataStat.setText("Receiving");
        viewGnssDataStat.setTextColor(Color.GREEN);
        viewLat.setText(strLat);
        viewLng.setText(strLng);
        viewGnssDateTimeZone.setText(strUtcTimeCount);


        if (mUpdateRequest) {
            //THETA用のフォーマットに成形してから日時設定を更新
            // format:YYYY:MM:DD hh:mm:ss+(-)hh:mm
            // hh is in 24-hour time, +(-)hh:mm is the time zone.
            String thetaDateTimeZone = strGnssDateTimeZone.replaceFirst("([0-9][0-9])$", ":$1");
            new SetDateTimeZoneTask(thetaDateTimeZone).execute();

            //更新完了をダイアログ表示でユーザーに通知
            AdjustCompletedDaialog dialog = new AdjustCompletedDaialog();
            dialog.show( getSupportFragmentManager(), "adj_comp_dialog" );

            //ボタンの状態を戻す
            mUpdateRequest = false;
            button.setText("Start adjustment");

            //時刻表示の周期動作を停止
            stopTimerTask();

            //WebAPI経由でおこなった時刻設定が反映されるまで待つ
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Deal with error.
                e.printStackTrace();
            }
            //時刻表示の周期動作を再開
            statTimerTask();

        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "onStatusChanged()");

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled()");

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "onProviderDisabled()");

    }


    //位置情報取得開始処理
    private void locationStart(){
        Log.d(TAG,"locationStart()");

        // LocationManager インスタンス生成
        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "location manager Enabled");

        } else {
            Log.d(TAG, "not gpsEnable, startActivity");

            //ダイアログでOK操作したらfinish()する。onPause()呼ばれる。
            PositionInfoAddOffDaialog dialog = new PositionInfoAddOffDaialog();
            dialog.show( getSupportFragmentManager(), "position_info_add_off_dialog" );
        }

        //パーミッションのチェックをしないとrequestLocationUpdates()がエラーになる
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);

            Log.d(TAG, "checkSelfPermission false");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                //1000, 50, this);
                200, 0, this);

    }
    //位置情報取得停止処理
    private void locationStop(){
        if (locationManager != null ) {
            Log.d(TAG, "locationManager.removeUpdates()");
            locationManager.removeUpdates(this);
        }
    }


    private void updateSysDateTimeZone() {
        //現在時刻取得
        Date curSysTime = new Date();
        String curSysDateTimeZone = dateTimeZone.format(curSysTime);
        //文字列置換して表示更新
        curSysDateTimeZone = curSysDateTimeZone.replaceFirst("([0-9][0-9])$", ":$1");
        viewSysDateTimeZone.setText(curSysDateTimeZone);

        //受信状態表示 更新
        long timeDiffMs = curSysTime.getTime() - lastRcvSysTime.getTime();
        if ( timeDiffMs >= 1000 ) {
            mSatelliteDataStatus = false;
            viewGnssDataStat.setText("Lost");
            viewGnssDataStat.setTextColor(Color.YELLOW);
        }
    }

    private void statTimerTask() {
        updateTimer = new Timer();
        updateHandler = new Handler();

        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateSysDateTimeZone();
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopTimerTask() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        updateTimer = null;
    }

    //パーミッションのチェックの結果を受け取るコードが必要であったため記載.THETA Xでは動作しない。
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[]permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"checkSelfPermission true");

                locationStart();

            } else {
                // それでも拒否された時の対応
                Toast toast = Toast.makeText(this,
                        "can not do anything.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}

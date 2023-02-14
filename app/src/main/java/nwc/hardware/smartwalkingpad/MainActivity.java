package nwc.hardware.smartwalkingpad;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import nwc.hardware.Interfaces.OnGattListener;
import nwc.hardware.bletool.BluetoothGeneralTool;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private ImageButton upBTN;
    private ImageButton downBTN;

    private TextView deviceNameTXT;
    private TextView speedTXT;
    private TextView angleTXT;

    private BluetoothGeneralTool bluetoothGeneralTool;

    private int speed = 0;
    private int angle = 0;
    private int status = 0;

    private final byte[] UP_DATA = {'S','U','E',0x0d};
    private final byte[] DOWN_DATA = {'S','D','E',0x0d};
    private final byte[] STOP_DATA = {'S','T','E',0x0d};

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        upBTN = findViewById(R.id.up_BTN);
        upBTN.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                bluetoothGeneralTool.Write(UP_DATA);
                return false;
            }
        });
        upBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothGeneralTool.Write(STOP_DATA);
            }
        });

        downBTN = findViewById(R.id.down_BTN);
        downBTN.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                bluetoothGeneralTool.Write(DOWN_DATA);
                return false;
            }
        });
        downBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothGeneralTool.Write(STOP_DATA);
            }
        });

        deviceNameTXT = findViewById(R.id.deviceInfo_TXT);
        speedTXT = findViewById(R.id.speed_TXT);
        angleTXT = findViewById(R.id.angle_TXT);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        bluetoothGeneralTool = BluetoothGeneralTool.getInstance(new OnGattListener() {
            @Override
            public void connectionStateConnecting(BluetoothGatt bluetoothGatt) {

            }

            @Override
            public void connectionStateConnected(BluetoothGatt bluetoothGatt) {

            }

            @Override
            public void connectionStateDisconnected(BluetoothGatt bluetoothGatt) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        deviceNameTXT.setText("연결이 끊겼습니다.");
                        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                        dlg.setTitle("연결"); //제목
                        dlg.setMessage("디바이스와 연결이 끊겼습니다. 재검색 하시겠습니까?"); // 메시지
                        //버튼 클릭시 동작
                        dlg.setPositiveButton("예", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }
                        );
                        dlg.setNegativeButton("취소",new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                //토스트 메시지
                                Toast.makeText(getApplicationContext(),"검색을 취소하였습니다.",Toast.LENGTH_SHORT).show();
                            }
                        });
                        dlg.setCancelable(false);
                        dlg.show();
                    }
                });
            }

            @Override
            public void discoveredServices() {

            }

            @Override
            public void characteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
                Log.d(TAG,"Data Send!");
            }

            @Override
            public void characteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {

            }

            @Override
            public void characteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                byte[] value = bluetoothGattCharacteristic.getValue();
                StringBuilder data = new StringBuilder();
                for(byte b : value){
                    if(b == 0x0d){ continue; }
                    Log.d(TAG,"DATA -> " + (char)b);
                    data.append((char)b);
                }
                String data_String = data.toString().replaceAll(" ", "");
                String[] datas = data_String.split(",");
                int temp = Integer.parseInt(datas[0].split(":")[1]) / 10;
                Log.d(TAG, "ANGLE -> " + (8 - ( (temp * 10) / 60 )));
                angle = 8 - ( (temp * 10) / 60 );

                Log.d(TAG, "SPEED -> " + datas[2].split(":")[1]);
                speed = Integer.parseInt(datas[2].split(":")[1]);

                Log.d(TAG, "STATUS -> " + datas[3].split(":")[1]);
                status = Integer.parseInt(datas[3].split(":")[1]);

                repaint();
            }

            @Override
            public void readRssi(BluetoothGatt bluetoothGatt, int i, int i1) {

            }

            private void repaint(){
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        angleTXT.setText(angle + "˚");
                        speedTXT.setText(((double)speed)/10 + "\nkm/h");
                    }
                });
            }
        });

        deviceNameTXT.setText(bluetoothGeneralTool.getGatt().getDevice().getName());
    }
}
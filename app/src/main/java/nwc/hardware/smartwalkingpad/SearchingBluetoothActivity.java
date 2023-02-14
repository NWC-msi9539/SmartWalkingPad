package nwc.hardware.smartwalkingpad;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import nwc.hardware.Adapters.SearchingDeviceAdapter;
import nwc.hardware.Interfaces.OnGattListener;
import nwc.hardware.bletool.BluetoothGeneralTool;
import nwc.hardware.bletool.BluetoothPermissionTool;
import nwc.hardware.bletool.BluetoothSearchingTool;

public class SearchingBluetoothActivity extends AppCompatActivity {
    private static String TAG = "SearchingBluetoothActivity";

    private BluetoothSearchingTool bluetoothSearchingTool;
    private BluetoothPermissionTool bluetoothPermissionTool;
    private BluetoothGeneralTool bluetoothGeneralTool;

    private RecyclerView RCC;
    private TextView statusText;
    private TextView connectedDeviceNameText;
    private TextView connectedDeviceAddressText;
    private CardView connectedDeviceInfo;

    private SearchingDeviceAdapter adapter;
    private Timer timer = new Timer();
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;

    private TimerTask timerTask = new TimerTask() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(!bluetoothSearchingTool.isScanning()){
                        connectedDeviceInfo.setVisibility(View.GONE);
                        if (progressBar.getVisibility() == View.GONE) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        statusText.setText("기기를 검색 중 입니다.");
                        bluetoothSearchingTool.startScan(SearchingBluetoothActivity.this);
                    }
                }
            });
        }

        @Override
        public boolean cancel() {
            return super.cancel();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_bluetooth);

    }

    @Override
    protected void onResume() {
        super.onResume();

        bluetoothSetting();
    }

    private void bluetoothSetting(){
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusTXT);
        connectedDeviceNameText = findViewById(R.id.connectedDevice_name);
        connectedDeviceAddressText = findViewById(R.id.connectedDevice_address);
        connectedDeviceInfo = findViewById(R.id.connectedDevice);
        connectedDeviceInfo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                bluetoothGeneralTool.close();
                timer = new Timer();
                timerTask = new TimerTask() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(!bluetoothSearchingTool.isScanning()){
                                    connectedDeviceInfo.setVisibility(View.GONE);
                                    if (progressBar.getVisibility() == View.GONE) {
                                        progressBar.setVisibility(View.VISIBLE);
                                    }
                                    statusText.setText("기기를 검색 중 입니다.");
                                    bluetoothSearchingTool.startScan(SearchingBluetoothActivity.this);
                                }
                            }
                        });
                    }

                    @Override
                    public boolean cancel() {
                        return super.cancel();
                    }
                };
                timer.schedule(timerTask,0,100);
                connectedDeviceInfo.setVisibility(View.GONE);
                return false;
            }
        });
        swipeRefreshLayout = findViewById(R.id.SEARCHING_deviceLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.resetItem();
                timer.cancel();
                timer = new Timer();
                timerTask = new TimerTask() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(!bluetoothSearchingTool.isScanning()){
                                    if (progressBar.getVisibility() == View.GONE) {
                                        progressBar.setVisibility(View.VISIBLE);
                                    }
                                    statusText.setText("기기를 검색 중 입니다.");
                                    bluetoothSearchingTool.startScan(SearchingBluetoothActivity.this);
                                }
                            }
                        });
                    }

                    @Override
                    public boolean cancel() {
                        return super.cancel();
                    }
                };
                timer.schedule(timerTask,0,1000);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        List<BluetoothDevice> devices = new ArrayList<>();
        adapter = new SearchingDeviceAdapter(devices, this) {
            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(getApplicationContext()).inflate(R.layout.device_item, viewGroup, false);
                MyViewHolder myViewHolder = new MyViewHolder(linearLayout) {
                    @Override
                    public void setContents() {
                        textViews.put("name", itemView.findViewById(R.id.devicename_txt));
                        textViews.put("address",itemView.findViewById(R.id.devicename_address));

                        itemView.setOnClickListener(new View.OnClickListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onClick(View view) {
                                BluetoothDevice device = getItem(getAdapterPosition());
                                AlertDialog.Builder dlg = new AlertDialog.Builder(SearchingBluetoothActivity.this);
                                dlg.setTitle("연결"); //제목
                                dlg.setMessage(device.getName() + ", 연결 하시겠습니까?"); // 메시지
                                //버튼 클릭시 동작
                                dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Handler handler = new Handler(Looper.getMainLooper());
                                                handler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Log.d(TAG, "연결 중 입니다.");
                                                        statusText.setText("연결 중 입니다.");
                                                        progressBar.setVisibility(View.GONE);
                                                        timer.cancel();
                                                        if(bluetoothSearchingTool.isScanning()) {
                                                            bluetoothSearchingTool.stopScan(SearchingBluetoothActivity.this);
                                                        }
                                                    }
                                                });

                                                bluetoothGeneralTool.connect(device, SearchingBluetoothActivity.this);
                                            }

                                        }
                                );
                                dlg.setNegativeButton("취소",new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog, int which) {
                                        //토스트 메시지
                                        Toast.makeText(getApplicationContext(),"연결을 취소 하였습니다.",Toast.LENGTH_SHORT).show();
                                    }
                                });
                                dlg.setCancelable(false);
                                dlg.show();
                            }
                        });
                    }
                };
                return myViewHolder;
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
                BluetoothDevice device = getItem(position);
                holder.textViews.get("name").setText("" + device.getName());
                holder.textViews.get("address").setText("" + device.getAddress());
            }
        };

        RCC = findViewById(R.id.deviceRCC);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(RCC.getContext());
        RCC.setLayoutManager(layoutManager1);
        RCC.setAdapter(adapter);

        bluetoothPermissionTool = new BluetoothPermissionTool(this);
        bluetoothSearchingTool = new BluetoothSearchingTool() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(BluetoothDevice bluetoothDevice) {
                Log.d(TAG, "FOUND DEVICE ----> " + bluetoothDevice.getName());
                adapter.addDevice(bluetoothDevice);
            }
        };

        bluetoothGeneralTool = BluetoothGeneralTool.getInstance(new OnGattListener() {
            @Override
            public void connectionStateConnecting(BluetoothGatt bluetoothGatt) {
                statusText.setText("연결 중 입니다.");
                timer.cancel();
                if(bluetoothSearchingTool.isScanning()) {
                    bluetoothSearchingTool.stopScan(SearchingBluetoothActivity.this);
                }
            }

            @Override
            public void connectionStateConnected(BluetoothGatt bluetoothGatt) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "연결 되었습니다.");
                        statusText.setText("연결 되었습니다.");
                    }
                });
            }

            @Override
            public void connectionStateDisconnected(BluetoothGatt bluetoothGatt) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("연결이 끊겼습니다.");
                        timer = new Timer();
                        timerTask = new TimerTask() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void run() {
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!bluetoothSearchingTool.isScanning()){
                                            connectedDeviceInfo.setVisibility(View.GONE);
                                            if (progressBar.getVisibility() == View.GONE) {
                                                progressBar.setVisibility(View.VISIBLE);
                                            }
                                            statusText.setText("기기를 검색 중 입니다.");
                                            bluetoothSearchingTool.startScan(SearchingBluetoothActivity.this);
                                        }
                                    }
                                });
                            }

                            @Override
                            public boolean cancel() {
                                return super.cancel();
                            }
                        };
                        timer.schedule(timerTask,0,100);
                        connectedDeviceInfo.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void discoveredServices() {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {
                        bluetoothGeneralTool.setMAIN(1, -1);
                        adapter.resetItem();
                        BluetoothDevice device = bluetoothGeneralTool.getGatt().getDevice();
                        connectedDeviceInfo.setVisibility(View.VISIBLE);
                        connectedDeviceNameText.setText(device.getName());
                        connectedDeviceAddressText.setText("(" + device.getAddress() + ")");

                        Intent intent = new Intent(SearchingBluetoothActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
                Log.d(TAG,"연결 됌");
            }

            @Override
            public void characteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
                Log.d(TAG,"(MAINACTIVITY)Write Success !! --> [" + bluetoothGattCharacteristic.getUuid() + "] " + new String(bluetoothGattCharacteristic.getValue()));
            }

            @Override
            public void characteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {

            }

            @Override
            public void characteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {

            }

            @Override
            public void readRssi(BluetoothGatt bluetoothGatt, int rssi, int status) {

            }
        });

        if(bluetoothGeneralTool.getGatt() == null){
            if (bluetoothPermissionTool.checkPermission()) {
                Log.d(TAG, "Permission ON");
                bluetoothSearchingTool.startScan(this);
                timer.schedule(timerTask, 0, 100);
            } else {
                Log.d(TAG, "Permission OUT");
                Toast.makeText(getApplicationContext(), "블루투스 권한을 허용해주세요.", Toast.LENGTH_SHORT).show();
            }
        }else{
            statusText.setText("연결 되었습니다.");
            progressBar.setVisibility(View.GONE);
            BluetoothDevice device = bluetoothGeneralTool.getGatt().getDevice();
            connectedDeviceInfo.setVisibility(View.VISIBLE);
            connectedDeviceNameText.setText(device.getName());
            connectedDeviceAddressText.setText("(" + device.getAddress() + ")");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressBar.setVisibility(View.GONE);
        timer.cancel();
        if(bluetoothSearchingTool.isScanning()) {
            bluetoothSearchingTool.stopScan(SearchingBluetoothActivity.this);
        }
    }
}
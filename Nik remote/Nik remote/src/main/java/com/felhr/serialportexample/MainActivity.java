package com.felhr.serialportexample;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.Objects;
import android.graphics.Color;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import pl.pawelkleczkowski.customgauge.CustomGauge;
import java.text.DecimalFormat;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private TextView display1;
    private MyHandler mHandler;
    private LineChart chart;
    private CustomGauge gauge1;

    DecimalFormat precision = new DecimalFormat("0.00");

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    int angle1;
    int speed1;
    int angle2;
    int speed2;

    int angle1buf;
    int speed1buf;
    int angle2buf;
    int speed2buf;

    boolean active;
    boolean activebuf;

    int mode1;
    int mode;
    int modebuf;
    int mode1buf;

    String buf;

    float voltage = 0.01f;
    float current = 0.01f;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mHandler = new MyHandler(this);

        display = findViewById(R.id.textView1);
        display.setMovementMethod(new ScrollingMovementMethod());

        display1 = findViewById(R.id.textViewGauge);

        gauge1 = findViewById(R.id.gauge1);

        chart = findViewById(R.id.chart1);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.rgb(58,58,58));
        LineData data = new LineData();
        data.setValueTextColor(Color.rgb(101,255,168));
        chart.setData(data);
        chart.getLegend().setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.rgb(101,255,168));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);

        XAxis xl = chart.getXAxis();
        xl.setEnabled(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        Description description = chart.getDescription();
        description.setEnabled(false);

        for(int i=0;i<100;i++){
            addEntry();
        }


        TextView display1 = findViewById(R.id.switch1_label);

        Handler handler = new Handler();
        int delay = 100; //milliseconds

        handler.postDelayed(new Runnable(){
            public void run(){
                byte[] initiator = {0x55,0x55};
                byte[] angle1_ = {(byte)((int)(angle1*0.7))};
                byte[] angle2_ = {(byte)((int)(angle2*0.7))};
                byte[] speed1_ = {(byte)speed1};
                byte[] speed2_ = {(byte)speed2};
                byte[] checksum = {0};
                byte[] active_ = {(byte)(active?1:0)};
                byte[] mode_ = {(byte)mode};
                byte[] mode1_ = {(byte)mode1};

                checksum[0] = (byte) ~(angle1_[0] + speed1_[0] + angle2_[0] + speed2_[0] + active_[0] + mode_[0] + mode1_[0]);

                if (usbService != null) {
                    usbService.write(initiator);
                    usbService.write(angle1_);
                    usbService.write(speed1_);
                    usbService.write(angle2_);
                    usbService.write(speed2_);
                    usbService.write(active_);
                    usbService.write(mode_);
                    usbService.write(mode1_);
                    usbService.write(checksum);
                }

                angle1buf = angle1;
                speed1buf = speed1;
                angle2buf = angle2;
                speed2buf = speed2;
                modebuf = mode;
                mode1buf = mode1;
                activebuf = active;

                handler.postDelayed(this, delay);
            }
        }, delay);

        JoystickView joystick = findViewById(R.id.joystickView);
        joystick.setOnMoveListener((angle, strength) -> {
            angle1 = angle;
            speed1 = strength;
        });

        JoystickView joystick2 = findViewById(R.id.joystickView2);
        joystick2.setOnMoveListener((angle, strength) -> {
            angle2 = angle;
            speed2 = strength;
        });

        Switch switch1 = findViewById(R.id.switch1);
        switch1.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            active = isChecked;
            if(isChecked){
                display1.setText("On");
            }
            else{
                display1.setText("Off");
            }
        });

        RadioGroup rg = findViewById(R.id.radio_group);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            switch(checkedId){
                case R.id.walk_mode:
                    mode = 0;
                    break;
                case R.id.move_mode:
                    mode = 1;
                    break;
                case R.id.leg_mode:
                    mode = 2;
                    break;
                case R.id.auto_mode:
                    mode = 3;
                    break;
            }
        });

        RadioGroup rg1 = findViewById(R.id.radio_group1);
        rg1.setOnCheckedChangeListener((group, checkedId) -> {
            switch(checkedId){
                case R.id.tarantula:
                    mode1 = 0;
                    break;
                case R.id.slow_gait:
                    mode1 = 1;
                    break;
                case R.id.ant_gait:
                    mode1 = 2;
                    break;
            }
        });
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "");
        set.setAxisDependency(AxisDependency.LEFT);
        set.setColor(Color.rgb(101,255,168));
        set.setCircleColor(Color.rgb(101,255,168));
        set.setLineWidth(2f);
        set.setCircleRadius(0f);
        set.setFillAlpha(Color.rgb(101,255,168));
        set.setFillColor(Color.rgb(101,255,168));
        set.setHighLightColor(Color.rgb(101,255,168));
        set.setDrawValues(false);
        set.setHighlightEnabled(false);
        set.setDrawCircles(false);
        return set;
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(usbConnection); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(ServiceConnection serviceConnection) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, UsbService.class);
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, UsbService.class);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    @SuppressLint("HandlerLeak")
    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String readMessage = (String) msg.obj;
                    buf += readMessage;
                    if(buf.contains("*") && buf.contains("#")){
                        int a,b;
                        a = buf.indexOf("*");
                        b = buf.indexOf("#");
                        if(a>b) {
                            display.setText("V: " + buf.substring(3,7) + "V\nA: " + buf.substring(8,12) + "A");
                            String lol = buf.substring(3,7);
                            String kek = buf.substring(8,12);
                            try {
                                voltage = Float.parseFloat(lol);
                                current = Float.parseFloat(kek);
                            } catch (NumberFormatException e) {
                                System.out.println("numberStr is not a number");
                            }
                            addEntry();
                            gauge1.setValue((int)(voltage*100));
                            display1.setText(precision.format(voltage) + "V");
                            buf = "";
                        }
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void addEntry() {
        LineData data = chart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), current), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(100);
            chart.moveViewToX(data.getEntryCount());
        }
    }
}
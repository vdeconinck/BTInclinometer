package info.deconinck.inclinometer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.charts.LineChart;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import info.deconinck.inclinometer.bluetooth.BluetoothService;
import info.deconinck.inclinometer.dialog.AddressDialog;
import info.deconinck.inclinometer.dialog.AngleDialog;
import info.deconinck.inclinometer.dialog.SmoothingDialog;
import info.deconinck.inclinometer.util.MyFile;
import info.deconinck.inclinometer.util.SharedUtil;
import info.deconinck.inclinometer.view.InclinometerView;

@SuppressWarnings("ALL")
@SuppressLint("DefaultLocale")
public class DataMonitorActivity extends FragmentActivity implements OnClickListener {
    public static final String TAG = DataMonitorActivity.class.getName();

    // Index of tabs
    public static final int TAB_SYSTEM = 0;
    public static final int TAB_ACCELERATION = 1;
    public static final int TAB_ANGULAR_VELOCITY = 2;
    public static final int TAB_ANGLE = 3;
    public static final int TAB_MAGNETIC_FIELD = 4;

    public static final int UNSELECTED_BACKGROUND_COLOR = 0xff33b5e5;
    public static final int SELECTED_BACKGROUND_COLOR = 0xff0099cc;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int RECORDING_STOPPED = -1;
    public static final int RECORDING_STOP_REQUESTED = 0;
    public static final int RECORDING_START_REQUESTED = 1;
    public static final int RECORDING_STARTED = 2;

    public static final int MESSAGE_START_BYTE = 0x55;

    private static final int REQUEST_CONNECT_DEVICE = 1;
    public static final String ROLL_COMPENSATION_ANGLE_KEY = "rollCompensationAngle";
    public static final String TILT_COMPENSATION_ANGLE_KEY = "tiltCompensationAngle";

    private static int sensorTypeNumaxis;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private String mConnectedDeviceName = null;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private boolean recordStartorStop = false;
    public byte[] writeBuffer;
    public byte[] readBuffer;
    static MyFile myFile;
    private Switch outputSwitch;
    private static int ar = 16, av = 2000;
    private static final float[] ac = new float[]{0, 0, 0};
    private static final float[] w = new float[]{0, 0, 0};
    private static final float[] h = new float[]{0, 0, 0};
    public static final float[] angle = new float[]{0, 0, 0};
    private static final float[] d = new float[]{0, 0, 0, 0};
    private static final float[] q = new float[]{0, 0, 0, 0};
    private static float T = 20;
    private static float pressure, height, longitude, latitude, altitude, yaw, velocity, sn, pdop, hdop, vdop, voltage, version;
    private static short fieldsToSaveBitmap = 0;
    private static short currentFieldsBitmap;
    private static int recordingState = RECORDING_STOPPED;
    private static int sDataSave = 0;
    static int currentTab = TAB_SYSTEM;
    private static String strDate = "", strTime = "";
    private boolean isBtConnection = false;
    private LineChart lineChart;
    private LineChartManager lineChartManager;
    private final List<Integer> qColour = new ArrayList<>(Arrays.asList(Color.RED, Color.GREEN, Color.BLUE, Color.GRAY)); //Polyline color collection
    private static InclinometerView inclinometerView;
    private Menu menu;

    private float norm(float x[]) {
        return (float) Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
    }

    private TextView tvLabelX, tvLabelY, tvLabelZ, tvLabelAll, tvX, tvY, tvZ, tvAll;

    public void setTableName(String str1, String str2, String str3, String str4) {
        tvLabelX.setText(str1);
        tvLabelY.setText(str2);
        tvLabelZ.setText(str3);
        tvLabelAll.setText(str4);
    }

    public void setTableData(String str1, String str2, String str3, String str4) {
        tvX.setText(str1);
        tvY.setText(str2);
        tvZ.setText(str3);
        tvAll.setText(str4);
    }

    public void setTableData(String format, Object d1, Object d2, Object d3, Object d4) {
        setTableData(String.format(format, d1), String.format(format, d2), String.format(format, d3), String.format(format, d4));
    }

    static float fTempT;
    static int iError = 0;
    static Queue<Byte> queueBuffer = new LinkedList<>();
    static boolean[] hasPendingUpdate = new boolean[20];

    public static void handleSerialData(int acceptedLen, byte[] tempInputBuffer) {
        byte[] dataBuffer = new byte[11];
        byte fieldTypeByte;
        float fTemp;
        for (int i = 0; i < acceptedLen; i++) {
            queueBuffer.add(tempInputBuffer[i]);// The data read from the buffer is stored in the queue
        }
        while (queueBuffer.size() >= 11) {
            // Decode message.
            // Format is: 0x55 (MESSAGE_START) + header byte (data_type) + data buffer (8 bytes)
            // Note: peek() returns to the first item but does not delete it. poll() removes and returns
            if ((queueBuffer.poll()) != MESSAGE_START_BYTE) {
                iError++;
                continue;
            }
            // First byte is a header indicating the type of data received
            fieldTypeByte = queueBuffer.poll();
            if ((fieldTypeByte & 0xF0) == 0x50) {
                // OK, this is a valid data type. Reset error count.
                iError = 0;
            }

            // Copy the 8 data bytes in the dataBuffer
            for (int j = 0; j < 9; j++) {
                dataBuffer[j] = queueBuffer.poll();
            }

            // Check message validity (checksum)
            byte checksum = (byte) (MESSAGE_START_BYTE + fieldTypeByte);
            for (int i = 0; i < 8; i++) {
                checksum = (byte) (checksum + dataBuffer[i]);
            }
            if (checksum != dataBuffer[8]) {
                Log.e(TAG, String.format("handleSerialData: %2x %2x %2x %2x %2x %2x %2x %2x %2x SUM:%2x %2x", fieldTypeByte, dataBuffer[0], dataBuffer[1], dataBuffer[2], dataBuffer[3], dataBuffer[4], dataBuffer[5], dataBuffer[6], dataBuffer[7], dataBuffer[8], checksum));
                continue;
            }

            // Interpret message
            switch (fieldTypeByte) {
                case 0x50: // Time
                    int ms = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff));
                    strDate = String.format("20%02d-%02d-%02d", dataBuffer[0], dataBuffer[1], dataBuffer[2]);
                    strTime = String.format("%02d:%02d:%02d.%03d", dataBuffer[3], dataBuffer[4], dataBuffer[5], ms);
                    break;

                case 0x51:
                    if (SharedUtil.getInt("ar") != -1) {
                        ar = SharedUtil.getInt("ar");
                    }
                    // ac[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        ac[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f * ar;
                    }
                    // temperature, 16-bit too
                    fTempT = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    if (sensorTypeNumaxis == 6) {
                        T = (float) (fTempT / 340 + 36.53);
                    }
                    else {
                        T = fTempT;
                    }
                    break;

                case 0x52: // Angular velocity
                    if (SharedUtil.getInt("av") != -1) {
                        av = SharedUtil.getInt("av");
                    }
                    // w[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        w[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f * av;
                    }
                    // voltage, 16-bit too
                    fTemp = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    if (fTemp != fTempT) {
                        voltage = fTemp;
                    }
                    else {
                        voltage = 0;
                    }
                    break;

                case 0x53: // Angle
                    // angle[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        angle[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f * 180;
                    }
                    // version, 16-bit too
                    fTemp = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    if (fTemp != fTempT) {
                        version = fTemp * 100;
                    }
                    else {
                        version = 0;
                    }
                    inclinometerView.setAngleArray(angle);
                    break;

                case 0x54: // Magnetic field
                    // h[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        h[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff));
                    }
                    break;

                case 0x55: // port
                    // d[4], 16-bit each
                    for (int i = 0; i < 4; i++) {
                        d[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff));
                    }
                    break;

                case 0x56: // Air pressure, height
                    // pressure, 32-bit
                    pressure = ((((long) dataBuffer[3]) << 24) & 0xff000000) | ((((long) dataBuffer[2]) << 16) & 0xff0000) | ((((long) dataBuffer[1]) << 8) & 0xff00) | ((((long) dataBuffer[0]) & 0xff));
                    // altitude, 32-bit
                    height = (((((long) dataBuffer[7]) << 24) & 0xff000000) | ((((long) dataBuffer[6]) << 16) & 0xff0000) | ((((long) dataBuffer[5]) << 8) & 0xff00) | ((((long) dataBuffer[4]) & 0xff))) / 100.0f;
                    break;

                case 0x57: // Latitude and longitude
                    // longitude, 32-bit
                    long binLongitude = ((((long) dataBuffer[3]) << 24) & 0xff000000) | ((((long) dataBuffer[2]) << 16) & 0xff0000) | ((((long) dataBuffer[1]) << 8) & 0xff00) | ((((long) dataBuffer[0]) & 0xff));
                    longitude = (float) (binLongitude / 10000000 + ((float) (binLongitude % 10000000) / 100000.0 / 60.0));
                    // latitude, 32-bit
                    long binLatitude = (((((long) dataBuffer[7]) << 24) & 0xff000000) | ((((long) dataBuffer[6]) << 16) & 0xff0000) | ((((long) dataBuffer[5]) << 8) & 0xff00) | ((((long) dataBuffer[4]) & 0xff)));
                    latitude = (float) (binLatitude / 10000000 + ((float) (binLatitude % 10000000) / 100000.0 / 60.0));
                    break;

                case 0x58: // Altitude, heading, ground speed
                    altitude = (float) ((((short) dataBuffer[1]) << 8) | ((short) dataBuffer[0] & 0xff)) / 10;
                    yaw = (float) ((((short) dataBuffer[3]) << 8) | ((short) dataBuffer[2] & 0xff)) / 100;
                    velocity = (float) (((((long) dataBuffer[7]) << 24) & 0xff000000) | ((((long) dataBuffer[6]) << 16) & 0xff0000) | ((((long) dataBuffer[5]) << 8) & 0xff00) | ((((long) dataBuffer[4]) & 0xff))) / 1000;
                    break;

                case 0x59: // Quaternion
                    // q[4], 16-bit each
                    for (int i = 0; i < 4; i++) {
                        q[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f;
                    }
                    break;

                case 0x5a: // Number of satellites
                    sn = ((((short) dataBuffer[1]) << 8) | ((short) dataBuffer[0] & 0xff));
                    pdop = ((((short) dataBuffer[3]) << 8) | ((short) dataBuffer[2] & 0xff)) / 100.0f;
                    hdop = ((((short) dataBuffer[5]) << 8) | ((short) dataBuffer[4] & 0xff)) / 100.0f;
                    vdop = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    break;
            } //switch

            if ((fieldTypeByte >= 0x50) && (fieldTypeByte <= 0x5a)) {
                recordData(fieldTypeByte);
                int fieldTypeId = fieldTypeByte - 0x50;
                hasPendingUpdate[fieldTypeId] = true;
            }
        }
    }

    private int byteToInt(byte byteL, byte byteH) {
        return (byteH << 8) | byteL;
    }

    private void writeReg(final int address, final int data, int delayMs) {
        //if(mBluetoothService==null) return;
        if (delayMs == 0) {
            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) address, (byte) (data & 0xff), (byte) ((data >> 8) & 0xff)});
        }
        else {
            new Handler().postDelayed(
                    () -> sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) address, (byte) (data & 0xff), (byte) ((data >> 8) & 0xff)}),
                    delayMs
            );
        }
    }

    private void writeReg(int addr, int data) {
        writeReg(addr, data, 0);
    }

    private void unLockReg(int delayMs) {
        writeReg(0x69, 0xb588, delayMs);//unlock
    }

    private void saveReg(int delayMs) {
        writeReg(0x00, 0x00, delayMs);//unlock
    }

    private void writeLockReg(int addr, int data) {
        unLockReg(0);//unlock
        writeReg(addr, data, 50);//write Reg
    }

    private void writeAndSaveReg(int addr, int data) {
        unLockReg(0);//unlock
        writeReg(addr, data, 50);//write Reg
        saveReg(100);//save
    }

    private void highlightCurrentTab(View v) {
        (findViewById(R.id.systemTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.accelerationTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.angularVelocityTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.angleTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.magneticFieldTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        v.setBackgroundColor(SELECTED_BACKGROUND_COLOR);
    }

    public void onOutputSwitchClick(View v) {
        Log.e(TAG, "onOutputSwitchClick: " + String.format("Output:0x%x", getOutputEnabledBitmap()));
        if (sensorTypeNumaxis == 9) {
            isOutputEnabled[currentTab] = outputSwitch.isChecked();
            int outputContent = getOutputEnabledBitmap();
            writeAndSaveReg(0x02, outputContent);
            SharedUtil.putInt("Out", outputContent);
        }
    }

    public void onTabBtnClick(View v) {
        lineChartManager.setbPause(true);
        int i = v.getId();
        highlightCurrentTab(v);
        if (i == R.id.systemTabBtn) {
            currentTab = TAB_SYSTEM;
            setTableName(getString(R.string.version), getString(R.string.voltage), getString(R.string.date), getString(R.string.time));
            Log.i(TAG, "Voltage:" + getString(R.string.voltage));
            setTableData("1.0", "3.3V", "2020-1-1", "00:00:00.0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
            if (sensorTypeNumaxis == 9) {
                unLockReg(0);
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                writeReg(0x30, byteToInt((byte) (year - 2000), (byte) month), 50);
                writeReg(0x31, byteToInt((byte) day, (byte) hour), 100);
                writeReg(0x32, byteToInt((byte) minute, (byte) second), 150);
            }
        }
        else if (i == R.id.accelerationTabBtn) {
            currentTab = TAB_ACCELERATION;
            setTableName("ax:", "ay:", "az:", "|a|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("ax", "ay", "az"), qColour);
            lineChartManager.setDescription(getString(R.string.acc_chart));
        }
        else if (i == R.id.angularVelocityTabBtn) {
            currentTab = TAB_ANGULAR_VELOCITY;
            setTableName("wx:", "wy:", "wz:", "|w|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("wx", "wy", "wz"), qColour);
            lineChartManager.setDescription(getString(R.string.w_chart));
        }
        else if (i == R.id.angleTabBtn) {
            currentTab = TAB_ANGLE;
            setTableName("AngleX:", "AngleY:", "AngleZ:", "T:");
            setTableData("0", "0", "0", "25℃");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        else if (i == R.id.magneticFieldTabBtn) {
            currentTab = TAB_MAGNETIC_FIELD;
            setTableName("hx:", "hy:", "hz:", "|h|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("hx", "hy", "hz"), qColour);
            lineChartManager.setDescription(getString(R.string.mag_chart));
        }

        if (sensorTypeNumaxis == 9) {
            outputSwitch.setVisibility(View.VISIBLE);
            outputSwitch.setChecked(isOutputEnabled[currentTab]);
        }
        else {
            outputSwitch.setVisibility(View.INVISIBLE);
        }

        new Handler().postDelayed(() -> lineChartManager.setbPause(false), 100);
    }

    public static void recordData(byte fieldTypeByte) {
        try {
            boolean isRepeat = false;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date curDate = new Date(System.currentTimeMillis()); // Get the current time

            // Convert the data type byte (0x50 - 0x5a) into a mask
            // Only keep last nibble (0x0 - 0xa)
            int fieldTypeId = fieldTypeByte & 0x0f;
            // Create corresponding mask
            short fieldTypeMask = (short) (0x01 << fieldTypeId);
            // If this data bit is set in the current mask and ... ? TODO
            if (((currentFieldsBitmap & fieldTypeMask) == fieldTypeMask) && (fieldTypeMask < sDataSave)) {
                fieldsToSaveBitmap = currentFieldsBitmap;
                currentFieldsBitmap = fieldTypeMask;
                isRepeat = true;
            }
            else {
                // Add the current data type to the current list
                currentFieldsBitmap |= fieldTypeMask;
            }
            sDataSave = fieldTypeMask;

            switch (recordingState) {
                case RECORDING_STOP_REQUESTED:
                    myFile.close();
                    recordingState = RECORDING_STOPPED;
                    break;

                case RECORDING_START_REQUESTED:
                    // Create file
                    SimpleDateFormat formatterFileName = new SimpleDateFormat("MMdd_HHmmss");
                    Date curDateFileName = new Date(System.currentTimeMillis()); // Get the current time
                    myFile = new MyFile(Environment.getExternalStorageDirectory() + "/Records/Rec_" + formatterFileName.format(curDateFileName) + ".txt");
                    // Write header line
                    String s = "Start time:" + formatter.format(curDate) + "\r\n" + "Record Time:";
                    if ((fieldsToSaveBitmap & 0x01) > 0) s += " ChipTime:";
                    if ((fieldsToSaveBitmap & 0x02) > 0) s += " ax: ay: az:";
                    if ((fieldsToSaveBitmap & 0x04) > 0) s += "  wx: wy: wz:";
                    if ((fieldsToSaveBitmap & 0x08) > 0) s += "    AngleX:   AngleY:   AngleZ:";
                    if ((fieldsToSaveBitmap & 0x10) > 0) s += "   hx:   hy:   hz:";
                    if ((fieldsToSaveBitmap & 0x20) > 0) s += "d0:d1:d2:d3:";
                    if ((fieldsToSaveBitmap & 0x40) > 0) s += "    Pressure:    Height:";
                    if ((fieldsToSaveBitmap & 0x80) > 0) s += "        Longitude:        Latitude:";
                    if ((fieldsToSaveBitmap & 0x100) > 0) s += "    ALtitude:    Yaw:    Velocity:";
                    if ((fieldsToSaveBitmap & 0x200) > 0) s += "   q0:   q1:   q2:   q3:";
                    if ((fieldsToSaveBitmap & 0x400) > 0) s += "SN:PDOP: HDOP: VDOP:";
                    myFile.write(s);
                    // Switch to "recording"
                    recordingState = RECORDING_STARTED;
                    break;

                case RECORDING_STARTED:
                    if (isRepeat) {
                        myFile.write("  \r\n");
                        myFile.write(formatter.format(curDate) + " ");
                        if ((fieldsToSaveBitmap & 0x01) > 0) {
                            myFile.write(strDate + " " + strTime + " ");
                        }
                        if ((fieldsToSaveBitmap & 0x02) > 0) {
                            myFile.write(String.format("% 10.4f", ac[0]) + String.format("% 10.4f", ac[1]) + String.format("% 10.4f", ac[2]) + " ");
                        }
                        if ((fieldsToSaveBitmap & 0x04) > 0) {
                            myFile.write(String.format("% 10.4f", w[0]) + String.format("% 10.4f", w[1]) + String.format("% 10.4f", w[2]) + " ");
                        }
                        if ((fieldsToSaveBitmap & 0x08) > 0) {
                            myFile.write(String.format("% 10.4f", angle[0]) + String.format("% 10.4f", angle[1]) + String.format("% 10.4f", angle[2]));
                        }
                        if ((fieldsToSaveBitmap & 0x10) > 0) {
                            myFile.write(String.format("% 10.0f", h[0]) + String.format("% 10.0f", h[1]) + String.format("% 10.0f", h[2]));
                        }
                        if ((fieldsToSaveBitmap & 0x20) > 0) {
                            myFile.write(String.format("% 7.0f", d[0]) + String.format("% 7.0f", d[1]) + String.format("% 7.0f", d[2]) + String.format("% 7.0f", d[3]));
                        }
                        if ((fieldsToSaveBitmap & 0x40) > 0) {
                            myFile.write(String.format("% 10.0f", pressure) + String.format("% 10.2f", height));
                        }
                        if ((fieldsToSaveBitmap & 0x80) > 0) {
                            myFile.write(String.format("% 14.6f", longitude) + String.format("% 14.6f", latitude));
                        }
                        if ((fieldsToSaveBitmap & 0x100) > 0) {
                            myFile.write(String.format("% 10.4f", altitude) + String.format("% 10.2f", yaw) + String.format("% 10.2f", velocity));
                        }
                        if ((fieldsToSaveBitmap & 0x200) > 0) {
                            myFile.write(String.format("% 7.4f", q[0]) + String.format("% 7.4f", q[1]) + String.format("% 7.4f", q[2]) + String.format("% 7.4f", q[3]));
                        }
                        if ((fieldsToSaveBitmap & 0x400) > 0) {
                            myFile.write(String.format("% 5.0f", sn) + String.format("% 7.1f", pdop) + String.format("% 7.1f", hdop) + String.format("% 7.1f", vdop));
                        }
                    }
                    break;

                default:
                    break;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "recordData: ", e);
        }
    }

    public void setRecording(boolean record) {
        if (record) {
            recordingState = RECORDING_START_REQUESTED;
        }
        else {
            recordingState = RECORDING_STOP_REQUESTED;
        }
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        // Anonymous inner class, implementing some of the Handler interface
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    String connectionStatus;
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            isBtConnection = true;
                            initTabs();
                            connectionStatus = getString(R.string.title_connected_to, mConnectedDeviceName);
                            inclinometerView.setConnectionStatus(connectionStatus);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            connectionStatus = getString(R.string.title_connecting);
                            inclinometerView.setConnectionStatus(connectionStatus);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            isBtConnection = false;
                            connectionStatus = getString(R.string.title_not_connected);
                            inclinometerView.setConnectionStatus(connectionStatus);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString("device_name");
                    connectionStatus = getString(R.string.title_connected_to, mConnectedDeviceName);
                    Toast.makeText(getApplicationContext(), connectionStatus, Toast.LENGTH_SHORT).show();
                    inclinometerView.setConnectionStatus(connectionStatus);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final Handler refreshHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (isPaused) return;
            // If there is no pending update for this tab, exit
            if (!hasPendingUpdate[currentTab]) return;

            hasPendingUpdate[currentTab] = false;
            switch (currentTab) {
                case TAB_SYSTEM:
                    ((TextView) findViewById(R.id.tvZ)).setText(strDate);
                    ((TextView) findViewById(R.id.tvAll)).setText(strTime);
                    ((TextView) findViewById(R.id.tvY)).setText(String.format("%10.2fV", voltage));
                    ((TextView) findViewById(R.id.tvX)).setText(String.format("% 10.0f", version));

                    // Fill chart with angle data, just to see some animation...
                    lineChartManager.addEntry(Arrays.asList(angle[0], angle[1], angle[2]));
                    break;

                case TAB_ACCELERATION:
                    setTableData("% 10.4fg", ac[0], ac[1], ac[2], norm(ac));
                    // Log.d("--",String.format("acc:% 10.2fg,% 10.2fg,% 10.2fg,% 10.2fg", ac[0], ac[1], ac[2], norm(ac)));
                    lineChartManager.addEntry(Arrays.asList(ac[0], ac[1], ac[2]));
                    break;

                case TAB_ANGULAR_VELOCITY:
                    setTableData("% 10.4f°/s", w[0], w[1], w[2], norm(w));
                    //  Log.d("--", String.format("axw:% 10.2f,% 10.2f,% 10.2f,% 10.2f", w[0], w[1], w[2], norm(w)));
                    lineChartManager.addEntry(Arrays.asList(w[0], w[1], w[2]));
                    break;

                case TAB_ANGLE:
                    setTableData(String.format("%10.4f°", angle[0]), String.format("%10.4f°", angle[1]), String.format("%10.4f°", angle[2]), String.format("%10.2f℃", T));
                    lineChartManager.addEntry(Arrays.asList(angle[0], angle[1], angle[2]));
                    break;

                case TAB_MAGNETIC_FIELD:
                    setTableData("% 10.0f", h[0], h[1], h[2], norm(h));
                    lineChartManager.addEntry(Arrays.asList(h[0], h[1], h[2]));
                    break;

            } // end switch

        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.data_monitor_activity);
        SharedUtil.init(getApplicationContext());
        setOutputEnabledBitmap(SharedUtil.getInt("Out"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        // Options menu
        toolbar.inflateMenu(R.menu.option_menu);
        // customize menu for other sensors
        enableMenuItems(toolbar.getMenu());
        installMenuClickListener(toolbar);

        Intent intent = getIntent();
        sensorTypeNumaxis = intent.getIntExtra("type", 0);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Keep the screen always on

        // Hide data tabs
        findViewById(R.id.dataArea).setVisibility(View.GONE);

        // Show inclinometer instead
        inclinometerView = findViewById(R.id.inclinometer);
        inclinometerView.setConnectionStatus(getString(R.string.title_connecting));
        inclinometerView.setVisibility(View.VISIBLE);

        writeBuffer = new byte[512];
        readBuffer = new byte[512];

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, getString(R.string.bluetooth_bad), Toast.LENGTH_LONG).show();
                return;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }

        if (displayThread == null) {
            bDisplay = true;
            displayThread = new Thread(() -> {
                while (bDisplay) {
                    refreshHandler.sendMessage(Message.obtain());
                    try {
                        Thread.sleep(100);
                    }
                    catch (Exception ignored) {
                    }
                }
            });
            displayThread.start();
        }
    }

    private void enableMenuItems(Menu menu) {
        enableMenu(menu, R.id.system, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.factory_reset, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.sleep, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.algorithm, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.installation_orientation, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.static_detection_threshold, sensorTypeNumaxis == 6);
        enableMenu(menu, R.id.__instruction_start, sensorTypeNumaxis == 9);

        enableMenu(menu, R.id.calibration, true);
        enableMenu(menu, R.id.zero_tilt, true);
        enableMenu(menu, R.id.zero_roll, true);
        enableMenu(menu, R.id.acc_calibration, true);
        enableMenu(menu, R.id.smoothing_factor, sensorTypeNumaxis == 3);
        enableMenu(menu, R.id.magnetic_field_calibration_start, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.magnetic_field_calibration_end, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.reset_height, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.gyroscope_automatic_calibration, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.reset_Z_axis, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.setting_angle_reference, sensorTypeNumaxis == 9);

        enableMenu(menu, R.id.range, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.acceleration_range, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.angular_velocity_range, sensorTypeNumaxis == 9);
        enableMenu(menu, R.id.measurement_bandwidth, sensorTypeNumaxis != 3);

        enableMenu(menu, R.id.signal_communication, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.retrieval_rate, sensorTypeNumaxis != 3);
        enableMenu(menu, R.id.address, sensorTypeNumaxis == 9);

    }

    private void enableMenu(Menu menu, int id, boolean enabled) {
        MenuItem item = menu.findItem(id);
        item.setEnabled(enabled);
        item.setVisible(enabled);
    }

    private void installMenuClickListener(Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.scanItem:
                        onBtConnectClicked(null);
                        return true;

                    // system menu

                    case R.id.change_sensor_type:
                        changeSensorType();
                        return true;
                    case R.id.toggle_debug_info:
                        inclinometerView.setShowDebug(!inclinometerView.isShowDebug());
                        return true;
                    case R.id.factory_reset:
                        factoryReset9();
                        return true;
                    case R.id.sleep:
                        switch (sensorTypeNumaxis) {
                            case 6:
                                sleep6();
                                return true;
                            case 9:
                                sleep9();
                                return true;
                        }
                    case R.id.algorithm:
                        selectAlgorithm9();
                        return true;
                    case R.id.installation_orientation:
                        switch (sensorTypeNumaxis) {
                            case 6:
                                selectOrientation6();
                                return true;
                            case 9:
                                selectOrientation9();
                                return true;
                        }
                    case R.id.static_detection_threshold: // Only exists for 6 axis
                        if (sensorTypeNumaxis == 6) {
                            selectStaticDetect6();
                        }
                    case R.id.__instruction_start:
                        cmdStartUp9();
                        return true;

                    // calibration menu

                    case R.id.zero_roll:
                        calibrateZeroRoll();
                        return true;
                    case R.id.zero_tilt:
                        calibrateZeroTilt();
                        return true;
                    case R.id.acc_calibration:
                        switch (sensorTypeNumaxis) {
                            case 3:
                                calibrateAcceleration3();
                                return true;
                            case 6:
                                calibrateAcceleration6();
                                return true;
                            case 9:
                                calibrateAcceleration9();
                                return true;
                        }
                        return true;
                    case R.id.smoothing_factor: // Only exists for 3 axis
                        if (sensorTypeNumaxis == 3) {
                            selectSmoothingFactor3();
                        }
                    case R.id.magnetic_field_calibration_start:
                        calibrateMagneticField9Start(); // Start calibration
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_calibrating), Toast.LENGTH_LONG).show();
                        return true;
                    case R.id.magnetic_field_calibration_end:
                        calibrateMagneticField9End();
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                        return true;
                    case R.id.reset_height:
                        resetHeight9();
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                        return true;
                    case R.id.gyroscope_automatic_calibration:
                        calibrateGyro9();
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                        return true;
                    case R.id.reset_Z_axis:
                        switch (sensorTypeNumaxis) {
                            case 6:
                                calibrateZAxisAngleToZero6();
                                return true;
                            case 9:
                                calibrateZAxisAngleToZero9();
                                return true;
                        }
                    case R.id.setting_angle_reference:
                        setAngleReference9();
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                        return true;

                    // range menu

                    case R.id.acceleration_range:
                        selectAccelerationRange9();
                        return true;
                    case R.id.angular_velocity_range:
                        selectAngularVelocityRange9();
                        return true;
                    case R.id.measurement_bandwidth:
                        switch (sensorTypeNumaxis) {
                            case 6:
                                selectBandwidth6();
                                return true;
                            case 9:
                                selectBandwidth9();
                                return true;
                        }

                        // signal_communication menu

                    case R.id.retrieval_rate:
                        switch (sensorTypeNumaxis) {
                            case 6:
                                selectRetrievelRate6();
                                return true;
                            case 9:
                                selectRetrievelRate9();
                                return true;
                        }
                    case R.id.address:
                        selectAddress9();
                        return true;
                }
                return false;
            }
        });
    }

    private void changeSensorType() {
        // TODO this should replace the sensor selection splash screen
    }


    private void calibrateZeroRoll() {
        AngleDialog angleDialog = AngleDialog.newInstance("Roll", angle, 0);
        angleDialog.setAngleDialogCallBack(new AngleDialog.AngleDialogCallBack() {
            @Override
            public void save(String value) {
                float rollCompensationAngle = Float.parseFloat(value);
                SharedUtil.putFloat(ROLL_COMPENSATION_ANGLE_KEY, rollCompensationAngle);
                inclinometerView.setRollCompensationAngle(rollCompensationAngle);
            }

            @Override
            public void back() {
            }
        });
        angleDialog.show(getSupportFragmentManager());
    }

    private void calibrateZeroTilt() {
        AngleDialog angleDialog = AngleDialog.newInstance("Tilt", angle, 1);
        angleDialog.setAngleDialogCallBack(new AngleDialog.AngleDialogCallBack() {
            @Override
            public void save(String value) {
                float tiltCompensationAngle = Float.parseFloat(value);
                SharedUtil.putFloat(TILT_COMPENSATION_ANGLE_KEY, tiltCompensationAngle);
                inclinometerView.setTiltCompensationAngle(tiltCompensationAngle);
            }

            @Override
            public void back() {
            }
        });
        angleDialog.show(getSupportFragmentManager());
    }



    private void calibrateZAxisAngleToZero9() {
        writeAndSaveReg(0x01, 0x04);
        Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
    }

    private void calibrateMagneticField9End() {
        writeAndSaveReg(0x01, 0x00);
    }

    private void sleep9() {
        writeLockReg(0x22, 0x01);
    }


    private boolean bDisplay = true;
    private Thread displayThread;

    private void initTabs() {
        tvLabelX = findViewById(R.id.X);
        tvLabelY = findViewById(R.id.Y);
        tvLabelZ = findViewById(R.id.Z);
        tvLabelAll = findViewById(R.id.all);
        tvX = findViewById(R.id.tvX);
        tvY = findViewById(R.id.tvY);
        tvZ = findViewById(R.id.tvZ);
        tvAll = findViewById(R.id.tvAll);

        if (sensorTypeNumaxis == 3) {
            // Hide most tabs
            findViewById(R.id.angularVelocityTabBtn).setVisibility(View.GONE);
            findViewById(R.id.magneticFieldTabBtn).setVisibility(View.GONE);
        }
        else if (sensorTypeNumaxis == 6) {
            // Hide some tabs
            findViewById(R.id.magneticFieldTabBtn).setVisibility(View.GONE);
        }
    }

    private void selectSmoothingFactor3() {
        SmoothingDialog smoothingDialog = SmoothingDialog.newInstance();
        smoothingDialog.setDevDialogCallBack(new SmoothingDialog.SmoothingDialogCallBack() {
            @Override
            public void save(String value) {
                byte[] values = value.getBytes();
                if (values.length == 1) {
                    values[0] = 0x00;
                }
                writeReg(0x6c, byteToInt(values[0], values[1]));
            }

            @Override
            public void back() {
                // noop
            }
        });
        smoothingDialog.show(getSupportFragmentManager());
    }

    private void calibrateAcceleration3() {
        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
    }

    private void calibrateZAxisAngleToZero6() {
        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x52});
        Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
    }

    private void setAngleReference9() {
        writeAndSaveReg(0x01, 0x08);
    }

    private void resetHeight9() {
        writeAndSaveReg(0x01, 0x03);
    }

    private void calibrateMagneticField9Start() {
        writeLockReg(0x01, 0x07);
    }

    private void factoryReset9() {
        writeLockReg(0x00, 0x01);
    }

    private void sleep6() {
        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x60});
    }

    private void calibrateAcceleration6() {
        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x67});
    }

    private void sendData(byte[] byteSend) {
        if (mBluetoothService != null) {
            mBluetoothService.send(byteSend);
        }
    }

    private void selectAddress9() {
        AddressDialog addressDialog = AddressDialog.newInstance();
        addressDialog.setAddressDialogCallBack(new AddressDialog.AddressDialogCallBack() {
            @Override
            public void save(String value) {
                int v = Integer.parseInt(value);
                writeAndSaveReg(0x1a, (byte) v);
            }

            @Override
            public void back() {
            }
        });
        addressDialog.show(getSupportFragmentManager());
    }


    int iRetrivalRateSelect = 5;

    private void selectRetrievelRate9() {
        String[] s = new String[]{"0.2Hz", "0.5Hz", "1Hz", "2Hz", "5Hz", "10HZ", "20Hz", "50Hz", "100Hz", "125Hz", "200Hz"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_return_rate))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iRetrivalRateSelect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iRetrivalRateSelect = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x03, (byte) (iRetrivalRateSelect + 1));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iBandwidth901 = 4;

    private void selectBandwidth9() {
        String[] s = new String[]{"256HZ", "184HZ", "94HZ", "42HZ", "21HZ", "10HZ", "5HZ"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_bandwith))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iBandwidth901, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iBandwidth901 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x1f, (byte) iBandwidth901);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int angularVelocityRangeParam = 3;

    private void selectAngularVelocityRange9() {
        String[] s = new String[]{"250deg/s", "500deg/s", "1000deg/s", "2000deg/s"};
        new AlertDialog.Builder(this)
                .setTitle((R.string.choose_angle))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, angularVelocityRangeParam, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        angularVelocityRangeParam = i;
                    }
                })
                .setPositiveButton((R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x20, (byte) angularVelocityRangeParam);
                        av = 250 * (int) Math.pow(2, angularVelocityRangeParam);//1,2,4,8
                        Log.i("range", String.format("w range = %d", av));
                        SharedUtil.putInt("av", av);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int accRangeParam = 3;

    private void selectAccelerationRange9() {
        String[] s = new String[]{"2g", "4g", "8g", "16g"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_range))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, accRangeParam, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        accRangeParam = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x21, (byte) accRangeParam);
                        ar = (int) Math.pow(2, accRangeParam + 1);
                        Log.i("range", String.format("acc range = %d", ar));
                        SharedUtil.putInt("ar", ar);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iAutoCali = 0;

    private void calibrateGyro9() {
        String[] s = new String[]{"Yes", "No"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.automatic_calibration_of_helix))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iAutoCali, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iAutoCali = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x63, (byte) iAutoCali);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void calibrateAcceleration9() {
        writeLockReg(0x01, 0x01);
        saveReg(3000);

        Toast.makeText(this, getString(R.string.calibrating), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(() -> Toast.makeText(getApplicationContext(), getString(R.string.calibrated), Toast.LENGTH_SHORT).show(), 3000);
    }

    int iAlgorithm = 1;

    private void selectAlgorithm9() {
        String[] s = new String[]{getString(R.string.six_axis_algorithm), getString(R.string.nine_axis_algorithm)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_algorithm))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iAlgorithm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iAlgorithm = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (iAlgorithm == 0) {
                            writeAndSaveReg(0x24, 0x01);
                        }
                        else if (iAlgorithm == 1) {
                            writeAndSaveReg(0x24, 0x00);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    public void onBtConnectClicked(View view) {
        try {
            if (mBluetoothService == null) {
                // Used to manage Bluetooth connections
                mBluetoothService = new BluetoothService(this, mHandler);
            }
            else {
                mBluetoothService.stop();
            }
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }
        catch (Exception e) {
            Log.e(TAG, "onBluetoothClicked: ", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public synchronized void onResume() {
        super.onResume();
        initTabs();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mBluetoothService != null) {
                    if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                        mBluetoothService.start();
                    }
                }
                else {
                    Log.e(TAG, "onResume.run: service is null");
                    String address = SharedUtil.getString("BTName");
                    if (address != null) {
                        Log.e("--", "BTName = " + address);
                        mBluetoothService = new BluetoothService(getApplicationContext(), mHandler); // Used to manage Bluetooth connections
                        device = mBluetoothAdapter.getRemoteDevice(address);// Get the BLuetoothDevice object
                        mBluetoothService.connect(device);// Attempt to connect to the device
                    }
                    else {
                        onBtConnectClicked(null);
                    }
                }
            }
        }, 1000);

        if (lineChart == null) {
            lineChart = findViewById(R.id.lineChart);
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        outputSwitch = findViewById(R.id.dataSwitch);
        if (sensorTypeNumaxis == 9) {
            outputSwitch.setVisibility(View.VISIBLE);
        }
        else {
            outputSwitch.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) mBluetoothService.stop();
        bDisplay = false;
    }

    public BluetoothDevice device;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    device = mBluetoothAdapter.getRemoteDevice(address);
                    SharedUtil.putString("BTName", address);
                    // Attempt to connect to the device
                    mBluetoothService.connect(device);
                }
                break;
        }
    }

    int iJY61Baud = 0;
    int iJY61RateSelect = 0;

    public void selectRetrievelRate6() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_return_rate))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"20Hz", "100Hz"}, iJY61RateSelect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iJY61RateSelect = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        try {
                            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x64 - iJY61RateSelect)});
                        }
                        catch (Exception e) {
                            Log.e(TAG, "onClick: ", e);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }


    int iDirection = 0;

    public void selectOrientation6() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_install_orientation))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{getString(R.string.horizontal_installation), getString(R.string.vertical_installation)}, iDirection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iDirection = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (iDirection == 0) {
                            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x65});
                        }
                        else if (iDirection == 1) {
                            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x66});
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int getiDirection901 = 1;

    public void selectOrientation9() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_install_orientation))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{getString(R.string.horizontal_installation), getString(R.string.vertical_installation)}, getiDirection901, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getiDirection901 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x23, 0x01 - getiDirection901);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iCmdStartup = 1;

    // No idea what this is about...
    public void cmdStartUp9() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.__whether_or_not))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"Yes", "No"}, iCmdStartup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iCmdStartup = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x2d, iCmdStartup);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iStaticDetect61 = 4;

    public void selectStaticDetect6() {
        String[] s = new String[]{"0.122°/s", "0.244°/s", "0.366°/s", "0.488°/s", "0.610°/s", "0.732°/s", "0.854°/s", "0.976°/s"
                , "1.098°/s", "1.221°/s", "1.343°/s", "1.456°/s", "1.587°/s", "1.709°/s", "1.831°/s"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.static_detection_threshold))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iStaticDetect61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iStaticDetect61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x71 + iStaticDetect61)});
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iBandwidth61 = 4;

    public void selectBandwidth6() {
        String[] s = new String[]{"256HZ", "184HZ", "94HZ", "44HZ", "21HZ", "10HZ", "5HZ"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_bandwith))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iBandwidth61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iBandwidth61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x81 + iBandwidth61)});
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }


    static boolean[] isOutputEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true};

    public void setOutputEnabledBitmap(int iOut) {
        if (iOut == -1) iOut = 0x0F;
        for (int i = 0; i < isOutputEnabled.length; i++) {
            isOutputEnabled[i] = ((iOut >> i) & 0x01) == 0x01;
        }
    }

    public int getOutputEnabledBitmap() {
        int iTemp = 0;
        for (int i = 0; i < isOutputEnabled.length; i++) {
            if (isOutputEnabled[i]) iTemp |= 0x01 << i;
        }
        return iTemp;
    }

    boolean isPaused = false;

    public void onPauseClick(View v) {
        isPaused = !isPaused;
    }

    public void onRecordClick(View v) {
        if (!this.recordStartorStop) {
            this.recordStartorStop = true;
            setRecording(true);
            ((Button) v).setText(getString(R.string.stop));
        }
        else {
            this.recordStartorStop = false;
            setRecording(false);
            ((Button) findViewById(R.id.btnRecord)).setText(getString(R.string.record));
            if (myFile == null) {
                Toast.makeText(DataMonitorActivity.this, "No file recorded!", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.recorded_root_directory) + myFile.file.getPath() + getString(R.string.open_file))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                myFile.openFile(getApplicationContext());
                            }
                            catch (Exception e) {
                                Log.e(TAG, "myFile.openFile: ", e);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }

    @Override
    public void onClick(View v) {
    }

}

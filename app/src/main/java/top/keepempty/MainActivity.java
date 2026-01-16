package top.keepempty;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import top.keepempty.sph.library.SerialPortConfig;
import top.keepempty.sph.library.SerialPortFinder;
import top.keepempty.sph.library.SerialPortHelper;
import top.keepempty.sph.library.SphCmdEntity;
import top.keepempty.sph.library.SphResultCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "SerialPortHelper";

    private SerialPortHelper serialPortHelper;

    private int baudRate = 115200;
    private int dataBits = 8;
    private char checkBits = 'N';
    private int stopBits = 1;
    private String path;

    private SerialPortFinder mSerialPortFinder;
    private Button btn_open, btn_open_relay, btn_close_relay, btn_close;
    private boolean isOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        btn_open = findViewById(R.id.btn_open);
        btn_open_relay = findViewById(R.id.btn_open_relay);
        btn_close_relay = findViewById(R.id.btn_close_relay);
        btn_close = findViewById(R.id.btn_close);
        btn_open.setOnClickListener(this);
        btn_open_relay.setOnClickListener(this);
        btn_close_relay.setOnClickListener(this);
        btn_close.setOnClickListener(this);
    }

    private void openSerialPort() {
        if (serialPortHelper != null && isOpen) {
            Toast.makeText(this, "The serial port has been opened!", Toast.LENGTH_SHORT).show();
            return;
        }
        mSerialPortFinder = new SerialPortFinder();
        path = mSerialPortFinder.findDevice("ttyACM", "19f5", "5740");
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this, "Not find target device!", Toast.LENGTH_SHORT).show();
            return;
        }
        SerialPortConfig serialPortConfig = new SerialPortConfig();
        serialPortConfig.mode = 0;
        serialPortConfig.path = path;
        serialPortConfig.baudRate = baudRate;
        serialPortConfig.dataBits = dataBits;
        serialPortConfig.parity = checkBits;
        serialPortConfig.stopBits = stopBits;

        serialPortHelper = new SerialPortHelper(16);
        serialPortHelper.setConfigInfo(serialPortConfig);
        isOpen = serialPortHelper.openDevice();
        if (!isOpen) {
            Toast.makeText(this, "Failed to open the serial port！", Toast.LENGTH_LONG).show();
            return;
        }
        serialPortHelper.setSphResultCallback(new SphResultCallback() {
            @Override
            public void onSendData(SphCmdEntity sendCom) {
                Log.d(TAG, "send data：" + sendCom.commandsHex);
            }

            @Override
            public void onReceiveData(SphCmdEntity data) {
                Log.d(TAG, "receive data：" + data.commandsHex);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "complete");
            }
        });
    }

    private void closeSerialPort() {
        if (serialPortHelper != null) {
            serialPortHelper.closeDevice();
            serialPortHelper = null;
        }
        isOpen = false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open:
                openSerialPort();
                break;
            case R.id.btn_open_relay:
                if (serialPortHelper != null && isOpen) {
                    serialPortHelper.addCommands(new byte[]{(byte) 0xA0, 0x01, 0x01, (byte) 0xA2});
                }
                break;
            case R.id.btn_close_relay:
                if (serialPortHelper != null && isOpen) {
                    serialPortHelper.addCommands(new byte[]{(byte) 0xA0, 0x01, 0x00, (byte) 0xA1});
                }
                break;
            case R.id.btn_close:
                closeSerialPort();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSerialPort();
    }
}
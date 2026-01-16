package top.keepempty.sph.library;

/*
 * Copyright 2009 Cedric Priscal
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
import android.util.Log;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Vector;

public class SerialPortFinder {

    public class Driver {
        public Driver(String name, String root) {
            mDriverName = name;
            mDeviceRoot = root;
        }

        private String mDriverName;
        private String mDeviceRoot;
        Vector<File> mDevices = null;

        public Vector<File> getDevices() {
            if (mDevices == null) {
                mDevices = new Vector<File>();
                File dev = new File("/dev");
                File[] files = dev.listFiles();
                int i;
                for (i = 0; i < files.length; i++) {
                    if (files[i].getAbsolutePath().startsWith(mDeviceRoot)) {
                        Log.d(TAG, "Found new device: " + files[i]);
                        mDevices.add(files[i]);
                    }
                }
            }
            return mDevices;
        }

        public String getName() {
            return mDriverName;
        }
    }

    private static final String TAG = "SerialPortHelper";

    private Vector<Driver> mDrivers = null;

    Vector<Driver> getDrivers() throws IOException {
        if (mDrivers == null) {
            mDrivers = new Vector<Driver>();
            LineNumberReader r = new LineNumberReader(new FileReader("/proc/tty/drivers"));
            String l;
            while ((l = r.readLine()) != null) {
                // Issue 3:
                // Since driver name may contain spaces, we do not extract driver name with split()
                String drivername = l.substring(0, 0x15).trim();
                String[] w = l.split(" +");
                if ((w.length >= 5) && (w[w.length - 1].equals("serial"))) {
                    Log.d(TAG, "Found new driver " + drivername + " on " + w[w.length - 4]);
                    mDrivers.add(new Driver(drivername, w[w.length - 4]));
                }
            }
            r.close();
        }
        return mDrivers;
    }

    public String[] getAllDevices() {
        Vector<String> devices = new Vector<String>();
        // Parse each driver
        Iterator<Driver> itdriv;
        try {
            itdriv = getDrivers().iterator();
            while (itdriv.hasNext()) {
                Driver driver = itdriv.next();
                Iterator<File> itdev = driver.getDevices().iterator();
                while (itdev.hasNext()) {
                    String device = itdev.next().getName();
                    String value = String.format("%s (%s)", device, driver.getName());
                    devices.add(value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return devices.toArray(new String[devices.size()]);
    }

    public String[] getAllDevicesPath() {
        Vector<String> devices = new Vector<String>();
        // Parse each driver
        Iterator<Driver> itdriv;
        try {
            itdriv = getDrivers().iterator();
            while (itdriv.hasNext()) {
                Driver driver = itdriv.next();
                Iterator<File> itdev = driver.getDevices().iterator();
                while (itdev.hasNext()) {
                    String device = itdev.next().getAbsolutePath();
                    devices.add(device);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return devices.toArray(new String[devices.size()]);
    }


    /**
     * 根据前缀、vid 和 pid 查找串口设备
     *
     * @param preName 串口名前缀 (如 "ttyACM" 或 "ttyUSB")
     * @param vid     Vendor ID (十六进制字符串，如 "1a86")
     * @param pid     Product ID (十六进制字符串，如 "7523")
     * @return 匹配到的串口设备路径，未找到返回 null
     */
    public String findDevice(String preName, String vid, String pid) {
        if (preName == null || vid == null || pid == null) {
            return null;
        }

        String[] paths = getAllDevicesPath();
        for (String path : paths) {
            File devFile = new File(path);
            String devName = devFile.getName();

            // 仅处理指定前缀的设备
            if (!devName.startsWith(preName)) {
                continue;
            }

            // 对应的 sysfs 路径通常在 /sys/class/tty/<name>/device/
            File sysfsDir = new File("/sys/class/tty/" + devName + "/device");
            if (!sysfsDir.exists()) {
                continue;
            }

            String currentVid = getValue(new File(sysfsDir, "idVendor"));
            String currentPid = getValue(new File(sysfsDir, "idProduct"));

            if (vid.equalsIgnoreCase(currentVid) && pid.equalsIgnoreCase(currentPid)) {
                Log.d(TAG, "Found matching device: " + path + " (VID=" + currentVid + ", PID=" + currentPid + ")");
                return path;
            }
        }
        return null;
    }

    private String getValue(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        FileReader reader = null;
        LineNumberReader lineReader = null;
        try {
            reader = new FileReader(file);
            lineReader = new LineNumberReader(reader);
            String line = lineReader.readLine();
            if (line != null) {
                return line.trim();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            try {
                if (lineReader != null) lineReader.close();
                if (reader != null) reader.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        return null;
    }
}

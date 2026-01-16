package top.keepempty.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * GPIO 统一管理器
 */
public class GpioManager {
    private static GpioManager instance;
    private final Map<String, GpioModuleExecutor> executors = new HashMap<>();

    // 假设的 GPIO 节点路径
    public static final String NODE_DOOR = "/sys/class/gpio/gpio1/value";
    public static final String NODE_ALARM = "/sys/class/gpio/gpio2/value";
    public static final String NODE_LIGHT_WHITE = "/sys/class/gpio/gpio3/value";
    public static final String NODE_LIGHT_GREEN = "/sys/class/gpio/gpio4/value";
    public static final String NODE_LIGHT_RED = "/sys/class/gpio/gpio5/value";

    private GpioManager() {
        executors.put("door", new GpioModuleExecutor("DoorThread"));
        executors.put("alarm", new GpioModuleExecutor("AlarmThread"));
        executors.put("light", new GpioModuleExecutor("LightThread"));
        
        for (GpioModuleExecutor executor : executors.values()) {
            executor.start();
        }
    }

    public static synchronized GpioManager getInstance() {
        if (instance == null) instance = new GpioManager();
        return instance;
    }

    // --- 业务接口 ---

    /** 门禁控制：打开 duration 毫秒后关闭，duration=0 则常开 */
    public void controlDoor(long duration) {
        executors.get("door").addTask(new GpioTask(NODE_DOOR, 1, duration, "door"));
    }

    /** 报警器控制 */
    public void controlAlarm(long duration) {
        executors.get("alarm").addTask(new GpioTask(NODE_ALARM, 1, duration, "alarm"));
    }

    /** 灯光控制：开机亮白灯 5s */
    public void lightBootFinish() {
        executors.get("light").addTask(new GpioTask(NODE_LIGHT_WHITE, 1, 5000, "light_white"));
    }

    /** 人脸识别成功：闪两次绿灯 */
    public void faceSuccess() {
        GpioModuleExecutor light = executors.get("light");
        light.addTask(new GpioTask(NODE_LIGHT_GREEN, 1, 200, "light_green"));
        light.addTask(new GpioTask(NODE_LIGHT_GREEN, 0, 200, "light_green"));
        light.addTask(new GpioTask(NODE_LIGHT_GREEN, 1, 200, "light_green"));
    }

    /** 人脸识别失败：闪两次红灯 */
    public void faceFail() {
        GpioModuleExecutor light = executors.get("light");
        light.addTask(new GpioTask(NODE_LIGHT_RED, 1, 200, "light_red"));
        light.addTask(new GpioTask(NODE_LIGHT_RED, 0, 200, "light_red"));
        light.addTask(new GpioTask(NODE_LIGHT_RED, 1, 200, "light_red"));
    }

    /** 火警处理 */
    public void setFireAlarm(boolean active) {
        // 火警时：报警器一直响，门常开
        if (active) {
            executors.get("alarm").setEmergency(true, new GpioTask(NODE_ALARM, 1, 0, "fire_alarm"));
            executors.get("door").setEmergency(true, new GpioTask(NODE_DOOR, 1, 0, "fire_door"));
        } else {
            // 取消火警：报警器关闭，门恢复正常（duration=-1 标识紧急取消任务）
            executors.get("alarm").setEmergency(false, new GpioTask(NODE_ALARM, 0, -1, "fire_cancel"));
            executors.get("door").setEmergency(false, new GpioTask(NODE_DOOR, 0, -1, "fire_cancel"));
        }
    }
}

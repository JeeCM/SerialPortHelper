package top.keepempty.utils;

/**
 * GPIO 任务模型
 */
public class GpioTask {
    public String node;      // GPIO 节点路径
    public int action;       // 1: 开启, 0: 关闭
    public long duration;    // 持续时间 (ms)，0 表示常开
    public String tag;       // 任务标签，用于识别任务类型（如 "door", "alarm"）

    public GpioTask(String node, int action, long duration, String tag) {
        this.node = node;
        this.action = action;
        this.duration = duration;
        this.tag = tag;
    }
}

package top.keepempty.utils;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * GPIO 模块执行器：每个模块一个队列、一个线程
 */
public class GpioModuleExecutor extends Thread {
    private static final String TAG = "GpioExecutor";
    private final BlockingQueue<GpioTask> taskQueue = new LinkedBlockingQueue<>();
    private volatile boolean isEmergency = false;
    private long currentTaskEndTime = 0;

    public GpioModuleExecutor(String name) {
        super(name);
    }

    /**
     * 添加任务
     */
    public void addTask(GpioTask task) {
        if (isEmergency && task.duration != -1) {
            // 紧急模式下，忽略非紧急取消任务
            return;
        }
        
        // 处理“如果门禁或报警器第一次打开还没关闭又收到下一次打开的任务则延长时间关闭”
        if (task.duration > 0 && System.currentTimeMillis() < currentTaskEndTime) {
            Log.d(TAG, "任务叠加，延长关闭时间: " + task.tag);
            currentTaskEndTime += task.duration;
            return;
        }

        taskQueue.offer(task);
    }

    /**
     * 设置紧急状态（火警）
     */
    public void setEmergency(boolean emergency, GpioTask emergencyTask) {
        this.isEmergency = emergency;
        if (emergency) {
            taskQueue.clear(); // 清空普通任务
            taskQueue.offer(emergencyTask);
        } else {
            // 收到火警取消，恢复正常，可以发送一个关闭任务
            taskQueue.offer(emergencyTask);
        }
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                GpioTask task = taskQueue.take();
                executeGpio(task);
                
                if (task.duration > 0) {
                    currentTaskEndTime = System.currentTimeMillis() + task.duration;
                    // 等待期间如果 currentTaskEndTime 被 addTask 动态修改，则继续等待
                    while (System.currentTimeMillis() < currentTaskEndTime) {
                        Thread.sleep(Math.min(100, currentTaskEndTime - System.currentTimeMillis()));
                        if (isEmergency && task.duration != -1) break; // 紧急状态强制跳出
                    }
                    // 自动关闭（除非是紧急状态锁死）
                    if (!isEmergency) {
                        executeGpio(new GpioTask(task.node, 0, 0, task.tag));
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void executeGpio(GpioTask task) {
        Log.d(TAG, "执行 GPIO: " + task.node + " -> " + task.action + " [" + task.tag + "]");
        try (FileOutputStream fos = new FileOutputStream(task.node)) {
            fos.write(String.valueOf(task.action).getBytes());
        } catch (IOException e) {
            Log.e(TAG, "GPIO 写入失败: " + task.node, e);
        }
    }
}

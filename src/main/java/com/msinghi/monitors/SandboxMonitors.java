package com.msinghi.monitors;

import java.util.concurrent.atomic.AtomicLong;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.Monitors;

public enum SandboxMonitors {

    HELLO_ALLOWED("helloAllowedCounter", "Number of calls to helloworld"),
    
    HELLO_DENIED("helloDeniedCounter", "Number of calls to helloworld"),
    
    HELLO("helloWorldCounter", "Number of calls to helloworld");

    private final String name;

    private final String description;

    private SandboxMonitors(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @com.netflix.servo.annotations.Monitor(name = "count", type = DataSourceType.COUNTER)
    private final AtomicLong counter = new AtomicLong();

    public void increment() {
        counter.incrementAndGet();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCount() {
        return counter.get();
    }
    
    public static void registerAllStats() {
        for (SandboxMonitors c : SandboxMonitors.values()) {
            Monitors.registerObject(c.getName(), c);
        }
    }

    public static void shutdown() {
        for (SandboxMonitors c : SandboxMonitors.values()) {
            DefaultMonitorRegistry.getInstance().unregister(
                    Monitors.newObjectMonitor(c.name(), c));
        }
    }

}

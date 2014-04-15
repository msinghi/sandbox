package com.msinghi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.msinghi.monitors.SandboxMonitors;
import com.msinghi.resources.HelloWorldResource;
import com.netflix.servo.publish.AsyncMetricObserver;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.JmxMetricPoller;
import com.netflix.servo.publish.JvmMetricPoller;
import com.netflix.servo.publish.LocalJmxConnector;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.graphite.GraphiteMetricObserver;

public class Bootstrap extends ResourceConfig {

    /**
     * Register JAX-RS application components.
     * 
     * @throws Exception
     */
    public Bootstrap() throws Exception {
        register(HelloWorldResource.class);

        register(JacksonFeature.class);

        SandboxMonitors.registerAllStats();
        initMetricsPublishing();
        SandboxMonitors.registerAllStats();

    }

    private static MetricObserver rateTransform(MetricObserver observer) {
        final long heartbeat = 2 * Config.getPollInterval();
        return new CounterToRateMetricTransform(observer, heartbeat, TimeUnit.SECONDS);
    }

    private static MetricObserver async(String name, MetricObserver observer) {
        final long expireTime = 2000 * Config.getPollInterval();
        final int queueSize = 10;
        return new AsyncMetricObserver(name, observer, queueSize, expireTime);
    }

    private static MetricObserver createGraphiteObserver() {
        final String prefix = Config.getGraphiteObserverPrefix();
        final String addr = Config.getGraphiteObserverAddress();
        return rateTransform(async("graphite", new GraphiteMetricObserver(prefix, addr)));
    }

    private static void schedule(MetricPoller poller, List<MetricObserver> observers) {
        final PollRunnable task = new PollRunnable(poller, BasicMetricFilter.MATCH_ALL, observers);
        PollScheduler.getInstance().addPoller(task, Config.getPollInterval(), TimeUnit.SECONDS);
    }

    private static void initMetricsPublishing() throws Exception {
        final List<MetricObserver> observers = new ArrayList<MetricObserver>();

        observers.add(createGraphiteObserver());

        /*
         * if (Config.isGraphiteObserverEnabled()) {
         * observers.add(createGraphiteObserver()); }
         */

        PollScheduler.getInstance().start();
        schedule(new MonitorRegistryMetricPoller(), observers);
        schedule(new JvmMetricPoller(), observers);
        schedule(new JmxMetricPoller(new LocalJmxConnector(), new ObjectName("com.netflix.servo:type=*,*"),
                BasicMetricFilter.MATCH_ALL), observers);

        /*
         * if (Config.isJvmPollerEnabled()) { schedule(new JvmMetricPoller(),
         * observers); }
         */
    }
}

package com.teragrep.rlp_09;

import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import sun.misc.Signal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    private static String target;
    private static int port;
    private static byte[] message;
    private static final AtomicLong messagesSent = new AtomicLong();
    private static RelpThread[] relpThreads;
    public static void main(String[] args) {
        Instant start = Instant.now();
        String hostname = System.getProperty("hostname", "localhost");
        String appname = System.getProperty("appname", "rlp_09");
        target = System.getProperty("target", "127.0.0.1");
        port = Integer.parseInt(System.getProperty("port", "1601"));
        int threads = Integer.parseInt(System.getProperty("threads", "4"));
        boolean useTls = Boolean.parseBoolean(System.getProperty("useTls", "false"));
        int messageSize = Integer.parseInt(System.getProperty("messageSize", "10"));
        relpThreads = new RelpThread[threads];
        message = new SyslogMessage()
                .withTimestamp(Instant.now().toEpochMilli())
                .withAppName(appname)
                .withHostname(hostname)
                .withFacility(Facility.USER)
                .withSeverity(Severity.INFORMATIONAL)
                .withMsg("X".repeat(messageSize))
                .toRfc5424SyslogMessage()
                .getBytes(StandardCharsets.UTF_8);
        System.out.printf("Using hostname <[%s]>%n", hostname);
        System.out.printf("Using appname <[%s]>%n", appname);
        System.out.printf("Adding <[%s]> characters to payload size%n", messageSize);
        System.out.printf("Sending messages to: <[%s]:[%s]>%n", target, port);
        System.out.printf("Using <[%s]> threads%n", threads);
        System.out.printf("TLS enabled (FIXME: Implement): <[%s]>%n", useTls);
        for(int i = 0; i< threads; i++) {
            RelpThread relpThread = new RelpThread();
            relpThread.setName("RelpThread #" + i);
            Thread thread = new Thread(relpThread);
            thread.start();
            relpThreads[i] = relpThread;
        }
        new Timer().scheduleAtFixedRate(new TimerTask(){
            long last = 0;
            long lastTime = Instant.now().toEpochMilli();
            @Override
            public void run() {
                long sent = messagesSent.get();
                long currentTime = Instant.now().toEpochMilli();
                float elapsed = (float) (currentTime - start.toEpochMilli()) /1000;
                System.out.printf("Sent total of <%s> messages in <%,.0f> seconds, <%s> in past %,.1fs seconds (<%.0f> EPS)%n",
                        sent,
                        elapsed,
                        sent-last,
                        (float) (currentTime-lastTime)/1000,
                        (sent-last)/((float)(currentTime-lastTime)/1000)
                );
                last = sent;
                lastTime = currentTime;
            }
        },10000,10000);
        Thread shutdownHook = new Thread(() -> {
            System.out.println("Detected ctrl-c, shutting down threads");
            for(RelpThread thread : relpThreads) {
                System.out.println("Shutting down thread " + thread.getName());
                thread.shutdown();
            }
            float elapsed = (float) (Instant.now().toEpochMilli() - start.toEpochMilli())/1000;
            System.out.printf("Sent <%s> messages in <%,.2f> seconds (%.0f EPS)%n", messagesSent, elapsed, messagesSent.get() / (elapsed == 0 ? 1 : elapsed));
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    static class RelpThread implements Runnable {
        private boolean stayRunning = true;
        private String name;
        @Override
        public void run() {
            System.out.printf("[%s] Starting%n", name);
            RelpConnection relpConnection = new RelpConnection();
            try {
                relpConnection.connect(target, port);
            } catch (IOException | TimeoutException e) {
                System.out.println("Failed to connect to the server");
                System.exit(1);
                throw new RuntimeException(e);
            }
            while(stayRunning) {
                RelpBatch relpBatch = new RelpBatch();
                relpBatch.insert(message);
                boolean notSent = true;
                while (notSent) {
                    messagesSent.incrementAndGet();
                    try {
                        relpConnection.commit(relpBatch);
                    } catch (IOException | TimeoutException e) {
                        System.out.printf("[%s] Failed to commit%n", name);
                    }

                    if (!relpBatch.verifyTransactionAll()) {
                        System.out.printf("[%s] Failed to send message%n", name);
                        relpBatch.retryAllFailed();
                        relpConnection.tearDown();
                        try {
                            relpConnection.connect(target, port);
                        } catch (IOException | TimeoutException e) {
                            System.out.printf("[%s] Failed to connect to the server%n", name);
                            System.exit(1);
                        }
                    } else {
                        notSent = false;
                    }
                }
            }
            System.out.printf("[%s] Exiting%n", name);
        }
        public void shutdown() {
            stayRunning = false;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

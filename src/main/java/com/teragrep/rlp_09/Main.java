package com.teragrep.rlp_09;

import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    private static final AtomicLong messagesSent = new AtomicLong();
    private static final HashMap<Thread, RelpThread> relpThreads = new HashMap<>();
    private static final long startTime = Instant.now().toEpochMilli();
    private static long lastReportEventsSent = 0;
    private static long lastReportTime = Instant.now().toEpochMilli();
    private static long messageLength;
    public static void main(String[] args) {;
        String hostname = System.getProperty("hostname", "localhost");
        String appname = System.getProperty("appname", "rlp_09");
        String target = System.getProperty("target", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("port", "1601"));
        int threads = Integer.parseInt(System.getProperty("threads", "4"));
        boolean useTls = Boolean.parseBoolean(System.getProperty("useTls", "false"));
        int payloadSize = Integer.parseInt(System.getProperty("payloadSize", "10"));
        byte[] message = new SyslogMessage()
                .withTimestamp(Instant.now().toEpochMilli())
                .withAppName(appname)
                .withHostname(hostname)
                .withFacility(Facility.USER)
                .withSeverity(Severity.INFORMATIONAL)
                .withMsg("X".repeat(payloadSize))
                .toRfc5424SyslogMessage()
                .getBytes(StandardCharsets.UTF_8);
        messageLength = message.length;
        System.out.printf("Using hostname <[%s]>%n", hostname);
        System.out.printf("Using appname <[%s]>%n", appname);
        System.out.printf("Adding <[%s]> characters to payload size making total event size <%s>%n", payloadSize, messageLength);
        System.out.printf("Sending messages to: <[%s]:[%s]>%n", target, port);
        System.out.printf("Using <[%s]> threads%n", threads);
        System.out.printf("TLS enabled (FIXME: Implement): <[%s]>%n", useTls);
        for(int i = 0; i< threads; i++) {
            RelpThread relpThread = new RelpThread("RelpThread #" + i, target, port, message, messagesSent);
            Thread thread = new Thread(relpThread);
            thread.start();
            relpThreads.put(thread, relpThread);
        }
        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run() {
                printEps();
            }
        },10000,10000);
        Thread shutdownHook = new Thread(() -> {
            System.out.println("Shutting down...");
            relpThreads.forEach((thread, relpThread) -> {
                relpThread.shutdown();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            printEps();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void printEps() {
        long totalSent = messagesSent.get();
        long deltaSent = totalSent-lastReportEventsSent;
        long currentTime = Instant.now().toEpochMilli();
        float totalElapsed = (float) (currentTime - startTime)/1000;
        float deltaElapsed = (float) (currentTime - lastReportTime)/1000;
        System.out.format(
                "Sent %,d messages in %,.1f seconds (%,.0f EPS / ~%,.2f MB/s), total messages sent %,1d in %,.1f seconds (%,.0f EPS / ~%,.2f MB/s)%n",
                deltaSent,
                deltaElapsed,
                deltaSent/deltaElapsed,
                ((deltaSent*messageLength)/deltaElapsed)/1024/1024,
                totalSent,
                totalElapsed,
                totalSent/totalElapsed,
                ((totalSent*messageLength)/totalElapsed)/1024/1024
        );
        lastReportEventsSent = totalSent;
        lastReportTime = currentTime;
    }
}

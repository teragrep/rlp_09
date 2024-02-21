/*
 * Teragrep RELP Flooder RLP_09
 * Copyright (C) 2024  Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://github.com/teragrep/teragrep/blob/main/LICENSE>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */

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
        int batchSize = Integer.parseInt(System.getProperty("batchSize", "1"));
        if(batchSize <= 0 || batchSize > 4096) {
            System.err.println("Batch size must be between 1 and 4096");
            System.exit(1);
        }
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
        System.out.printf("Sending <[%s]> messages per batch%n", batchSize);
        System.out.printf("Sending messages to: <[%s]:[%s]>%n", target, port);
        System.out.printf("TLS enabled (FIXME: Implement): <[%s]>%n", useTls);

        System.out.printf("Starting <[%s]> threads%n", threads);
        for(int i = 0; i< threads; i++) {
            RelpThread relpThread = new RelpThread("RelpThread #" + i, target, port, message, messagesSent, batchSize);
            Thread thread = new Thread(relpThread);
            thread.start();
            relpThreads.put(thread, relpThread);
        }
        Timer statsReporter = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                printEps();
            }
        };
        statsReporter.scheduleAtFixedRate(timerTask, 10000, 10000);
        Thread shutdownHook = new Thread(() -> {
            timerTask.cancel();
            System.out.println("Shutting down threads in parallel...");
            relpThreads.entrySet().parallelStream().forEach((entry) -> {
                entry.getValue().shutdown();
                try {
                    entry.getKey().join(1000);
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
        float totalBytes = (float) totalSent*messageLength;
        float deltaBytes = (float) deltaSent*messageLength;
        long currentTime = Instant.now().toEpochMilli();
        float totalElapsed = (float) (currentTime - startTime)/1000;
        float deltaElapsed = (float) (currentTime - lastReportTime)/1000;
        System.out.format(
                "Sent %,d messages / %,.2f MB in %,.1f seconds (%,.0f EPS / ~%,.2f MB/s), total sent %,1d messages / %,.1f MB in %,.1f seconds (%,.0f EPS / ~%,.2f MB/s)%n",
                deltaSent,
                deltaBytes/1024/1024,
                deltaElapsed,
                deltaSent/deltaElapsed,
                (deltaBytes/deltaElapsed)/1024/1024,
                totalSent,
                totalBytes/1024/1024,
                totalElapsed,
                totalSent/totalElapsed,
                (totalBytes/totalElapsed)/1024/1024
        );
        lastReportEventsSent = totalSent;
        lastReportTime = currentTime;
    }
}

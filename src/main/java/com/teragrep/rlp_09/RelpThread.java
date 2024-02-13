package com.teragrep.rlp_09;

import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

class RelpThread implements Runnable {
    private boolean stayRunning = true;
    private final String name;
    private final String target;
    private final int port;
    private final byte[] message;
    private final AtomicLong messagesSent;
    RelpThread(String name, String target, int port, byte[] message, AtomicLong messagesSent) {
        this.name = name;
        this.target = target;
        this.port = port;
        this.message = message;
        this.messagesSent = messagesSent;
    }
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
}
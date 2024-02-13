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
    private RelpConnection relpConnection;
    private int batchSize;
    RelpThread(String name, String target, int port, byte[] message, AtomicLong messagesSent, int batchSize) {
        this.name = name;
        this.target = target;
        this.port = port;
        this.message = message;
        this.messagesSent = messagesSent;
        this.batchSize = batchSize;
    }
    @Override
    public void run() {
        relpConnection = new RelpConnection();
        connect();
        while(stayRunning) {
            RelpBatch relpBatch = new RelpBatch();
            for(int i=1; i<=batchSize; i++) {
                relpBatch.insert(message);
            }
            boolean notSent = true;
            while (notSent) {
                try {
                    relpConnection.commit(relpBatch);
                } catch (IllegalStateException e) {
                    System.out.printf("[%s] Failed to send message: %s", name, e.getMessage());
                    System.exit(1);
                } catch (IOException | TimeoutException e) {
                    System.out.printf("[%s] Failed to commit: %s%n", name, e.getMessage());
                    System.exit(1);
                }
                if (!relpBatch.verifyTransactionAll()) {
                    System.out.printf("[%s] Failed to send message%n", name);
                    relpBatch.retryAllFailed();
                    relpConnection.tearDown();
                } else {
                    notSent = false;
                }
            }
            messagesSent.addAndGet(batchSize);
        }
    }

    public void connect() {
        try {
            relpConnection.connect(target, port);
        } catch (IOException | TimeoutException e) {
            System.out.printf("[%s] Failed to connect to the server%n", name);
            System.exit(1);
        }
    }
    public void shutdown() {
        stayRunning = false;
    }
}
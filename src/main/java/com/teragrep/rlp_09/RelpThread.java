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

/*
 * Teragrep RELP Flooder Library RLP_09
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
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class RelpFlooderTask implements Callable<Object> {
    private RelpConnection relpConnection = new RelpConnection();
    private int recordsSent = 0;
    private int bytesSent = 0;
    private boolean stayRunning = true;
    private final RelpFlooderConfig relpFlooderConfig;
    private final int threadId;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Iterator<byte[]> iterator;
    RelpFlooderTask(int threadId, RelpFlooderConfig relpFlooderConfig, Iterator<byte[]> iterator) throws RuntimeException {
        this.threadId = threadId;
        this.iterator = iterator;
        this.relpFlooderConfig = relpFlooderConfig;
    }

    @Override
    public Object call() {
        relpConnection = new RelpConnection();
        connect();
        while (stayRunning && iterator.hasNext()) {
            byte[] message = iterator.next();
            RelpBatch relpBatch = new RelpBatch();
            relpBatch.insert(message);
            try {
                relpConnection.commit(relpBatch);
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException("Can't commit batch: " + e.getMessage());
            }
            if (!relpBatch.verifyTransactionAll()) {
                throw new RuntimeException("Can't verify transactions");
            }
            recordsSent++;
            bytesSent += message.length;
        }
        disconnect();
        latch.countDown();
        return null;
    }

    public void connect() {
        try {
            relpConnection.connect(relpFlooderConfig.getTarget(), relpFlooderConfig.getPort());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Can't connect properly: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            relpConnection.disconnect();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Can't disconnect properly: ", e);
        }
    }

    public int getRecordsSent() {
        return recordsSent;
    }
    public int getBytesSent() {
        return bytesSent;
    }
    public int getThreadId() {
        return threadId;
    }
    public void stop()  {
        stayRunning=false;
        try {
            if(!latch.await(5L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out waiting for thread to shut down");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

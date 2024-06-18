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

import com.teragrep.rlp_03.channel.socket.PlainFactory;
import com.teragrep.rlp_03.channel.context.ConnectContextFactory;
import com.teragrep.rlp_03.channel.socket.SocketFactory;
import com.teragrep.rlp_03.client.ClientFactory;
import com.teragrep.rlp_03.eventloop.EventLoop;
import com.teragrep.rlp_03.eventloop.EventLoopFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RelpFlooder {
    private final ExecutorService executorService;
    List<RelpFlooderTask> relpFlooderTaskList = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(RelpFlooder.class);
    public HashMap<Integer, Long> getRecordsSentPerThread() {
        HashMap<Integer, Long> recordsSent = new HashMap<>();
        for(RelpFlooderTask relpFlooderTask : relpFlooderTaskList){
            recordsSent.put(relpFlooderTask.getThreadId(), relpFlooderTask.getRecordsSent());
        }
        return recordsSent;
    }

    public long getTotalRecordsSent() {
        long totalrecordsSent = 0;
        for(RelpFlooderTask relpFlooderTask : relpFlooderTaskList){
            totalrecordsSent += relpFlooderTask.getRecordsSent();
        }
        return totalrecordsSent;
    }

    public HashMap<Integer, Long> getTotalBytesSentPerThread() {
        HashMap<Integer, Long> bytesSent = new HashMap<>();
        for(RelpFlooderTask relpFlooderTask : relpFlooderTaskList){
            bytesSent.put(relpFlooderTask.getThreadId(), relpFlooderTask.getBytesSent());
        }
        return bytesSent;
    }

    public long getTotalBytesSent() {
        long totalBytesSent = 0;
        for(RelpFlooderTask relpFlooderTask : relpFlooderTaskList){
            totalBytesSent += relpFlooderTask.getBytesSent();
        }
        return totalBytesSent;
    }

    private final RelpFlooderConfig relpFlooderConfig;
    private final RelpFlooderIteratorFactory iteratorFactory;
    public RelpFlooder() {
        this(new RelpFlooderConfig(), new ExampleRelpFlooderIteratorFactory());
    }

    public RelpFlooder(RelpFlooderConfig relpFlooderConfig){
        this(relpFlooderConfig, new ExampleRelpFlooderIteratorFactory());
    }

    public RelpFlooder(RelpFlooderConfig relpFlooderConfig, RelpFlooderIteratorFactory iteratorFactory) {
        this.relpFlooderConfig = relpFlooderConfig;
        this.iteratorFactory = iteratorFactory;
        this.executorService = Executors.newFixedThreadPool(relpFlooderConfig.threads);
    }

    public void start() {
        LOGGER.trace("Creating a PlainFactory");
        SocketFactory socketFactory = new PlainFactory();
        LOGGER.trace("Creating ConnectContextFactory");
        ConnectContextFactory connectContextFactory = new ConnectContextFactory(Executors.newFixedThreadPool(relpFlooderConfig.threads), socketFactory);
        LOGGER.trace("Creating EventLoopfactory");
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        EventLoop eventLoop;
        try {
            LOGGER.trace("Creating EventLoop from EventLoopFactory");
            eventLoop = eventLoopFactory.create();
        } catch (IOException e) {
            throw new RuntimeException("Can't create EventLoop: " + e.getMessage());
        }
        LOGGER.trace("Creating EventLoopThread");
        Thread eventLoopThread = new Thread(eventLoop);
        LOGGER.trace("Starting EventLoopThread");
        eventLoopThread.start();
        LOGGER.trace("Creating ClientFactory");
        ClientFactory clientFactory = new ClientFactory(connectContextFactory, eventLoop);

        for (int i=0; i<relpFlooderConfig.threads; i++) {
            LOGGER.trace("Creating RelpFlooderTask number <{}>", i);
            RelpFlooderTask relpFlooderTask = new RelpFlooderTask(i, relpFlooderConfig, iteratorFactory.get(i), clientFactory);
            relpFlooderTaskList.add(relpFlooderTask);
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got <{}> tasks in RelpFlooderTaskList: <{}>", relpFlooderTaskList.size(), relpFlooderTaskList);
        }
        try {
            LOGGER.trace("Invoking all RelpFlooderTaskList tasks");
            List<Future<Object>> futures = executorService.invokeAll(relpFlooderTaskList);
            for(Future<Object> future : futures) {
                LOGGER.trace("Waiting for future <{}>", future);
                future.get();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOGGER.trace("Stopping EventLoop");
        eventLoop.stop();
        try {
            LOGGER.trace("Joining EventLoopThread");
            eventLoopThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOGGER.trace("Shutting down ExecutorService");
        executorService.shutdown();
    }

    public void stop() {
        LOGGER.trace("Entering stop(), running RelpFlooderTask::stop");
        relpFlooderTaskList.parallelStream().forEach(RelpFlooderTask::stop);
        LOGGER.trace("Exiting stop()");
    }
}

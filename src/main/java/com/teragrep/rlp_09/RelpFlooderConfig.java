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

import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.time.Instant;

public class RelpFlooderConfig {
    private String hostname = "localhost";
    private String appname = "rlp_09";
    private String target = "127.0.0.1";
    private int port = 601;
    private int threads = 1;
    private boolean useTls = false;
    private int payloadSize = 10;
    private int batchSize = 1;
    private byte[] message;
    private int messageLength;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
        updateMessage();
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
        updateMessage();
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        if(payloadSize < 0) {
            throw new InvalidParameterException("Payload size must be a positive number");
        }
        this.payloadSize = payloadSize;
        updateMessage();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if(batchSize <= 0 || batchSize > 4096) {
            throw new RuntimeException("Batch size must be between 1 and 4096");
        }
        this.batchSize = batchSize;
    }

    public byte[] getMessage() {
        return message;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public RelpFlooderConfig() {
        updateMessage();
    }

    public RelpFlooderConfig(String hostname, String appname, String target, int port, int threads, boolean useTls, int payloadSize, int batchSize) {
        setHostname(hostname);
        setAppname(appname);
        setTarget(target);
        setPort(port);
        setThreads(threads);
        setUseTls(useTls);
        setPayloadSize(payloadSize);
        setBatchSize(batchSize);
        updateMessage();
    }

    private void updateMessage() {
        this.message = new SyslogMessage()
                .withTimestamp(Instant.now().toEpochMilli())
                .withAppName(this.appname)
                .withHostname(this.hostname)
                .withFacility(Facility.USER)
                .withSeverity(Severity.INFORMATIONAL)
                .withMsg(new String(new char[this.payloadSize]).replace("\0", "X"))
                .toRfc5424SyslogMessage()
                .getBytes(StandardCharsets.UTF_8);
        this.messageLength = message.length;
    }
}

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

class RelpConfig {
    final String hostname;
    final String appname;
    final String target;
    final int port;
    final int threads;
    final boolean useTls;
    final int payloadSize;
    final int batchSize;
    final byte[] message;
    final int messageLength;
    RelpConfig() {
        this.hostname = System.getProperty("hostname", "localhost");
        this.appname = System.getProperty("appname", "rlp_09");
        this.target = System.getProperty("target", "127.0.0.1");
        this.port = Integer.parseInt(System.getProperty("port", "1601"));
        this.threads = Integer.parseInt(System.getProperty("threads", "4"));
        this.useTls = Boolean.parseBoolean(System.getProperty("useTls", "false"));
        this.payloadSize = Integer.parseInt(System.getProperty("payloadSize", "10"));
        this.batchSize = Integer.parseInt(System.getProperty("batchSize", "1"));
        if(this.batchSize <= 0 || this.batchSize > 4096) {
            System.err.println("Batch size must be between 1 and 4096");
            System.exit(1);
        }
        this.message = new SyslogMessage()
            .withTimestamp(Instant.now().toEpochMilli())
            .withAppName(this.appname)
            .withHostname(this.hostname)
            .withFacility(Facility.USER)
            .withSeverity(Severity.INFORMATIONAL)
            .withMsg("X".repeat(this.payloadSize))
            .toRfc5424SyslogMessage()
            .getBytes(StandardCharsets.UTF_8);
        this.messageLength = message.length;
    }
}

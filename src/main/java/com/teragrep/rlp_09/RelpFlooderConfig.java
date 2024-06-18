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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelpFlooderConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelpFlooderConfig.class);
    public final String target;
    public final int port;
    public final int threads;
    public final int connectTimeout;
    public final boolean waitForAcks;

    public RelpFlooderConfig() {
        this("127.0.0.1", 601);
    }

    public RelpFlooderConfig(String target, int port) {
        this(target, port, 1, 1, true);
    }

    public RelpFlooderConfig(String target, int port, int threads, int connectTimeout, boolean waitForAcks) {
        this.target = target;
        this.port = port;
        this.threads = threads;
        this.connectTimeout = connectTimeout;
        this.waitForAcks = waitForAcks;
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Got RelpFlooderConfig: <{}>", this);
        }
    }

    @Override
    public String toString() {
        return "RelpFlooderConfig{" +
                "target='" + target + '\'' +
                ", port=" + port +
                ", threads=" + threads +
                ", connectTimeout=" + connectTimeout +
                ", waitForAcks=" + waitForAcks +
                '}';
    }
}

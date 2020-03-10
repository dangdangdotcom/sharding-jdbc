/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.database.protocol.mysql.packet.handshake;

import com.google.common.base.Preconditions;
import lombok.Getter;

import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLAuthenticationMethod;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLCapabilityFlag;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLServerInfo;
import org.apache.shardingsphere.database.protocol.mysql.constant.MySQLStatusFlag;
import org.apache.shardingsphere.database.protocol.mysql.packet.MySQLPacket;
import org.apache.shardingsphere.database.protocol.mysql.payload.MySQLPacketPayload;

/**
 * Handshake packet protocol for MySQL.
 * 
 * @see <a href="https://dev.mysql.com/doc/internals/en/connection-phase-packets.html#packet-Protocol::Handshake">Handshake</a>
 */
@Getter
public final class MySQLHandshakePacket implements MySQLPacket {
    
    private final int protocolVersion = MySQLServerInfo.PROTOCOL_VERSION;
    
    private final String serverVersion;
    
    private final int connectionId;
    
    private final int capabilityFlagsLower;
    
    private final int characterSet;
    
    private final MySQLStatusFlag statusFlag;
    
    private final MySQLAuthPluginData authPluginData;
    
    private int capabilityFlagsUpper;
    
    private String authPluginName;
    
    public MySQLHandshakePacket(final int connectionId, final MySQLAuthPluginData authPluginData) {
        this.serverVersion = MySQLServerInfo.SERVER_VERSION;
        this.connectionId = connectionId;
        this.capabilityFlagsLower = MySQLCapabilityFlag.calculateHandshakeCapabilityFlagsLower();
        this.characterSet = MySQLServerInfo.CHARSET;
        this.statusFlag = MySQLStatusFlag.SERVER_STATUS_AUTOCOMMIT;
        this.capabilityFlagsUpper = MySQLCapabilityFlag.calculateHandshakeCapabilityFlagsUpper();
        this.authPluginData = authPluginData;
        this.authPluginName = null;
    }
    
    public MySQLHandshakePacket(final MySQLPacketPayload payload) {
        Preconditions.checkArgument(0 == payload.readInt1(), "Sequence ID of MySQL handshake packet must be `0`.");
        Preconditions.checkArgument(protocolVersion == payload.readInt1());
        serverVersion = payload.readStringNul();
        connectionId = payload.readInt4();
        final byte[] authPluginDataPart1 = payload.readStringNulByBytes();
        capabilityFlagsLower = payload.readInt2();
        characterSet = payload.readInt1();
        statusFlag = MySQLStatusFlag.valueOf(payload.readInt2());
        capabilityFlagsUpper = payload.readInt2();
        payload.readInt1();
        payload.skipReserved(10);
        authPluginData = new MySQLAuthPluginData(authPluginDataPart1, readAuthPluginDataPart2(payload));
        authPluginName = readAuthPluginName(payload);
    }
    
    /**
     * There are some different between implement of handshake initialization packet and document.
     * In source code of 5.7 version, authPluginDataPart2 should be at least 12 bytes,
     * and then follow a nul byte.
     * But in document, authPluginDataPart2 is at least 13 bytes, and not nul byte.
     * From test, the 13th byte is nul byte and should be excluded from authPluginDataPart2.
     *
     * @param payload MySQL packet payload
     */
    private byte[] readAuthPluginDataPart2(final MySQLPacketPayload payload) {
        return isClientSecureConnection() ? payload.readStringNulByBytes() : new byte[0];
    }
    
    private String readAuthPluginName(final MySQLPacketPayload payload) {
        return isClientPluginAuth() ? payload.readStringNul() : null;
    }
    
    /**
     * Set auth plugin name.
     *
     * @param mySQLAuthenticationMethod MySQL authentication method
     */
    public void setAuthPluginName(final MySQLAuthenticationMethod mySQLAuthenticationMethod) {
        this.authPluginName = mySQLAuthenticationMethod.getMethodName();
        capabilityFlagsUpper |= MySQLCapabilityFlag.CLIENT_PLUGIN_AUTH.getValue() >> 16;
    }
    
    @Override
    public void write(final MySQLPacketPayload payload) {
        payload.writeInt1(protocolVersion);
        payload.writeStringNul(serverVersion);
        payload.writeInt4(connectionId);
        payload.writeStringNul(new String(authPluginData.getAuthPluginDataPart1()));
        payload.writeInt2(capabilityFlagsLower);
        payload.writeInt1(characterSet);
        payload.writeInt2(statusFlag.getValue());
        payload.writeInt2(capabilityFlagsUpper);
        payload.writeInt1(isClientPluginAuth() ? authPluginData.getAuthPluginData().length : 0);
        payload.writeReserved(10);
        writeAuthPluginDataPart2(payload);
        writeAuthPluginName(payload);
    }
    
    private void writeAuthPluginDataPart2(final MySQLPacketPayload payload) {
        if (isClientSecureConnection()) {
            payload.writeStringNul(new String(authPluginData.getAuthPluginDataPart2()));
        }
    }
    
    private void writeAuthPluginName(final MySQLPacketPayload payload) {
        if (isClientPluginAuth()) {
            payload.writeStringNul(authPluginName);
        }
    }
    
    private boolean isClientSecureConnection () {
        return 0 != (capabilityFlagsLower & MySQLCapabilityFlag.CLIENT_SECURE_CONNECTION.getValue());
    }
    
    private boolean isClientPluginAuth() {
        return 0 != ((capabilityFlagsUpper << 16) & MySQLCapabilityFlag.CLIENT_PLUGIN_AUTH.getValue());
    }
    
    @Override
    public int getSequenceId() {
        return 0;
    }
}

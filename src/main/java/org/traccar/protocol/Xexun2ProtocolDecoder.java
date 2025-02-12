package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.NetworkMessage;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.Checksum;
import org.traccar.helper.DataConverter;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.model.WifiAccessPoint;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.Date;

public class Xexun2ProtocolDecoder extends BaseProtocolDecoder {
    public static final int FLAG = 0xfaaf;
    public static final int MSG_GPS = 0x00;
    public static final int MSG_WIFI = 0x01;
    public static final int MSG_LBS = 0x02;
    public static final int MSG_ALARM = 0x04;
    public static final int MSG_DEVICE_STATUS = 0x06;
    public static final int MSG_COMMAND = 0x07;
    public static final int MSG_VERSION = 0x14;

    public Xexun2ProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private void sendResponse(Channel channel, int type, int index, ByteBuf imei) {
        if (channel != null) {
            ByteBuf response = Unpooled.buffer();
            response.writeShort(FLAG);
            response.writeShort(type);
            response.writeShort(index);
            response.writeBytes(imei);
            response.writeShort(1); // attributes / length
            response.writeShort(Checksum.ip(Unpooled.wrappedBuffer(DataConverter.parseHex("2")).nioBuffer())); // checksum
            response.writeByte(2); // response
            response.writeShort(FLAG);
            channel.writeAndFlush(new NetworkMessage(response, channel.remoteAddress()));
        }
    }

    private String decodeAlarm(long value) {
        if (BitUtil.check(value, 0)) {
            return Position.ALARM_SOS;
        }
        if (BitUtil.check(value, 9)) {
            return Position.ALARM_REMOVING;
        }
        if (BitUtil.check(value, 23)) {
            return Position.ALARM_FALL_DOWN;
        }
        if (BitUtil.check(value, 25)) {
            return Position.ALARM_DOOR;
        }
        return null;
    }

    private void decodeGps(Position position, ByteBuf buf) {
        position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));
        position.setLatitude(buf.readFloat());
        position.setLongitude(buf.readFloat());
        position.setAltitude(buf.readFloat());
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort() * 0.1));
        position.setCourse(buf.readUnsignedShort() * 0.1);
    }

    private void decodeWifi(Position position, ByteBuf buf) {
        Network network = new Network();
        int count = buf.readUnsignedByte();
        for (int i = 0; i < count; i++) {
            String mac = ByteBufUtil.hexDump(buf.readSlice(6)).replaceAll("(..)", "$1:");
            int signal = buf.readUnsignedByte();
            network.addWifiAccessPoint(WifiAccessPoint.from(mac, signal));
        }
        position.setNetwork(network);
    }

    private void decodeLbs(Position position, ByteBuf buf) {
        int mcc = buf.readUnsignedShort();
        int mnc = buf.readUnsignedShort();
        int lac = buf.readInt();
        int cid = buf.readInt();
        int rssi = buf.readUnsignedByte();
        Network network = new Network(CellTower.from(mcc, mnc, lac, cid, rssi));
        position.setNetwork(network);
    }

    private void decodeAlarm(Position position, ByteBuf buf) {
        long timestamp = buf.readUnsignedInt() * 1000L;
        long alarmType = buf.readUnsignedInt();
        position.setTime(new Date(timestamp));
        position.set(Position.KEY_ALARM, decodeAlarm(alarmType));

    }

    private void decodeStatus(Position position, ByteBuf buf) {
        position.set(Position.KEY_RSSI, buf.readUnsignedByte());
        position.set(Position.KEY_BATTERY, buf.readUnsignedByte());
        position.set(Position.KEY_STATUS, buf.readUnsignedByte());
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(2); // flag

        int type = buf.readUnsignedShort();
        int index = buf.readUnsignedShort();

        ByteBuf imei = buf.readSlice(8);
        DeviceSession deviceSession = getDeviceSession(
                channel, remoteAddress, ByteBufUtil.hexDump(imei).substring(0, 15));
        if (deviceSession == null) {
            return null;
        }

        int payloadSize = buf.readUnsignedShort() & 0x03ff;
        int checksum = buf.readUnsignedShort();

        if (checksum != Checksum.ip(buf.nioBuffer(buf.readerIndex(), payloadSize))) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());
        position.setDeviceId(deviceSession.getDeviceId());

        switch (type) {
            case MSG_GPS:
                decodeGps(position, buf);
            case MSG_WIFI:
                getLastLocation(position, null);
                decodeWifi(position, buf);
                break;
            case MSG_LBS:
                getLastLocation(position, null);
                decodeLbs(position, buf);
                break;
            case MSG_ALARM:
                getLastLocation(position, null);
                decodeAlarm(position, buf);
                break;
            case MSG_DEVICE_STATUS:
                getLastLocation(position, null);
                decodeStatus(position, buf);
                break;
            case MSG_VERSION:
                sendResponse(channel, type, index, imei);
                break;
        }

        return position;
    }
}

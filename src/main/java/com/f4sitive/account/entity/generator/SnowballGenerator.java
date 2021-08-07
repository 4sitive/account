package com.f4sitive.account.entity.generator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.util.Base64Utils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SnowballGenerator implements IdentifierGenerator {
    private static final int UNUSED_BITS = 1;
    private static final int TIMESTAMP_BITS = 41;
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12; //long sequenceBits = 12L;

    private static final long maxInstance = (1L << INSTANCE_BITS) - 1; // -1L ^ (-1L << INSTANCE_BITS);
    private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;// -1L ^ (-1L << SEQUENCE_BITS);

    private static final long DEFAULT_EPOCH = 1420070400000L; //twepoch = 1288834974657L;

    private final long instance;
    private final long epoch;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowballGenerator() throws SocketException, UnknownHostException {
//        LocalDate.of()
        System.out.println("maxInstance" +  maxInstance);
        long instance = instance();
        if (instance < 0 || instance > maxInstance) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, maxInstance));
        }
        System.out.println(instance);
//        this.instance = instance;
        this.instance = instance & maxInstance | 1L;
//        this.epoch = DEFAULT_EPOCH;
        System.out.println("instance" + this.instance);
        this.epoch = 0L;
//        this.epoch = LocalDate.of(2020, 01, 01).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private long instance() throws UnknownHostException, SocketException {
        long id = 0L;
        for (byte x : InetAddress.getLocalHost().getAddress()) {
            id = ((id << 8) - Byte.MIN_VALUE + x) & maxInstance;
        }
        System.out.println(new String(InetAddress.getLocalHost().getAddress()));
//        System.out.println(new String(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress()));
//        byte[] mac = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
//        id = ((0x000000FF & (long) mac[mac.length - 1]) | (0x0000FF00 & (((long) mac[mac.length - 2]) << 8))) >> 6;
        StringBuilder sb = new StringBuilder();
//        return Optional.ofNullable(src)
//                .map(Base64Utils::decodeFromUrlSafeString)
//                .map(String::new)
//                .map(token -> token.split("&"))
//                .map(Arrays::asList)
//                .orElse(Collections.emptyList())
//                .stream()
//                .map(name -> name.split("="))
//                .filter(names -> names.length == 2)
//                .collect(LinkedHashMap::new,
//                        (map, names) -> map.put(names[0], names[1]),
//                        Map::putAll);
//        Collections.list(NetworkInterface.getNetworkInterfaces())
//                .stream()
//                .map(NetworkInterface::getHardwareAddress)
//                .
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            byte[] mac = networkInterface.getHardwareAddress();
            if (mac != null) {
                for (byte macPort : mac) {
                    sb.append(String.format("%02X", macPort));
                }
            }
        }
        System.out.println(sb);
//    } catch (Exception ex) {
//        nodeId = (new SecureRandom().nextInt());
//    }
//    nodeId = nodeId & maxNodeId;
//        return nodeId;
//        nodeId = sb.toString().hashCode();
        return id;
    }

    public Serializable generate(SharedSessionContractImplementor session, Object o) throws HibernateException {
        return Long.toString(generateId(), 36);
    }

    public long[] parse(long id) {
        long maskInstance = ((1L << INSTANCE_BITS) - 1) << SEQUENCE_BITS;
        long maskSequence = (1L << SEQUENCE_BITS) - 1;
        long timestamp = (id >> (INSTANCE_BITS + SEQUENCE_BITS)) + epoch;
        long instance = (id & maskInstance) >> SEQUENCE_BITS;
        long sequence = id & maskSequence;
        return new long[]{timestamp, instance, sequence};
    }

    public synchronized long generateId() {
        long timestamp = System.currentTimeMillis(); //Instant.now().toEpochMilli()
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                System.out.println("EEEE");
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return (timestamp - epoch) << (INSTANCE_BITS + SEQUENCE_BITS) | (instance << SEQUENCE_BITS) | sequence;
    }
}
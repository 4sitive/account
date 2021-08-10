package com.f4sitive.account.entity.generator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SnowballGenerator implements IdentifierGenerator {
    public static long GREGORIAN_EPOCH = -12219292800000L;
    private static final int UNUSED_BITS = 1;
    private static final int TIMESTAMP_BITS = 41;
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long maxTimestamp = (1L << TIMESTAMP_BITS) - 1; // -1L ^ (-1L << INSTANCE_BITS);
    private static final long maxInstance = (1L << INSTANCE_BITS) - 1; // -1L ^ (-1L << INSTANCE_BITS);
    private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;// -1L ^ (-1L << SEQUENCE_BITS);

    private static final long DEFAULT_EPOCH = 1288834974657L; //LocalDate.of(2020, 01, 01).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() twepoch = 1288834974657L;

    private final long instance;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowballGenerator() throws SocketException {

        StringUtils.hasText(id) ? (id.hashCode() & maxInstance) : (new SecureRandom().nextLong() & maxInstance);
        long instance = instance();
        if (instance < 0 || instance > maxInstance) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, maxInstance));
        }
        this.instance = instance & maxInstance;
        System.out.println("instance: " + Long.toBinaryString(this.instance));
//        this.epoch = LocalDate.of(2020, 01, 01).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private String instance() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces())
                .stream()
                .filter(networkInterface -> {
                    try {
                        return !networkInterface.isLoopback() && !networkInterface.isPointToPoint() && !networkInterface.isVirtual() && networkInterface.isUp();
                    } catch (SocketException e) {
                        return false;
                    }
                })
                .sorted()
                .map(networkInterface -> {
                    try {
                        return networkInterface.getHardwareAddress();
                    } catch (SocketException e) {
                        return null;
                    }
                })
                .map(hardwareAddress -> IntStream.generate(ByteBuffer.wrap(hardwareAddress)::get)
                        .limit(hardwareAddress.length)
                        .mapToObj(b -> String.format("%02X", (byte) b))
                        .collect(Collectors.joining("-")))
                .collect(Collectors.joining());
    }

    public Serializable generate(SharedSessionContractImplementor session, Object o) throws HibernateException {
        return Long.toString(generateId(), 36);
    }

    public static long timestamp(UUID uuid) {
        return (uuid.timestamp() / 10000) + GREGORIAN_EPOCH;
    }

    public static UUID uuid(long id) {
        long timestamp = (id >> (INSTANCE_BITS + SEQUENCE_BITS)) + DEFAULT_EPOCH;
        long instance = (id & (maxInstance << SEQUENCE_BITS)) >> SEQUENCE_BITS;
        long sequence = id & maxSequence;
        timestamp = ((timestamp - GREGORIAN_EPOCH) * 10000L);
        long msb = 0L;
        msb |= (0x00000000ffffffffL & timestamp) << 32;
        msb |= (0x0000ffff00000000L & timestamp) >>> 16;
        msb |= (0xffff000000000000L & timestamp) >>> 48;
        msb |= 0x0000000000001000L;

        long lsb = 0L;
        lsb |= 0x8000000000000000L;
        lsb |= (sequence & 0x0000000000003FFFL) << 48;
        lsb |= instance | 0x0000010000000000L;
        return new UUID(msb, lsb);
    }

    public long[] parse(long id) {
        long timestamp = id >> (INSTANCE_BITS + SEQUENCE_BITS);
        long instance = id & (maxInstance << SEQUENCE_BITS);
        long sequence = id & maxSequence;
        return new long[]{timestamp + DEFAULT_EPOCH, instance >> SEQUENCE_BITS, sequence};
    }

    public synchronized long generateId() {
        long timestamp = System.currentTimeMillis(); //Instant.now().toEpochMilli()  Clock.systemUTC().millis()
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) {
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return (timestamp - DEFAULT_EPOCH) << (INSTANCE_BITS + SEQUENCE_BITS) | (instance << SEQUENCE_BITS) | sequence;
    }
}
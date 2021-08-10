package com.f4sitive.account.entity.generator;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnowballGenerator implements IdentifierGenerator {
    private final static Logger log = LoggerFactory.getLogger(SnowballGenerator.class);
    private static final long GREGORIAN_EPOCH = -12219292800000L;
    private static final long TW_EPOCH = 1288834974657L;

    private static final int UNUSED_BITS = 1;
    private static final int TIMESTAMP_BITS = 41;
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long maxTimestamp = (1L << TIMESTAMP_BITS) - 1;
    private static final long maxInstance = (1L << INSTANCE_BITS) - 1;
    private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;// -1L ^ (-1L << SEQUENCE_BITS);


    private final long instance;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowballGenerator() {
        String mac = mac();
        this.instance = (StringUtils.hasText(mac) ? mac.hashCode() : new SecureRandom().nextLong()) & ((1L << INSTANCE_BITS) - 1);
        log.info("MAC: {}", mac);
        log.info("TW_EPOCH: {}", Instant.ofEpochMilli(TW_EPOCH));
        log.info("GREGORIAN_EPOCH: {}", Instant.ofEpochMilli(GREGORIAN_EPOCH));
        log.info("instance: {}, {}, {}, {}", instance, Long.toBinaryString(instance), Long.toBinaryString(instance | 0x0000010000000000L), instance | 0x0000010000000000L);

    }

    String mac() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces())
                    .stream()
                    .filter(networkInterface -> {
                        try {
                            return !networkInterface.isLoopback() && !networkInterface.isPointToPoint() && !networkInterface.isVirtual() && networkInterface.isUp();
                        } catch (SocketException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(networkInterface -> {
                        try {
                            return networkInterface.getHardwareAddress();
                        } catch (SocketException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(hardwareAddress -> String.format("%0" + (hardwareAddress.length << 1) + "X", new BigInteger(1, hardwareAddress)))
                    .collect(Collectors.toSet())
                    .stream()
                    .sorted()
                    .collect(Collectors.joining("|", "|", "|"));
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object o) throws HibernateException {
        return Long.toString(generateId(), 36);
    }

    public static long timestamp(UUID uuid) {
        return (uuid.timestamp() / 10000) + GREGORIAN_EPOCH;
    }

    public static UUID uuid(long id) {
        long timestamp = (id >> (INSTANCE_BITS + SEQUENCE_BITS)) + TW_EPOCH;
        long instance = (id & (((1L << INSTANCE_BITS) - 1) << SEQUENCE_BITS)) >> SEQUENCE_BITS;
        long sequence = id & ((1L << SEQUENCE_BITS) - 1);
        long msb = 0L;
        msb |= (0x00000000ffffffffL & ((timestamp - GREGORIAN_EPOCH) * 10000L)) << 32;
        msb |= (0x0000ffff00000000L & ((timestamp - GREGORIAN_EPOCH) * 10000L)) >>> 16;
        msb |= (0xffff000000000000L & ((timestamp - GREGORIAN_EPOCH) * 10000L)) >>> 48;
        msb |= 0x0000000000001000L;
        long lsb = 0L;
        lsb |= 0x8000000000000000L;
        lsb |= (sequence & 0x0000000000003FFFL) << 48;
        lsb |= instance;
        UUID uuid = new UUID(msb, lsb);
        log.info("id: {}, timestamp: {}, instance: {}, sequence: {}\n", id, timestamp, instance, sequence);
        log.info("id: {}, timestamp: {}, instance: {}, sequence: {}", id, timestamp(uuid), instance, sequence);
        return uuid;
    }

    public synchronized long generateId() {
        long timestamp = System.currentTimeMillis();
        assert timestamp >= lastTimestamp;
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & ((1L << SEQUENCE_BITS) - 1);
            if (sequence == 0) {
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return (timestamp - TW_EPOCH) << (INSTANCE_BITS + SEQUENCE_BITS) | (instance << SEQUENCE_BITS) | sequence;
    }
}
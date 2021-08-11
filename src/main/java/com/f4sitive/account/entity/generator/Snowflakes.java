package com.f4sitive.account.entity.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class Snowflakes {
    private final Logger log = LoggerFactory.getLogger(Snowflakes.class);
    private static final long GREGORIAN_EPOCH = -12219292800000L;
    private static final long TW_EPOCH = 1288834974657L;

    private static final int UNUSED_BITS = 1;
    private static final int TIMESTAMP_BITS = 41;
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private final long instance;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public static final Snowflakes INSTANCE = new Snowflakes(mac());

    public Snowflakes(String id) {
        log.info(id);
        this.instance = (StringUtils.hasText(id) ? id.hashCode() : new SecureRandom().nextLong()) & (-1L ^ (-1L << INSTANCE_BITS));
    }

    static String mac() {
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

    public long getInstance() {
        return instance;
    }

    public static long timestamp(UUID uuid) {
        return (uuid.timestamp() / 10000) + GREGORIAN_EPOCH;
    }

    public static long timestamp(long id) {
        return (id >> (INSTANCE_BITS + SEQUENCE_BITS)) + TW_EPOCH;
    }

    public static long instance(long id) {
        return (id & (((1L << INSTANCE_BITS) - 1) << SEQUENCE_BITS)) >> SEQUENCE_BITS;
    }

    public static long sequence(long id) {
        return id & ((1L << SEQUENCE_BITS) - 1);
    }

    public static UUID uuid(long id) {
        return uuid(timestamp(id), instance(id), sequence(id));
    }

    public static UUID uuid(long timestamp, long instance, long sequence) {
        timestamp = (timestamp - GREGORIAN_EPOCH) * 10000L;
        long msb = 0x0000000000001000L | (0x00000000ffffffffL & timestamp) << 32 | (0x0000ffff00000000L & timestamp) >>> 16 | (0xffff000000000000L & timestamp) >>> 48;
        long lsb = 0x8000000000000000L | (sequence & 0x0000000000003FFFL) << 48 | instance;
        return new UUID(msb, lsb);
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

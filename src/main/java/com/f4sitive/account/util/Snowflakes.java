package com.f4sitive.account.util;

import lombok.Getter;

import java.security.SecureRandom;
import java.util.UUID;

public class Snowflakes {
    private static final long GREGORIAN_EPOCH = -12219292800000L;
    private static final long TW_EPOCH = 1288834974657L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int TIMESTAMP_BITS = 41;
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    @Getter
    private final long instance;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public Snowflakes(long instance) {
        this.instance = instance & ~(-1L << INSTANCE_BITS);
    }

    public static long timestamp(UUID uuid) {
        return (uuid.timestamp() / 10000) + GREGORIAN_EPOCH;
    }

    public static long instance(UUID uuid) {
        return uuid.node() & ~(-1L << INSTANCE_BITS);
    }

    public static long timestamp(long id) {
        return ((id >> (INSTANCE_BITS + SEQUENCE_BITS)) & ~(-1L << TIMESTAMP_BITS)) + TW_EPOCH;
    }

    public static long instance(long id) {
        return (id >> SEQUENCE_BITS) & ~(-1L << INSTANCE_BITS);
    }

    public static long sequence(long id) {
        return id & ~(-1L << SEQUENCE_BITS);
    }

    public static UUID uuid(long timestamp, long instance, long sequence) {
        return uuid(timestamp, instance, sequence, false);
    }

    public static UUID uuid(long timestamp, long instance, long sequence, boolean random) {
        long time = (timestamp - GREGORIAN_EPOCH) * 10000L;
        long msb = (0x00000000ffffffffL & time) << 32 | (0x0000ffff00000000L & time) >>> 16 | (0xffff000000000000L & time) >>> 48 | 0x0000000000001000L;
        long lsb = 0x8000000000000000L | (sequence & 0x0000000000003fffL) << 48 | instance | 0x0000010000000000L | (random ? RANDOM.nextLong() & 0x000000fffffffc00L : 0x0000000000000000L);
        return new UUID(msb, lsb);
    }

    public static long id(long timestamp, long instance, long sequence) {
        return ((timestamp - TW_EPOCH) & ~(-1L << TIMESTAMP_BITS)) << (INSTANCE_BITS + SEQUENCE_BITS) | (instance << SEQUENCE_BITS) | sequence;
    }

    public synchronized long generate() {
        long timestamp = System.currentTimeMillis();
        assert timestamp >= lastTimestamp;
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & ~(-1L << SEQUENCE_BITS);
            if (sequence == 0) {
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return id(timestamp, instance, sequence);
    }
}
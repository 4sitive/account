package com.f4sitive.account.util;

import lombok.Getter;

import java.util.UUID;

public class Snowflakes {
    private static final long GREGORIAN_EPOCH = -12219292800000L;
    private static final long TW_EPOCH = 1288834974657L;

    private static final int TIMESTAMP_BITS = 41;
    private static final int INSTANCE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    @Getter
    private final long instance;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public Snowflakes(long instance) {
        this.instance = instance & 0x00000000000003ffL;
    }

    public static long timestamp(UUID uuid) {
        return (uuid.timestamp() / 10000) + GREGORIAN_EPOCH;
    }

    public static long instance(UUID uuid) {
        return uuid.node() & 0x00000000000003ffL;
    }

    public static long sequence(UUID uuid) {
        return uuid.clockSequence() & 0x0000000000000fffL;
    }

    public static long timestamp(long id) {
        return ((id >>> (INSTANCE_BITS + SEQUENCE_BITS)) & 0x000001ffffffffffL) + TW_EPOCH;
    }

    public static long instance(long id) {
        return (id >>> SEQUENCE_BITS) & 0x00000000000003ffL;
    }

    public static long sequence(long id) {
        return id & 0x0000000000000fffL;
    }

    public static UUID uuid(long id) {
        long timestamp = (timestamp(id) - GREGORIAN_EPOCH) * 10000;
        long msb = (0x00000000ffffffffL & timestamp) << 32 | (0x0000ffff00000000L & timestamp) >>> 16 | (0xffff000000000000L & timestamp) >>> 48 | 0x0000000000001000L;
        long lsb = 0x8000000000000000L | (sequence(id) & 0x0000000000003fffL) << 48 | instance(id) | 0x0000010000000000L | (Integer.toUnsignedLong(Long.toString(id, Character.MAX_RADIX).hashCode()) * 63 & 0x0000003fbfffffffL) << 10;
        return new UUID(msb, lsb);
    }

    public static long id(long timestamp, long instance, long sequence) {
        return ((timestamp - TW_EPOCH) & ~(-1L << TIMESTAMP_BITS)) << (INSTANCE_BITS + SEQUENCE_BITS) | (instance & ~(-1L << INSTANCE_BITS)) << SEQUENCE_BITS | (sequence & ~(-1L << SEQUENCE_BITS));
    }

    public synchronized long generate() {
        long timestamp = System.currentTimeMillis();
        assert timestamp >= lastTimestamp;
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & 0x0000000000000fffL;
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
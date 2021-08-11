package com.f4sitive.account.util;

import lombok.Getter;

import java.util.UUID;

public class Snowflakes {
    private static final long GREGORIAN_EPOCH = -12219292800000L;
    private static final long TW_EPOCH = 1288834974657L;

    private static final int TIMESTAMP_BITS = 41;
    private static final int NODE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    @Getter
    private final long node;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public Snowflakes(long node) {
        this.node = node & ~(-1L << NODE_BITS);
    }

    public static long timestamp(UUID uuid) {
        return (uuid.timestamp() / 10000) + GREGORIAN_EPOCH;
    }

    public static long timestamp(long id) {
        return ((id >> (NODE_BITS + SEQUENCE_BITS)) & ~(-1L << TIMESTAMP_BITS)) + TW_EPOCH;
    }

    public static long node(long id) {
        return (id >> SEQUENCE_BITS) & ~(-1L << NODE_BITS);
    }

    public static long sequence(long id) {
        return id & ~(-1L << SEQUENCE_BITS);
    }

    public static UUID uuid(long timestamp, long node, long sequence) {
        long gregorianEpoch = (timestamp - GREGORIAN_EPOCH) * 10000L;
        long msb = 0x0000000000001000L | (0x00000000ffffffffL & gregorianEpoch) << 32 | (0x0000ffff00000000L & gregorianEpoch) >>> 16 | (0xffff000000000000L & gregorianEpoch) >>> 48;
        long lsb = 0x8000000000000000L | (sequence & 0x0000000000003FFFL) << 48 | node;
        return new UUID(msb, lsb);
    }

    public static long id(long timestamp, long node, long sequence) {
        return ((timestamp - TW_EPOCH) & ~(-1L << TIMESTAMP_BITS)) << (NODE_BITS + SEQUENCE_BITS) | (node << SEQUENCE_BITS) | sequence;
    }

    public static long max() {
        return (~(-1L << TIMESTAMP_BITS) << (NODE_BITS + SEQUENCE_BITS)) | (~(-1L << NODE_BITS) << SEQUENCE_BITS) | ~(-1L << SEQUENCE_BITS);
    }

    public synchronized long generate() {
        long timestamp = System.currentTimeMillis();
        assert timestamp >= lastTimestamp;
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & ~(-1L << SEQUENCE_BITS);
            if (sequence == 0) {
                System.out.println("!");
                do {
                    timestamp = System.currentTimeMillis();
                } while (timestamp <= lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return id(timestamp, node, sequence);
    }
}
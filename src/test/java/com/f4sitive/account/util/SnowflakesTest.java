package com.f4sitive.account.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

class SnowflakesTest {
    @Test
    void test() {
        long timestamp = ZonedDateTime.of(2020, 01, 01, 00, 00, 00, 000 * 1000_000, ZoneOffset.UTC).toInstant().toEpochMilli();
        long instance = 3L;
        long sequence = 127L;
        Assertions.assertEquals(Snowflakes.id(timestamp, instance, sequence), 1212161512043458687L);
        Assertions.assertEquals(Snowflakes.uuid(1212161512043458687L), UUID.fromString("a747c000-2c29-11ea-807f-ffffdb760803"));
    }

    @Test
    void max() {
        long max = Long.MAX_VALUE;
        Assertions.assertEquals(Long.toString(max, Character.MAX_RADIX), "1y2p0ij32e8e7");
        long timestamp = Snowflakes.timestamp(max);
        long instance = Snowflakes.instance(max);
        long sequence = Snowflakes.sequence(max);
        Instant instant = Instant.from(ZonedDateTime.of(2080, 07, 10, 17, 30, 30, 208 * 1000_000, ZoneOffset.UTC));
        Assertions.assertTrue(Duration.between(Instant.ofEpochMilli(timestamp), instant).isZero());
        Assertions.assertEquals(instance, 1023L);
        Assertions.assertEquals(sequence, 4095L);

        UUID uuid = Snowflakes.uuid(max);
        Assertions.assertEquals(Snowflakes.timestamp(uuid), timestamp);
        Assertions.assertEquals(Snowflakes.instance(uuid), instance);
        Assertions.assertEquals(Snowflakes.sequence(uuid), sequence);
        Assertions.assertEquals(uuid, UUID.fromString("d7e3bc00-07b4-122e-8fff-01004c5dbfff"));
    }

    @Test
    void min() {
        long min = 0L;
        Assertions.assertEquals(Long.toString(min, Character.MAX_RADIX), "0");
        long timestamp = Snowflakes.timestamp(min);
        long instance = Snowflakes.instance(min);
        long sequence = Snowflakes.sequence(min);
        Instant instant = Instant.from(ZonedDateTime.of(2010, 11, 04, 01, 42, 54, 657 * 1000_000, ZoneOffset.UTC));
        Assertions.assertTrue(Duration.between(Instant.ofEpochMilli(timestamp), instant).isZero());
        Assertions.assertEquals(instance, 0L);
        Assertions.assertEquals(sequence, 0L);

        UUID uuid = Snowflakes.uuid(min);
        Assertions.assertEquals(Snowflakes.timestamp(uuid), timestamp);
        Assertions.assertEquals(Snowflakes.instance(uuid), instance);
        Assertions.assertEquals(Snowflakes.sequence(uuid), sequence);
        Assertions.assertEquals(uuid, UUID.fromString("d7e3e310-e7b4-11df-8000-01000000c000"));
    }

    @Test
    void generate() throws InterruptedException {
        long currentTimeMillis = System.currentTimeMillis();
        Snowflakes snowflakes = new Snowflakes(1024L);
        Map<Long, Integer> ids = new ConcurrentHashMap<>();
        int numberOfThreads = 1000000;
        ExecutorService service = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                ids.merge(snowflakes.generate(), 1, Integer::sum);
                latch.countDown();
            });
        }
        latch.await();

        Assertions.assertTrue(ids.values().stream().allMatch(count -> count == 1));
        ids.keySet()
                .stream()
                .forEach(id -> {
                    long timestamp = Snowflakes.timestamp(id);
                    long instance = Snowflakes.instance(id);
                    long sequence = Snowflakes.sequence(id);

                    UUID uuid = Snowflakes.uuid(id);
                    Assertions.assertEquals(uuid.version(), 1);
                    Assertions.assertEquals(uuid.variant(), 2);

                    Assertions.assertTrue(currentTimeMillis <= Snowflakes.timestamp(uuid));
                    Assertions.assertEquals(timestamp, Snowflakes.timestamp(uuid));
                    Assertions.assertEquals(instance, Snowflakes.instance(uuid));
                    Assertions.assertEquals(sequence, Snowflakes.sequence(uuid));

                    id = Snowflakes.id(timestamp, instance, sequence);
                    Assertions.assertEquals(timestamp, Snowflakes.timestamp(id));
                    Assertions.assertEquals(instance, Snowflakes.instance(id));
                    Assertions.assertEquals(sequence, Snowflakes.sequence(id));
                });
        Assertions.assertTrue(ids.keySet()
                .stream()
                .map(id -> Long.toString(id, Character.MAX_RADIX))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
        Assertions.assertTrue(ids.keySet()
                .stream()
                .map(id -> Snowflakes.uuid(id).toString())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
    }
}
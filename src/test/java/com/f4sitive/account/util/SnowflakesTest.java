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
        long instance = 0L;
        long sequence = 0L;
        Assertions.assertEquals(Snowflakes.id(timestamp, instance, sequence), 1212161512043446272L);
        Assertions.assertEquals(Snowflakes.uuid(timestamp, instance, sequence).timestamp(), UUID.fromString("a747c000-2c29-11ea-8000-010000000000").timestamp());
        Assertions.assertEquals(Snowflakes.uuid(timestamp, instance, sequence).clockSequence(), UUID.fromString("a747c000-2c29-11ea-8000-010000000000").clockSequence());
        Assertions.assertEquals(Snowflakes.uuid(timestamp, instance, sequence), UUID.fromString("a747c000-2c29-11ea-8000-010000000000"));
        Assertions.assertEquals(Snowflakes.instance(Snowflakes.uuid(timestamp, instance, sequence)), Snowflakes.instance(UUID.fromString("a747c000-2c29-11ea-8000-010000000000")));
    }

    @Test
    void max() {
        long max = Long.MAX_VALUE;
        Assertions.assertEquals(Long.toString(max, Character.MAX_RADIX), "1y2p0ij32e8e7");
        Instant instant = Instant.from(ZonedDateTime.of(2080, 07, 10, 17, 30, 30, 208 * 1000_000, ZoneOffset.UTC));
        Assertions.assertEquals(Instant.ofEpochMilli(Snowflakes.timestamp(max)), instant);
        Assertions.assertTrue(Duration.between(Instant.ofEpochMilli(Snowflakes.timestamp(max)), instant).isZero());
        Assertions.assertEquals(Snowflakes.instance(max), 1023L);
        Assertions.assertEquals(Snowflakes.sequence(max), 4095L);
    }

    @Test
    void min() {
        long min = 0L;
        Assertions.assertEquals(Long.toString(min, Character.MAX_RADIX), "0");
        Instant instant = Instant.from(ZonedDateTime.of(2010, 11, 04, 01, 42, 54, 657 * 1000_000, ZoneOffset.UTC));
        Assertions.assertEquals(Instant.ofEpochMilli(Snowflakes.timestamp(min)), instant);
        Assertions.assertTrue(Duration.between(Instant.ofEpochMilli(Snowflakes.timestamp(min)), instant).isZero());
        Assertions.assertEquals(Snowflakes.instance(min), 0L);
        Assertions.assertEquals(Snowflakes.sequence(min), 0L);
    }

    @Test
    void generate() throws InterruptedException {
        long currentTimeMillis = System.currentTimeMillis();
        Snowflakes snowflakes = new Snowflakes(1024L);
        Map<Long, Integer> ids = new ConcurrentHashMap<>();
        int numberOfThreads = 100000;
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

                    UUID uuid = Snowflakes.uuid(timestamp, instance, sequence);
                    Assertions.assertEquals(uuid.version(), 1);
                    Assertions.assertEquals(uuid.variant(), 2);

                    Assertions.assertTrue(currentTimeMillis <= Snowflakes.timestamp(uuid));
                    Assertions.assertEquals(timestamp, Snowflakes.timestamp(uuid));
                    Assertions.assertEquals(instance, Snowflakes.instance(uuid));
                    Assertions.assertEquals(sequence, uuid.clockSequence());

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
                .map(id -> Snowflakes.uuid(Snowflakes.timestamp(id), Snowflakes.instance(id), Snowflakes.sequence(id)).toString())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
    }
}
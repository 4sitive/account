package com.f4sitive.account.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
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
        System.out.println("someString".chars().filter(Character::isDigit).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append));
        System.out.println("10.10.10.10".chars().filter(Character::isDigit).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append));
        System.out.println("1022-31.2030d12n3j5".chars().filter(Character::isDigit).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append));
        long max = Snowflakes.max();
        Assertions.assertEquals(Long.MAX_VALUE, max);
        Assertions.assertEquals(Long.toString(Long.MAX_VALUE, Character.MAX_RADIX), "1y2p0ij32e8e7");
        Instant instant = Instant.from(ZonedDateTime.of(2080, 07, 10, 17, 30, 30, 208 * 1000_000, ZoneOffset.UTC));
        Assertions.assertEquals(Instant.ofEpochMilli(Snowflakes.timestamp(max)), instant);
        Assertions.assertTrue(Duration.between(Instant.ofEpochMilli(Snowflakes.timestamp(max)), instant).isZero());
        Assertions.assertEquals(Snowflakes.node(max), 1023L);
        Assertions.assertEquals(Snowflakes.sequence(max), 4095L);
        Assertions.assertEquals(new Snowflakes(1024L).getNode(), 0L);
        Assertions.assertEquals(new Snowflakes(2047L).getNode(), 1023L);
        System.out.println(UUID.randomUUID().toString().replace("-", ""));
    }

    @Test
    void generate() throws InterruptedException {
        Assertions.assertEquals(Long.toBinaryString(Snowflakes.max()).length(), 63);
        long currentTimeMillis = System.currentTimeMillis();
        Snowflakes snowflakes = new Snowflakes(0L);
        Map<Long, Integer> ids = new ConcurrentHashMap<>();
        int numberOfThreads = 100000;
        ExecutorService service = Executors.newFixedThreadPool(10);
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
                    long node = Snowflakes.node(id);
                    long sequence = Snowflakes.sequence(id);

                    UUID uuid = Snowflakes.uuid(timestamp, node, sequence);
                    Assertions.assertEquals(uuid.version(), 1);
                    Assertions.assertEquals(uuid.variant(), 2);

                    Assertions.assertTrue(currentTimeMillis <= Snowflakes.timestamp(uuid));
                    Assertions.assertEquals(timestamp, Snowflakes.timestamp(uuid));
                    Assertions.assertEquals(node, uuid.node());
                    Assertions.assertEquals(sequence, uuid.clockSequence());

                    id = Snowflakes.id(timestamp, node, sequence);
                    Assertions.assertEquals(timestamp, Snowflakes.timestamp(id));
                    Assertions.assertEquals(node, Snowflakes.node(id));
                    Assertions.assertEquals(sequence, Snowflakes.sequence(id));
                });
        Assertions.assertTrue(ids.keySet()
                .stream()
                .map(id -> Long.toString(id, Character.MAX_RADIX).toUpperCase())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
        Assertions.assertTrue(ids.keySet()
                .stream()
                .map(id -> Snowflakes.uuid(Snowflakes.timestamp(id), Snowflakes.node(id), Snowflakes.sequence(id)).toString())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
    }
}
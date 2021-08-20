package com.f4sitive.account.util;

import com.f4sitive.account.entity.generator.UserIdentifierGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import java.security.SecureRandom;
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
//        Assertions.assertEquals(Snowflakes.uuid(1212161512043458687L), UUID.fromString("a747c000-2c29-11ea-807f-ffffdb760803"));
        Assertions.assertEquals(Snowflakes.uuid(1212161512043458687L), UUID.fromString("a747c000-2c29-11ea-807f-a167020bf803"));

        System.out.println(Snowflakes.timestamp(UserIdentifierGenerator.id("CAUKTOOU5SDFK3FDF")));
        System.out.println(Snowflakes.instance(UserIdentifierGenerator.id("CAUKTOOU5SDFK3FDF")));
        System.out.println(Snowflakes.sequence(UserIdentifierGenerator.id("CAUKTOOU5SDFK3FDF")));
        String id = Long.toString(new Snowflakes(1L).generate(), Character.MAX_RADIX);
        System.out.println(Long.toHexString(Double.doubleToLongBits(Math.random())));
        System.out.println(Long.toHexString(Double.doubleToLongBits(Math.random())));
        System.out.println(Integer.toString(id.length(), Character.MAX_RADIX));
        System.out.println(Long.toString(new SecureRandom().nextLong(), Character.MAX_RADIX));
        System.out.println(("APL" + Integer.toString(id.length(), Character.MAX_RADIX) + id + Long.toHexString(Double.doubleToLongBits(Math.random()))).substring(0, 20).toUpperCase());
        int size = Integer.parseInt("APLCAUKSG8YI0NB43FED".substring(3, 4), Character.MAX_RADIX);
        System.out.println("APLCAUKSG8YI0NB43FED".substring(4, size+4));
        System.out.println("APLAUKQKEUHR2TC5371C".substring(0, 1));
        System.out.println(Long.toString(12, Character.MAX_RADIX));
        System.out.println(Long.toString(13, Character.MAX_RADIX));
        System.out.println(Long.toString(14, Character.MAX_RADIX));
        System.out.println(Long.toString(15, Character.MAX_RADIX));
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
//        Assertions.assertEquals(uuid, UUID.fromString("d7e3bc00-07b4-122e-8fff-01004c5dbfff"));
        Assertions.assertEquals(uuid, UUID.fromString("d7e3bc00-07b4-122e-8fff-e5b1cb1147ff"));
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
        System.out.println(Long.toString(Snowflakes.id(1288834974658L,instance,sequence), Character.MAX_RADIX));
        System.out.println(Long.toString(Snowflakes.id(1L,instance,sequence), Character.MAX_RADIX));
        System.out.println(Long.toString(Snowflakes.id(System.currentTimeMillis(),instance,sequence), Character.MAX_RADIX));
        System.out.println(Long.toString(Snowflakes.id(Long.MAX_VALUE,instance,sequence), Character.MAX_RADIX));

        UUID uuid = Snowflakes.uuid(min);
        Assertions.assertEquals(Snowflakes.timestamp(uuid), timestamp);
        Assertions.assertEquals(Snowflakes.instance(uuid), instance);
        Assertions.assertEquals(Snowflakes.sequence(uuid), sequence);
//        Assertions.assertEquals(uuid, UUID.fromString("d7e3e310-e7b4-11df-8000-01000000c000"));
        Assertions.assertEquals(uuid, UUID.fromString("d7e3e310-e7b4-11df-8000-0100002f4000"));
    }

    @Test
    void generate() throws InterruptedException {
        long currentTimeMillis = System.currentTimeMillis();
        Snowflakes snowflakes = new Snowflakes(1024L);
        Map<Long, Integer> ids = new ConcurrentHashMap<>();
        int numberOfThreads = 10000;
        ExecutorService service = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                long id = snowflakes.generate();
                System.out.println(Snowflakes.uuid(id));
                ids.merge(id, 1, Integer::sum);
                latch.countDown();
                return id;
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
package com.f4sitive.account.entity.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    public void runsMultipleTimes() throws InterruptedException {
        final Map<Long, Integer> ids = new ConcurrentHashMap<>();

        int numberOfThreads = 1000000;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                ids.merge(Snowflakes.INSTANCE.generateId(), 1, Integer::sum);
                latch.countDown();
            });
        }
        latch.await();
        Assertions.assertTrue(ids.values().stream().allMatch(count -> count == 1));
        ids.keySet()
                .stream()
                .forEach(id -> {
                    UUID uuid = Snowflakes.uuid(id);
                    Assertions.assertEquals(uuid, Snowflakes.uuid(Snowflakes.timestamp(id), Snowflakes.instance(id), Snowflakes.sequence(id)));
                    Assertions.assertEquals(uuid.toString(), Snowflakes.uuid(Snowflakes.timestamp(id), Snowflakes.instance(id), Snowflakes.sequence(id)).toString());
                    Assertions.assertEquals(uuid, Snowflakes.uuid(uuid.timestamp() / 10000 - 12219292800000L, uuid.node(), uuid.clockSequence()));
                    Assertions.assertEquals(uuid.toString(), Snowflakes.uuid(uuid.timestamp() / 10000 - 12219292800000L, uuid.node(), uuid.clockSequence()).toString());
                    Assertions.assertEquals(Snowflakes.instance(id), Snowflakes.INSTANCE.getInstance());
                    Assertions.assertEquals(uuid.node(), Snowflakes.instance(id));
                    Assertions.assertEquals(uuid.node(), Snowflakes.INSTANCE.getInstance());
                    Assertions.assertEquals(uuid.clockSequence(), Snowflakes.sequence(id));
                    Assertions.assertEquals(uuid.version(), 1);
                    Assertions.assertEquals(uuid.variant(), 2);
                    Assertions.assertEquals(uuid.timestamp() / 10000 - 12219292800000L, Snowflakes.timestamp(id));
                    Assertions.assertEquals(uuid.timestamp(), (Snowflakes.timestamp(id) + 12219292800000L) * 10000);
                    Assertions.assertEquals(Snowflakes.timestamp(uuid), Snowflakes.timestamp(id));
//                    StringBuilder builder = new StringBuilder();
//                    builder.append(id);
//                    builder.append("\t");
//                    builder.append(" UUID: ");
//                    builder.append(uuid);
//                    builder.append("\t");
//                    builder.append(" clockSequence: ");
//                    builder.append(uuid.clockSequence());
//                    builder.append("\t");
//                    builder.append(" Instant: ");
//                    builder.append(Instant.ofEpochMilli(SnowballGenerator.timestamp(uuid)));
//                    builder.append("\t");
//                    builder.append(" timestamp: ");
//                    builder.append(uuid.timestamp());
//                    builder.append("\t");
//                    builder.append(" variant: ");
//                    builder.append(uuid.variant());
//                    builder.append("\t");
//                    builder.append(" node: ");
//                    builder.append(uuid.node());
//                    System.out.println(builder);
                });
        Assertions.assertTrue(ids.keySet()
                .stream()
                .map(id -> Long.toString(id, Character.MAX_RADIX).toUpperCase())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
        Assertions.assertTrue(ids.keySet()
                .stream()
                .map(id -> Snowflakes.uuid(id).toString())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream().allMatch(count -> count == 1));
//        result.keySet()
//                .stream()
//                .forEach(System.out::println);
    }
}
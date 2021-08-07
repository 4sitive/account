package com.f4sitive.account.entity.generator;

import com.f4sitive.account.util.ThreadUtils;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.junit.jupiter.api.Test;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SnowballGeneratorTest {

    @Test
    void generateId() throws SocketException, UnknownHostException {
        System.out.println(Instant.ofEpochMilli(1288834974657L));
        System.out.println(LocalDate.of(2010, 11, 04));
        System.out.println(LocalDate.of(2010, 11, 04).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
        SnowballGenerator generator = new SnowballGenerator();
        Map<Long, Long> result = new ConcurrentHashMap<>();
        List<Long> data = new ArrayList<>();
        ThreadUtils.get(() -> {
//            result.com
            Long id = generator.generateId();
            long[] ss = generator.parse(id);
            System.out.println(System.currentTimeMillis());
            System.out.println(ss[0]);
            System.out.println(ss[1]);
            System.out.println(ss[2]);
            System.out.println(id);
            System.out.println(Long.toString(id, 36));
//            ObjectIdGenerator.IdKey
            System.out.println((int)((id >> 12) & 0x0f));
            System.out.println(new UUID(id,id).timestamp());
//            5ec862f1-1c19-b000-5ec8-62f11c19b000
//            5ec862fb-79d9-b000-5ec8-62fb79d9b000
//            5ec8634c-9a19-b000-5ec8-634c9a19b000
//            System.out.println(UUID.nameUUIDFromBytes(ByteBuffer.allocate(8).putLong(id).array()));
//            System.out.println(UUID.fromString("f4c36093-ae5b-3762-97e1-59481d2f6fc8").timestamp());
            return id;
        }, 1);
    }
}
package com.f4sitive.account.entity.generator;

import com.f4sitive.account.util.ThreadUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class SnowballGeneratorTest {
    @Test
    void generateId() {
        SnowballGenerator generator = new SnowballGenerator();
        ConcurrentHashMap<Long, Integer> ids = new ConcurrentHashMap<>();
        ConcurrentHashMap<UUID, Integer> uuids = new ConcurrentHashMap<>();
//        LinkedMultiValueMap<Long, UUID> test = new LinkedMultiValueMap<>();
        ThreadUtils.get(() -> {
            Long id = generator.generateId();
            UUID uuid = SnowballGenerator.uuid(id);
            ids.merge(id, 1, Integer::sum);
            uuids.merge(uuid, 1, Integer::sum);
            StringBuilder builder = new StringBuilder();
            builder.append(id);
            builder.append("\t");
            builder.append(Instant.now());
            builder.append("\t");
            builder.append(" UUID: ");
            builder.append(uuid);
            builder.append("\t");
            builder.append(" Instant: ");
            builder.append(Instant.ofEpochMilli(SnowballGenerator.timestamp(uuid)));
            builder.append("\t");
            builder.append(" timestamp: ");
            builder.append(uuid.timestamp());
            builder.append("\t");
            builder.append(" variant: ");
            builder.append(uuid.variant());
            builder.append("\t");
            builder.append(" node: ");
            builder.append(uuid.node());
            builder.append("\t");
            builder.append(" clockSequence: ");
            builder.append(uuid.clockSequence());
            System.out.println(builder);
            return id;
        }, 3000);
        ids.entrySet().stream().forEach(entry -> {
            if(entry.getValue() > 1){
                System.out.println(entry);
            }
        });
        uuids.entrySet().stream().forEach(entry -> {
            if(entry.getValue() > 1){
                System.out.println(entry);
            }
        });
    }
}
package com.f4sitive.account;


import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class JavaTest {
    @Test
    public void test(){
        System.out.println((2019-1970)*31536000L*1000);
        System.out.println(Long.toString(101,36));
        System.out.println(System.nanoTime());
        System.out.println(System.currentTimeMillis());
        System.out.println(Long.toString(System.currentTimeMillis(), 36));
        System.out.println(Long.toString(Long.MAX_VALUE, 36));
//        System.out.println(Long.toString(Long.MAX_VALUE, 62));
        System.out.println(Instant.now().getEpochSecond());
        System.out.println(Instant.now().toEpochMilli());
        System.out.println("Clock.systemDefaultZone().millis()" + Clock.systemDefaultZone().millis());
        System.out.println(Instant.ofEpochSecond(1628185634L, 1));
        System.out.println(Instant.ofEpochSecond(1628185634L, 2).toString());
//        System.out.println(DateTimeFormatter.ISO_INSTANT.formatTo(Instant.ofEpochSecond(1628185634L, 1)););
        System.out.println(Instant.ofEpochSecond(1628185634L, 2));

        System.out.println(Instant.ofEpochSecond(1628185634L, 1).toEpochMilli());
        System.out.println(Instant.ofEpochSecond(1628185634L, 2).toEpochMilli());
        System.out.println(Long.toString(System.currentTimeMillis(), 36));
        System.out.println(Long.toString(System.currentTimeMillis(), 36).length());
        System.out.println(Long.toString(100000000L, 36));
        System.out.println(Long.toString(100000000L, 36).length());
        System.out.println(UUID.randomUUID().toString().substring(0, 8));
        System.out.println(UUID.randomUUID().toString().substring(0, 8).length());
    }
}

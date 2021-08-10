package com.f4sitive.account;


import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class JavaTest {
    @Test
    public void testid(){
        System.out.println(Long.toBinaryString(3L << 12));
        System.out.println(Long.toBinaryString(3L << 12 | 0x0f));
        System.out.println(Long.toBinaryString(15L ^ 14L));
        System.out.println(Long.toBinaryString(3L << 3 ^ 0x0f));
        System.out.println(11000000000000L >> 12 & 0x0f );
        System.out.println(Long.toBinaryString(0x0f));
//        System.out.println(Long.toBinaryString(1L << 12));
        System.out.println(0x000000000000F000 >> 12 & 0x0f);
        System.out.println((int)((0x000000000000F000 >> 12) & 0x0f));
//        UUID.randomUUID().version()
        System.out.println(LocalDate.of(2020, 01, 01).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
        System.out.println(1L | 1L);
        System.out.println(1L | 2L);
        System.out.println(1L & 1L << 2 >> 2);
        System.out.println((1L & 1L << 2) >> 2);
        System.out.println(1L<< 1+1);
        System.out.println(1L<< 1+1 + 1);
        System.out.println((1L<< 1+1) + 1);
        System.out.println(1L << (1+1));
        System.out.println(1L & 1L);
        System.out.println(1L << 2);
        System.out.println(1L & 1L << 2);
        System.out.println(1L & (1L << 2));
        long l = 0xffffffffffffffffL; // any long, e.g. -1

        // to string
        BigInteger bi = BigInteger.valueOf(l);
//        if (l < 0) bi = bi.setBit(64);
        final String b36 = bi.toString(36);
        System.out.println("original long:" + l);
        System.out.println("result 36: " + b36);

        System.out.println(BigInteger.valueOf((Long.MAX_VALUE) << 22).toString(Character.MAX_RADIX));
        System.out.println(Long.toString(Long.MAX_VALUE, Character.MAX_RADIX));
        System.out.println(Clock.systemUTC().millis());
        System.out.println(System.currentTimeMillis());
        System.out.println(Long.toBinaryString(System.currentTimeMillis()));
        System.out.println(Long.toBinaryString(System.currentTimeMillis() - LocalDate.of(2030, 01, 01).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()));
        System.out.println(Long.toBinaryString(1L << 22));
        System.out.println(Long.toBinaryString(Long.MAX_VALUE << 22));
        System.out.println(Long.toString(System.currentTimeMillis() << 22, 36));
        System.out.println(Long.toString(System.currentTimeMillis() << 22, 36).length());
        System.out.println(Long.toString((Long.MAX_VALUE) << 22, Character.MAX_RADIX));
        System.out.println(Long.toBinaryString(1L << 22 | 1L << 12));
        System.out.println(Long.toBinaryString(2L << 22));
        System.out.println((1L << 41) - 1);
//        Long.toBinaryString()//String.format("%8s", Integer.toBinaryString(b2 & 0xFF)).replace(' ', '0');
        System.out.println(Long.toBinaryString((1L << 41) - 1));
        System.out.println(Long.toBinaryString((1L << 41) - 1).length());
        System.out.println(Long.toBinaryString(-1L ^ (-1L << 41 + 10 + 12))); //-1L ^ (-1L << INSTANCE_BITS)
        System.out.println(Long.toBinaryString((1L << 41 + 10 + 12) - 1));
        System.out.println(Long.toBinaryString((1L << 41 + 10 + 12) - 1).length());
        System.out.println(Long.toString((1L << 41 + 10 + 12) - 1, 36));
        System.out.println(Long.toString((1L << 41 + 10 + 12) - 1, 36).length());
    }
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

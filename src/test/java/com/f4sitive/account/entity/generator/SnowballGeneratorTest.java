package com.f4sitive.account.entity.generator;

import com.f4sitive.account.util.ThreadUtils;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SnowballGeneratorTest {

    @Test
    public void sssd() {
        long sequence=0L;
//        ThreadContext tctx = ThreadContext.getThreadContext();
        long msb = 1L;
        long lsb = (System.currentTimeMillis() - SnowballGenerator.GREGORIAN_EPOCH) * 1000 + ((sequence++) % 1000);
        lsb = (lsb & 0xCFFFFFFFFFFFFFFFL) | (0x8000000000000000L);
        msb = (msb & 0xFFFFFFFFFFFF0FFFL) | (0x0000000000001000L);

//        long clock = new SecureRandom().nextLong();
//        long lsb = 0;
//        lsb |= 0x8000000000000000L;                 // variant (2 bits)
//        lsb |= (clock & 0x0000000000003FFFL) << 48; // clock sequence (14 bits)
//        lsb |= makeNode();                          // 6 bytes
//        return lsb;
        System.out.println(new Date(new UUID(msb, lsb).timestamp() / 10000L + 12219292800000L));
        System.out.println(Instant.ofEpochMilli(TimeUnit.MICROSECONDS.toMillis((new UUID(msb, lsb).timestamp()-SnowballGenerator.GREGORIAN_EPOCH*1000)/10)));
    }
    @Test
    public void time(){
        long nanee = System.nanoTime();
        System.out.println(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        System.out.println(TimeUnit.NANOSECONDS.toSeconds(nanee));
        System.out.println((nanee % 1000L));
        long time = (System.currentTimeMillis() * 1000L) + (System.nanoTime() % 1000L);
//        long time = System.currentTimeMillis();
        long nanos = (time - (time / 1000 * 1000)) * 10;
System.out.println(nanos);
System.out.println(Instant.ofEpochMilli(-12219292800000L));
        long nanosSince = ((time/1000 + 12219292800000L) * 10000) + nanos;
        System.out.println(Instant.ofEpochMilli(nanosSince));
        long msb = 0L;
        msb |= (0x00000000ffffffffL & nanosSince) << 32;
        msb |= (0x0000ffff00000000L & nanosSince) >>> 16;
        msb |= (0xffff000000000000L & nanosSince) >>> 48;
        msb |= 0x0000000000001000L; // sets the version to 1.
        long tet = (new UUID(msb,msb).timestamp() / 10) - 12219292800000L * 1000;

System.out.println(Instant.ofEpochMilli(TimeUnit.MICROSECONDS.toMillis(tet)));
System.out.println(Instant.ofEpochSecond(TimeUnit.NANOSECONDS.toSeconds(tet), TimeUnit.MICROSECONDS.toNanos(tet)));
        System.out.println(Clock.systemUTC().instant());
        System.out.println(Instant.ofEpochSecond(TimeUnit.NANOSECONDS.toSeconds(time), TimeUnit.NANOSECONDS.toNanos(time)));
        System.out.println(Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(time)));
        System.out.println(Instant.ofEpochSecond(time/1000_000_000L, time%1000_000_000L));
    }
    @Test
    public void sss() throws SocketException, UnknownHostException {
        System.out.println(System.currentTimeMillis());
        System.out.println(System.nanoTime());
        System.out.println(System.nanoTime());
        System.out.println(Duration.between(Instant.EPOCH, Clock.systemUTC().instant()).getNano());
        System.out.println(System.nanoTime() - Duration.between(Instant.EPOCH, Clock.systemUTC().instant()).getNano());
        System.out.println(Instant.ofEpochMilli(System.nanoTime()/10));
        System.out.println(System.currentTimeMillis() *1000_000 + System.nanoTime());
//        1628445321476321108
//        1628669025551654322
        long  nanoss = System.currentTimeMillis()*1000_000+System.nanoTime();
//        long  nanoss = System.currentTimeMillis() * 10E9 + System.nanoTime();
        System.out.println("NANA"+nanoss);
        Clock clock = Clock.systemDefaultZone();
        Instant instant = clock.instant();   // or Instant.now();
        long seconds = instant.getEpochSecond();
        long nano = System.nanoTime()%1000_000_000;
//        long nano = instant.getNano();
//        new Timestamp(System.currentTimeMillis()).getNanos()
        System.out.println(nano);
        nanoss = seconds*1000_000_000 + nano;
        System.out.println(seconds*1000_000_000 + nano);
        System.out.println(instant);
        System.out.println(System.nanoTime()%1000000000);
        System.out.println(TimeUnit.MICROSECONDS.toNanos(Math.floorMod(System.nanoTime(), TimeUnit.SECONDS.toMicros(1))));
        System.out.println(Instant.ofEpochMilli(seconds*1000_000_000 + nano));
        System.out.println(Instant.ofEpochSecond(nanoss/1000_000_000L, nanoss%1000_000_000L).atOffset(ZoneOffset.UTC));
        Generators.timeBasedGenerator().generate();
        Calendar uuidEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        uuidEpoch.clear();
        uuidEpoch.set(1582, 9, 15, 0, 0, 0); // 9 = October
        long epochMillis = uuidEpoch.getTime().getTime();
System.out.println("epochMillis" + epochMillis);
//        long time = (uuid.timestamp() / 10000L) + epochMillis;

        System.out.println(ZonedDateTime.of(1582, 10, 15, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli());
        SnowballGenerator generator = new SnowballGenerator();
        long id = generator.generateId();
        UUID parse = generator.to(id);
        System.out.println("UUID:" + Instant.ofEpochMilli((parse.timestamp()-0x01b21dd213814000L)/10000));
        System.out.println("UUID:" + Instant.ofEpochMilli(parse.timestamp()/10000L-12219323272000L));
        System.out.println("UUID:" + Instant.ofEpochSecond(parse.timestamp()/10000000L));
        System.out.println("UUID:" + parse.timestamp());
        System.out.println("UUID:" + parse.variant());
        System.out.println("UUID:" + parse.node());
        System.out.println("UUID:" + parse.clockSequence());
        System.out.println("UUID:" + Generators.timeBasedGenerator().generate().timestamp());
        System.out.println("UUID:" + Generators.timeBasedGenerator().generate().variant());
        System.out.println("UUID:" + Generators.timeBasedGenerator().generate().node());
        System.out.println("UUID:" + Generators.timeBasedGenerator().generate().clockSequence());
    }
    @Test
    public void test() throws SocketException, UnknownHostException {
        System.out.println(Long.toBinaryString(0x0000FFFFFFFFFFFFL));
        SnowballGenerator generator = new SnowballGenerator();
        long id = generator.generateId();
        long[] parse = generator.parse(id);
        System.out.println("0:" + parse[0]);
        System.out.println("1:" + parse[1]);
        System.out.println("2:" + parse[2]);
//        UUID:232379255050388
//        UUID:0
//        UUID:88600131211264
//        UUID:755
        System.out.println("UUID:" + new UUID(id, id).timestamp());
        System.out.println("UUID:" + new UUID(id, id).variant());
        System.out.println("UUID:" + new UUID(id, id).node());
        System.out.println("UUID:" + new UUID(id, id).clockSequence());
    }

    private static long get64LeastSignificantBitsForVersion1() {
        Random random = new Random();
        long random63BitLong = random.nextLong() & 0x3FFFFFFFFFFFFFFFL;
        long variant3BitFlag = 0x8000000000000000L;
        return random63BitLong + variant3BitFlag;
    }
    public static UUID generateType1UUID() {
        long most64SigBits = get64MostSignificantBitsForVersion1();
        long least64SigBits = get64LeastSignificantBitsForVersion1();
        return new UUID(most64SigBits, least64SigBits);
    }
    private static long get64MostSignificantBitsForVersion1() {
//        Instant.
        ZonedDateTime.of(1582, 10, 15, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli();
        LocalDateTime start = LocalDateTime.of(1582, 10, 15, 0, 0, 0);
        Duration duration = Duration.between(start, LocalDateTime.now());
        long timeForUuidIn100Nanos = duration.getSeconds() * 10000000 + duration.getNano() * 100;
        long least12SignificatBitOfTime = (timeForUuidIn100Nanos & 0x000000000000FFFFL) >> 4;
        long version = 1 << 12;
        return (timeForUuidIn100Nanos & 0xFFFFFFFFFFFF0000L) + version + least12SignificatBitOfTime;
    }

    @Test
    void generateId() throws SocketException, UnknownHostException {
        SnowballGenerator generator = new SnowballGenerator();
        ThreadUtils.get(() -> {
            Long id = generator.generateId();
            UUID uuid = generator.to(id);
            System.out.println("UUID:" + uuid);
            System.out.println("UUID:" + Instant.now());
            System.out.println("UUIDtimestamp:" + Instant.ofEpochMilli((uuid.timestamp() / 10000) + SnowballGenerator.GREGORIAN_EPOCH));
            System.out.println("UUIDtimestamp:" + uuid.timestamp());
            System.out.println("UUIDvariant:" + uuid.variant());
            System.out.println("UUIDnode:" + uuid.node());
            System.out.println("UUIDclockSequence:" + uuid.clockSequence());
            System.out.println(System.currentTimeMillis());
            return id;
        }, 10);
    }
}
package com.f4sitive.account.util;

import org.springframework.util.function.SupplierUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ThreadUtils {
    public static <T> void get(Callable<T> callable, int thread, int count) throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(thread);
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            service.submit(SupplierUtils.resolve(() -> {
                latch.countDown();
                return callable;
            }));
        }
        latch.await();
    }

    public static <T> void get(Supplier<T> supplier, int thread) {
        ExecutorService executor = Executors.newFixedThreadPool(thread);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < thread; i++) {
//            CompletableFuture.
            futures.add(executor.submit(() -> SupplierUtils.resolve(supplier)));
        }
        executor.shutdown();
        futures.stream().forEachOrdered(future -> {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}

package com.f4sitive.account.util;

import org.springframework.util.function.SupplierUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ThreadUtils {
    public static <T> void get(Supplier<T> supplier, int thread) {
        ExecutorService executor = Executors.newFixedThreadPool(thread);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < thread; i++) {
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

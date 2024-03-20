package com.owl.lyra;

import android.os.Handler;
import android.os.Looper;


import com.owl.lyra.services.LoggingService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentTask {
    private final ExecutorService executor;
    private final Handler handler;

    private static ConcurrentTask instance = null;

    // TESTING VARIABLE - REMOVE LATER!!!!
    public static int counter;

    private ConcurrentTask() {
        executor = Executors.newCachedThreadPool();
        handler = new Handler(Looper.getMainLooper());
        counter++;
        LoggingService.Logger.addRecordToLog("CONCURRENT TASK INSTANCE CREATED, INSTANCE NUMBER: " + counter);
    }

    public static ConcurrentTask getInstance() {
        if(instance == null) {
            instance = new ConcurrentTask();
        }

        return instance;
    }

    public interface Callback<R> {
        void onComplete(R result);
    }

    public interface ExceptionCallback<Exception> {
        void onError(Exception result);
    }

    public <R> void executeAsync(Callable<R> callable, Callback<R> successCallback, ExceptionCallback<Exception> failureCallback) {
        executor.execute(() -> {
            R result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                failureCallback.onError(e);
            }
            R finalResult = result;
            handler.post(() -> {
                successCallback.onComplete(finalResult);
            });
        });
    }

    public <R> void executeAsync(Callable<R> callable, Callback<R> successCallback) {
        executor.execute(() -> {
            R result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            R finalResult = result;
            handler.post(() -> {
                successCallback.onComplete(finalResult);
            });
        });
    }
}

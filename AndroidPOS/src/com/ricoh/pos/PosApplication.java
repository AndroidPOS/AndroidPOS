package com.ricoh.pos;

import android.app.Application;
import android.os.Environment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class PosApplication extends Application {

    private static final String EXCEPTION_LOGS_FILE_NAME = "/Ricoh/exception_logs.log";
    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler savedUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    String logFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + EXCEPTION_LOGS_FILE_NAME;
                    FileWriter fileWriter = new FileWriter(logFilePath, true);
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.println(new Date().toString());
                    throwable.printStackTrace(printWriter);
                    printWriter.println("");
                    printWriter.println("----------");
                    printWriter.println("");
                    printWriter.flush();
                    printWriter.close();
                    fileWriter.close();
                } catch (IOException e) {
                } finally {
                    savedUncaughtExceptionHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
}
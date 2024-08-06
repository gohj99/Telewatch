//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2021
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.drinkless.td.libcore.telegram;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main class for interaction with the TDLib.
 */
public final class Client implements Runnable {
    /**
     * Interface for handler for results of queries to TDLib and incoming updates from TDLib.
     */
    public interface ResultHandler {
        /**
         * Callback called on result of query to TDLib or incoming update from TDLib.
         *
         * @param object Result of query or update of type TdApi.Update about new events.
         */
        void onResult(TdApi.Object object);
    }

    /**
     * Interface for handler of exceptions thrown while invoking ResultHandler.
     * By default, all such exceptions are ignored.
     * All exceptions thrown from ExceptionHandler are ignored.
     */
    public interface ExceptionHandler {
        /**
         * Callback called on exceptions thrown while invoking ResultHandler.
         *
         * @param e Exception thrown by ResultHandler.
         */
        void onException(Throwable e);
    }

    /**
     * Sends a request to the TDLib.
     *
     * @param query            Object representing a query to the TDLib.
     * @param resultHandler    Result handler with onResult method which will be called with result
     *                         of the query or with TdApi.Error as parameter. If it is null, nothing
     *                         will be called.
     * @param exceptionHandler Exception handler with onException method which will be called on
     *                         exception thrown from resultHandler. If it is null, then
     *                         defaultExceptionHandler will be called.
     * @throws NullPointerException if query is null.
     */
    public void send(TdApi.Function query, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        if (query == null) {
            throw new NullPointerException("query is null");
        }

        readLock.lock();
        try {
            if (isClientDestroyed) {
                if (resultHandler != null) {
                    handleResult(new TdApi.Error(500, "Client is closed"), resultHandler, exceptionHandler);
                }
                return;
            }

            long queryId = currentQueryId.incrementAndGet();
            handlers.put(queryId, new Handler(resultHandler, exceptionHandler));
            NativeClient.clientSend(nativeClientId, queryId, query);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Sends a request to the TDLib with an empty ExceptionHandler.
     *
     * @param query         Object representing a query to the TDLib.
     * @param resultHandler Result handler with onResult method which will be called with result
     *                      of the query or with TdApi.Error as parameter. If it is null, then
     *                      defaultExceptionHandler will be called.
     * @throws NullPointerException if query is null.
     */
    public void send(TdApi.Function query, ResultHandler resultHandler) {
        send(query, resultHandler, null);
    }

    /**
     * Synchronously executes a TDLib request. Only a few marked accordingly requests can be executed synchronously.
     *
     * @param query Object representing a query to the TDLib.
     * @return request result.
     * @throws NullPointerException if query is null.
     */
    public static TdApi.Object execute(TdApi.Function query) {
        if (query == null) {
            throw new NullPointerException("query is null");
        }
        return NativeClient.clientExecute(query);
    }

    /**
     * Overridden method from Runnable, do not call it directly.
     */
    @Override
    public void run() {
        while (!stopFlag) {
            receiveQueries(300.0 /*seconds*/);
        }
    }

    /**
     * Creates new Client.
     *
     * @param updateHandler           Handler for incoming updates.
     * @param updateExceptionHandler  Handler for exceptions thrown from updateHandler. If it is null, exceptions will be iggnored.
     * @param defaultExceptionHandler Default handler for exceptions thrown from all ResultHandler. If it is null, exceptions will be iggnored.
     * @return created Client
     */
    public static Client create(ResultHandler updateHandler, ExceptionHandler updateExceptionHandler, ExceptionHandler defaultExceptionHandler) {
        Client client = new Client(updateHandler, updateExceptionHandler, defaultExceptionHandler);
        new Thread(client, "TDLib thread").start();
        return client;
    }

    /**
     * Changes TDLib log verbosity.
     *
     * @deprecated As of TDLib 1.4.0 in favor of {@link TdApi.SetLogVerbosityLevel}, to be removed in the future.
     * @param newLogVerbosity New value of log verbosity. Must be non-negative.
     *                        Value 0 corresponds to android.util.Log.ASSERT,
     *                        value 1 corresponds to android.util.Log.ERROR,
     *                        value 2 corresponds to android.util.Log.WARNING,
     *                        value 3 corresponds to android.util.Log.INFO,
     *                        value 4 corresponds to android.util.Log.DEBUG,
     *                        value 5 corresponds to android.util.Log.VERBOSE,
     *                        value greater than 5 can be used to enable even more logging.
     *                        Default value of the log verbosity is 5.
     * @throws IllegalArgumentException if newLogVerbosity is negative.
     */
    @Deprecated
    public static void setLogVerbosityLevel(int newLogVerbosity) {
        if (newLogVerbosity < 0) {
            throw new IllegalArgumentException("newLogVerbosity can't be negative");
        }
        NativeClient.setLogVerbosityLevel(newLogVerbosity);
    }

    /**
     * Sets file path for writing TDLib internal log.
     * By default TDLib writes logs to the Android Log.
     * Use this method to write the log to a file instead.
     *
     * @deprecated As of TDLib 1.4.0 in favor of {@link TdApi.SetLogStream}, to be removed in the future.
     * @param filePath Path to a file for writing TDLib internal log. Use an empty path to
     *                 switch back to logging to the Android Log.
     * @return whether opening the log file succeeded
     */
    @Deprecated
    public static boolean setLogFilePath(String filePath) {
        return NativeClient.setLogFilePath(filePath);
    }

    /**
     * Changes maximum size of TDLib log file.
     *
     * @deprecated As of TDLib 1.4.0 in favor of {@link TdApi.SetLogStream}, to be removed in the future.
     * @param maxFileSize Maximum size of the file to where the internal TDLib log is written
     *                    before the file will be auto-rotated. Must be positive. Defaults to 10 MB.
     * @throws IllegalArgumentException if max_file_size is non-positive.
     */
    @Deprecated
    public static void setLogMaxFileSize(long maxFileSize) {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("maxFileSize should be positive");
        }
        NativeClient.setLogMaxFileSize(maxFileSize);
    }

    /**
     * Closes Client.
     */
    public void close() {
        writeLock.lock();
        try {
            if (isClientDestroyed) {
                return;
            }
            if (!stopFlag) {
                send(new TdApi.Close(), null);
            }
            isClientDestroyed = true;
            while (!stopFlag) {
                Thread.yield();
            }
            if (handlers.size() != 1) {
                receiveQueries(0.0);

                for (Long key : handlers.keySet()) {
                    if (key != 0) {
                        processResult(key, new TdApi.Error(500, "Client is closed"));
                    }
                }
            }
            NativeClient.destroyClient(nativeClientId);
            clientCount.decrementAndGet();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * This function is called from the JNI when a fatal error happens to provide a better error message.
     * It shouldn't return. Do not call it directly.
     *
     * @param errorMessage Error message.
     */
    static void onFatalError(String errorMessage) {
        final class ThrowError implements Runnable {
            private final String errorMessage;

            private ThrowError(String errorMessage) {
                this.errorMessage = errorMessage;
            }

            @Override
            public void run() {
                if (isExternalError(errorMessage)) {
                    processExternalError();
                    return;
                }

                throw new ClientException("TDLib fatal error (" + clientCount.get() + "): " + errorMessage);
            }

            private void processExternalError() {
                throw new ClientException("Fatal error (" + clientCount.get() + "): " + errorMessage);
            }
        }

        new Thread(new ThrowError(errorMessage), "TDLib fatal error thread").start();
        while (true) {
            try {
                Thread.sleep(1000 /* milliseconds */);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean isDatabaseBrokenError(String message) {
        return message.contains("Wrong key or database is corrupted") ||
                message.contains("SQL logic error or missing database") ||
                message.contains("database disk image is malformed") ||
                message.contains("file is encrypted or is not a database") ||
                message.contains("unsupported file format") ||
                (message.contains("Database was deleted during execution and can't be recreated") &&
                message.contains("PosixError : No such file or directory"));
    }

    private static boolean isDiskFullError(String message) {
        return message.contains("PosixError : No space left on device") ||
                message.contains("database or disk is full");
    }

    private static boolean isDiskError(String message) {
        return message.contains("I/O error") || message.contains("Structure needs cleaning");
    }

    private static boolean isExternalError(String message) {
        return isDatabaseBrokenError(message) || isDiskFullError(message) || isDiskError(message);
    }

    private static final class ClientException extends RuntimeException {
        private ClientException(String message) {
            super(message);
        }
    }

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private static AtomicLong clientCount = new AtomicLong();

    private volatile boolean stopFlag = false;
    private volatile boolean isClientDestroyed = false;
    private final long nativeClientId;

    private final ConcurrentHashMap<Long, Handler> handlers = new ConcurrentHashMap<Long, Handler>();
    private final AtomicLong currentQueryId = new AtomicLong();

    private volatile ExceptionHandler defaultExceptionHandler = null;

    private static final int MAX_EVENTS = 1000;
    private final long[] eventIds = new long[MAX_EVENTS];
    private final TdApi.Object[] events = new TdApi.Object[MAX_EVENTS];

    private static class Handler {
        final ResultHandler resultHandler;
        final ExceptionHandler exceptionHandler;

        Handler(ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
            this.resultHandler = resultHandler;
            this.exceptionHandler = exceptionHandler;
        }
    }

    private Client(ResultHandler updateHandler, ExceptionHandler updateExceptionHandler, ExceptionHandler defaultExceptionHandler) {
        clientCount.incrementAndGet();
        nativeClientId = NativeClient.createClient();
        handlers.put(0L, new Handler(updateHandler, updateExceptionHandler));
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void processResult(long id, TdApi.Object object) {
        Handler handler;
        if (id == 0) {
            // update handler stays forever
            handler = handlers.get(id);

            if (object instanceof TdApi.UpdateAuthorizationState) {
                if (((TdApi.UpdateAuthorizationState) object).authorizationState instanceof TdApi.AuthorizationStateClosed) {
                    stopFlag = true;
                }
            }
        } else {
            handler = handlers.remove(id);
        }
        if (handler == null) {
            return;
        }

        handleResult(object, handler.resultHandler, handler.exceptionHandler);
    }

    private void handleResult(TdApi.Object object, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        if (resultHandler == null) {
            return;
        }

        try {
            resultHandler.onResult(object);
        } catch (Throwable cause) {
            if (exceptionHandler == null) {
                exceptionHandler = defaultExceptionHandler;
            }
            if (exceptionHandler != null) {
                try {
                    exceptionHandler.onException(cause);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void receiveQueries(double timeout) {
        int resultN = NativeClient.clientReceive(nativeClientId, eventIds, events, timeout);
        for (int i = 0; i < resultN; i++) {
            processResult(eventIds[i], events[i]);
            events[i] = null;
        }
    }
}

//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2021
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package org.drinkless.td.libcore.telegram;

import android.util.Log;

/**
 * This class is used internally by Client to send requests to the TDLib.
 */
final class NativeClient {
    static {
        try {
            System.loadLibrary("tdjni");
            Log.w("DLTD", "TDJNI loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.w("DLTD", "Can't find tdjni", e);
        }
    }

    public static native long createClient();

    public static native void destroyClient(long clientId);

    public static native void clientSend(long clientId, long eventId, TdApi.Object event);

    public static native int clientReceive(long clientId, long[] eventIds, TdApi.Object[] events, double timeout);

    public static native TdApi.Object clientExecute(TdApi.Object event);

    public static native void setLogVerbosityLevel(int newLogVerbosity);

    public static native boolean setLogFilePath(String filePath);

    public static native void setLogMaxFileSize(long maxFileSize);

    //Just for testing
    public static native TdApi.Object pingPong(TdApi.Object object);

    public static native void ping(TdApi.Object object);
}

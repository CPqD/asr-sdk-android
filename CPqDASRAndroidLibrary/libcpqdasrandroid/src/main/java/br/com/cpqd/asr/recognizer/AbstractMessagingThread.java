/*******************************************************************************
 * Copyright 2017 CPqD. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package br.com.cpqd.asr.recognizer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;

/**
 * <p>Abstract {@link HandlerThread} used for inter-thread communication.
 * It contains a {@link Handler} associated to the looper of this thread
 * and exposes some of the handler's methods so an external thread can
 * handle messages for an instance of this thread to process.</p>
 * <p>Subclasses are expected to implement {@link Handler.Callback},
 * that is, how received messages should be interpreted.</p>
 */
public abstract class AbstractMessagingThread extends HandlerThread implements Handler.Callback {

    private Handler mHandler;

    /**
     * Sets up object initial state.
     *
     * @param threadName thread name.
     */
    AbstractMessagingThread(@NonNull String threadName) {

        super(threadName);

        setDaemon(true);

        mHandler = null;
    }

    /**
     * @see Handler#obtainMessage()
     */
    public Message obtainMessage() {

        if (mHandler == null) {

            mHandler = new Handler(getLooper(), this);
        }

        return mHandler.obtainMessage();
    }

    /**
     * @see Handler#sendMessageDelayed(Message, long)
     */
    boolean sendMessageDelayed(Message msg, long delayMillis) {

        if (mHandler == null) {

            mHandler = new Handler(getLooper(), this);
        }

        return mHandler.sendMessageDelayed(msg, delayMillis);
    }

    /**
     * @see Handler#removeMessages(int)
     */
    void removeMessages(int what) {

        if (mHandler == null) {

            mHandler = new Handler(getLooper(), this);
        }

        mHandler.removeMessages(what);
    }

    /**
     * Handles received messages.
     * Subclasses are expected to define message types, e.g. by defining codes
     * for {@link Message#arg1}, and to implement the desired behavior
     * that follows the arrival of the messages.
     *
     * @param msg the received message.
     * @return it seems that this method should return {@code true} if
     * it consumes the given message, or {@code false} otherwise.
     * See the source of {@link Handler#dispatchMessage(Message)}.
     */
    @Override
    public abstract boolean handleMessage(Message msg);
}

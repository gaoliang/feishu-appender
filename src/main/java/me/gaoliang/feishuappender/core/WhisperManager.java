/**
 * Copyright (c) 2013 Eclectic Logic LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package me.gaoliang.feishuappender.core;

import me.gaoliang.feishuappender.logback.FeishuAppender;
import me.gaoliang.feishuappender.spi.Message;
import me.gaoliang.feishuappender.spi.MessageWriter;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The core logic is coordinated by this class. It tracks mufflers for various error messages and schedules digests.
 *
 * @author Karthik Abram
 *
 */
public class WhisperManager<E> extends TimerTask {

    private int suppressionOnMessagesCount;
    private long suppressAfterMillis;

    private long suppressionExpirationAfterMillis;

    private String projectName;
    private List<String> alertUserIds;

    private MessageWriter<E> writer;

    private ConcurrentMap<String, Muffler<E>> queuesByMessage = new ConcurrentHashMap<String, Muffler<E>>();
    private Timer digestTimer;


    public WhisperManager(MessageWriter<E> writer, FeishuAppender feishuAppender) {
        super();
        this.suppressAfterMillis = ParameterUtil.suppressionTimeForSuppression(feishuAppender.getSuppressAfter());
        this.suppressionOnMessagesCount = ParameterUtil.messageCountForSuppression(feishuAppender.getSuppressAfter());
        this.suppressionExpirationAfterMillis = ParameterUtil.expireAfterToMillis(feishuAppender.getExpireAfter());
        this.alertUserIds = Arrays.asList(feishuAppender.getAlertUserIds().split(","));
        this.projectName = feishuAppender.getProjectName();
        this.writer = writer;
    }


    /**
     * @return milliseconds to suppress after.
     */
    public long getSuppressAfter() {
        return suppressAfterMillis;
    }


    /**
     * @return Number of messages to suppress after.
     */
    public int getSuppressionOnMessagesCount() {
        return suppressionOnMessagesCount;
    }


    /**
     * @return Time (in millis) to expire suppression if no messages received in that timeframe.
     */
    public long getSuppressionExpirationTime() {
        return suppressionExpirationAfterMillis;
    }


    public void log(Message<E> message) {
        String messageKey = message.getKeyForDeduplicate();
        // See https://github.com/eclecticlogic/whisper/issues/6
        if (messageKey == null) {
            messageKey = "null"; // ConcurrentHashMap will throw a NPE otherwise.
        }
        Muffler<E> muffler = queuesByMessage.get(messageKey);
        if (muffler == null) {
            muffler = new Muffler<E>(this, messageKey);
            Muffler<E> temp = queuesByMessage.putIfAbsent(messageKey, muffler);
            if (temp != null) {
                muffler = temp;
            }
        }
        muffler.log(message);
    }


    public void remove(String messageKey) {
        queuesByMessage.remove(messageKey);
    }


    /**
     * @param message Logs through to the attached appender for immediate logging.
     */
    public void logThrough(Message<E> message) {
        writer.logThrough(message, this.projectName, this.alertUserIds);
    }


    public void start(long digestFrequencyInMillis) {
        digestTimer = new Timer("whisper-timer", true);
        digestTimer.scheduleAtFixedRate(this, digestFrequencyInMillis, digestFrequencyInMillis);
    }


    public void stop() {
        digestTimer.cancel();
    }


    @Override
    public void run() {
        Digest digest = new Digest();
        for (Muffler<E> muffler : queuesByMessage.values()) {
            muffler.digest(digest);
        }
        if (digest.isMessagesSuppressed()) {
            writer.logDigest(digest, this.projectName, this.alertUserIds);
            digest.clear();
        }
    }
}

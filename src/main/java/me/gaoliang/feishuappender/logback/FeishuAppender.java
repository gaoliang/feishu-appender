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
package me.gaoliang.feishuappender.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import me.gaoliang.feishuappender.core.ParameterUtil;
import me.gaoliang.feishuappender.core.WhisperManager;

/**
 * @author kabram
 *
 */
public class FeishuAppender extends AppenderBase<ILoggingEvent> {


    private WhisperManager<ILoggingEvent> whisperManager;
    private String suppressAfter;
    private String expireAfter;
    private String digestFrequency;
    private String projectName;
    private String alertUserIds;

    public String getSuppressAfter() {
        return suppressAfter;
    }


    public void setSuppressAfter(String suppressionAfter) {
        this.suppressAfter = suppressionAfter;
    }


    public String getExpireAfter() {
        return expireAfter;
    }


    public void setExpireAfter(String expireAfter) {
        this.expireAfter = expireAfter;
    }


    public String getDigestFrequency() {
        return digestFrequency;
    }


    public void setDigestFrequency(String digestFrequency) {
        this.digestFrequency = digestFrequency;
    }

    public String getAlertUserIds() {
        return alertUserIds;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    protected void append(ILoggingEvent event) {
        LogbackMessage message = new LogbackMessage(event);
        whisperManager.log(message);
    }

    @Override
    public void start() {
        super.start();
        whisperManager = new WhisperManager<>(new FeishuMessageWriter(), this);
        whisperManager.start(ParameterUtil.digestFrequencyToMillis(getDigestFrequency()));
    }


    @Override
    public void stop() {
        whisperManager.stop();
        super.stop();
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setAlertUserIds(String alertUserIds) {
        this.alertUserIds = alertUserIds;
    }
}

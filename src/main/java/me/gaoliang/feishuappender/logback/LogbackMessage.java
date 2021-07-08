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
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import me.gaoliang.feishuappender.spi.AbstractMessage;

/**
 * @author Karthik Abram
 *
 */
public class LogbackMessage extends AbstractMessage<ILoggingEvent> {

    public LogbackMessage(ILoggingEvent event) {
        super(event);
    }


    @Override
    public long getMessageAge() {
        return System.currentTimeMillis() - getEvent().getTimeStamp();
    }


    @Override
    public long getMessageTime() {
        return getEvent().getTimeStamp();
    }


    @Override
    public String getCanonicalMessage() {
        return getEvent().getMessage();
    }


    @Override
    public String getMessage() {
        return getEvent().getFormattedMessage();
    }


    @Override
    public String getFullMessage() {
        if (getEvent().getThrowableProxy() != null) {
            return ThrowableProxyUtil.asString(getEvent().getThrowableProxy());
        } else {
            return getMessage();
        }
    }

    @Override
    public String getKeyForDeduplicate() {
        if (getEvent().getThrowableProxy() != null) {
            return getCanonicalMessage() + " => " + getEvent().getThrowableProxy().getClassName();
        }
        return getCanonicalMessage();
    }

}

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
package me.gaoliang.feishuappender.spi;


/**
 * @author Karthik Abram
 *
 */
public abstract class AbstractMessage<E> implements Message<E> {

    private E event;

    public AbstractMessage(E event) {
        super();
        this.event = event;
    }

    @Override
    public E getEvent() {
        return event;
    }
}

/*
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.bonitasoft.performance.perfcake;

import java.util.List;

import org.apache.log4j.Logger;
import org.bonitasoft.engine.TestsInitializer;
import org.perfcake.message.MessageTemplate;
import org.perfcake.message.generator.DefaultMessageGenerator;
import org.perfcake.message.sender.MessageSenderManager;

/**
 * @author Baptiste Mesta
 */
public class BonitaGenerator extends DefaultMessageGenerator {

    static Logger logger = Logger.getLogger(BonitaGenerator.class);

    @Override
    public void init(MessageSenderManager messageSenderManager, List<MessageTemplate> messageStore) throws Exception {
        logger.warn("init");
        TestsInitializer.beforeAll();
        super.init(messageSenderManager, messageStore);
        for (MessageTemplate messageTemplate : messageStore) {
            messageTemplate.getMessage().getProperty("testName")
        }
    }

    @Override
    public void generate() throws Exception {
        logger.warn("generate");
        Thread.sleep(50);
        super.generate();
    }

    @Override
    protected void shutdown() throws InterruptedException {
        Thread.sleep(50);
        super.shutdown();
        logger.warn("shutdown");
        try {
            TestsInitializer.afterAll();
       } catch (Exception e) {
           logger.error("on shutdown:",e);
        }
    }
}

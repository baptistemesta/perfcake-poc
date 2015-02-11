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

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bonitasoft.performance.Credential;
import org.bonitasoft.performance.SimpleTest;
import org.perfcake.PerfCakeException;
import org.perfcake.message.Message;
import org.perfcake.message.sender.AbstractSender;
import org.perfcake.reporting.MeasurementUnit;

/**
 * @author Baptiste Mesta
 */
public class BonitaSender extends AbstractSender {

    static Logger logger = Logger.getLogger(BonitaGenerator.class);

    private String test = "";

    private SimpleTest theTest;
    private String tenant;
    private String username;
    private String password;
    private Credential credential;

    @Override
    public void init() throws Exception {
        logger.warn("init sender with test "+test);
        Class<?> testClass = Thread.currentThread().getContextClassLoader().loadClass(test);
        theTest = (SimpleTest) testClass.newInstance();

        credential = new Credential(tenant, username, password);
        theTest.install();
        Thread.sleep(50);

    }

    @Override
    public void close() throws PerfCakeException {

        try {
            theTest.uninstall();
        } catch (Exception e) {
            throw new PerfCakeException(e);
        }
        logger.warn("close sender");
    }

    @Override
    public Serializable doSend(Message message, Map<String, String> map, MeasurementUnit measurementUnit) throws Exception {
        logger.warn("do send " + message);
        Thread.sleep(500);
        return (Serializable) theTest.executeInstance();
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

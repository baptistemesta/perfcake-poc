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

package org.bonitasoft.performance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.actor.ActorCriterion;
import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.flownode.GatewayType;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.impl.AutomaticTaskDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ConnectorDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.operation.OperationBuilder;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.test.APITestUtil;

public class PerfProcess1 implements SimpleTest {

    private final Map<String, Object> tenantProcessId = new HashMap<String, Object>();

    private static final String PROCESS_VERSION = "1.0";
    private APITestUtil apiTestUtil;

    @Override
    public String getVersion() {
        return PROCESS_VERSION;
    }

    @Override
    public void install() throws Exception {
        apiTestUtil = new APITestUtil();
        apiTestUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
        final ProcessAPI processAPI = apiTestUtil.getProcessAPI();
        APISession apiSession = apiTestUtil.getSession();

        try
        {
            long subPerfProcess2Id = processAPI.deploy(getSubProcess2BusinessArchive()).getId();
            long subPerfProcess1Id = processAPI.deploy(getSubProcess1BusinessArchive()).getId();
            long perfProcess1Id = processAPI.deploy(getProcess1BusinessArchive()).getId();

            // create actors
            final List<ActorInstance> actors = processAPI.getActors(perfProcess1Id, 0, 1, ActorCriterion.NAME_ASC);
            processAPI.addUserToActor(actors.get(0).getId(), apiSession.getUserId());

            processAPI.enableProcess(subPerfProcess1Id);
            processAPI.enableProcess(subPerfProcess2Id);
            processAPI.enableProcess(perfProcess1Id);

            long tenantId = apiSession.getTenantId();
            tenantProcessId.put(tenantId + "@" + buildProcessKey(tenantId), perfProcess1Id);
            tenantProcessId.put(tenantId + "@" + buildSubProcess1Key(tenantId), subPerfProcess1Id);
            tenantProcessId.put(tenantId + "@" + buildSubProcess2Key(tenantId), subPerfProcess2Id);

        } catch (AlreadyExistsException e) {
            long tenantId = apiSession.getTenantId();
            long subPerfProcess1Id = processAPI.getProcessDefinitionId(getPerfSubProcess1Name(), getVersion());
            long subPerfProcess2Id = processAPI.getProcessDefinitionId(getPerfSubProcess2Name(), getVersion());
            long perfProcess1Id = processAPI.getProcessDefinitionId(getName(), getVersion());
            tenantProcessId.put(tenantId + "@" + buildProcessKey(tenantId), perfProcess1Id);
            tenantProcessId.put(tenantId + "@" + buildSubProcess1Key(tenantId), subPerfProcess1Id);
            tenantProcessId.put(tenantId + "@" + buildSubProcess2Key(tenantId), subPerfProcess2Id);
        } finally {
            apiTestUtil.logoutOnTenant();
        }
    }

    private BusinessArchive getProcess1BusinessArchive() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(getName(), "1.0");
        final ExpressionBuilder expBuilder = new ExpressionBuilder();

        processBuilder.addShortTextData("str1", expBuilder.createConstantStringExpression("defaultStr1Value"));
        processBuilder.addShortTextData("str2", expBuilder.createConstantStringExpression("defaultStr1Value"));
        processBuilder.addShortTextData("str3", expBuilder.createConstantStringExpression("defaultStr1Value"));
        processBuilder.addIntegerData("int1", expBuilder.createConstantIntegerExpression(1));
        processBuilder.addIntegerData("int2", expBuilder.createConstantIntegerExpression(2));
        processBuilder.addIntegerData("int3", expBuilder.createConstantIntegerExpression(3));
        processBuilder.addBooleanData("bool1", expBuilder.createConstantBooleanExpression(true));
        processBuilder.addBooleanData("bool2", expBuilder.createConstantBooleanExpression(false));
        processBuilder.addShortTextData("correlationKey", null);

        final String defaultActor = "defaultActor";
        processBuilder.addActor(defaultActor);

        // steps
        processBuilder.addStartEvent("PerfProcess1Start");

        processBuilder.addUserTask("HumanStep1", defaultActor);
        processBuilder.addTransition("PerfProcess1Start", "HumanStep1");

        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep1"), "AutoStep1");
        processBuilder.addTransition("HumanStep1", "AutoStep1");

        processBuilder.addCallActivity("CallSubPerfProcess1", expBuilder.createConstantStringExpression("PerfSubProcess1"),
                expBuilder.createConstantStringExpression("1.0"));
        processBuilder.addTransition("AutoStep1", "CallSubPerfProcess1");

        processBuilder.addGateway("SplitAndGate1", GatewayType.PARALLEL);
        processBuilder.addTransition("CallSubPerfProcess1", "SplitAndGate1");

        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep2"), "AutoStep2");
        processBuilder.addTransition("SplitAndGate1", "AutoStep2");
        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep3"), "AutoStep3");
        processBuilder.addTransition("AutoStep2", "AutoStep3");

        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep4"), "AutoStep4");
        processBuilder.addTransition("SplitAndGate1", "AutoStep4");
        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep5"), "AutoStep5");
        processBuilder.addTransition("AutoStep4", "AutoStep5");

        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep6"), "AutoStep6");
        processBuilder.addTransition("SplitAndGate1", "AutoStep6");
        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep7"), "AutoStep7");
        processBuilder.addTransition("AutoStep6", "AutoStep7");

        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep8"), "AutoStep8");
        processBuilder.addTransition("SplitAndGate1", "AutoStep8");
        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep9"), "AutoStep9");
        processBuilder.addTransition("AutoStep8", "AutoStep9");

        processBuilder.addGateway("MergeAndGate1", GatewayType.PARALLEL);
        processBuilder.addTransition("AutoStep3", "MergeAndGate1");
        processBuilder.addTransition("AutoStep5", "MergeAndGate1");
        processBuilder.addTransition("AutoStep7", "MergeAndGate1");
        processBuilder.addTransition("AutoStep9", "MergeAndGate1");

        processBuilder.addUserTask("HumanStep2", defaultActor);
        processBuilder.addTransition("MergeAndGate1", "HumanStep2");

        processBuilder.addGateway("SplitXORGate2", GatewayType.EXCLUSIVE);
        processBuilder.addTransition("HumanStep2", "SplitXORGate2");

        processBuilder.addUserTask("HumanStep3", defaultActor);
        processBuilder.addTransition("SplitXORGate2", "HumanStep3", expBuilder.createConstantBooleanExpression(true));

        processBuilder.addUserTask("HumanStep4", defaultActor);
        processBuilder.addDefaultTransition("SplitXORGate2", "HumanStep4");

        processBuilder.addGateway("MergeXORGate1", GatewayType.EXCLUSIVE);
        processBuilder.addTransition("HumanStep3", "MergeXORGate1");
        processBuilder.addTransition("HumanStep4", "MergeXORGate1");

        final AutomaticTaskDefinitionBuilder correlationKeyTaskBuilder = processBuilder.addAutomaticTask("CreateCorrelationKey");
        correlationKeyTaskBuilder.addOperation(new OperationBuilder().createSetDataOperation("correlationKey",
                expBuilder.createGroovyScriptExpression("crrelationKeyScript", "return java.util.UUID.randomUUID().toString();", String.class.getName())));
        processBuilder.addTransition("MergeXORGate1", "CreateCorrelationKey");

        addDummyConnectorToTask(processBuilder.addAutomaticTask("AutoStep10"), "AutoStep10");
        processBuilder.addTransition("CreateCorrelationKey", "AutoStep10");

        processBuilder
                .addIntermediateThrowEvent("ThrowMessage1")
                .addMessageEventTrigger("msg1", expBuilder.createConstantStringExpression("PerfSubProcess2"),
                        expBuilder.createConstantStringExpression("CatchMsg1"))
                .addCorrelation(expBuilder.createConstantStringExpression("correlationKey"),
                        expBuilder.createDataExpression("correlationKey", "java.lang.String"));
        processBuilder.addTransition("CreateCorrelationKey", "ThrowMessage1");

        processBuilder.addCallActivity("CallSubPerfProcess2", expBuilder.createConstantStringExpression("PerfSubProcess2"),
                expBuilder.createConstantStringExpression("1.0")).addDataInputOperation(
                new OperationBuilder().createSetDataOperation("correlationKey", expBuilder.createDataExpression("correlationKey", String.class.getName())));
        processBuilder.addTransition("AutoStep10", "CallSubPerfProcess2");

        processBuilder
                .addIntermediateCatchEvent("CatchMessage2")
                .addMessageEventTrigger("throwMsg2")
                .addCorrelation(expBuilder.createConstantStringExpression("correlationKey"),
                        expBuilder.createDataExpression("correlationKey", "java.lang.String"));
        processBuilder.addTransition("CallSubPerfProcess2", "CatchMessage2");

        processBuilder.addGateway("Gateway3", GatewayType.PARALLEL);
        processBuilder.addTransition("ThrowMessage1", "Gateway3");
        processBuilder.addTransition("CatchMessage2", "Gateway3");

        processBuilder.addEndEvent("PerfProcess1End");
        processBuilder.addTransition("Gateway3", "PerfProcess1End");

        final DesignProcessDefinition processDefinition = processBuilder.done();

        final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchiveBuilder.setProcessDefinition(processDefinition);

        final BusinessArchive businessArchive = businessArchiveBuilder.done();

        return businessArchive;
    }

    private void addDummyConnectorToTask(final AutomaticTaskDefinitionBuilder autoTaskBuilder, final String stepName) throws Exception {
        return;
//        final ExpressionBuilder expBuilder = new ExpressionBuilder();
//        final OperationBuilder opBuilder = new OperationBuilder();
//        final String connectorDefinition = "org.bonitasoft.engine.performance.integration.DummyConnector";
//
//        final ConnectorDefinitionBuilder connBuilder = autoTaskBuilder.addConnector("dummyConnector" + stepName, connectorDefinition, "1.0",
//                ConnectorEvent.ON_FINISH);
//
//        connBuilder.addInput("str1", expBuilder.createDataExpression("str1", String.class.getName()));
//        connBuilder.addInput("str2", expBuilder.createDataExpression("str2", String.class.getName()));
//        connBuilder.addInput("str3", expBuilder.createDataExpression("str3", String.class.getName()));
//        connBuilder.addInput("int1", expBuilder.createDataExpression("int1", Integer.class.getName()));
//        connBuilder.addInput("int2", expBuilder.createDataExpression("int2", Integer.class.getName()));
//        connBuilder.addInput("int3", expBuilder.createDataExpression("int3", Integer.class.getName()));
//        connBuilder.addInput("bool1", expBuilder.createDataExpression("bool1", Boolean.class.getName()));
//        connBuilder.addInput("bool2", expBuilder.createDataExpression("bool2", Boolean.class.getName()));
//
//        connBuilder.addOutput(opBuilder.createSetDataOperation("str1", expBuilder.createInputExpression("oStr1", String.class.getName())));
    }

    private BusinessArchive getSubProcess1BusinessArchive() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(getPerfSubProcess1Name(), "1.0");

        processBuilder.addStartEvent("PerfSubProcess1Start");

        // first branch
        processBuilder.addAutomaticTask("SPP1Step1");
        processBuilder.addTransition("PerfSubProcess1Start", "SPP1Step1");
        processBuilder.addAutomaticTask("SPP1Step2");
        processBuilder.addTransition("SPP1Step1", "SPP1Step2");
        processBuilder.addAutomaticTask("SPP1Step3");
        processBuilder.addTransition("SPP1Step2", "SPP1Step3");
        processBuilder.addAutomaticTask("SPP1Step4");
        processBuilder.addTransition("SPP1Step3", "SPP1Step4");

        // second branch
        processBuilder.addAutomaticTask("SPP1Step5");
        processBuilder.addTransition("PerfSubProcess1Start", "SPP1Step5");
        processBuilder.addAutomaticTask("SPP1Step6");
        processBuilder.addTransition("SPP1Step5", "SPP1Step6");
        processBuilder.addAutomaticTask("SPP1Step7");
        processBuilder.addTransition("SPP1Step6", "SPP1Step7");
        processBuilder.addAutomaticTask("SPP1Step8");
        processBuilder.addTransition("SPP1Step7", "SPP1Step8");

        // third branch
        processBuilder.addAutomaticTask("SPP1Step9");
        processBuilder.addTransition("PerfSubProcess1Start", "SPP1Step9");
        processBuilder.addAutomaticTask("SPP1Step10");
        processBuilder.addTransition("SPP1Step9", "SPP1Step10");
        processBuilder.addAutomaticTask("SPP1Step11");
        processBuilder.addTransition("SPP1Step10", "SPP1Step11");
        processBuilder.addAutomaticTask("SPP1Step12");
        processBuilder.addTransition("SPP1Step11", "SPP1Step12");
        // branches in third branch
        processBuilder.addAutomaticTask("SPP1Step13");
        processBuilder.addTransition("SPP1Step12", "SPP1Step13");
        processBuilder.addAutomaticTask("SPP1Step14");
        processBuilder.addTransition("SPP1Step12", "SPP1Step14");
        // thirs branch sub branches merge
        processBuilder.addGateway("SPP1Gateway1", GatewayType.PARALLEL);
        processBuilder.addTransition("SPP1Step13", "SPP1Gateway1");
        processBuilder.addTransition("SPP1Step14", "SPP1Gateway1");

        // final merge
        processBuilder.addGateway("SPP1Gateway2", GatewayType.PARALLEL);
        processBuilder.addTransition("SPP1Step4", "SPP1Gateway2");
        processBuilder.addTransition("SPP1Step8", "SPP1Gateway2");
        processBuilder.addTransition("SPP1Gateway1", "SPP1Gateway2");

        processBuilder.addEndEvent("PerfSubProcess1End");
        processBuilder.addTransition("SPP1Gateway2", "PerfSubProcess1End");

        final DesignProcessDefinition processDefinition = processBuilder.done();

        final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchiveBuilder.setProcessDefinition(processDefinition);

        final BusinessArchive businessArchive = businessArchiveBuilder.done();

        return businessArchive;
    }

    private BusinessArchive getSubProcess2BusinessArchive() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(getPerfSubProcess2Name(), "1.0");
        final ExpressionBuilder expBuilder = new ExpressionBuilder();

        processBuilder.addShortTextData("correlationKey", null);

        processBuilder.addStartEvent("PerfSubProcess2Start");

        processBuilder
                .addIntermediateCatchEvent("CatchMsg1")
                .addMessageEventTrigger("msg1")
                .addCorrelation(expBuilder.createConstantStringExpression("correlationKey"),
                        expBuilder.createDataExpression("correlationKey", "java.lang.String"));
        processBuilder.addTransition("PerfSubProcess2Start", "CatchMsg1");

        processBuilder
                .addIntermediateThrowEvent("ThrowMessage2")
                .addMessageEventTrigger("throwMsg2", expBuilder.createConstantStringExpression("PerfProcess1"),
                        expBuilder.createConstantStringExpression("CatchMessage2"))
                .addCorrelation(expBuilder.createConstantStringExpression("correlationKey"),
                        expBuilder.createDataExpression("correlationKey", "java.lang.String"));
        processBuilder.addTransition("CatchMsg1", "ThrowMessage2");

        processBuilder.addEndEvent("PerfSubProcess2End");
        processBuilder.addTransition("ThrowMessage2", "PerfSubProcess2End");

        final DesignProcessDefinition processDefinition = processBuilder.done();

        final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchiveBuilder.setProcessDefinition(processDefinition);

        final BusinessArchive businessArchive = businessArchiveBuilder.done();

        return businessArchive;
    }

    @Override
    public void uninstall() throws Exception {
        apiTestUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
        APISession apiSession = apiTestUtil.getSession();

        final LoginAPI loginAPI = apiTestUtil.getLoginAPI();

        long perfProcess1Id = getProcess(apiSession);
        long subPerfProcess1Id = getSubProcess1(apiSession);
        long subPerfProcess2Id = getSubProcess2(apiSession);

        apiTestUtil.disableAndDeleteProcess(perfProcess1Id);
        apiTestUtil.disableAndDeleteProcess(subPerfProcess2Id);
        apiTestUtil.disableAndDeleteProcess(subPerfProcess1Id);
        loginAPI.logout(apiSession);
    }

    @Override
    public Map<String, Long> executeInstance() throws Exception {
        apiTestUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
        APISession apiSession = apiTestUtil.getSession();
        try {
            final ProcessAPI processAPI = apiTestUtil.getProcessAPI();
            final long userId = apiSession.getUserId();

            long perfProcess1Id = getProcess(apiSession);

            final long startTime = System.currentTimeMillis();
            final ProcessInstance instance = processAPI.startProcess(perfProcess1Id);
            final long instanceId = instance.getId();

            apiTestUtil.waitForUserTask(instanceId, "HumanStep1");
            apiTestUtil.waitForUserTask(instanceId, "HumanStep2");
            apiTestUtil.waitForUserTask(instanceId, "HumanStep3");
            apiTestUtil.waitForProcessToFinish(instanceId);

            final long instanceExecTime = System.currentTimeMillis() - startTime;

            final Map<String, Long> measures = new TreeMap<String, Long>();
            measures.put("instance exec", instanceExecTime);
            return measures;
        } finally {
            apiTestUtil.logoutOnTenant();
        }
    }

    @Override
    public String getName() {
        return "PerfProcess1";
    }

    public String getPerfSubProcess1Name() {
        return "PerfSubProcess1";
    }

    public String getPerfSubProcess2Name() {
        return "PerfSubProcess2";
    }

    @Override
    public String toString() {
        return getName();
    }

    private String buildProcessKey(final long tenantId) {
        return tenantId + getName() + "@" + getVersion() + "@process";
    }

    private String buildSubProcess1Key(final long tenantId) {
        return tenantId + getName() + "@" + getVersion() + "@subProcess1";
    }

    private String buildSubProcess2Key(final long tenantId) {
        return tenantId + getName() + "@" + getVersion() + "@subProcess2";
    }

    private long getProcess(final APISession apiSession)
    {
        long tenantId = apiSession.getTenantId();
        return (Long) tenantProcessId.get(tenantId + "@" + buildProcessKey(tenantId));
    }

    private long getSubProcess1(final APISession apiSession)
    {
        long tenantId = apiSession.getTenantId();
        return (Long) tenantProcessId.get(tenantId + "@" + buildSubProcess1Key(tenantId));
    }

    private long getSubProcess2(final APISession apiSession)
    {
        long tenantId = apiSession.getTenantId();
        return (Long) tenantProcessId.get(tenantId + "@" + buildSubProcess2Key(tenantId));
    }
}

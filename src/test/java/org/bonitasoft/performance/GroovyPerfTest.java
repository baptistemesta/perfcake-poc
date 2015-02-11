package org.bonitasoft.performance;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.actor.ActorCriterion;
import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.expression.ExpressionConstants;
import org.bonitasoft.engine.io.IOUtil;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.test.APITestUtil;

public class GroovyPerfTest implements SimpleTest {

    private static final String PROCESS_VERSION = "1.0";

    private String bigScript;

    private String apiScript;

    private long activityInstanceId;

    private Expression bigScriptExpr;

    private Expression apiScriptExpr;

    protected Map<String, Map<String, Long>> tenantProcessIds = new HashMap<String, Map<String, Long>>();
    private APITestUtil apiTestUtil;

    @Override
    public String getName() {
        return "GroovyPerf";
    }

    @Override
    public String getVersion() {
        return PROCESS_VERSION;
    }

    @Override
    public void install() throws Exception {
        apiTestUtil = new APITestUtil();
        apiTestUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
        APISession apiSession = apiTestUtil.getSession();
        final ProcessAPI processAPI = apiTestUtil.getProcessAPI();
        final long userId = apiSession.getUserId();

        try {
            final BusinessArchive businessArchive = getBusinessArchive();
            final ProcessDefinition processDef = processAPI.deploy(businessArchive);
            long processDefinitionId = processDef.getId();
            manageActors(apiSession, processDefinitionId);
            processAPI.enableProcess(processDefinitionId);
            storeTenantProcess(processDef.getName(), processDef.getId());
            final long processInstanceId = processAPI.startProcess(processDefinitionId).getId();
            apiTestUtil.waitForUserTask(processInstanceId, "human");

            final Collection<HumanTaskInstance> humanTaskInstances = processAPI.getPendingHumanTaskInstances(userId, 0, 10, ActivityInstanceCriterion.DEFAULT);
            activityInstanceId = humanTaskInstances.iterator().next().getId();

            bigScript = IOUtil.read(this.getClass().getResourceAsStream("/Fib.groovy"));
            bigScriptExpr = new ExpressionBuilder().createGroovyScriptExpression("bigScript", bigScript, Integer.class.getName());
            apiScript = IOUtil.read(this.getClass().getResourceAsStream("/APICall.groovy"));
            apiScriptExpr = new ExpressionBuilder().createGroovyScriptExpression("apiCallScript", apiScript, Collection.class.getName(),
                    // apiAccessor as a dependency:
                    new ExpressionBuilder().createAPIAccessorExpression(),
                    new ExpressionBuilder().createEngineConstant(ExpressionConstants.ACTIVITY_INSTANCE_ID));

        } finally {
            apiTestUtil.logoutOnTenant();
        }
    }

    @Override
    public Map<String, Long> executeInstance() throws Exception {
        apiTestUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
        try {
            final ProcessAPI processAPI = apiTestUtil.getProcessAPI();
            final Map<String, Long> measures = new TreeMap<String, Long>();
            measures.put("[G1.8.6] 1-short script",
                    getExecTime(new ExpressionBuilder().createGroovyScriptExpression("short script", "1+2", Integer.class.getName()), processAPI));
            measures.put("[G1.8.6] 2-api script", getExecTime(apiScriptExpr, processAPI));
            measures.put("[G1.8.6] 3-big script", getExecTime(bigScriptExpr, processAPI));
            return measures;
        } finally {
            apiTestUtil.logoutOnTenant();
        }
    }

    @Override
    public void uninstall() throws Exception {
        apiTestUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
            APISession apiSession = apiTestUtil.getSession();
            final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            try {
                long processDefinitionId = fetchProcessId( getName());
                apiTestUtil.deleteProcessInstanceAndArchived(processDefinitionId);
                processAPI.disableProcess(processDefinitionId);
                processAPI.deleteProcessDefinition(processDefinitionId);
            } finally {
                apiTestUtil.logoutOnTenant();
            }
    }

    private Long getExecTime(final Expression groovyExpression, final ProcessAPI processAPI) throws Exception {
        final long beforeEval = System.currentTimeMillis();
        final Map<Expression, Map<String, Serializable>> expressions = new HashMap<Expression, Map<String, Serializable>>(1);
        expressions.put(groovyExpression, null);
        processAPI.evaluateExpressionsOnActivityInstance(activityInstanceId, expressions);
        final long afterEval = System.currentTimeMillis();
        return afterEval - beforeEval;
    }

    protected BusinessArchive getBusinessArchive() throws Exception {
        final ProcessDefinitionBuilder processBuilder = new ProcessDefinitionBuilder().createNewInstance(getName(), PROCESS_VERSION);
        processBuilder.addActor("actor");
        processBuilder.setActorInitiator("actor");
        processBuilder.addUserTask("human", "actor");

        final DesignProcessDefinition processDefinition = processBuilder.done();
        final BusinessArchiveBuilder businessArchiveBuilder = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchiveBuilder.setProcessDefinition(processDefinition);

        final BusinessArchive businessArchive = businessArchiveBuilder.done();

        return businessArchive;
    }

    protected void manageActors(final APISession apiSession, final long processDefinitionId) throws Exception {
        final ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
        // create actor members:
        final List<ActorInstance> actors = processAPI.getActors(processDefinitionId, 0, 1, ActorCriterion.NAME_ASC);
        processAPI.addUserToActor(actors.get(0).getId(), apiSession.getUserId());
    }

    private void storeTenantProcess( final String processName, final long processDefId) {
        final String tenantName = apiTestUtil.getSession().getTenantName();

        if (!tenantProcessIds.containsKey(tenantName)) {
            tenantProcessIds.put(tenantName, new HashMap<String, Long>());
        }
        Map<String, Long> processIds = tenantProcessIds.get(tenantName);

        processIds.put(processName, processDefId);
    }

    private long fetchProcessId(final String processName) {
        final String tenantName = apiTestUtil.getSession().getTenantName();
        final Map<String, Long> processIds = tenantProcessIds.get(tenantName);

        return processIds.get(processName);
    }

    @Override
    public String toString() {
        return getName();
    }
}

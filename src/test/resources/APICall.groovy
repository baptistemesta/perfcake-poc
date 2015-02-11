import org.bonitasoft.engine.api.APIAccessor
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder

class APICall {
    public static Collection<ActivityInstance> callAPI(APIAccessor apiAccessor, long taskId) {
        def taskList = apiAccessor.getProcessAPI().searchActivities(new SearchOptionsBuilder(0, 10).filter(ActivityInstanceSearchDescriptor.PROCESS_INSTANCE_ID, taskId).done()).getResult();
        def resList = [];
        taskList.each{resList.add(it.toString())};
        def a = [];
        resList.each{ a.add(it.toString() + 5)};
        return a;
    }
}
return APICall.callAPI(apiAccessor, activityInstanceId);
import org.junit.Test;
import org.perfcake.PerfCakeException;
import org.perfcake.scenario.Scenario;
import org.perfcake.scenario.ScenarioLoader;

/**
 * @author Baptiste Mesta
 */
public class BonitaPerfCakeTest {


    @Test
    public void test() throws PerfCakeException {
        Scenario scenario = ScenarioLoader.load("/home/baptiste/git/bonita-performance2/src/test/resources/test.xml");
        scenario.init();
        scenario.run();
    }
}

package cz.muni.fi.civ.newohybat.bpmn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import cz.muni.fi.civ.newohybat.drools.listeners.AgendaListener;
import cz.muni.fi.civ.newohybat.drools.listeners.ProcessListener;
import cz.muni.fi.civ.newohybat.jbpm.itemhandler.CityWorkItemHandler;
import cz.muni.fi.civ.newohybat.jbpm.itemhandler.UnitWorkItemHandler;


public abstract class BaseJUnitTest {
	protected static Logger logger = Logger.getAnonymousLogger();
	protected KieSession ksession;

    protected KieBase kbase;
	
    public BaseJUnitTest() {
        kbase = kbase();
    }

    public void closeTestEnv(){
    	ksession.dispose();
    }

    protected KieBase kbase(){
    	KieServices kieServ = KieServices.Factory.get();

    	KieContainer kContainer = kieServ.getKieClasspathContainer();
    	KieBase kbase = kContainer.getKieBase("org.kie.kbase1");
    	
    	return kbase;
    }
    @Before
	public void before(){
		setSessions(kbase);
	}
	@After
	public void after(){
		ksession.halt();
		ksession.dispose();
		ksession = null;
	}
    protected void setSessions(KieBase kbase){
    	ksession = kbase.newKieSession();
    	appendListeners();
    }
    protected void appendListeners(){
    	ksession.addEventListener(new AgendaListener());
                
        ksession.addEventListener(new ProcessListener());
        
        ksession.getWorkItemManager().registerWorkItemHandler("Unit", new UnitWorkItemHandler(ksession));
        ksession.getWorkItemManager().registerWorkItemHandler("City", new CityWorkItemHandler(ksession));
    }

    protected List<String> getFiredRules(List<AfterMatchFiredEvent> events){
    	List<String> runRules = new ArrayList<String>();
		for(AfterMatchFiredEvent e:events){
			runRules.add(e.getMatch().getRule().getName());
		}
		return runRules;
    }
    
    public void assertProcessInstanceCompleted(long processInstanceId, KieSession ksession) {
    		Assert.assertNull(ksession.getProcessInstance(processInstanceId));
    	}
    	
    	public void assertProcessInstanceAborted(long processInstanceId, KieSession ksession) {
    		Assert.assertNull(ksession.getProcessInstance(processInstanceId));
    	}
    	
    	public void assertProcessInstanceActive(long processInstanceId, KieSession ksession) {
    			Assert.assertNotNull(ksession.getProcessInstance(processInstanceId));
    	}
	
}
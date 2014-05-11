package cz.muni.fi.civ.newohybat.drools.listeners;

import java.util.Map;

import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.runtime.StatefulKnowledgeSession;

public class ProcessListener implements ProcessEventListener{
	public void beforeVariableChanged(
			org.kie.api.event.process.ProcessVariableChangedEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void beforeProcessStarted(ProcessStartedEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void beforeProcessCompleted(
			org.kie.api.event.process.ProcessCompletedEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void beforeNodeLeft(ProcessNodeLeftEvent arg0) {
		// TODO Auto-generated method stub
	}
	
	public void afterVariableChanged(
			org.kie.api.event.process.ProcessVariableChangedEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void afterProcessStarted(ProcessStartedEvent event) {
//        event.getKieRuntime().insert(((WorkflowProcessInstance)event.getProcessInstance()));
    }
	
	public void afterProcessCompleted(ProcessCompletedEvent event){
    	if(event.getProcessInstance().getState()==WorkflowProcessInstance.STATE_COMPLETED){
        	WorkflowProcessInstance wpi = (WorkflowProcessInstance)event.getProcessInstance();
        	VariableScopeInstance variableScope = (VariableScopeInstance) wpi.getContextInstance(VariableScope.VARIABLE_SCOPE);
        	Map<String,Object> variables = variableScope.getVariables();
        	for(Object o:variables.values()){
        		if(o!=null){
        		org.kie.api.runtime.rule.FactHandle h = event.getKieRuntime().getFactHandle(o);
        		if(h!=null){
        			event.getKieRuntime().update(h, o);
        		}
        		}
        	}
    	}
    	FactHandle fH = event.getKieRuntime().getFactHandle(event.getProcessInstance());
    	if(fH!=null){
    		event.getKieRuntime().delete(fH);
    	}
    	((StatefulKnowledgeSession)event.getKieRuntime()).fireAllRules();
    }
	
	public void afterNodeTriggered(ProcessNodeTriggeredEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void afterNodeLeft(ProcessNodeLeftEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}

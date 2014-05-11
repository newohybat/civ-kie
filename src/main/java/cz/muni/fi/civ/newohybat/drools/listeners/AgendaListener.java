package cz.muni.fi.civ.newohybat.drools.listeners;

import java.util.Iterator;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;

public class AgendaListener implements AgendaEventListener{

		public void afterMatchFired(AfterMatchFiredEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void afterRuleFlowGroupActivated(
				org.kie.api.event.rule.RuleFlowGroupActivatedEvent arg0) {
			((StatefulKnowledgeSession)arg0.getKieRuntime()).fireAllRules();
			
		}
		public void afterRuleFlowGroupDeactivated(
				RuleFlowGroupDeactivatedEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void agendaGroupPopped(AgendaGroupPoppedEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void agendaGroupPushed(AgendaGroupPushedEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void beforeMatchFired(BeforeMatchFiredEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void beforeRuleFlowGroupActivated(
				org.kie.api.event.rule.RuleFlowGroupActivatedEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void beforeRuleFlowGroupDeactivated(
				RuleFlowGroupDeactivatedEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void matchCancelled(MatchCancelledEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		public void matchCreated(MatchCreatedEvent arg0) {
			// TODO Auto-generated method stub
//			((StatefulKnowledgeSession)arg0.getKieRuntime()).fireAllRules();
		}
}

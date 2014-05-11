package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.core.event.DebugProcessEventListener;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.CityImprovementEvent;
import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.AdvanceDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;


public class BuildCityImprovementRulesJUnitTest extends BaseJUnitTest {
	/*
	 * Tests build of CityImprovement.
	 * One TurnEvent is needed to complete, because city has sufficient production
	 */
	@Test
    public void testWaitForNewTurnToComplete(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	ksession.addEventListener(new DebugProcessEventListener());
		// Add mock eventlistener
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	CityDTO city = getCity(1L, "marefy");
    	Set<String> improvements = new HashSet<String>();
    	improvements.add("granary");
    	improvements.add("bank");
    	city.setImprovements(improvements);
    	
    	// 300 resources to spent per turn
    	city.setResourcesSurplus(300);
    	Set<String> enabledImprovements = new HashSet<String>();
    	enabledImprovements.add("courthouse");
    	city.setEnabledImprovements(enabledImprovements);
    	// triggers the buildCityImprovement process
    	city.setCurrentImprovement("courthouse");
    	
    	// cost is less than available resources
    	CityImprovementDTO courtHouse = getImprovement("courthouse",250);
		// insert test data as facts
		ksession.insert(courtHouse);
		ksession.insert(city);
		
		ksession.fireAllRules();
		
		// get active processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Rule Build City Improvement Fired",firedRules.contains("Build City Improvement"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()==1);
		// get the process
		Long pId = processes.get(0).getId();
		
		assertProcessInstanceActive(pId, ksession);
		
		// New Turn occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		assertProcessInstanceCompleted(pId, ksession);
		
		Assert.assertTrue("City Contains Courthouse Improvement.",city.getImprovements().contains("courthouse"));
		
		Assert.assertTrue("No Pending Processes Are There",ksession.getProcessInstances().size()==0);
	}
	/*
	 * Tests the behaviour when two consecutive improvements are built
	 */
	@Test
    public void testTwoImprovementsConsecutive(){
    	// Add mock eventlistener
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	CityDTO city = getCity(1L, "marefy");
    	Set<String> improvements = new HashSet<String>();
    	improvements.add("granary");
    	city.setImprovements(improvements);
    	
    	// 300 resources to spent per turn
    	city.setResourcesSurplus(300);
    	Set<String> enabledImprovements = new HashSet<String>();
    	enabledImprovements.add("courthouse");
    	enabledImprovements.add("bank");
    	city.setEnabledImprovements(enabledImprovements);
    	// triggers the buildCityImprovement process
    	city.setCurrentImprovement("courthouse");
    	
    	// cost is less than available resources
    	CityImprovementDTO bank = getImprovement("bank",100);
    	CityImprovementDTO courtHouse = getImprovement("courthouse",100);
		// insert test data as facts
		ksession.insert(courtHouse);
		ksession.insert(bank);
		FactHandle cityHandle = ksession.insert(city);
		
		ksession.fireAllRules();
		
		// get active processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Rule Build City Improvement Fired",firedRules.contains("Build City Improvement"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()==1);
		// get the process
		Long pId = processes.get(0).getId();
		
		assertProcessInstanceActive(pId, ksession);
		
		// New Turn occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		assertProcessInstanceCompleted(pId, ksession);
		
		Assert.assertTrue("City Contains Courthouse Improvement.",city.getImprovements().contains("courthouse"));
		
		Assert.assertTrue("No Pending Processes Are There",ksession.getProcessInstances().size()==0);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		city.setCurrentImprovement("bank");
		ksession.update(cityHandle, city);
		ksession.fireAllRules();
		
		// get active processes
		List<ProcessInstance> processes2 = (List<ProcessInstance>)ksession.getProcessInstances();
		Assert.assertTrue("One Process Should Be Active",processes2.size()==1);
		// get the process
		Long pId2 = processes2.get(0).getId();
		
		assertProcessInstanceActive(pId2, ksession);
		
		// New Turn occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		assertProcessInstanceCompleted(pId2, ksession);
		
		Assert.assertTrue("City Contains Bank Improvement.",city.getImprovements().contains("bank"));
		
		Assert.assertTrue("No Pending Processes Are There",ksession.getProcessInstances().size()==0);
	}
    
    @Test
    public void testCancelImprovement(){
    	// Add mock eventlistener
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	CityDTO city = getCity(1L, "marefy");
    	Set<String> improvements = new HashSet<String>();
    	improvements.add("granary");
    	improvements.add("bank");
    	city.setImprovements(improvements);
    	city.setCurrentImprovement("courthouse");
    	Set<String> enabledImprovements = new HashSet<String>();
    	enabledImprovements.add("courthouse");
    	city.setEnabledImprovements(enabledImprovements);
    	
    	// enough to finish with first TurnEvent
    	city.setResourcesSurplus(300);
    	CityImprovementDTO courtHouse = getImprovement("courthouse",250);
    	
		// insert test data as facts
		ksession.insert(courtHouse);
		
		ksession.insert(city);
		
		ksession.fireAllRules();
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Rule Build City Improvement Fired",firedRules.contains("Build City Improvement"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()==1);
		ProcessInstance p = processes.get(0);
		
		assertProcessInstanceActive(p.getId(), ksession);
		
		// cancel the process
		ksession.getEntryPoint("ActionCanceledStream").insert(new CityImprovementEvent(city.getId()));
		ksession.fireAllRules();
		
		// New Turn occured late
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		Assert.assertFalse("City Not Contains Courthouse Improvement.",city.getImprovements().contains("courthouse"));
		
		Assert.assertTrue("No Pending Processes Are There",ksession.getProcessInstances().size()==0);
	}
    @Test
    public void testCompleteAfterThreeTurns(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	ksession.addEventListener(new DebugProcessEventListener());
    	// Add mock eventlistener
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	CityDTO city = getCity(1L, "marefy");
    	Set<String> improvements = new HashSet<String>();
    	city.setImprovements(improvements);
    	city.setCurrentImprovement("courthouse");
    	// not sufficient to build it at once
    	city.setResourcesSurplus(100);
    	Set<String> enabledImprovements = new HashSet<String>();
    	enabledImprovements.add("courthouse");
    	city.setEnabledImprovements(enabledImprovements);
    	
    	CityImprovementDTO courtHouse = getImprovement("courthouse",251);
    	
		// insert test data as facts
		ksession.insert(courtHouse);
		
		FactHandle cityHandle = ksession.insert(city);
		
		ksession.fireAllRules();
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		
		Assert.assertTrue("Rule Build City Improvement Fired",firedRules.contains("Build City Improvement"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()==1);
		
		WorkflowProcessInstance p = (WorkflowProcessInstance)processes.get(0);
		
		// New Turn occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// not enough
		assertProcessInstanceActive(p.getId(), ksession);
		city.setResourcesSurplus(100);
		ksession.update(cityHandle, city);

		// New Turn occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// not enough
		assertProcessInstanceActive(p.getId(), ksession);
		city.setResourcesSurplus(100);
		ksession.update(cityHandle, city);
		
		// New Turn occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// finally
		assertProcessInstanceCompleted(p.getId(), ksession);
		
		Assert.assertTrue("No Pending Processes Are There",ksession.getProcessInstances().size()==0);
		
		Assert.assertTrue("City Contains Courthouse Improvement.",city.getImprovements().contains("courthouse"));
		
	}
    /*
     * Wonder can be built only once throughout the game. When it is built, all other attempts have to be stopped,
     * the possibility to built it should disappear (The linkage is through player's advances).
     */
    @Test
    public void testCancelWhenWonderBuilt(){
    	// Add mock eventlistener
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	
    	CityDTO city = getCity(1L, "marefy");
    	Set<String> improvements = new HashSet<String>();
    	improvements.add("granary");
    	improvements.add("bank");
    	city.setImprovements(improvements);
    	city.setResourcesSurplus(300);
    	Set<String> enabledImprovements = new HashSet<String>();
    	enabledImprovements.add("colossus");
    	city.setEnabledImprovements(enabledImprovements);
    	// wants to build colossus
    	city.setCurrentImprovement("colossus");
    	
    	// already has colossus
    	CityDTO cityWithColossus = getCity(2L, "Rhodos");
    	Set<String> improvements2 = new HashSet<String>();
    	improvements2.add("colossus");
    	improvements2.add("bank");
    	cityWithColossus.setImprovements(improvements2);
    	
    	AdvanceDTO advance = new AdvanceDTO();
    	advance.setIdent("advance");
    	Set<String> enabledCityImprovements = new HashSet<String>();
    	enabledCityImprovements.add("colossus");
    	advance.setEnabledCityImprovements(enabledCityImprovements);
    	
    	PlayerDTO owner = getPlayer(1L, "honzik");
    	city.setOwner(owner.getId());
    	cityWithColossus.setOwner(owner.getId());
    	Set<String>advances = new HashSet<String>();
    	advances.add(advance.getIdent());
    	owner.setAdvances(advances);
    	
    	// finally define colossus as wonder
    	CityImprovementDTO wonder = getImprovement("colossus",250);
    	wonder.setWonder(true);
    	
		// insert test data as facts but not the cityWithColossus
    	ksession.insert(advance);
		ksession.insert(wonder);
		
		ksession.insert(owner);
		ksession.insert(city);
		
		ksession.fireAllRules();
		
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Rule Build City Improvement Fired",firedRules.contains("Build City Improvement"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()>=1);
		ProcessInstance p = processes.get(0);
		
		assertProcessInstanceActive(p.getId(), ksession);
		// insert now cityWithColossus, it should stop the process for city called city
		ksession.insert(cityWithColossus);
		ksession.fireAllRules();
		
		// process is gone
		assertProcessInstanceCompleted(p.getId(), ksession);
		
		Assert.assertFalse("City Not Contains Colossus.",city.getImprovements().contains("colossus"));
		
		Assert.assertFalse("Enabled Improvements Not Contains Colossus.",city.getEnabledImprovements().contains("colossus"));
		
		Assert.assertTrue("No Pending Processes Are There",ksession.getProcessInstances().size()==0);
	}
    

	
	private static CityDTO getCity(Long id, String name){
		CityDTO city = new CityDTO();
    	city.setId(id);
    	city.setName(name);
    	city.setResourcesConsumption(0);
    	city.setResourcesProduction(0);
    	city.setUnitsSupport(0);
    	city.setFoodConsumption(0);
    	city.setFoodProduction(0);
    	city.setFoodStock(0);
    	city.setSize(0);
    	city.setTradeProduction(0);
    	city.setPeopleEntertainers(0);
		city.setPeopleScientists(0);
		city.setPeopleTaxmen(0);
		city.setWeLoveDay(false);
		city.setDisorder(false);
		city.setSize(1);
		city.setPeopleHappy(0);
		city.setPeopleContent(0);
		city.setPeopleUnhappy(0);
		city.setImprovements(new HashSet<String>());
    	return city;
	}
	
    private static PlayerDTO getPlayer(Long id, String name){
		PlayerDTO player = new PlayerDTO();
		player.setId(id);
		player.setName(name);
		player.setLuxuriesRatio(0);
		player.setTaxesRatio(0);
		player.setResearchRatio(0);
		player.setResearch(0);
		return player;
	}
    private static CityImprovementDTO getImprovement(String ident, Integer constructionCost){
    	CityImprovementDTO imp = new CityImprovementDTO();
    	imp.setConstructionCost(constructionCost);
    	imp.setIdent(ident);
    	return imp;
    }
    
}
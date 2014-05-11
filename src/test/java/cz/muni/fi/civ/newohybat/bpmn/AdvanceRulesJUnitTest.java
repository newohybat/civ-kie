package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.core.base.RuleNameMatchesAgendaFilter;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.AdvanceEvent;
import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.AdvanceDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.GovernmentDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;


public class AdvanceRulesJUnitTest extends BaseJUnitTest {
	
    /*
     * This test case shows research of an advance by a player.
     * Process is dependent on the TurnEvent occurrences.
     * Scenario: Player with one enabled advance (can research it), starts the research, has sufficient production,
     * process can be completed after single new TurnEvent.
     * Check: 
     * 		process active before turn, waits for it
     * 		process completed after one turn
     * 		player can research next advance
     * 		player can build improvement/create unit/change government, which was invented by researched advance
     */
	@Test
    public void testWaitForNewTurnToComplete(){
    	// Add mock eventlistener to check which rules fired
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	GovernmentDTO mercantilism = getGovernment("mercantilism");
    	// new player with research points 205, "basicOne" advance reached, "consecutiveOne" to research
    	PlayerDTO player = getPlayer(1L, "honza");
    	player.setGovernment("democracy");
    	// Define advances
    	AdvanceDTO basicOne = getAdvance("basicOne", 100);
    	basicOne.getEnabledAdvances().add("consecutiveOne");
    	
    	// Advance to be researched next, its cost is 100 units, player has enough, should complete after one turn
    	AdvanceDTO consecutiveOne = getAdvance("consecutiveOne", 100);
    	consecutiveOne.getEnabledAdvances().add("consecutiveTwo");
    	consecutiveOne.getEnabledCityImprovements().add("bank");
    	consecutiveOne.getEnabledGovernments().add("mercantilism");
    	consecutiveOne.getEnabledUnitTypes().add("warClerk");
    	
    	// init the advance tree by setting reached and enabled advances manually
    	player.getAdvances().add("basicOne");
    	player.getEnabledAdvances().add("consecutiveOne");
    	player.setResearchRatio(100);
    	
    	
    	// create a city of player
    	CityDTO city = getCity(1L, "marefy");
    	city.setTradeProduction(205);
    	Set<String> improvements = new HashSet<String>();
    	city.setImprovements(improvements);
    	city.setOwner(player.getId());
    	TileDTO cheatTile = new TileDTO();
    	cheatTile.setTradeProduction(205);
    	cheatTile.setId(1L);
    	city.getManagedTiles().add(cheatTile.getId());
    	
		// insert test data as facts
    	ksession.insert(cheatTile);
    	ksession.insert(basicOne);
		ksession.insert(consecutiveOne);
		ksession.insert(getAdvance("consecutiveTwo", 10));
		ksession.insert(mercantilism);
		FactHandle pH = ksession.insert(player);
		ksession.insert(city);
		
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		// currentAdvance not set, just to prepare data inserted in session
		ksession.fireAllRules();
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules();
		
		
		// begin research
		player.setCurrentAdvance("consecutiveOne");
		ksession.update(pH, player);
		// now it should start the process
		ksession.fireAllRules();
		// get all active processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		// Catch the afterMatchFired events, which contains fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Discover Advance rule fired.",firedRules.contains("Discover Advance"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()==1);
		// get the process
		Long pId = processes.get(0).getId();
		
		assertProcessInstanceActive(pId, ksession);
		
		// new TurnEvent occured
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();

		// process should be completed
		assertProcessInstanceCompleted(pId, ksession);
		
		
		Assert.assertTrue("Player has reached advance.",player.getAdvances().contains(consecutiveOne.getIdent()));
		Assert.assertTrue("Player can discover advance.",player.getEnabledAdvances().containsAll(consecutiveOne.getEnabledAdvances()));

		Assert.assertTrue("Player can build bank.",city.getEnabledImprovements().contains("bank"));
		Assert.assertTrue("Player can make warClerk.",city.getEnabledUnitTypes().contains("warClerk"));
		Assert.assertTrue("Player can convert to mercantilism.",player.getEnabledGovernments().contains("mercantilism"));
		
	}
	/*
	 * Tests ability to cancel the research before it finishes.
	 */
	@Test
    public void testCancel(){
    	// Add mock eventlistener to check which rules fired
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	GovernmentDTO mercantilism = getGovernment("mercantilism");
    	// new player with research points 205, "basicOne" advance reached, "consecutiveOne" to research
    	PlayerDTO player = getPlayer(1L, "honza");
    	player.setResearch(205);
    	
    	// Define advances
    	AdvanceDTO basicOne = getAdvance("basicOne", 100);
    	basicOne.getEnabledAdvances().add("consecutiveOne");
    	
    	// Advance to be researched next, its cost is 100 units, player has enough, should complete after one turn
    	AdvanceDTO consecutiveOne = getAdvance("consecutiveOne", 100);
    	consecutiveOne.getEnabledAdvances().add("consecutiveTwo");
    	consecutiveOne.getEnabledCityImprovements().add("bank");
    	consecutiveOne.getEnabledGovernments().add("mercantilism");
    	consecutiveOne.getEnabledUnitTypes().add("warClerk");
    	
    	// init the advance tree by setting reached and enabled advances manually
    	player.getAdvances().add("basicOne");
    	player.getEnabledAdvances().add("consecutiveOne");
    	
    	
    	// create a city of player
    	CityDTO city = getCity(1L, "marefy");
    	Set<String> improvements = new HashSet<String>();
    	city.setImprovements(improvements);
    	city.setOwner(player.getId());
    	
		// insert test data as facts
    	ksession.insert(basicOne);
		ksession.insert(consecutiveOne);
		ksession.insert(getAdvance("consecutiveTwo", 10));
		ksession.insert(mercantilism);
		FactHandle pH = ksession.insert(player);
		ksession.insert(city);
		
		// currentAdvance not set, just to prepare data inserted in session
		ksession.fireAllRules();
		
		// begin research
		player.setCurrentAdvance("consecutiveOne");
		ksession.update(pH, player);
		// now it should start the process
		ksession.fireAllRules();
		// get all active processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		// Catch the afterMatchFired events, which contains fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Discover Advance rule fired.",firedRules.contains("Discover Advance"));
		
		Assert.assertTrue("One Process Should Be Active",processes.size()==1);
		// get the process
		Long pId = processes.get(0).getId();
		
		assertProcessInstanceActive(pId, ksession);
		
		ksession.getEntryPoint("ActionCanceledStream").insert(new AdvanceEvent(player.getId()));
		ksession.fireAllRules();
		
		// process should be completed
				assertProcessInstanceCompleted(pId, ksession);
		
		// new TurnEvent occured, should not have any effect
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// process should be completed
		assertProcessInstanceCompleted(pId, ksession);
		
		
		Assert.assertTrue("Player has basic advance.",player.getAdvances().contains(basicOne.getIdent()));
		Assert.assertFalse("Player can't discover advance.",player.getEnabledAdvances().containsAll(consecutiveOne.getEnabledAdvances()));

		Assert.assertFalse("Player cannot build bank.",city.getEnabledImprovements().contains("bank"));
		Assert.assertFalse("Player cannot make warClerk.",city.getEnabledUnitTypes().contains("warClerk"));
		Assert.assertFalse("Player cannot convert to mercantilism.",player.getEnabledGovernments().contains("mercantilism"));
		
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
		city.setHomeUnits(new HashSet<Long>());
		city.setEnabledUnitTypes(new HashSet<String>());
		city.setEnabledImprovements(new HashSet<String>());
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
		player.setResearchSpent(0);
		player.setAdvances(new HashSet<String>());
		player.setEnabledAdvances(new HashSet<String>());
		player.setEnabledGovernments(new HashSet<String>());
		return player;
	}
    
    private static AdvanceDTO getAdvance(String ident, Integer cost){
    	AdvanceDTO advance = new AdvanceDTO();
    	advance.setIdent(ident);
    	advance.setEnabledAdvances(new HashSet<String>());
    	advance.setEnabledCityImprovements(new HashSet<String>());
    	advance.setEnabledGovernments(new HashSet<String>());
    	advance.setEnabledUnitTypes(new HashSet<String>());
    	advance.setCost(cost);
    	return advance;
    }
    
    private static GovernmentDTO getGovernment(String ident){
    	GovernmentDTO gov = new GovernmentDTO();
    	gov.setIdent(ident);
    	return gov;
    }
}
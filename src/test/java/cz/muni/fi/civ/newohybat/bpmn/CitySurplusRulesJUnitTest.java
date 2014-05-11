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
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;


public class CitySurplusRulesJUnitTest extends BaseJUnitTest {
	
	/*
	 * Tests that tradeProduction is divided between luxuries, taxes and research according to ratios set with player
	 */
    @Test
    public void testTradeResolution(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setLuxuriesRatio(30);
    	player.setTaxesRatio(30);
    	player.setResearchRatio(40);
    	CityDTO city = getCity(5L,"marefy");
		city.setTradeProduction(10);
		city.setOwner(player.getId());
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Trade To Luxuries/Taxes/Research Resolution"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Trade To Luxuries/Taxes/Research Resolution Fired",firedRules.contains("Trade To Luxuries/Taxes/Research Resolution"));
		Assert.assertTrue("City Luxuries According to Luxuries Ratio",city.getLuxuriesAmount()==3);
		Assert.assertTrue("City Taxes According to Taxes Ratio",city.getTaxesAmount()==3);
		Assert.assertTrue("City Research According to Research Ratio",city.getResearchAmount()==4);
    }
    /*
     * Tests that taxes (derived from tradeProduction) are given to player's treasury
     */
    @Test
    public void testCityTaxesToTreasury(){

    	
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setTaxesRatio(50);
    	player.setTreasury(0);
    	
    	// two cities of the player
    	CityDTO city = getCity(5L,"marefy");
		city.setTradeProduction(150);
		city.setImprovementsUpkeep(0);
		city.setOwner(player.getId());
		
		CityDTO city2 = getCity(6L,"brno");
		city2.setTradeProduction(100);
		city2.setImprovementsUpkeep(0);
		city2.setOwner(player.getId());
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(city2); 
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Taxes Surplus To Treasury"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Taxes Surplus To Treasury Fired",firedRules.contains("Taxes Surplus To Treasury"));
		Assert.assertTrue("City Taxes Go To Treasury",player.getTreasury()==125);
    }
    /*
     * tests when city has unsufficient taxes production but enough treasury to cover the lack
     */
    @Test
    public void testCityTaxesShortageFromTreasury(){

    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setTreasury(60);
    	CityDTO city = getCity(5L,"marefy");
		city.setTaxesAmount(0);
		city.setImprovementsUpkeep(50);
		city.setOwner(player.getId());
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());	
		ksession.fireAllRules();
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Taxes Shortage Covered From Treasury"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Taxes Shortage Covered From Treasury Fired",firedRules.contains("Taxes Shortage Covered From Treasury"));
		Assert.assertTrue("City Taxes Shortage Get From Treasury",player.getTreasury()==10);
    }
    /*
     * tests when city has unsufficient taxes production and not enough treasury to cover the lack -> remove one improvement
     */
    @Test
    public void testCityTaxesShortageNotCoveredFromTreasury(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	Set<String> improvements = new HashSet<String>();
    	improvements.add("bank");
    	improvements.add("factory");
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setTreasury(40);
    	CityDTO city = getCity(5L,"marefy");
		city.setTaxesAmount(0);
		city.setImprovementsUpkeep(50);
		city.setOwner(player.getId());
		city.setImprovements(improvements);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Taxes Shortage Not Covered From Treasury"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Taxes Shortage Not Covered From Treasury Fired",firedRules.contains("Taxes Shortage Not Covered From Treasury"));
		Assert.assertTrue("City Taxes Shortage Zero Treasury",player.getTreasury()==0);
		Assert.assertTrue("City Improvement Sold.",city.getImprovements().size()<2);
    }
    /*
     * 	Tests that research produced in city is added to global
     */
    @Test
    public void testCityResearchContribute(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setResearch(0);
    	player.setResearchRatio(100);
    	
    	CityDTO city = getCity(5L,"marefy");
		city.setTradeProduction(10);
		city.setCorruption(0);
		city.setOwner(player.getId());
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);
		
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// simulate Rule Task
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Research Of City To Global"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Research Of City To Global Fired",firedRules.contains("Research Of City To Global"));
		Assert.assertTrue("Research Of City To Global",player.getResearch()==10);
    }
    /*
     * Tests addition of food surplus to food stock of the city
     */
    @Test
    public void testCityFoodSurplusToStock(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setResearch(60);
    	CityDTO city = getCity(5L,"marefy");
		city.setResearchAmount(10);
		city.setOwner(player.getId());
		city.setFoodProduction(250);
		city.setFoodConsumption(120);
		city.setFoodStock(10);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);
		
		//new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		// simulate rule task
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Food Surplus To Stock"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Food Surplus To Stock Fired",firedRules.contains("Food Surplus To Stock"));
		Assert.assertTrue("Food Surplus To Stock Check",city.getFoodStock()==140);
    }
    /*
     * Tests when foodConsumption is higher than foodProduction, but is enough in foodStock
     */
    @Test
    public void testCityFoodShortageFromStock(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setFoodProduction(250);
		city.setFoodConsumption(300);
		city.setSize(3);
		city.setFoodStock(60);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		// simulate Rule task 
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("affectPopulation");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Food Shortage Covered From Stock"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Food Shortage Covered From Stock Fired",firedRules.contains("Food Shortage Covered From Stock"));
		System.out.println(city.getFoodStock());Assert.assertTrue("Food Stock Decreased",city.getFoodStock()==10);
		Assert.assertTrue("City Size Unchanged",city.getSize()==3);
    }
    /*
     * Tests that food shortage not covered from food stock of the city results in decreasing size of city.
     */
    @Test
    public void testCityFoodShortageNotFromStock(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setFoodProduction(250);
		city.setFoodConsumption(300);
		city.setSize(3);
		city.setFoodStock(10);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		// simulate Rule task 
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("affectPopulation");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Food Shortage Not Covered From Stock"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Food Shortage Not Covered From Stock Fired",firedRules.contains("Food Shortage Not Covered From Stock"));
		Assert.assertTrue("Food Stock Empty",city.getFoodStock()==0);
		Assert.assertTrue("City Size Decreased",city.getSize()==2);
    }
    /*
     * Tests that after overflow of foodStock it is empty, and size increased
     */
    @Test
    public void testCityFoodStockOverflows(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// set limit of foodStock, overflows -> increase size of city
    	ksession.setGlobal("foodStockLimit", 10);
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setSize(3);
		city.setFoodStock(15);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		// simulate Rule task 
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("affectPopulation");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Food Stock Overflows Expand City"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Food Stock Overflows Expand City Fired",firedRules.contains("Food Stock Overflows Expand City"));
		Assert.assertTrue("Food Stock Empty",city.getFoodStock()==0);
		Assert.assertTrue("City Size Increased",city.getSize()==4);
    }
    /*
     * Tests that after overflow of foodStock it is half empty, and size increased
     */
    @Test
    public void testCityFoodStockOverflowsWithGranary(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// set limit of foodStock, overflows -> increase size of city
    	ksession.setGlobal("foodStockLimit", 10);
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setSize(3);
		city.setFoodStock(15);
		city.getImprovements().add("granary");
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// simulate Rule task 
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("affectPopulation");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Food Stock Overflows Expand City With Granary"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Food Stock Overflows Expand City With Granary Fired",firedRules.contains("Food Stock Overflows Expand City With Granary"));
		Assert.assertTrue("Food Stock Half Full",city.getFoodStock()==5);
		Assert.assertTrue("City Size Increased",city.getSize()==4);
    }
    /*
     * city bigger than 10 has to have aqueduct to grow even more
     */
    @Test
    public void testCityFoodStockOverflowsWithoutAqueduct(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// set limit of foodStock, overflows -> increase size of city
    	ksession.setGlobal("foodStockLimit", 10);
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setSize(10);
		city.setFoodStock(15);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		// simulate Rule task 
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("affectPopulation");
		ksession.fireAllRules(new RuleNameMatchesAgendaFilter("Food Stock Overflows City Reached Population Limit Without Aqueduct"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Food Stock Overflows City Reached Population Limit Without Aqueduct Fired",firedRules.contains("Food Stock Overflows City Reached Population Limit Without Aqueduct"));
		Assert.assertTrue("Food Stock Full",city.getFoodStock()==10);
		Assert.assertTrue("City Size Unchanged",city.getSize()==10);
    }
    /*
     * Every specialist add 2 points to its specific field
     */
    @Test
    public void testSpecialistsEffects(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setLuxuriesRatio(30);
    	player.setTaxesRatio(30);
    	player.setResearchRatio(40);
    	CityDTO city = getCity(5L,"marefy");
		city.setTradeProduction(10);
		city.setOwner(player.getId());
		city.setPeopleEntertainers(2);
		city.setPeopleTaxmen(1);
		city.setPeopleScientists(3);
		city.setSize(10);
		city.setPeopleContent(2);
		city.setPeopleUnhappy(2);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Trade To Luxuries/Taxes/Research Resolution Fired",firedRules.contains("Trade To Luxuries/Taxes/Research Resolution"));
		Assert.assertTrue("Entertainers Increase Luxuries Fired",firedRules.contains("Entertainers Increase Luxuries"));
		Assert.assertTrue("Taxmen Increase Taxes Fired",firedRules.contains("Taxmen Increase Taxes"));
		Assert.assertTrue("Scientists Increase Research Fired",firedRules.contains("Scientists Increase Research"));
		
		Assert.assertTrue("City Luxuries Boosted By Entertainers",city.getLuxuriesAmount()==7);
		Assert.assertTrue("City Taxes Boosted By Taxmen",city.getTaxesAmount()==5);
		Assert.assertTrue("City Research Boosted By Scientists",city.getResearchAmount()==10);
		
    }
    /*
     * Tests effect of luxuries produced in a city. 
     */
    @Test
    public void testLuxuriesEffects(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	player.setLuxuriesRatio(30);
    	player.setTaxesRatio(30);
    	player.setResearchRatio(40);
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setSize(4);
		city.setPeopleContent(2);
		city.setPeopleUnhappy(2);
		city.setLuxuriesAmount(7);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("affectPopulation");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Luxuries Make People Happier Fired", firedRules.contains("Luxuries Make People Happier"));
		
		Assert.assertTrue("Content People Changed To Happy Because Of Luxuries",city.getPeopleHappy()==2);
		Assert.assertTrue("One Unhappy Changed To Content Because Of Luxuries",city.getPeopleContent()==1);
		Assert.assertTrue("One Unhappy Stayed As Was",city.getPeopleUnhappy()==1);
		Assert.assertTrue("Luxuries Spent",city.getLuxuriesSpent()==6);
		
    }
    @Test
    public void testResourcesSurplus(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "honza", "despotism");
    	CityDTO city = getCity(5L,"marefy");
		city.setOwner(player.getId());
		city.setResourcesProduction(150);
		city.setResourcesConsumption(100);
		
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(player);

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Resources Surplus Fired", firedRules.contains("Resources Surplus"));
		
		Assert.assertTrue("Resources Surplus As Difference Between Production And Consumption",city.getResourcesSurplus()==50);
		
    }
    /*
     * When There is not sufficient resources (resourcesSurplus<0), farthest unit which is homeUnit in city is disbanded
     */
    @Test
    public void testResourcesSurplusNotSufficientDisbandUnit(){
//    	ksession.addEventListener(new DebugAgendaEventListener());
    	// mock event listener to collect fired rules
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "HONZA", "monarchy");
    	
    	UnitTypeDTO settler = getUnitType("settler");// consumes food
    	settler.setMilitary(false);
    	
    	UnitTypeDTO phalanx = getUnitType("phalanx");// consumes resources
    	phalanx.setMilitary(true);
    	
    	TileDTO tile1 = getTile(1L, 1L, 1L);
    	TileDTO tile2 = getTile(2L, 3L, 1L);
    	TileDTO tile3 = getTile(10L, 10L, 10L);
    	TileDTO cityCentre = getTile(5L, 5L, 5L);
    	
    	UnitDTO unit = getUnit(1L, tile1.getId(),"phalanx", player.getId());
    	UnitDTO unit2 = getUnit(2L, tile2.getId(),"phalanx", player.getId());
    	UnitDTO unit3 = getUnit(3L, tile3.getId(),"settler", player.getId());
    	UnitDTO unit4 = getUnit(4L, cityCentre.getId(),"phalanx", player.getId());
    	Set<Long> homeUnits = new HashSet<Long>();
    	homeUnits.add(unit.getId());
    	homeUnits.add(unit2.getId());
    	homeUnits.add(unit3.getId());
    	homeUnits.add(unit4.getId());
    	
    	
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setOwner(player.getId());
    	city.setHomeUnits(homeUnits);
    	city.setResourcesSurplus(-50);
    	city.setCityCentre(cityCentre.getId());
		// insert facts
    	ksession.insert(tile1);
    	ksession.insert(tile2);
    	ksession.insert(tile3);
    	ksession.insert(cityCentre);
		ksession.insert(player);
		ksession.insert(phalanx);
		ksession.insert(settler);
		ksession.insert(unit);
		ksession.insert(unit2);
		ksession.insert(unit3);
		ksession.insert(unit4);
    	
    	FactHandle cityHandle = ksession.insert(city);
		
    	ksession.fireAllRules();
    	
    	((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
    	ksession.fireAllRules();
    	
    	// alter city resources surplus for test purpose
    	city.setResourcesSurplus(-50);
    	ksession.update(cityHandle, city);
    	
    	// new turn
    	ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
    		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageSurpluses");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Resources Shortage Disband Farthest Unit Fired",firedRules.contains("Resources Shortage Disband Farthest Unit"));
		Assert.assertTrue("One Unit Disbanded",city.getHomeUnits().size()==3);
		Assert.assertFalse("Farthest Unit Was Disbanded", city.getHomeUnits().contains(unit.getId()));
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

	
	private static TileDTO getTile(Long id, Long posX, Long posY){
    	TileDTO tile = new TileDTO();
    	tile.setId(id);
    	tile.setPosX(posX);
    	tile.setPosY(posY);
    	tile.setDefenseBonus(0);
    	return tile;
    }
	private static PlayerDTO getPlayer(Long id, String name, String government){
		PlayerDTO player = new PlayerDTO();
		player.setId(id);
		player.setName(name);
		player.setGovernment(government);
		player.setLuxuriesRatio(0);
		player.setTaxesRatio(0);
		player.setResearchRatio(0);
		player.setResearch(0);
		return player;
	}
    private static CityImprovementDTO getImprovement(String ident, Integer upkeepCost){
    	CityImprovementDTO imp = new CityImprovementDTO();
    	imp.setUpkeepCost(upkeepCost);
    	imp.setIdent(ident);
    	return imp;
    }
    private static UnitDTO getUnit(Long id,Long tile,String type, Long owner){
    	UnitDTO unit = new UnitDTO();
    	unit.setId(id);
    	unit.setType(type);
    	unit.setTile(tile);
    	unit.setAttackStrength(0);
    	unit.setDefenseStrength(0);
    	unit.setOwner(owner);
    	return unit;
    }
    private static UnitTypeDTO getUnitType(String ident){
    	UnitTypeDTO type = new UnitTypeDTO();
		type.setIdent(ident);
		Set<String> actions = new HashSet<String>();
		type.setActions(actions);
		return type;
    }
    
}
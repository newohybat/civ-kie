package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;


public class CityConsumptionRulesJUnitTest extends BaseJUnitTest {
    /*
     * Tests that cityImprovementsUpkeep is sum of upkeeps of its improvements
     */
	@Test
    public void testCityImprovementsUpkeep(){
		// add mock event listener to collect fired rules
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	Set<String> improvements = new HashSet<String>();
    	improvements.add("bank");
    	improvements.add("factory");
    	CityDTO city = getCity(5L,"marefy");
    	city.setImprovements(improvements);
		
    	CityImprovementDTO bank = getImprovement("bank", 10);
    	CityImprovementDTO factory = getImprovement("factory", 5);
    	
    	
		// insert facts
		ksession.insert(city);
		ksession.insert(bank);
		ksession.insert(factory);

		ksession.fireAllRules();
		
		// activate the ruleFlowGroup to simulate function of process with Rule Task
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("City Improvements Upkeep Fired",firedRules.contains("City Improvements Upkeep"));
		Assert.assertTrue("City Improvements Upkeep Is Sum Of imps",city.getImprovementsUpkeep()==15);
    }
    /*
     * Tests that each population point consumes 2 units of food.
     */
    @Test
    public void testCityPopulationFoodConsumption(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	CityDTO city = getCity(5L,"marefy");
    	city.setSize(5);
    	
		// insert facts
		ksession.insert(city);

		ksession.fireAllRules();

		// activate rule flow group of the desired rule
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Population Point Requires 2 Food Units Fired",firedRules.contains("Population Point Requires 2 Food Units"));
		Assert.assertTrue("City Population Consumption",city.getFoodConsumption()==10);
    }
    /*
     *  Tests that support of units is added to resourcesConsumption
     */
    @Test
    public void testCitySupportOfUnitsToResourcesConsumption(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	CityDTO city = getCity(5L,"marefy");
    	city.setUnitsSupport(15);
    	
		// insert facts
		ksession.insert(city);

		ksession.fireAllRules();
		// signal new turn to trigger action each turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		// activate desired rule flow group
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Support Of Units To Resources Consumption Fired",firedRules.contains("Support Of Units To Resources Consumption"));
		Assert.assertTrue("City Population Consumption",city.getResourcesConsumption()==15);
    }
    /*
     * Despotism each unit over city population needs to be supported by resources.
     * Also for each population point 2 food units + each settler 1 unit of food.
     */
    @Test
    public void testCityDespotismUnitsSupport(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	// add mock event listener to collect fired rules
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "HONZA", "despotism");
    	
    	// non-military unit consumes food
    	UnitTypeDTO settler = getUnitType("settler");
    	settler.setMilitary(false);
    	// each military unit over population count consumes resources
    	UnitTypeDTO phalanx = getUnitType("phalanx");
    	phalanx.setMilitary(true);
    	
    	UnitDTO unit = getUnit(1L, "phalanx", player.getId());
    	UnitDTO unit2 = getUnit(2L, "phalanx", player.getId());
    	UnitDTO unit3 = getUnit(3L, "settler", player.getId());
    	UnitDTO unit4 = getUnit(4L, "phalanx", player.getId());
    	UnitDTO deadUnit = getUnit(5L, "phalanx", player.getId());
    	Set<Long> homeUnits = new HashSet<Long>();
    	homeUnits.add(unit.getId());
    	homeUnits.add(unit2.getId());
    	homeUnits.add(unit3.getId());
    	homeUnits.add(unit4.getId());
    	homeUnits.add(deadUnit.getId());
    	
    	
    	
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setOwner(player.getId());
    	city.setHomeUnits(homeUnits);
    	city.setSize(2);// this generates foodConsumption 4 
    	
		// insert facts
		ksession.insert(player);
		ksession.insert(phalanx);
		ksession.insert(settler);
		ksession.insert(unit);
		ksession.insert(unit2);
		ksession.insert(unit3);
		ksession.insert(unit4);
		FactHandle fh = ksession.insert(deadUnit);
		ksession.fireAllRules();
		deadUnit.setHealthPoints(0);
		ksession.update(fh, deadUnit);
    	
    	ksession.insert(city);

		ksession.fireAllRules();

		// activate desired rule flow group
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Despotism Units Support Fired",firedRules.contains("Despotism Units Support"));
		Assert.assertTrue("Despotism Non Military Units Consume Food",city.getFoodConsumption()==5 );
		Assert.assertTrue("Despotism Military Units Over City Size Support",city.getUnitsSupport()==1);
    }
    /*
     * Tests units support when government is monarchy.
     * Military unit consumes 1 resources
     * Non-military consumes 2 food
     */
    @Test
    public void testCityMonarchyUnitsSupport(){
    	// mock event listener to collect fired rules
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "HONZA", "monarchy");
    	
    	UnitTypeDTO settler = getUnitType("settler");// consumes food
    	settler.setMilitary(false);
    	
    	UnitTypeDTO phalanx = getUnitType("phalanx");// consumes resources
    	phalanx.setMilitary(true);
    	
    	UnitDTO unit = getUnit(1L, "phalanx", player.getId());
    	UnitDTO unit2 = getUnit(2L, "phalanx", player.getId());
    	UnitDTO unit3 = getUnit(3L, "settler", player.getId());
    	UnitDTO unit4 = getUnit(4L, "phalanx", player.getId());
    	Set<Long> homeUnits = new HashSet<Long>();
    	homeUnits.add(unit.getId());
    	homeUnits.add(unit2.getId());
    	homeUnits.add(unit3.getId());
    	homeUnits.add(unit4.getId());
    	
    	
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setOwner(player.getId());
    	city.setHomeUnits(homeUnits);
    	// produces 4 foodConsumption
    	city.setSize(2);
		// insert facts
		ksession.insert(player);
		ksession.insert(phalanx);
		ksession.insert(settler);
		ksession.insert(unit);
		ksession.insert(unit2);
		ksession.insert(unit3);
		ksession.insert(unit4);
    	
    	ksession.insert(city);
		
		ksession.fireAllRules();

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Monarchy Units Support Fired",firedRules.contains("Monarchy Units Support"));
		Assert.assertTrue("Monarchy Non Military Units Consume Food",city.getFoodConsumption()==6);
		Assert.assertTrue("Monarchy Military Units Support",city.getUnitsSupport()==3);
    }
    /*
     * Tests units support when government is communism.
     * Military unit consumes 1 resources
     * Non-military consumes 2 food
     */
    @Test
    public void testCityCommunismUnitsSupport(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "HONZA", "communism");
    	
    	UnitTypeDTO settler = getUnitType("settler");
    	settler.setMilitary(false);
    	
    	UnitTypeDTO phalanx = getUnitType("phalanx");
    	phalanx.setMilitary(true);
    	
    	UnitDTO unit = getUnit(1L, "phalanx", player.getId());
    	UnitDTO unit2 = getUnit(2L, "phalanx", player.getId());
    	UnitDTO unit3 = getUnit(3L, "settler", player.getId());
    	UnitDTO unit4 = getUnit(4L, "phalanx", player.getId());
    	Set<Long> homeUnits = new HashSet<Long>();
    	homeUnits.add(unit.getId());
    	homeUnits.add(unit2.getId());
    	homeUnits.add(unit3.getId());
    	homeUnits.add(unit4.getId());
    	
    	
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setOwner(player.getId());
    	city.setHomeUnits(homeUnits);
    	// produces additional 4 foodConsumption
    	city.setSize(2);
		// insert facts
		ksession.insert(player);
		ksession.insert(phalanx);
		ksession.insert(settler);
		ksession.insert(unit);
		ksession.insert(unit2);
		ksession.insert(unit3);
		ksession.insert(unit4);
    	
		ksession.insert(city);
		
		ksession.fireAllRules();
		
    	ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Communism Units Support Fired",firedRules.contains("Communism Units Support"));
		Assert.assertTrue("Communism Non Military Units Consume Food",city.getFoodConsumption()==6);
		Assert.assertTrue("Communism Military Units Support",city.getUnitsSupport()==3);
    }
    /*
     * Tests units support when government is theRepublic.
     * Military unit consumes 1 resources
     * Non-military consumes 2 food
     */
    @Test
    public void testCityTheRepublicUnitsSupport(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "HONZA", "theRepublic");
    	
    	UnitTypeDTO settler = getUnitType("settler");
    	settler.setMilitary(false);
    	
    	UnitTypeDTO phalanx = getUnitType("phalanx");
    	phalanx.setMilitary(true);
    	
    	UnitDTO unit = getUnit(1L, "phalanx", player.getId());
    	UnitDTO unit2 = getUnit(2L, "phalanx", player.getId());
    	UnitDTO unit3 = getUnit(3L, "settler", player.getId());
    	UnitDTO unit4 = getUnit(4L, "phalanx", player.getId());
    	Set<Long> homeUnits = new HashSet<Long>();
    	homeUnits.add(unit.getId());
    	homeUnits.add(unit2.getId());
    	homeUnits.add(unit3.getId());
    	homeUnits.add(unit4.getId());
    	
    	
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setOwner(player.getId());
    	city.setHomeUnits(homeUnits);
    	city.setSize(2);
		// insert facts
		ksession.insert(player);
		ksession.insert(phalanx);
		ksession.insert(settler);
		ksession.insert(unit);
		ksession.insert(unit2);
		ksession.insert(unit3);
		ksession.insert(unit4);
    	
    	ksession.insert(city);

		ksession.fireAllRules();

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("The Republic Units Support Fired",firedRules.contains("The Republic Units Support"));
		Assert.assertTrue("The Republic Non Military Units Consume Food",city.getFoodConsumption()==6);
		Assert.assertTrue("The Republic Military Units Support",city.getUnitsSupport()==3);
    }
    /*
     * Tests units support when government is democracy.
     * Military unit consumes 1 resources
     * Non-military consumes 2 food
     */
    @Test
    public void testCityDemocracyUnitsSupport(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player = getPlayer(1L, "HONZA", "democracy");
    	
    	UnitTypeDTO settler = getUnitType("settler");
    	settler.setMilitary(false);
    	
    	UnitTypeDTO phalanx = getUnitType("phalanx");
    	phalanx.setMilitary(true);
    	
    	UnitDTO unit = getUnit(1L, "phalanx", player.getId());
    	UnitDTO unit2 = getUnit(2L, "phalanx", player.getId());
    	UnitDTO unit3 = getUnit(3L, "settler", player.getId());
    	UnitDTO unit4 = getUnit(4L, "phalanx", player.getId());
    	Set<Long> homeUnits = new HashSet<Long>();
    	homeUnits.add(unit.getId());
    	homeUnits.add(unit2.getId());
    	homeUnits.add(unit3.getId());
    	homeUnits.add(unit4.getId());
    	
    	
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setOwner(player.getId());
    	city.setHomeUnits(homeUnits);
    	city.setSize(2);
		// insert facts
		ksession.insert(player);
		ksession.insert(phalanx);
		ksession.insert(settler);
		ksession.insert(unit);
		ksession.insert(unit2);
		ksession.insert(unit3);
		ksession.insert(unit4);
    	
    	ksession.insert(city);

		ksession.fireAllRules();

		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageConsumptions");
		ksession.fireAllRules();
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Democracy Units Support Fired",firedRules.contains("Democracy Units Support"));
		Assert.assertTrue("Democracy Non Military Units Consume Food",city.getFoodConsumption()==6);
		Assert.assertTrue("Democracy Military Units Support",city.getUnitsSupport()==3);
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

	
	private static TileDTO getTile(Long id, Integer foodProduction, Integer resourcesProduction, Integer tradeProduction){
    	TileDTO tile = new TileDTO();
    	tile.setId(id);
    	tile.setFoodProduction(foodProduction);
    	tile.setResourcesProduction(resourcesProduction);
    	tile.setTradeProduction(tradeProduction);
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
    private static UnitDTO getUnit(Long id,String type, Long owner){
    	UnitDTO unit = new UnitDTO();
    	unit.setId(id);
    	unit.setType(type);
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
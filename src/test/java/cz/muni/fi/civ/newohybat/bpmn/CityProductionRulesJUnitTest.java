package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.core.base.RuleNameEqualsAgendaFilter;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;


public class CityProductionRulesJUnitTest extends BaseJUnitTest {
	/*
	 * Tiles Production With despotism is restricted. When the production of a tile is >=3,
	 * then its real value is decreased by 1.
	 */
	@Test
    public void testCityDespotismProduction(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	TileDTO tile = getTile(1L,1L,1L, 1,1,1);
    	TileDTO tile2 = getTile(2L,2L,2L, 2,2,2);
    	TileDTO tile3 = getTile(3L,3L,3L, 2,3,3);
    	PlayerDTO owner = getPlayer(1L,"newohybat","despotism");
    	
    	CityDTO city = getCity(5L,"marefy");
    	Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(tile.getId());
		managedTiles.add(tile2.getId());
		managedTiles.add(tile3.getId());
		city.setManagedTiles(managedTiles);
		city.setOwner(owner.getId());
		// insert facts
		ksession.insert(city);
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(tile3);

		ksession.insert(owner);
		
//		new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// activate desired rule flow group
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Despotism Restricted City Tiles Production"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		
		Assert.assertTrue("Despotism Restricted City Tiles Production Fired",firedRules.contains("Despotism Restricted City Tiles Production"));
		Assert.assertTrue("City Food Production Is Sum Of Tiles Productions.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Sum Of Tiles Productions.",city.getResourcesProduction()==5);
		Assert.assertTrue("City Trade Production Is Sum Of Tiles Productions.",city.getTradeProduction()==5);
    }
	/*
	 * With monarchy productions are only sums of productions of tiles.
	 */
	@Test
    public void testCityMonarchyProduction(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	TileDTO tile = getTile(1L,1L,1L, 1,1,1);
    	TileDTO tile2 = getTile(2L,2L,2L, 2,2,2);
    	TileDTO tile3 = getTile(3L,3L,3L, 3,3,3);
    	PlayerDTO owner = getPlayer(1L,"newohybat","monarchy");
    	
    	CityDTO city = getCity(5L,"marefy");
    	Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(tile.getId());
		managedTiles.add(tile2.getId());
		managedTiles.add(tile3.getId());
		city.setManagedTiles(managedTiles);
		city.setOwner(owner.getId());
		
		// insert facts
		
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(tile3);
		
		ksession.insert(owner);
		
		ksession.insert(city);
		
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// activate rule flow group of the desired rule
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Monarchy City Tiles Production Fired",firedRules.contains("Monarchy City Tiles Production"));
		Assert.assertTrue("City Food Production Is Sum Of Tiles Productions.",city.getFoodProduction()==6);
		Assert.assertTrue("City Resources Production Is Sum Of Tiles Productions.",city.getResourcesProduction()==6);
		Assert.assertTrue("City Trade Production Is Sum Of Tiles Productions.",city.getTradeProduction()==6);
    }
	/*
	 * With monarchy productions are only sums of productions of tiles.
	 */
	@Test
    public void testCityMonarchyWeLoveDayProduction(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	TileDTO tile = getTile(1L,1L,1L, 1,1,1);
    	TileDTO tile2 = getTile(2L,2L,2L, 2,2,2);
    	TileDTO tile3 = getTile(3L,3L,3L, 3,3,3);
    	PlayerDTO owner = getPlayer(1L,"newohybat","monarchy");
    	
    	CityDTO city = getCity(5L,"marefy");
    	Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(tile.getId());
		managedTiles.add(tile2.getId());
		managedTiles.add(tile3.getId());
		city.setManagedTiles(managedTiles);
		city.setOwner(owner.getId());
		city.setWeLoveDay(true);
		
		// insert facts
		
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(tile3);
		
		ksession.insert(owner);
		
		ksession.insert(city);
		
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// activate rule flow group of the desired rule
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Monarchy WeLoveDay City Tiles Production Fired",firedRules.contains("Monarchy WeLoveDay City Tiles Production"));
		Assert.assertTrue("City Food Production Is Sum Of Tiles Productions.",city.getFoodProduction()==6);
		Assert.assertTrue("City Resources Production Is Sum Of Tiles Productions.",city.getResourcesProduction()==6);
		Assert.assertTrue("City Trade Production Is Sum Of Tiles Productions.",city.getTradeProduction()==9);
    }
	/*
	 * Communism has same production as monarchy.
	 */
	@Test
    public void testCityCommunismProduction(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	TileDTO tile = getTile(1L,1L,1L, 1,1,1);
    	TileDTO tile2 = getTile(2L,2L,2L, 2,2,2);
    	TileDTO tile3 = getTile(3L,3L,3L, 3,3,3);
    	PlayerDTO owner = getPlayer(1L,"newohybat","communism");
    	
    	CityDTO city = getCity(5L,"marefy");
    	Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(tile.getId());
		managedTiles.add(tile2.getId());
		managedTiles.add(tile3.getId());
		city.setManagedTiles(managedTiles);
		city.setOwner(owner.getId());
		// insert facts
		ksession.insert(city);
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(tile3);

		ksession.insert(owner);
		
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// activate rule flow group and filter triggered rule
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Basic City Tiles Production"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Communism City Tiles Production Fired",firedRules.contains("Communism City Tiles Production"));
		Assert.assertTrue("City Food Production Is Sum Of Tiles Productions.",city.getFoodProduction()==6);
		Assert.assertTrue("City Resources Production Is Sum Of Tiles Productions.",city.getResourcesProduction()==6);
		Assert.assertTrue("City Trade Production Is Sum Of Tiles Productions.",city.getTradeProduction()==6);
    }
	/*
	 * The Republic has bonus for trade production: tradeProduction>0 -> then increase by one
	 */
	@Test
    public void testCityTheRepublicProduction(){
    	
		AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	TileDTO tile = getTile(1L,1L,1L, 1,1,1);
    	TileDTO tile2 = getTile(2L,2L,2L, 2,2,2);
    	TileDTO tile3 = getTile(3L,3L,3L, 3,3,3);
    	PlayerDTO owner = getPlayer(1L,"newohybat","theRepublic");
    	
    	CityDTO city = getCity(5L,"marefy");
    	Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(tile.getId());
		managedTiles.add(tile2.getId());
		managedTiles.add(tile3.getId());
		city.setManagedTiles(managedTiles);
		city.setOwner(owner.getId());
		
		// insert facts
		ksession.insert(city);
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(tile3);

		ksession.insert(owner);
		
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		// activate rule flow group and filter triggered rule
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("The Republic City Tiles Production"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("The Republic City Tiles Production Fired",firedRules.contains("The Republic City Tiles Production"));
		Assert.assertTrue("City Food Production Is Sum Of Tiles Productions.",city.getFoodProduction()==6);
		Assert.assertTrue("City Resources Production Is Sum Of Tiles Productions.",city.getResourcesProduction()==6);
		Assert.assertTrue("City Trade Production Is Sum Of Tiles Productions.",city.getTradeProduction()==9);
    }
	/*
	 * The same as republic
	 */
	@Test
    public void testCityDemocracyProduction(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	TileDTO tile = getTile(1L,1L,1L, 1,1,1);
    	TileDTO tile2 = getTile(2L,2L,2L, 2,2,2);
    	TileDTO tile3 = getTile(3L,3L,3L, 3,3,3);
    	PlayerDTO owner = getPlayer(1L,"newohybat","democracy");
    	
    	CityDTO city = getCity(5L,"marefy");
		Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(tile.getId());
		managedTiles.add(tile2.getId());
		managedTiles.add(tile3.getId());
		city.setManagedTiles(managedTiles);
		city.setOwner(owner.getId());
		// insert facts
		ksession.insert(city);
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(tile3);

		ksession.insert(owner);
		
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		ksession.fireAllRules();
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Democracy City Tiles Production"));
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Democracy City Tiles Production Fired",firedRules.contains("Democracy City Tiles Production"));
		Assert.assertTrue("City Food Production Is Sum Of Tiles Productions.",city.getFoodProduction()==6);
		Assert.assertTrue("City Resources Production Is Sum Of Tiles Productions.",city.getResourcesProduction()==6);
		Assert.assertTrue("City Trade Production Is Sum Of Tiles Productions.",city.getTradeProduction()==9);
    }
	/*
	 * factory increases resources production by half. Math.floor applied to get whole number.
	 */
	@Test
    public void testCityWithFactory(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("factory");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Factory"));
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		

		Assert.assertTrue("Factory",firedRules.contains("Factory"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==7);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
    }
	/*
	 * power plant + factory : first apply effect of plant (resourcesProduction*1.5), then the combo effect: resourcesProduction*2
	 * Each step is rounded by Math.floor
	 */
	@Test
    public void testCityWithFactoryAndPowerPlant(){		
		AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("factory");
    	city.getImprovements().add("hydroPlant");
    	
    	
    	ksession.insert(city);
    	ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
    	ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Factory And Some Power Plant"));
    	
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Factory And Some Power Plant"));
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Factory And Some Power Plant Fired",firedRules.contains("Factory And Some Power Plant"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==14);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
    }
	/*
	 * hydroplant boosts resourcesProduction by 1.5
	 */
	@Test
    public void testCityWithHydroPlant(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("hydroPlant");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Hydro Plant"));
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Hydro Plant Fired",firedRules.contains("Hydro Plant"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==7);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
    }
	/*
	 * when manufacturing plant present, effect of factory is suppressed
	 */
	@Test
    public void testCityManufacturingPlantObsoletesFactory(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("manufacturingPlant");
    	city.getImprovements().add("factory");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules();
		
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Manufacturing Plant Obsoletes Factory Fired",firedRules.contains("Manufacturing Plant"));
		Assert.assertFalse("Factory Didn't Fire",firedRules.contains("Factory"));
		
    }
	/*
	 * manufacturing plant doubles resourcesProduction
	 */
	@Test
    public void testCityManufacturingPlant(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("manufacturingPlant");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Manufacturing Plant"));
		
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
//		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		Assert.assertTrue("Manufacturing Plant Fired",firedRules.contains("Manufacturing Plant"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==10);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
    }
	/*
	 * power plant boosts resourcesProduction by 1.5, after that this combo has effect multiply resourcesProduction by 3
	 */
	@Test
    public void testCityManufacturingPlantWithPowerPlant(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("manufacturingPlant");
    	city.getImprovements().add("hydroPlant");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Manufacturing Plant With A Power Plant"));
		
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		

		Assert.assertTrue("Manufacturing Plant With A Power Plant Fired",firedRules.contains("Manufacturing Plant With A Power Plant"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==21);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
    }
	/*
	 * resourcesProduction *1.5
	 */
	@Test
    public void testCityWithNuclearPlant(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("nuclearPlant");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Nuclear Plant"));
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Nuclear Plant Fired",firedRules.contains("Nuclear Plant"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==7);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
    }
	/*
	 * resourcesProduction *1.5
	 */
	@Test
    public void testCityWithPowerPlant(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
    	city.setFoodProduction(5);
    	city.setResourcesProduction(5);
    	city.setTradeProduction(5);
    	city.getImprovements().add("powerPlant");
    	
    	
    	ksession.insert(city);
		
		((StatefulKnowledgeSessionImpl)ksession).session.getAgenda().activateRuleFlowGroup("manageProductions");
		ksession.fireAllRules(new RuleNameEqualsAgendaFilter("Power Plant"));
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		Assert.assertTrue("Power Plant Fired",firedRules.contains("Power Plant"));
		Assert.assertTrue("City Food Production Is Not Affected By Factory.",city.getFoodProduction()==5);
		Assert.assertTrue("City Resources Production Is Affected By Factory.",city.getResourcesProduction()==7);
		Assert.assertTrue("City Trade Production Is Not Affected By Factory.",city.getTradeProduction()==5);
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

	
	private static TileDTO getTile(Long id, Long posX, Long posY, Integer foodProduction, Integer resourcesProduction, Integer tradeProduction){
    	TileDTO tile = new TileDTO();
    	tile.setId(id);
    	tile.setPosX(posX);
    	tile.setPosY(posY);
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
    
	
	
	
	
	
	
}
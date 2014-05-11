package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.process.ProcessInstance;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;


public class TurnRuleJUnitTest extends BaseJUnitTest {
	/*
	 * Tests turn process, which manages run of turns
	 */
    @Test
    public void testFoodStockIncreasesEachTurn(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	PlayerDTO player1 = getPlayer(1L, "honza", "despotism");
    	PlayerDTO player2 = getPlayer(2L, "jirka", "democracy");
    	CityDTO city1 = getCity(5L,"marefy");
    	city1.setOwner(player1.getId());
    	CityDTO city2 = getCity(6L, "bucovice");
    	city2.setOwner(player2.getId());
		
		TileDTO tile1 = getTile(1L,1L,1L, "plains",null);
		TileDTO tile2 = getTile(2L,2L,2L, "rivers",null);
		TileDTO tile3 = getTile(3L,3L,3L, "swamp","oil");
		TileDTO tile4 = getTile(4L,4L,4L, "hills","coal");
		
		Set<Long> managedTiles1 = new HashSet<Long>();
		managedTiles1.add(tile1.getId());
		managedTiles1.add(tile2.getId());
		city1.setManagedTiles(managedTiles1);
		
		Set<Long> managedTiles2 = new HashSet<Long>();
		managedTiles2.add(tile3.getId());
		managedTiles2.add(tile4.getId());
		city2.setManagedTiles(managedTiles2);
		
		// insert facts		
    	ksession.insert(tile1);
		ksession.insert(tile2);
		ksession.insert(tile3);
		ksession.insert(tile4);
		ksession.insert(player1);
		ksession.insert(player2);
		ksession.insert(city1);
		ksession.insert(city2);
		
		ksession.fireAllRules();
		// initial turn to activate rules which should be fired in first run of turn process, others will be fired by turn process
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		HashMap<String,Object> params = new HashMap<String, Object>();
		// set the length of turn
		params.put("timer-delay", "5s");
		// start the game
		ProcessInstance pi = ksession.startProcess("cz.muni.fi.civ.newohybat.bpmn.turn", params);
		
		ksession.fireAllRules();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		
		Assert.assertTrue("City1 has foodproduction 3", city1.getFoodProduction()==3);
		Assert.assertTrue("City1 has foodconsumption 2", city1.getFoodConsumption()==2);
		
		Assert.assertTrue("City1 has foodStock 1", city1.getFoodStock()==1);
		
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Assert.assertTrue("City1 has foodproduction 3", city1.getFoodProduction()==3);
		Assert.assertTrue("City1 has foodconsumption 2", city1.getFoodConsumption()==2);
		Assert.assertTrue("City1 has foodStock 2", city1.getFoodStock()==2);
    }
    @Test
    public void testCityStarvesOutAnotherGrows(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	ksession.setGlobal("foodStockLimit", 10);
    	
    	// prepare test data
    	PlayerDTO player1 = getPlayer(1L, "honza", "democracy");
    	CityDTO starvingCity = getCity(5L,"marefy");
    	starvingCity.setOwner(player1.getId());
    	// foodConsumption 10 per turn
    	starvingCity.setSize(5);
    	starvingCity.setPeopleHappy(2);
    	starvingCity.setPeopleContent(1);
    	starvingCity.setPeopleTaxmen(1); 
    	starvingCity.setPeopleUnhappy(1);
    	// each turn somebody dies, until there is only 1 person - after four turns
    	starvingCity.setFoodStock(0);
		
    	// together 3 foodProduction per turn
		TileDTO tile1 = getTile(1L,1L,1L, "plains",null);
		TileDTO tile2 = getTile(2L,2L,2L, "rivers",null);
		
		Set<Long> managedTiles1 = new HashSet<Long>();
		managedTiles1.add(tile1.getId());
		managedTiles1.add(tile2.getId());
		starvingCity.setManagedTiles(managedTiles1);
		
		
		CityDTO growingCity = getCity(7L, "Brno");
		growingCity.setOwner(player1.getId());
		
		// foodConsumption 2 per turn
		growingCity.setSize(1);
		growingCity.setPeopleContent(1);
		
		growingCity.setFoodStock(0);
		
		// foodProduction 6 per turn
		TileDTO tile3 = getTile(3L, 2L, 10L, "forest", "game");
		TileDTO tile4 = getTile(4L, 2L, 11L, "grassland", null);
		tile4.getImprovements().add("irrigation");

		Set<Long> managedTiles2 = new HashSet<Long>();
		managedTiles2.add(tile3.getId());
		managedTiles2.add(tile4.getId());
		growingCity.setManagedTiles(managedTiles2);
		
		// insert facts		
		ksession.insert(tile1);ksession.fireAllRules();
		ksession.insert(tile2);ksession.fireAllRules();
		ksession.insert(tile3);ksession.fireAllRules();
		ksession.insert(tile4);ksession.fireAllRules();
		ksession.insert(player1);ksession.fireAllRules();
		ksession.insert(starvingCity);ksession.fireAllRules();
		ksession.insert(growingCity);ksession.fireAllRules();
		
		
		// initial turn to activate rules which should be fired in first run of turn process, others will be fired by turn process
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		
		ksession.fireAllRules();
		
		HashMap<String,Object> params = new HashMap<String, Object>();
		// set the length of turn
		params.put("timer-delay", "1s");
		// start the game
		ProcessInstance pi = ksession.startProcess("cz.muni.fi.civ.newohybat.bpmn.turn", params);
		
		Thread t1 = new Thread(new Runnable() {
	     public void run()
	     {
	          // code goes here.
	    	 ksession.fireAllRules();
	     }});  t1.start();
		
		
		try {
			Thread.sleep(800);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertTrue("Starving City has size 4", starvingCity.getSize()==4);
		Assert.assertTrue("Starving city has foodConsumption 10", starvingCity.getFoodConsumption()==10);
		Assert.assertTrue("Growing City Has Food Surplus 4",(growingCity.getFoodProduction()-growingCity.getFoodConsumption())==4);
		Assert.assertTrue("Growing City Has Food Stock 4",growingCity.getFoodStock()==4);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertTrue("Starving City has size 3", starvingCity.getSize()==3);
		Assert.assertTrue("Starving city has foodConsumption 8", starvingCity.getFoodConsumption()==8);
		Assert.assertTrue("Growing City Has Food Stock 8",growingCity.getFoodStock()==8);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertTrue("Starving City has size 2", starvingCity.getSize()==2);
		Assert.assertTrue("Growing City has size 2", growingCity.getSize()==2);
		Assert.assertTrue("Growing City Has Food Stock 0",growingCity.getFoodStock()==0);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertTrue("Starving City has size 1", starvingCity.getSize()==1);
		Assert.assertTrue("Growing City Has FoodStock 2", growingCity.getFoodStock()==2);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertTrue("Starving City has size 1", starvingCity.getSize()==1);
		Assert.assertTrue("Only One Person Lives In Starving City", starvingCity.getPeopleHappy()==1);
		Assert.assertTrue("Growing City Has FoodStock 4", growingCity.getFoodStock()==4);
		
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
	
	private static TileDTO getTile(Long id,Long posX,Long posY, String terrain,String special){
    	TileDTO tile = new TileDTO();
    	tile.setId(id);
    	tile.setPosX(posX);
    	tile.setPosY(posY);
    	tile.setTerrain(terrain);
    	tile.setSpecial(special);
    	tile.setFoodProduction(0);
    	tile.setResourcesProduction(0);
    	tile.setTradeProduction(0);
    	tile.setImprovements(new HashSet<String>());
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
		player.setTreasury(0);
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
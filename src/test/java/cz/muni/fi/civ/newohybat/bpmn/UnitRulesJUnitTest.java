package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import org.kie.api.runtime.rule.FactHandle;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.drools.events.TileImprovementEvent;
import cz.muni.fi.civ.newohybat.drools.events.TurnEvent;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;


public class UnitRulesJUnitTest extends BaseJUnitTest {
    /*
     * Tests properties of unit derived from properties of its UnitType and bonuses of the tile
     */
    @Test
    public void testPhalanx(){
		
		UnitDTO unit = getUnit(1L,"phalanx",3L);
		UnitTypeDTO unitType = getUnitType("phalanx"); 
		ksession.insert(unitType);
		
		TileDTO tile = getTile(3L, 3L,3L,"plains", new HashSet<String>());
		tile.setDefenseBonus(0);
		ksession.insert(tile);	
		ksession.insert(unit);
		ksession.fireAllRules();		
		
		Assert.assertTrue("Phalanx type defense strength", unitType.getDefenseStrength().equals(2));
		Assert.assertTrue("Phalanx without bonus.",unit.getDefenseStrength().equals(2));
	}
    /*
     * Tests properties of unit derived from properties of its UnitType and bonuses of the tile
     */
    @Test
    public void testPhalanxInForest(){
    	UnitDTO unit = getUnit(1L,"phalanx",3L);
    	// forest has 50% defense bonus
		TileDTO tile = getTile(3L, 3L,3L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50);
		
		ksession.insert(tile);
		ksession.insert(getUnitType("phalanx"));
		ksession.insert(unit);
		
        ksession.fireAllRules();
		
		Assert.assertTrue("Phalanx with forest defense bonus.",unit.getDefenseStrength().equals(3));
	}
    /*
     * Tests properties of unit derived from properties of its UnitType and bonuses of the tile and their change after unit
     * moved(changed tile).
     */
    @Test
    public void testPhalanxChangeTile(){
    	UnitDTO unit = getUnit(1L,"phalanx",3L);
    	// forest has 50% defense bonus
		TileDTO tile = getTile(3L, 3L,3L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50);
		ksession.insert(tile);
		// plains has no defense bonus
		TileDTO tile2 = getTile(4L,4L,4L, "plains", new HashSet<String>());
		tile2.setDefenseBonus(0);
		ksession.insert(tile2);
		
		ksession.insert(getUnitType("phalanx"));
		
		
		ksession.fireAllRules();
		
		FactHandle unitHandle = ksession.insert(unit);
		
        ksession.fireAllRules();
		
        // unit is in the forest
		Assert.assertTrue("Phalanx with forest defense bonus.",unit.getDefenseStrength().equals(3));
		// move the unit
		unit.setTile(tile2.getId());
		ksession.update(unitHandle,unit);
		ksession.fireAllRules();
		// unit is on the plains
		Assert.assertTrue("Phalanx plains no defense bonus.",unit.getDefenseStrength().equals(2));
    }
    
    @Test
    public void testUnitSetHomeCity(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	//prepare test data
    	PlayerDTO player  = getPlayer(1L, "Galbatorix");
    	PlayerDTO player2  = getPlayer(2L, "Islansadi");
    	
    	TileDTO tile1 = getTile(1L, 1L, 1L, "plains", new HashSet<String>());
    	TileDTO tile2 = getTile(2L, 1L, 2L, "plains", new HashSet<String>());
    	TileDTO tile3 = getTile(3L, 2L, 1L, "plains", new HashSet<String>());
    	TileDTO tile4 = getTile(4L, 2L, 2L, "plains", new HashSet<String>());
    	
    	CityDTO city = getCity(1L, "Urrubain", tile1.getId());
    	city.setOwner(player.getId());
    	CityDTO city2 = getCity(2L, "Illiria", tile2.getId());
    	city2.setOwner(player2.getId());
    	CityDTO city3 = getCity(3L, "Trongim", tile3.getId());
    	city3.setOwner(player2.getId());
    	
    	UnitTypeDTO phalanx = getUnitType("phalanx");
    	phalanx.getActions().add("setHomeCity");
    	
    	UnitDTO unit = getUnit(15L,phalanx.getIdent(), tile4.getId());
    	unit.setOwner(player.getId());
    	
    	UnitDTO unit2 = getUnit(32L, phalanx.getIdent(), tile3.getId());
    	unit2.setOwner(player2.getId());
    	
    	city.getHomeUnits().add(unit.getId());
    	city3.getHomeUnits().add(unit2.getId());
    	
    	// insert as facts
    	ksession.insert(tile1);
    	ksession.insert(tile2);
    	ksession.insert(tile3);
    	ksession.insert(tile4);
    	
    	ksession.insert(phalanx);
    	
    	ksession.insert(player);
    	ksession.insert(player2);
    	
    	ksession.insert(city);
    	ksession.insert(city2);
    	ksession.insert(city3);
    	
    	FactHandle unitHandle = ksession.insert(unit);
    	FactHandle unitHandle2 = ksession.insert(unit2);
    	
    	ksession.fireAllRules();
    	
    	// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
    	
    	Assert.assertTrue("Can't Set Home City When Not In A City Fired", firedRules.contains("Can't Set Home City When Not In A City"));
    	Assert.assertFalse("Unit Can't Set Home City", unit.getActions().contains("setHomeCity"));
    	
    	unit.setTile(city2.getCityCentre());
    	ksession.update(unitHandle, unit);
    	ksession.fireAllRules();
    	
    	// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe2 = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe2.capture() );
		List<String> firedRules2 = getFiredRules(aafe2.getAllValues());
    	
    	Assert.assertTrue("Can't Set Home City When Not In Own City Fired", firedRules2.contains("Can't Set Home City When Not In Own City"));
    	Assert.assertFalse("Unit Can't Set Home City", unit.getActions().contains("setHomeCity"));
    	
    	unit.setTile(tile4.getId());
    	ksession.update(unitHandle, unit);
    	unit2.setTile(city2.getCityCentre());
    	ksession.update(unitHandle2, unit2);
    	ksession.fireAllRules();
    	
    	Assert.assertTrue("Unit Can Set Home City", unit2.getActions().contains("setHomeCity"));
    }
    
    @Test
    public void testCanEstablishTradeRoute(){
    	
    }
    
    @Test
    public void testCanBuildMine(){
    	
    }
    
    private static CityDTO getCity(Long id, String name,Long tileId){
    	CityDTO city = new CityDTO();
    	city.setId(id);
    	city.setName(name);
    	city.setCityCentre(tileId);
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
    	return city;
	}
    private static UnitDTO getUnit(Long id,String type, Long pos){
    	UnitDTO unit = new UnitDTO();
    	unit.setId(id);
    	unit.setType(type);
    	unit.setAttackStrength(0);
    	unit.setDefenseStrength(0);
    	unit.setTile(pos);
    	unit.setActions(new HashSet<String>());
    	return unit;
    }
    private static TileDTO getTile(Long id,Long posX, Long posY, String terrain, Set<String> imps){
    	TileDTO tile = new TileDTO();
    	tile.setId(id);
    	tile.setPosX(posX);
    	tile.setPosY(posY);
    	tile.setImprovements(imps);
    	tile.setTerrain(terrain);
    	tile.setDefenseBonus(0);
    	return tile;
    }
    private static PlayerDTO getPlayer(Long id, String name){
    	PlayerDTO player = new PlayerDTO();
    	player.setId(id);
    	player.setName(name);
    	return player;
    }
    private static UnitTypeDTO getUnitType(String ident){
    	UnitTypeDTO type = new UnitTypeDTO();
		type.setIdent(ident);
		Set<String> actions = new HashSet<String>();
		type.setActions(actions);
		return type;
    }
    private static TileImprovementDTO getTileImp(String ident, Integer cost){
    	TileImprovementDTO imp = new TileImprovementDTO();
    	imp.setIdent(ident);
    	imp.setCost(cost);
    	return imp;
    }
}
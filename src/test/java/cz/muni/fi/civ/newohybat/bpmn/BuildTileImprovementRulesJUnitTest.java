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
import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;


public class BuildTileImprovementRulesJUnitTest extends BaseJUnitTest {
    /*
     * Test available action update after each move of unit. Some actions, like here buildIrrigation, are dependent
     * upon its surrounding. Tile can be irrigated when it is close to source of water.
     * In this case it is not possible.
     */
    @Test
    public void testCantIrrigateWhenCloseTilesAreNot(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	// Add mock eventlistener
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	// prepare test data
    	PlayerDTO player = getPlayer(11L, "honza");
    	UnitDTO unit = getUnit("phalanx",5L);
		unit.setOwner(player.getId());
		unit.getActions().add("buildIrrigation");
		// this triggers the "Build Irrigation" rule when it is possible to process
		unit.setCurrentAction("buildIrrigation");
		// map definition - tiles without source of water
		TileDTO tile = getTile(5L, 5L,5L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50); 
		tile.setPosX(45L);
		tile.setPosY(56L);
		TileDTO tile2 = getTile(6L, 3L,3L, "forest", new HashSet<String>());
		tile2.setDefenseBonus(50); 
		tile2.setPosX(45L);
		tile2.setPosY(57L);
		
		// insert test data as facts
		ksession.insert(player);
		ksession.insert(getTileImp("irrigation",1));
		ksession.insert(getUnitType("phalanx"));
		ksession.insert(tile);
		ksession.insert(tile2);
		
		ksession.insert(unit);
		
		ksession.fireAllRules();
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		// rule "Build Irrigation" didn't fire
		Assert.assertFalse(firedRules.contains("Build Irrigation"));
		
	}
    /*
     * Tests that can build irrigation, when close tile is irrigated
     */
    @Test
    public void testCanIrrigateWhenOneCloseTileIs(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	PlayerDTO player = getPlayer(11L, "honza");
    	TileDTO tile = getTile(3L, 3L,3L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50);
		
		UnitDTO unit = getUnit("phalanx",tile.getId());
		unit.setOwner(player.getId());
		unit.getActions().add("buildIrrigation");
		unit.setCurrentAction("buildIrrigation");
		// already irrigated tile
		TileDTO tile2 = getTile(4L,3L,4L, "plains", new HashSet<String>());
		tile2.getImprovements().add("irrigation");
		tile2.setDefenseBonus(0);
		// cost of improvement irrigation is two turns, so action will last 2 turns
		TileImprovementDTO irrigation = getTileImp("irrigation",2);
		
		// insert facts
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(irrigation);
		ksession.insert(getUnitType("phalanx"));
		
		ksession.insert(player);
		
		ksession.fireAllRules();
		
		ksession.insert(unit);
		
		// this starts the process of build irrigation
		ksession.fireAllRules();
		// get processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		Assert.assertTrue("Only one process should be in session",processes.size()==1);
		// get the process
		ProcessInstance pi = processes.get(0);
		// simulate the two turns to complete action
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// still running
		assertProcessInstanceActive(pi.getId(), ksession);
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// completed
		assertProcessInstanceCompleted(pi.getId(), ksession);
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		// rule "Build Irrigation" did fire
		Assert.assertTrue("Build Irrigation Rule fired.",firedRules.contains("Build Irrigation"));
		Assert.assertTrue("Process Build Irrigation completed.",tile.getImprovements().contains("irrigation"));
		Assert.assertNull("Current action should change to null", unit.getCurrentAction());
    }
    /*
     * Tests that can build irrigation, when close tile is ocean
     */
    @Test
    public void testCanIrrigateWhenOneCloseTileIsOcean(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	PlayerDTO player = getPlayer(11L, "honza");
    	TileDTO tile = getTile(3L, 3L,3L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50);
		
		UnitDTO unit = getUnit("phalanx",tile.getId());
		unit.setOwner(player.getId());
		unit.getActions().add("buildIrrigation");
		unit.setCurrentAction("buildIrrigation");
		// tile with ocean
		TileDTO tile2 = getTile(4L,3L,4L, "ocean", new HashSet<String>());
		tile2.setDefenseBonus(0);
		// cost of improvement irrigation is two turns, so action will last 2 turns
		TileImprovementDTO irrigation = getTileImp("irrigation",2);
		
		// insert facts
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(irrigation);
		ksession.insert(getUnitType("phalanx"));
		
		ksession.insert(player);
		
		ksession.fireAllRules();
		
		ksession.insert(unit);
		
		// this starts the process of build irrigation
		ksession.fireAllRules();
		// get processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		Assert.assertTrue("Only one process should be in session",processes.size()==1);
		// get the process
		ProcessInstance pi = processes.get(0);
		// simulate the two turns to complete action
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// still running
		assertProcessInstanceActive(pi.getId(), ksession);
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// completed
		assertProcessInstanceCompleted(pi.getId(), ksession);
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		// rule "Build Irrigation" did fire
		Assert.assertTrue("Build Irrigation Rule fired.",firedRules.contains("Build Irrigation"));
		Assert.assertTrue("Process Build Irrigation completed.",tile.getImprovements().contains("irrigation"));
		Assert.assertNull("Current action should change to null", unit.getCurrentAction());
    }
    /*
     * Tests that can build irrigation, when close tile is river
     */
    @Test
    public void testCanIrrigateWhenOneCloseTileIsRiver(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	PlayerDTO player = getPlayer(11L, "honza");
    	TileDTO tile = getTile(3L, 3L,3L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50);
		
		UnitDTO unit = getUnit("phalanx",tile.getId());
		unit.setOwner(player.getId());
		unit.getActions().add("buildIrrigation");
		unit.setCurrentAction("buildIrrigation");
		// tile with river
		TileDTO tile2 = getTile(4L,3L,4L, "rivers", new HashSet<String>());
		tile2.setDefenseBonus(0);
		// cost of improvement irrigation is two turns, so action will last 2 turns
		TileImprovementDTO irrigation = getTileImp("irrigation",2);
		
		// insert facts
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(irrigation);
		ksession.insert(getUnitType("phalanx"));
		
		ksession.insert(player);
		
		ksession.fireAllRules();
		
		ksession.insert(unit);
		
		// this starts the process of build irrigation
		ksession.fireAllRules();
		// get processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		Assert.assertTrue("Only one process should be in session",processes.size()==1);
		// get the process
		ProcessInstance pi = processes.get(0);
		// simulate the two turns to complete action
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// still running
		assertProcessInstanceActive(pi.getId(), ksession);
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// completed
		assertProcessInstanceCompleted(pi.getId(), ksession);
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		// rule "Build Irrigation" did fire
		Assert.assertTrue("Build Irrigation Rule fired.",firedRules.contains("Build Irrigation"));
		Assert.assertTrue("Process Build Irrigation completed.",tile.getImprovements().contains("irrigation"));
		Assert.assertNull("Current action should change to null", unit.getCurrentAction());
    }
    @Test
    public void testCancelIrrigation(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	PlayerDTO player = getPlayer(11L, "honza");
    	TileDTO tile = getTile(3L, 3L,3L,"forest", new HashSet<String>());
		tile.setDefenseBonus(50);
		
		UnitDTO unit = getUnit("phalanx",tile.getId());
		unit.setOwner(player.getId());
		unit.getActions().add("buildIrrigation");
		unit.setCurrentAction("buildIrrigation");
		// tile with river
		TileDTO tile2 = getTile(4L,3L,4L, "rivers", new HashSet<String>());
		tile2.setDefenseBonus(0);
		// cost of improvement irrigation is two turns, so action will last 2 turns
		TileImprovementDTO irrigation = getTileImp("irrigation",2);
		
		// insert facts
		ksession.insert(tile);
		ksession.insert(tile2);
		ksession.insert(irrigation);
		ksession.insert(getUnitType("phalanx"));
		
		ksession.insert(player);
		
		ksession.fireAllRules();
		
		ksession.insert(unit);
		
		// this starts the process of build irrigation
		ksession.fireAllRules();
		// get processes
		List<ProcessInstance> processes = (List<ProcessInstance>)ksession.getProcessInstances();
		
		Assert.assertTrue("Only one process should be in session",processes.size()==1);
		// get the process
		ProcessInstance pi = processes.get(0);
		// simulate the two turns to complete action
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		// still running
		assertProcessInstanceActive(pi.getId(), ksession);
		// cancel the action of unit
		ksession.getEntryPoint("ActionCanceledStream").insert(new TileImprovementEvent(unit.getId()));
		ksession.fireAllRules();
		// completed
		assertProcessInstanceCompleted(pi.getId(), ksession);
		// new turn
		ksession.getEntryPoint("GameControlStream").insert(new TurnEvent());
		ksession.fireAllRules();
		
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		// rule "Build Irrigation" did fire
		Assert.assertTrue("Build Irrigation Rule fired.",firedRules.contains("Build Irrigation"));
		Assert.assertFalse("Process Build Irrigation canceled.",tile.getImprovements().contains("irrigation"));
		Assert.assertNull("Current action should change to null", unit.getCurrentAction());
    }
    private static UnitDTO getUnit(String type, Long pos){
    	UnitDTO unit = new UnitDTO();
    	unit.setId(3L);
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
		actions.add("buildIrrigation");
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
package cz.muni.fi.civ.newohybat.bpmn;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.rule.QueryResults;

import cz.muni.fi.civ.newohybat.persistence.facade.dto.PlayerDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.TileImprovementDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;


public class FightRulesJUnitTest extends BaseJUnitTest {
    /*
     * Tests that when units of two players stand on the same tile, fight begins. Fight is defined only as demonstration.
     */
	@Test
    public void testUnitsOnSameFileFight(){
		ksession.addEventListener(new DebugAgendaEventListener());
		ksession.addEventListener(new DebugRuleRuntimeEventListener());
    	TileDTO tile = getTile(3L, "forest");
		tile.setDefenseBonus(50);
		tile.setPosX(1L);
		tile.setPosY(1L);
		
		PlayerDTO player1 = getPlayer(1L, "Looser");
    	UnitDTO defender = getUnit(1L,"phalanx",tile.getId(), player1.getId());
    	defender.setHealthPoints(10);
    	defender.setDefenseStrength(10);
		
		PlayerDTO player2 = getPlayer(2L, "Conqueror");
		UnitDTO attacker = getUnit(2L,"phalanx", tile.getId(), player2.getId());
		attacker.setHealthPoints(10);
		attacker.setAttackStrength(15);
		
		ksession.setGlobal("timeout", "1s");
		
		ksession.insert(getUnitType("phalanx"));
		
		ksession.fireAllRules();
		
		ksession.insert(tile);		ksession.fireAllRules();
		ksession.insert(player1);		ksession.fireAllRules();
		ksession.insert(player2);		ksession.fireAllRules();
		ksession.insert(defender);		ksession.fireAllRules();
		ksession.insert(attacker);
		
		ksession.fireAllRules();
		
		// check whether unit died
		QueryResults results = ksession.getQueryResults("Get Unit Dead", new Object[]{defender});
		Assert.assertTrue("Defender died.",results.size()>0);
		Assert.assertTrue("Dead Unit Still In Memory", ksession.getFactHandle(defender)!=null);
		
		// check that after removing dead unit, all supporting objects - UnitDead - are removed.
		// get unit dead and delete it
		if(results.size()==1){
			ksession.delete(ksession.getFactHandle(defender));
		}
		ksession.fireAllRules();
		// get all unitdead after delete
		QueryResults resultsAfter = ksession.getQueryResults("Get Unit Dead", new Object[]{defender});
		
		Assert.assertTrue("Defender Is Not Anymore.",resultsAfter.size()==0);
		
		Assert.assertTrue("Dead Unit Removed From Memory", ksession.getFactHandle(defender)==null);
		
    }

    private static UnitDTO getUnit(Long id,String type, Long pos, Long owner){
    	UnitDTO unit = new UnitDTO();
    	unit.setId(id);
    	unit.setType(type);
    	unit.setAttackStrength(0);
    	unit.setDefenseStrength(0);
    	unit.setTile(pos);
    	unit.setOwner(owner);
    	return unit;
    }
    private static TileDTO getTile(Long id, String terrain){
    	TileDTO tile = new TileDTO();
    	tile.setId(id);
    	tile.setImprovements(new HashSet<String>());
    	tile.setTerrain(terrain);
    	//tile.setDefenseBonus(50);
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
		type.setIdent("phalanx");
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
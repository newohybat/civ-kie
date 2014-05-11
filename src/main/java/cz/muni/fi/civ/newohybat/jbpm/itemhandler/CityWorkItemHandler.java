package cz.muni.fi.civ.newohybat.jbpm.itemhandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;







import java.util.Set;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;

public class CityWorkItemHandler implements WorkItemHandler {

	private KieSession ksession;
	
	public CityWorkItemHandler(KieSession ksession) {
		this.ksession=ksession;
	}
	
	public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {
	
	}

	public void executeWorkItem(WorkItem item, WorkItemManager manager) {
		UnitDTO unit = (UnitDTO)item.getParameter("unit");
		CityDTO city = new CityDTO();
		city.setCityCentre(unit.getTile());
		city.setOwner(unit.getOwner());
		Set<Long> managedTiles = new HashSet<Long>();
		managedTiles.add(city.getCityCentre());
		city.setManagedTiles(managedTiles);
		city.setEnabledImprovements(new HashSet<String>());
		city.setEnabledUnitTypes(new HashSet<String>());
		city.setHomeUnits(new HashSet<Long>());
		city.setImprovements(new HashSet<String>());
		city.setSize(1);
		city.setPeopleContent(1);
		city.setPeopleEntertainers(0);
		city.setPeopleHappy(0);
		city.setPeopleScientists(0);
		city.setPeopleTaxmen(0);
		city.setPeopleUnhappy(0);
		// cityService.create(city);
		city.setId((long)Math.ceil(Math.random()*1000000));	// after preceding line uncomented, delete this one
		ksession.insert(city);
		// unitService.remove(unit);
		unit.setCurrentAction(null);
		Map<String,Object> results = new HashMap<String, Object>();
		results.put("city", city);
		manager.completeWorkItem(item.getId(), results);
	}

}

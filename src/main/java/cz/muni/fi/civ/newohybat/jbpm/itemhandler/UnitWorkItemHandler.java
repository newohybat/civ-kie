package cz.muni.fi.civ.newohybat.jbpm.itemhandler;

import java.util.HashMap;
import java.util.Map;






import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitDTO;
import cz.muni.fi.civ.newohybat.persistence.facade.dto.UnitTypeDTO;

public class UnitWorkItemHandler implements WorkItemHandler {

	private KieSession ksession;
	
	public UnitWorkItemHandler(KieSession ksession) {
		this.ksession=ksession;
	}
	
	public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {
	
	}

	public void executeWorkItem(WorkItem item, WorkItemManager manager) {
		UnitTypeDTO unitType = (UnitTypeDTO)item.getParameter("unitType");
		CityDTO city = (CityDTO)item.getParameter("city");
		UnitDTO unit = new UnitDTO();
		unit.setType(unitType.getIdent());
		unit.setCurrentAction(null);
		unit.setDistanceHome(0);
		unit.setOwner(city.getOwner());
		unit.setTile(city.getCityCentre());
		// unitService.create(unit);
		unit.setId((long)Math.ceil(Math.random()*1000000));	// after preceding line uncomented, delete this one
		ksession.insert(unit);
		city.getHomeUnits().add(unit.getId());
		city.setCurrentUnit(null);
		Map<String,Object> results = new HashMap<String, Object>();
		results.put("unit", unit);
		manager.completeWorkItem(item.getId(), results);
	}

}

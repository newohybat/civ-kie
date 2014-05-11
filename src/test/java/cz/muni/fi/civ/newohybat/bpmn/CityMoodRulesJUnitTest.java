package cz.muni.fi.civ.newohybat.bpmn;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.process.ProcessInstance;
import org.mockito.ArgumentCaptor;

import cz.muni.fi.civ.newohybat.persistence.facade.dto.CityDTO;


public class CityMoodRulesJUnitTest extends BaseJUnitTest {
	@Test
    public void testCityNeitherWeLoveDayNor(){
    	ksession.addEventListener(new DebugAgendaEventListener());
    	ksession.addEventListener(new DebugRuleRuntimeEventListener());
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = new CityDTO();
		// insert facts
		ksession.insert(city);
		ksession.fireAllRules();
	}
	@Test
    public void testCityNeitherWeLoveDayNorDisorder(){
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
		city.setPeopleHappy(5);
		city.setPeopleContent(6);
		city.setPeopleUnhappy(1);
		city.setPeopleEntertainers(0);
		city.setPeopleScientists(0);
		city.setPeopleTaxmen(0);
		city.setWeLoveDay(false);
		city.setDisorder(false);
		city.setSize(4);
		// insert facts
		ksession.insert(city);
		ProcessInstance processInstance = ksession.startProcess("cz.muni.fi.civ.newohybat.bpmn.cityturnprocess");
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		Assert.assertFalse("City Is Not In Disorder.",city.getDisorder());
		Assert.assertFalse("City Is Not Celebrating We Love Day.", city.getWeLoveDay());
    }
	@Test
    public void testCityWeLoveDay(){
    	
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
		city.setPeopleHappy(5);
		city.setPeopleContent(3);
		city.setPeopleUnhappy(0);
		city.setPeopleEntertainers(0);
		city.setPeopleScientists(0);
		city.setPeopleTaxmen(0);
		city.setWeLoveDay(false);
		city.setDisorder(false);
		city.setSize(4);
		// insert facts
		ksession.insert(city);
		ProcessInstance processInstance = ksession.startProcess("cz.muni.fi.civ.newohybat.bpmn.cityturnprocess");
		
		
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		Assert.assertFalse("City Is Not In Disorder.",city.getDisorder());
		Assert.assertTrue("City Is Celebrating We Love Day.", city.getWeLoveDay());
    }
	@Test
    public void testCityDisorder(){
    	
    	AgendaEventListener ael = mock( AgendaEventListener.class );
    	ksession.addEventListener( ael );
    	
    	// prepare test data
    	
    	CityDTO city = getCity(5L,"marefy");
		city.setPeopleHappy(5);
		city.setPeopleContent(3);
		city.setPeopleUnhappy(6);
		city.setPeopleEntertainers(0);
		city.setPeopleScientists(0);
		city.setPeopleTaxmen(0);
		city.setWeLoveDay(false);
		city.setDisorder(false);
		city.setSize(4);
		
		// insert facts
		ksession.insert(city);
		ProcessInstance processInstance = ksession.startProcess("cz.muni.fi.civ.newohybat.bpmn.cityturnprocess");
		
		// get fired rules
		ArgumentCaptor<AfterMatchFiredEvent> aafe = ArgumentCaptor.forClass( AfterMatchFiredEvent.class );
		verify( ael ,atLeastOnce()).afterMatchFired( aafe.capture() );
		List<String> firedRules = getFiredRules(aafe.getAllValues());
		
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		Assert.assertTrue("City Is In Disorder.",city.getDisorder());
		Assert.assertFalse("City Is  Not Celebrating We Love Day.", city.getWeLoveDay());
    }

	private static CityDTO getCity(Long id, String name){
		CityDTO city = new CityDTO();
		city.setId(id);
		city.setName(name);
		city.setResourcesConsumption(0);
		city.setResourcesProduction(0);
		city.setUnitsSupport(0);
		city.setFoodConsumption(0);
		city.setSize(0);
		city.setTradeProduction(0);
		return city;
	}

}
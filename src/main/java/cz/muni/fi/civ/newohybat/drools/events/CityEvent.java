package cz.muni.fi.civ.newohybat.drools.events;

import java.io.Serializable;

public class CityEvent implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1512267179727810820L;
	private final Long unitId;

	public CityEvent(Long unitId){
		this.unitId=unitId;
	}
	
	public Long getUnitId() {
		return unitId;
	}
	
	private transient String toString;
	
	@Override
	public String toString() {
		if (toString == null) {
			toString = super.toString() + this.getClass().toString() + ":" + unitId;
		}
	return toString;
	}
}

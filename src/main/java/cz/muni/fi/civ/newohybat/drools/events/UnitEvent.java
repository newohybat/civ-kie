package cz.muni.fi.civ.newohybat.drools.events;

import java.io.Serializable;

public class UnitEvent implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1512267179727810820L;
	private final Long cityId;

	public UnitEvent(Long cityId){
		this.cityId=cityId;
	}
	
	public Long getCityId() {
		return cityId;
	}
	
	private transient String toString;
	
	@Override
	public String toString() {
		if (toString == null) {
			toString = super.toString() + this.getClass().toString() + ":" + cityId;
		}
	return toString;
	}
}

package cz.muni.fi.civ.newohybat.drools.events;

import java.io.Serializable;

public class AdvanceEvent implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8030921318481503020L;
	private final Long playerId;

	public AdvanceEvent(Long playerId){
		this.playerId=playerId;
	}
	
	public Long getPlayerId() {
		return playerId;
	}
	
	private transient String toString;
	
	@Override
	public String toString() {
		if (toString == null) {
			toString = super.toString() + this.getClass().toString() + ":" + playerId;
		}
	return toString;
	}
}

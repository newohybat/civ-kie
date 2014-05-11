package cz.muni.fi.civ.newohybat.drools.events;

import java.io.Serializable;

public class GameEvent implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1512267179727810820L;
	public GameEvent(){
	}
	
	
	private transient String toString;
	
	@Override
	public String toString() {
		if (toString == null) {
			toString = super.toString() + this.getClass().toString();
		}
	return toString;
	}
}

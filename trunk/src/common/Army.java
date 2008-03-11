/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

/*
 * Created on 21.05.2004
 *
 */
package common;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * A virtual army which can contain any combination of units
 * @author Helge Richter
 *
 */
public class Army {
	
	//STATIC VARIABLES
	public static final int NO_LIMIT = -1;
	
	//VARIABLES
	private Vector<Unit> units = new Vector<Unit>(1,1);
	private String name = " ";
	
	private int upperLimiter = NO_LIMIT;
	private int lowerLimiter = NO_LIMIT;

	private int bv = 0;
	private int id;
	private boolean locked = false;
	
	private boolean armyPlayerLocked = false; //Used by players to keep armies from being cleared
	private boolean armyDisabled = false;
	
	private float opForceSize = NO_LIMIT;
	
	private Hashtable<Integer,Integer> c3Network = new Hashtable<Integer,Integer>();

	private Vector<Integer> commanders = new Vector<Integer>(1,1);
	
	//CONSTRUCTORS
	public Army(){
		//no content
	}
	
	//METHODS
	public int getAmountOfUnits() {
		return units.size();
	}
	
	public boolean isDisabled() {
		return armyDisabled;
	}
	
	public void disableArmy() {
		armyDisabled = true;
	}
	
	public void enableArmy() {
		armyDisabled = false;
	}
	
	public void toggleArmyDisabled() {
		armyDisabled = !armyDisabled;
	}
	
	public boolean isPlayerLocked() {
		return armyPlayerLocked;
	}
	
	public void playerLockArmy() {
		armyPlayerLocked = true;
	}
	
	public void playerUnlockArmy() {
		armyPlayerLocked = false;
	}
	
	/**
	 * @return Returns the locked.
	 */
	public boolean isLocked() {
		return locked;
	}
	
	/**
	 * @param locked The locked to set.
	 */
	public void setLocked(boolean b) {
		locked = b;
	}
	
	/**
	 * @return return the BV.
	 */
	public int getBV() {
	    
		if (bv < 0)
		    return 0;
		
		return bv;
	}
	
	/**
	 * @param bv The bV to set.
	 */
	public void setBV(int i) {
		bv = i;
	}
	
	/**
	 * @return Returns the lowerLimit.
	 */
	public int getLowerLimiter() {
		return lowerLimiter;
	}
	
	/**
	 * @param lowerLimit The lowerLimit to set.
	 */
	public void setLowerLimiter(int lowerLimit) {		
		lowerLimiter = lowerLimit;
	}
	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name The name to set.
	 */
	public void setName(String s) {
		name = s;
	}
	

    /**
     * Add a unit to a specific position.
     * @param unit
     * @param Position
     */
    public void addUnit(Unit unit, int Position){
        units.add(Position,unit);
    }
    /**
     * add units to the army vector.
     * @param unit
     */
    public void addUnit(Unit unit){
        units.add(unit);
    }
    
	/**
	 * @return Returns the units.
	 */
	public Vector<Unit> getUnits() {
		return units;
	}
    
    /**
     * This will pull The number of unit types this army holds
     *    i.e. type = Unit.MEK all meks will be counted.
     * @param type The unit type to check against. MEK VEHICLE
     * @return number of unit type that exist in this army
     */
    public int getNumberOfUnitTypes(int type){
        int count = 0;

        for ( Unit unit : getUnits() ){
            if ( unit.getType() == type )
                count++;
        }
        
        return count;
    }
    
	/**
	 * @return Returns the upperLimit.
	 */
	public int getUpperLimiter() {
		return upperLimiter;
	}
	
	/**
	 * @param upperLimit The upperLimit to set.
	 */
	public void setUpperLimiter(int upperLimit) {
		upperLimiter = upperLimit;
	}
	

	/**
	 * @return Returns the iD.
	 */
	public int getID() {
		return id;
	}
	
	public Unit getUnit(int unitId){
	    
	    for (Unit currU : this.getUnits()){
	        if (currU.getId() == unitId)
	            return currU;
	    }
	    
		return null;
	}
	
	/**
	 * @param id The iD to set.
	 */
	public void setID(int id) {
		this.id = id;
	}
	
	public String toString(boolean toClient, String delimiter){
		StringBuilder result = new StringBuilder();
		result.append(getID());
		result.append(delimiter);
		if (toClient) {
			result.append(getBV());
			result.append(delimiter);
			result.append(isLocked());
			result.append(delimiter);
		}
		if (getName().length() > 0 )
			result.append(getName());
		else 
			result.append(" ");
		result.append(delimiter);
		result.append(getLowerLimiter());
		result.append(delimiter);
		result.append(getUpperLimiter());
		result.append(delimiter);
		result.append(getUnits().size());
		result.append(delimiter);
		for ( Unit unit : getUnits()){
			result.append(unit.getId());
			result.append(delimiter);
		}
		result.append(delimiter);
		result.append(getC3Network().size());
		result.append(delimiter);
		for (Integer currI : this.getC3Network().keySet()) {
			result.append(currI);
			result.append(delimiter);
			result.append(this.getC3Network().get(currI));
			result.append(delimiter);
		}
		result.append(opForceSize);
		result.append(delimiter);
		
		result.append(commanders.size());
		result.append(delimiter);
		for ( Integer unitId : commanders){
			result.append(unitId);
			result.append(delimiter);
		}
		result.append(Boolean.toString(armyPlayerLocked));
		result.append(delimiter);
		result.append(Boolean.toString(armyDisabled));
		result.append(delimiter);
		return result.toString();
	}

	/**
	 * @return Returns the C3Networks.
	 */
	public Hashtable<Integer,Integer> getC3Network() {
		return c3Network;
	}
	
	/**
	 * @param c3Network The C3Networks to set.
	 */
	public void setC3Network(Hashtable<Integer,Integer> network) {
		c3Network = network;
	}
	
	public void removeUnitFromC3Network(int unitID){
	    
		if (this.getC3Network().get(new Integer(unitID)) != null) {
	        this.getC3Network().remove(new Integer(unitID));
	        return;
	    }
		
		Iterator<Integer> i = this.getC3Network().keySet().iterator();
	    while (i.hasNext()) {
	        Integer slave = i.next();
	        Integer master = this.getC3Network().get(slave);
	        if (master.intValue() == unitID)
	            i.remove();
	    }
	        
	}
	/**
	 * Return the number of C3 networks in this army.
	 * @return
	 */
	public int getNumberOfNetworks() {
		int count = 0;
		
		for ( int uid : c3Network.values() ) {
			
			try {
				Unit master = this.getUnit(uid);
				if ( !c3Network.containsKey(uid) && master.hasBeenC3LinkedTo(this) )
					count++;
			}catch ( Exception ex)
			{}
			
		}
		
		return Math.max(1, count);
	}
	
	public float getOpForceSize() {
		return this.opForceSize;
	}
	
	public void setOpForceSize(float force) {
		this.opForceSize = force;
	}
	
	public Vector<Integer> getCommanders(){
		return commanders;
	}
	
	public boolean isCommander(int id){
		
		if ( commanders.contains(id) )
			return true;
		
		return false;
	}
	
	public void removeCommander(int id){
		commanders.removeElement(id);
		commanders.trimToSize();
	}
	
	public void addCommander(int id){
		if ( isCommander(id) )
			return;
		commanders.add(id);
		commanders.trimToSize();
	}
	
}
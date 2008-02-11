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
 * Created:       03/25/05
 * Last refactor: 01/12/06
 */
package server.campaign;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import server.MWServ;
import server.campaign.pilot.SPilot;

import common.Unit;
import common.campaign.pilot.Pilot;


/**
 * @author Torren (Jason Tighe)
 *
 * Server-side holder of Personal Pilot Queue information. The queue
 * is a collection of pilots, managed by a player, which may be moved
 * between eligible units (restricted by type and weightclass). 
 */
@SuppressWarnings({"serial"})
public class SPersonalPilotQueues {
	
	//VARIABLES
	/*
	 * In the past, we've stored pilots in a master vector of types. This worked
	 * well; however, it forced a unit-type catch and transition every time a Proto
	 * pilot was sent to the queue because protos were at get(1) but have a type
	 * constant of 3 (Unit.PROTOMEK).
	 * 
	 * Instead of constantly changing the type ID's being passed in, we'll just
	 * use seperate list-holding vectors.
	 */
	private Vector<LinkedList<Pilot>> mekPilots = new Vector<LinkedList<Pilot>>(4,1);
    private Vector<LinkedList<Pilot>> protoPilots = new Vector<LinkedList<Pilot>>(4,1);
	private int playerID = 0;
	
    //CONSTRUCTOR
    /**
     * Simple no-paramater constructor that creates the list-holding
     * vectors and populates the weightclasses. LIGHTONLY values for
     * infantry and vehicles are not checked, and Lists are created
     * for all types/weightclasses. This ensures that a null is never
     * returned by a getPilotQueue() call.
     */
	public SPersonalPilotQueues() {

		for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {// for (0 - 3)
			mekPilots.add(i , new LinkedList<Pilot>());
			protoPilots.add(i, new LinkedList<Pilot>());
		}
		
	}
	
	//METHODS
	/**
     * Rather than if/else'ing meks and protos throughout
     * the other methods of the class, use a private get
     * method which returns mek or proto as needed and then
     * work on the vector without regard to type.
     */
    private Vector<LinkedList<Pilot>> getUnitTypeQueue(int typeToGet) {
    	 
    		if (typeToGet == Unit.PROTOMEK)
    			return protoPilots;
    		//else
    		return mekPilots;
    }

    /**
     * Used if the pilot type has not been set for the pilot yet.
     * @param p
     * @param type
     * @param weightClass
     */
    public void addPilot(Pilot p, int type, int weightClass) {
        p.setUnitType(type);
        addPilot(p,weightClass);
    }
    public void setOwnerID(int ID) {
    	this.playerID = ID;
    }
    /**
     * Add a pilot to the queue. Many different events can trigger
     * an addition, including game resolution, sale via market, the
     * hiring/purchase of a new pilot, and more.
     * 
     * The type of unit that the pilot may use is embedded within
     * the Pilot/SPilot that is passed as a param; however, the
     * weight class is not and must be set here.
     *
     * @param p      - the actual pilot to add.
     * @param weight - weightclass of unit the pilot may use
     */
	public void addPilot(Pilot p, int weightClass) {
		
		/*
		 * On the off chance a VACANT pilot is somehow added to
		 * the player's queue, kill it off.
		 */
	    if (p.getName().trim().equalsIgnoreCase("Vacant")){
	        p = null;//some how a bad pilot go through the checks.
	        return;
	    }

	    //add the pilot to the correct weightclass list.
	    this.getUnitTypeQueue(p.getUnitType()).get(weightClass).addLast(p);
	    
	    if(CampaignMain.cm.isUsingMySQL()) {
	    	int type = p.getUnitType();
    		((SPilot)p).toDB(type, weightClass);
	    		
	    	CampaignMain.cm.MySQL.linkPilotToPlayer(((SPilot)p).getDBId(), this.playerID);
	    }
	}
	
	/**
	 * Check the specified queue's size and send a warning to the named player if
	 * they've exceeded the queue cap. Have to do this in a stand alone method b/c
	 * the PPQ has no knowledge of it's owning player and must be sent a name.
	 */
	public void checkQueueAndWarn(String playerName, int unitType, int weightClass) {
		int size = this.getPilotQueue(unitType, weightClass).size();
		if (size > CampaignMain.cm.getIntegerConfig("MaxAllowedPilotsInQueueToBuyFromHouse"))
			CampaignMain.cm.toUser("WARNING: You have more "+ Unit.getWeightClassDesc(weightClass)+" "+ Unit.getDescriptionForID(unitType) + " pilots than allowed. HQ will randomly reassign some of them, if you do not.", playerName);
	}
	
	/**
	 * Remove a given pilot from the player's personal queue (as
	 * defined by type, weight and position in the LList) and return
	 * him to the calling class.
	 */
	public Pilot getPilot(int unitType,int weightClass, int position) {
		try {
			LinkedList<Pilot> list = this.getUnitTypeQueue(unitType).get(weightClass);
			return list.remove(position);
		}catch (Exception ex) {
			return null;
		}
	}
	
	/**
	 * Return the complete pilot list for a given unitType/weightClass.
	 */
	public LinkedList<Pilot> getPilotQueue(int unitType, int weightClass) {
		return this.getUnitTypeQueue(unitType).get(weightClass);
	}
	
	/**
	 * Obliterate all queued pilots. Whatever calls this
	 * should send a PL|PPQ to the player.
	 */
	public void flushQueue() {
		mekPilots.clear();
		protoPilots.clear();
		for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {// for (0 - 3)
			mekPilots.add(i , new LinkedList<Pilot>());
			protoPilots.add(i, new LinkedList<Pilot>());
		}
	}

	/**
	 * Convert the pilot queue information into a data string. Although
	 * toClient doesn't change this string-out directly, it is needed
	 * by SPilot's toString equivalent (SPilot.toFileFormat()).
	 * 
	 * WARNING: This format MAY NOT BE CHANGED. Any restructuring
	 *          of this data would break servers' player saves.
	 *       
	 * @return - a data string.
	 */
	public String toString(boolean toClient) {
		
	    StringBuilder result = new StringBuilder();
	    
	    //meks first
	    for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
	    		LinkedList<Pilot> currList = this.getPilotQueue(Unit.MEK, weightClass);
	    		result.append(currList.size());
	    		result.append("$");
	    		for (int position = 0; position < currList.size(); position++) {	
	    			result.append(((SPilot)currList.get(position)).toFileFormat("#",toClient));
	    			result.append("$");
	    		}
	    }
	    
	    //protos second
	    for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
	    		LinkedList<Pilot> currList = this.getPilotQueue(Unit.PROTOMEK, weightClass);
	    		result.append(currList.size());
	    		result.append("$");
	    		for (int position = 0; position < currList.size(); position++) {	
	    			result.append(((SPilot)currList.get(position)).toFileFormat("#",toClient));
	    			result.append("$");
	    		}
	    }
	    
	    return result.toString();
	    
	    /*
	     * OLD SAVE STYLE PRESERVED FOR OUTPUT FORMATTING REFERENCE
	     */
	    /*
	    for (int type = 0; type <= ppProto; type++){
	        for ( int weight = 0; weight <= SUnit.ASSAULT; weight++){
	            LinkedList list = getPilotQueue(type,weight);
	            result.append(list.size());
	            result.append("$");
	            for ( int count = 0; count < list.size(); count++ ){
	                result.append(((SPilot)list.get(count)).toFileFormat("#",toClient));
	                result.append("$");
	            }
	        }
	    }
	    return result.toString();
	    */
	}
	
	/**
	 * Read pilot queue data in from a saved string.
	 * 
	 * @param buffer
	 * @param delimiter
	 */
	
	public void fromDB(int playerID) {
		int capSize = CampaignMain.cm.getIntegerConfig("MaxAllowedPilotsInQueueToBuyFromHouse");
		ResultSet rs;
		Statement stmt;
		int currentCount=0;
		
		try {
			stmt = CampaignMain.cm.MySQL.getStatement();
			rs = stmt.executeQuery("SELECT pilotID, pilotType, pilotSize from pilots WHERE playerID = " + playerID);
			while(rs.next()) {
				SPilot p = CampaignMain.cm.MySQL.loadPilot(rs.getInt("pilotID"));
				if(capSize < 1 || currentCount < capSize) {
					this.addPilot(p, rs.getInt("pilotType"), rs.getInt("pilotSize"));
				}
				currentCount++;					
			}
			rs.close();
			stmt.close();
			for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
		        while ( this.getPilotQueue(Unit.MEK, weightClass).size() > capSize ) {
		        	this.getPilot(Unit.MEK,weightClass,CampaignMain.cm.getRandomNumber(this.getPilotQueue(Unit.MEK, weightClass).size()));
		        }
		        while ( this.getPilotQueue(Unit.PROTOMEK, weightClass).size() > capSize ) {
		        	this.getPilot(Unit.PROTOMEK,weightClass,CampaignMain.cm.getRandomNumber(this.getPilotQueue(Unit.PROTOMEK, weightClass).size()));
		        }
		    }

		}catch(SQLException e) {
			MWServ.mwlog.dbLog("SQL Error in SPersonalPilotQueues.fromDB: " + e.getMessage());
		}
		
	}
	public void fromString(String buffer, String delimiter) {
		
		StringTokenizer mainTokenizer = new StringTokenizer(buffer,delimiter);
		int capSize = CampaignMain.cm.getIntegerConfig("MaxAllowedPilotsInQueueToBuyFromHouse");
        
		//loop once to read in meks (light -> assault lists)
		for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
	        int listSize = Integer.parseInt(mainTokenizer.nextToken());
	        for(int count = 0 ; count < listSize; count++){
	            SPilot filePilot = new SPilot();
	            filePilot.fromFileFormat(mainTokenizer.nextToken(),"#");
                this.addPilot(filePilot,Unit.MEK, weightClass);
	        }
	        while ( this.getPilotQueue(Unit.MEK, weightClass).size() > capSize ) {
	        	this.getPilot(Unit.MEK,weightClass,CampaignMain.cm.getRandomNumber(this.getPilotQueue(Unit.MEK, weightClass).size()));
	        }
	    }
		
		//a second loop will read in protos (light -> assault lists)
		for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
	        int listSize = Integer.parseInt(mainTokenizer.nextToken());
	        for(int count = 0 ; count < listSize; count++){
	            SPilot filePilot = new SPilot();
	            filePilot.fromFileFormat(mainTokenizer.nextToken(),"#");
                this.addPilot(filePilot,Unit.PROTOMEK, weightClass);
	        }
	        while ( this.getPilotQueue(Unit.PROTOMEK, weightClass).size() > capSize ) {
	        	this.getPilot(Unit.PROTOMEK,weightClass,CampaignMain.cm.getRandomNumber(this.getPilotQueue(Unit.PROTOMEK, weightClass).size()));
	        }
	    }
		
		/*
		 * OLD PASRING PRESERVED FOR REFERENCE
		 */	
		/*
		StringTokenizer ST = new StringTokenizer(buffer,delimiter);
		for (int type = 0; type <= ppProto; type++ ){
		    for ( int weight = 0; weight <= SUnit.ASSAULT; weight++ ){
		        int size = Integer.parseInt(ST.nextToken());
		        for( int count = 0 ; count < size; count++ ){
		            SPilot pilot = new SPilot();
		            pilot.fromFileFormat(ST.nextToken(),"#");
		            this.addPilot(type,weight,pilot);
		        }
		    }
		}
		*/
	}
	
}//end SPersonalPilotQueues.java
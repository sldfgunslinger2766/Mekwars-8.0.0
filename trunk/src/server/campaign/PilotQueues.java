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
 * Created on 25.04.2004
 *
 */
package server.campaign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import common.Unit;
import common.campaign.pilot.skills.PilotSkill;

import server.MMServ;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.skills.SPilotSkill;
import server.campaign.pilot.skills.TraitSkill;

/**
 * @author Helge Richter
 */
@SuppressWarnings({"unchecked","serial"})
public class PilotQueues {
	
	private Vector<LinkedList<SPilot>> queues = new Vector<LinkedList<SPilot>>();
	private Vector<Integer> baseGunnery = new Vector<Integer>(Unit.MAXBUILD);
	private Vector<Integer> basePiloting = new Vector<Integer>(Unit.MAXBUILD);
	private Vector<String>  basePilotSkills = new Vector<String>(Unit.MAXBUILD);
	private String factionString = "";//string for faction specific name list
	private int factionID;
	
	public PilotQueues(Vector<Integer>baseGunnery, Vector<Integer>basePiloting, Vector<String>basePilotSkill) {
		for (int i = Unit.MEK; i < Unit.MAXBUILD; i++) {
			LinkedList<SPilot> v = new LinkedList<SPilot>();
			queues.add(i,v);
		}
		this.baseGunnery = baseGunnery;
		this.basePiloting = basePiloting;
		this.basePilotSkills = basePilotSkill;
		this.factionString = "";
	}
	
	public PilotQueues() {
		//Only needed for XStream
	}
	
	/**
	 * 
	 * @param type int type of pilot (mek, veh, etc.)
	 * @param p pilot to add
	 * @param skipSkillChange is false, call normal add. if true, bypass it.
	 * 
	 * This particular add mechanism should only be called from *within*
	 * the PilotQueues class or when captureing a pilot. It is used to add
	 * newly generated pilots and captured enemy pilot to the queue, with
	 * their level ups (server gen'ed 3/5 and 4/4 skills, and other oddball
	 * pilots with the WizWom option true) intact.
	 */
	public void addPilot(int type, SPilot p, boolean skipSkillChange) {
		
		if (p.getName().equalsIgnoreCase("Vacant")) {
			p = null;//nuke him
			return;
		}
		
		p.setCurrentFaction(factionString);
	    if ( p.getSkills().has(PilotSkill.WeaponSpecialistSkillID)
	            && !p.getWeapon().equals("Default")){
	        
	        Iterator ski = p.getSkills().getSkillIterator();
	        while ( ski.hasNext() ){
	            SPilotSkill skill = (SPilotSkill)ski.next();
	            if ( skill.getName().equals("Weapon Specialist")){
	                p.getSkills().remove(skill);
	                break;
	            }
	        }
	    }
	    
		if (!skipSkillChange) {
			this.addPilot(type, p);
		}
		else {//skip the skill adjustmebnt
			queues.get(type).addLast(p);
			if(CampaignMain.cm.isUsingMySQL()) {
				if (!p.getName().equalsIgnoreCase("Vacant")){
						CampaignMain.cm.MySQL.savePilot(p, type, -1);
						CampaignMain.cm.MySQL.linkPilotToFaction(p.getDBId(), factionID);
				}
			}
			
		}//end else(bypass the adjustmenbt)
	}
	
	/**
	 * @param type  - int indicating which que to add to (inf, veh, etc.)
	 * @param p 	- SPilot to add to the que
	 * 
	 * Check config to see if pilots should always retain their skills. If not,
	 * check for an elite pilot and downshift his abilities. Green units should
	 * not be improved; however, a 4/5 pilot will be set in place of a pilot
	 * with 5 gunnery and improved piloting (no 5/2, 5/3). Also check config to
	 * see if pilot XP should have a percentage chance to be cleared w/ skills.
	 * 
	 * @urgru 6/07/04
	 */
	public void addPilot(int type, SPilot p) {
		
		if (p.getName().equalsIgnoreCase("Vacant")) {
			p = null;//nuke him
			return;
		}
		
		p.setCurrentFaction(factionString);
		if ( p.getSkills().has(PilotSkill.WeaponSpecialistSkillID)
	            && !p.getWeapon().equals("Default")){
	        
	        Iterator ski = p.getSkills().getSkillIterator();
	        while ( ski.hasNext() ){
	            SPilotSkill skill = (SPilotSkill)ski.next();
	            if ( skill.getName().equals("Weapon Specialist")){
	                p.getSkills().remove(skill);
	                break;
	            }
	        }
	    }

	    if (CampaignMain.cm.getBooleanConfig("ReduceSkillsInQue")) {
			
			int rnd = CampaignMain.cm.getR().nextInt(100);
			if (rnd >= CampaignMain.cm.getIntegerConfig("ClearXPInQue")) {
				p.setExperience(0);
			}//end if(XP should be cleared)
			
			boolean gunnerAdjust = false;
			boolean pilotAdjust = false;
			if (p.getGunnery() < this.getBaseGunnery(type))
				gunnerAdjust = true;
			if (p.getPiloting() < this.getBasePiloting(type))
				pilotAdjust = true;
			
			if (!gunnerAdjust && !pilotAdjust) {
				if (p.getGunnery() > this.getBaseGunnery(type)) {
					int i = CampaignMain.cm.getR().nextInt(100);
					if (i >= 50) {
						p.setGunnery(this.getBaseGunnery(type));				
					}//end if(rnd roll decreases gunnery)
				}
				else if (p.getPiloting() > this.getBasePiloting(type)) {
					int i = CampaignMain.cm.getR().nextInt(100);
					if (i >= 50) {
						p.setPiloting(this.getBasePiloting(type));
					}//end if(rnd roll decreases piloting)
				}			
			}//end if(not elite, check for green)
			else if (gunnerAdjust && !pilotAdjust) {
				p.setGunnery(p.getGunnery() + 1);
			}//end else if (pilot is 1-3/5)
			else if (!gunnerAdjust && pilotAdjust) {
				if (p.getPiloting() < (this.getBasePiloting(type) - 1) && p.getGunnery() > this.getBaseGunnery(type)) {
					p.setGunnery(this.getBaseGunnery(type));
					p.setPiloting(this.getBasePiloting(type));
				}//end if (pilot is 5/1-3)
				else {
					p.setPiloting(p.getPiloting() + 1);	
				}//end if (pilot is 4/1-4)
			}//end else if(piloting needs to be increased)
			else if (gunnerAdjust && pilotAdjust) {
				if (p.getGunnery() + 1 == p.getPiloting()) {
					int i = CampaignMain.cm.getR().nextInt(100);
					if (i >= 50) {
						p.setPiloting(p.getPiloting() + 1);						
					}//end if(rnd roll increases piloting)
					else {//otherwise, increase guns
						p.setGunnery(p.getGunnery() + 1);
					}//end else(it increases gunnery)
				}//end if(pilot is elite and staged -- 3/4 or 2/3)
				else if (p.getGunnery() < p.getPiloting()) {
					p.setGunnery(p.getGunnery() + 1);
				}
				else {//piloting is less than or equal to gunnery
					p.setPiloting(p.getPiloting() + 1);
				}//end else(piloting less or equal)
			}//end else if(both pilot and gunner have skill improvements)
		}//end if(downgrade elites)
		
		queues.get(type).addLast(p);
		
		if(CampaignMain.cm.isUsingMySQL()) {
			if (!p.getName().equalsIgnoreCase("Vacant")){
					CampaignMain.cm.MySQL.savePilot(p, type, -1);
					CampaignMain.cm.MySQL.linkPilotToFaction(p.getDBId(), factionID);
			}
		}

	}//end void addPilot()
	
	/**
	 * @@author Torren (Jason Tighe)
	 * @param type
	 * @param p
	 * 
	 * This is called from SHouse.fromString to load pilots directly
	 * from the dat files without reprocessing them.
	 */
	public void loadPilot(int type, SPilot p) {
	    p.setCurrentFaction(factionString);
	    queues.get(type).addLast(p);
	}//end void loadPilot()
	
	public SPilot getPilot(int type) {
		LinkedList<SPilot> list = queues.get(type);
		while (list.size() < 10) 
			addPilot(type, rollNewPilot(type), true);
		
		SPilot pilot = list.remove(CampaignMain.cm.getR().nextInt(list.size()));
		
		StringTokenizer ST = new StringTokenizer(getBasePilotSkill(type),"$");
		
		while ( ST.hasMoreTokens() ) {
		    SPilotSkill pSkill = CampaignMain.cm.getPilotSkill(ST.nextToken());
		    if ( !pilot.getSkills().has(pSkill) )
			pilot.getSkills().add(pSkill);
		}
		
		return pilot;
	}
	
	private SPilot rollNewPilot(int unitType) {
		
		SPilot result;
		int gunnery = this.getBaseGunnery(unitType);
		int piloting = this.getBasePiloting(unitType);
		int skillChance = CampaignMain.cm.getIntegerConfig("BornSkillChance");
			
		int rnd = CampaignMain.cm.getR().nextInt(100);//reroll rnd, use to check for improved pilots
		boolean allowGreenPilots = CampaignMain.cm.getBooleanConfig("AllowGreenPilots");
			
		//Green Pilots
		if (rnd < 10 && allowGreenPilots) {
			if (rnd < 5)
				gunnery++;
			else if (rnd >= 5 && rnd < 10)
				piloting++;
			skillChance = 100;
		}
			
		//Improved Pilots
		if (rnd >= 90) {
			if (rnd >= 90 && rnd < 95)
				piloting--;
			else if (rnd >= 95)
				gunnery--;
		}
			 
		result = new SPilot(getRandomPilotName(),gunnery,piloting);
		result.setCurrentFaction(factionString);
		
		rnd = CampaignMain.cm.getR().nextInt(100);//reroll rnd, use to check for improved pilots
		if (rnd <= skillChance && CampaignMain.cm.getBooleanConfig("PilotSkills")) {
			
			SPilotSkill skill = CampaignMain.cm.getRandomSkill(result,unitType);
			if (skill != null) {
				if (skill instanceof TraitSkill)
					((TraitSkill)skill).assignTrait(result);
				skill.addToPilot(result);
				skill.modifyPilot(result);
			}
		}
        
        result.setPilotId(CampaignMain.cm.getAndUpdateCurrentPilotID());
		return result;
	}
	
	public int getQueueSize(int type) {
		return queues.get(type).size();
	}
	
	/**
	 * @param s faction name string
	 * 
	 * A method which should be called immedaitely after a pilot
	 * que is contructed, if faction specific name lists are enabled.
	 */
	public void setFactionString(String s) {
		factionString = s;
	}
	
	/**
	 * @return the faction string
	 */
	public String getFactionString() {
		return factionString;
	}
	
	public void setFactionBasePilotSkills(String skills) {
	    StringTokenizer ST = new StringTokenizer(skills);
	    
	    while ( ST.hasMoreTokens() ) {
		
	    }
	}
	
	/**
	 * @return int this queue's base piloting #
	 */
	public int getBasePiloting(int type) {
		return this.basePiloting.elementAt(type);
	}
	
	/**
	 * @return int this queue's base gunnery #
	 */
	public int getBaseGunnery(int type) {
		return this.baseGunnery.elementAt(type);
	}
	
	/**
	 * @param type
	 * @return base piloting skills for specific unit type.
	 */
	public String getBasePilotSkill(int type) {
	    return this.basePilotSkills.elementAt(type);
	}
	
	/*
	 * Sets BasePiloting for this queue
	 */
	
	public void setBasePiloting(int piloting,int type){
	    this.basePiloting.set(type,piloting);
	}
	
	/*
	 * Sets Base Gunnery for this queue
	 */
	
	public void setBaseGunnery(int gunnery,int type){
	    this.baseGunnery.set(type,gunnery);
	}
	
	public void setBasePilotSkill(String skills, int type) {
	    this.basePilotSkills.set(type,skills);
	}
	
	public void setFactionID(int factionID) {
		this.factionID = factionID;
	}
	/**
	 * @return a pilot name
	 * 
	 * NOTE: this is modelled on getRandomPilotName() from SUnit, but DOES NOT
	 * replace it. SUnit's naming defaults to the common list and is used for
	 * newbie faction units and for units generated with CreateMechCommand.
	 */
	public String getRandomPilotName() {
		
		String result = "Noelle";//something we hope never returns
        if (CampaignMain.cm.getBooleanConfig("UseCommonPilotNameFileOnly"))
        	return SPilot.getRandomPilotName(CampaignMain.cm.getR());
       
        try {
        	File configFile = new File("./data/pilotnames/" + factionString + "Pilotnames.txt");
        	FileInputStream fis = new FileInputStream(configFile);
        	BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
        	int names = Integer.parseInt(dis.readLine());
        	int pilotid = CampaignMain.cm.getR().nextInt(names);
        	while (dis.ready()) {
        		String line = dis.readLine();
        		if (pilotid <= 0)
        			return line;
        		//else
        		pilotid--;
        	}

        	dis.close();
        	fis.close();

        } catch (Exception e) {
        	MMServ.mmlog.errLog("A problem occured while retreiving a name from the " + factionString + " Pilotnames File! Tried using Pilotnames.txt instead.");
        	result = SPilot.getRandomPilotName(CampaignMain.cm.getR());
        }
        
	    return result;
	  }//end getRandomPilotName
	
	public LinkedList<SPilot> getPilotQueue(int type) {
		LinkedList<SPilot> list = queues.get(type);
		return list;
	}

	/**
	 * Obliterate all queued pilots.
	 */
	public void flushQueue() {
            queues.clear();
            for (int i = Unit.MEK; i < Unit.MAXBUILD; i++) {
            	LinkedList<SPilot> v = new LinkedList<SPilot>();
            	queues.add(i,v);
            }
	}

}

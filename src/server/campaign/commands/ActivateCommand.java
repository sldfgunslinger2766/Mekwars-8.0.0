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

package server.campaign.commands;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import common.Unit;
import common.util.UnitUtils;
import server.MMServ;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SArmy;
import server.campaign.SUnit;

import megamek.common.Mech;

public class ActivateCommand implements Command {
	
	int accessLevel = 0;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (new Boolean(CampaignMain.cm.getConfig("CampaignLock")).booleanValue() == true) {
			CampaignMain.cm.toUser("The campaign is currently locked. Player activation is disabled until the campaign is unlocked. NOTE: Running games will resolve normally.",Username,true);
			return;
		}
		
		//Put it into a try block. One user was causing FormatExceptions
		try{
			if (!(MMServ.SERVER_VERSION).substring(0,server.MMServ.SERVER_VERSION.lastIndexOf(".")).equals(p.getPlayerClientVersion().substring(0,p.getPlayerClientVersion().lastIndexOf("."))) ) {
				//server.MMServ.mmlog.modLog(Username + " failed to activate. Was using version " + p.getPlayerClientVersion()+" Server Version: "+ MMServ.SERVER_VERSION);
				CampaignMain.cm.doSendModMail("NOTE",Username + " failed to activate. Was using version " + p.getPlayerClientVersion()+" Server Version: "+ MMServ.SERVER_VERSION);
				CampaignMain.cm.toUser("You may not go active with an incompatible client version! Please switch to version " + MMServ.SERVER_VERSION +"!",Username,true);
				return;
			}
		} catch (Exception ex) {
			MMServ.mmlog.errLog("Error activating player. User reported client verson: " + p.getPlayerClientVersion() + " --- Stack Trace Follows.");	   
			//MMServ.mmlog.errLog(ex);
			CampaignMain.cm.toUser("Your clients version was not reported to the server. <a href=\"MEKWARS/c setclientversion#"+ Username+ "#" + MMServ.SERVER_VERSION + "\">Click here to update the server.</a> then try to activate again.", Username);
			return;
		}
		
		int currentStatus = p.getDutyStatus();
		if (currentStatus == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("You are already fighting!",Username,true);
			return;
		}
		
		if (currentStatus == SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("You are already on active duty!",Username,true);
			return;
		}
		
		//this should never come up, but better safe than sorry
		if (currentStatus == SPlayer.STATUS_LOGGEDOUT) {
			CampaignMain.cm.toUser("You are logged out and may not activate.",Username,true);
			return;
		}
		
		//if player has no armies, break out
		if (p.getArmies().size() == 0){
			CampaignMain.cm.toUser("You must have armies constructed in order to activate.",Username,true);
			return;
		}

		//check for empty armies, pilotless units
		for (SArmy currA : p.getArmies()) {
			
			if (currA.getAmountOfUnits() == 0) {
				CampaignMain.cm.toUser("You may not activate with empty armies.",Username,true);
				return;
			}
			
			int protoCount = 0;
			for (Unit currU : currA.getUnits()) {
				
				if (currU.hasVacantPilot()) {
					CampaignMain.cm.toUser("You may not activate with pilotless units!",Username,true);
					return;
				}
				
				if (currU.getType() == Unit.PROTOMEK)
					protoCount++;
			}
			
			if (protoCount % 5 != 0) {
				CampaignMain.cm.toUser("Armies must contain full points of ProtoMeks in order to activate.",Username,true);
				return;
			}
				
		}
		
		
		//AR-only activation checks
		if (CampaignMain.cm.isUsingAdvanceRepair()) {
			
			if (this.armiesContainEnginedUnit(p.getArmies())){
				CampaignMain.cm.toUser("You may not activate with an engine-disabled unit in an army.",Username,true);
				return;
			}
			
			if (this.armiesContainLeggedUnit(p.getArmies())){
				CampaignMain.cm.toUser("You may not activate with a legless unit in an army.",Username,true);
				return;
			}
			
			if (p.hasRepairingUnits()){
				CampaignMain.cm.toUser("You may not activate while units in your armies are undergoing repairs.",Username,true);
				return;
			}
			
            if ( CampaignMain.cm.getBooleanConfig("UseSimpleRepair") && this.armiesDamagedUnits(p.getArmies())){
                CampaignMain.cm.toUser("You may not activate while units in your armies have damage.",Username,true);
                return;
            }
            
            if ( !CampaignMain.cm.getBooleanConfig("AllowUnitsToActivateWithPartialBins") && this.armiesPartialAmmoBinUnits(p.getArmies())){
                CampaignMain.cm.toUser("You may not activate while units in your armies have damage.",Username,true);
                return;
            }

		}
		
		/*
		 * We can now be sure that the player sending the command is not
		 * active. Check him for unmaintained units and bar him from the
		 * front if an unmaint is in an army.
		 * 
		 * Might be cleaner to put this in Army as hasUnmaintainedUnits()
		 * and call as needed, but this should be the only place this check
		 * is currently done.
		 */
		for (SArmy currA : p.getArmies()) {
			for (Unit currUnit : currA.getUnits()) {
				if (currUnit.getStatus() == Unit.STATUS_UNMAINTAINED) {
					CampaignMain.cm.toUser("You may not send armies containing unmaintained units to the front lines!",Username,true);
					return;
				}
			}
		}
		
		if (p.getFreeBays() < 0){
			if (CampaignMain.cm.isUsingAdvanceRepair())
				CampaignMain.cm.toUser("You may not activate with negative bays!",Username,true);
			else
				CampaignMain.cm.toUser("You may not activate with negative techs!",Username,true);
			return;
		}
		
		p.setActive(true);
		
		CampaignMain.cm.toUser("<font color=\"black\">[!] You're on your way to the front lines.</font>",Username,true);
		CampaignMain.cm.sendPlayerStatusUpdate(p,!new Boolean(CampaignMain.cm.getConfig("HideActiveStatus")).booleanValue());
		
		//set up a thread which will do an auto /c ca once the minactivetime expires
		int threadLenth = CampaignMain.cm.getIntegerConfig("MinActiveTime") * 1000;
		CheckAttackThread caThread = new CheckAttackThread(p, threadLenth);
		caThread.start();
		
	}//end process()
	
	/**
	 * Method which returns a boolean indicating whether
	 * any unit in a set of armies has 3 engine crits.
	 * 
	 * Separate method, instead of inline in process(), so
	 * this can be moved into SPlayer easily if other code
	 * needs to do engine checks.
	 */
	private boolean armiesContainEnginedUnit(Vector<SArmy> armies) {
		
		//loop though all units in all armies
		for (SArmy army : armies) {
			Iterator units = army.getUnits().iterator();
			while (units.hasNext()) {
				SUnit unit = (SUnit) units.next();
				if (!UnitUtils.canStartUp(unit.getEntity()))
					return true;
			}
		}
		
		//no 3-hit units found. return false.
		return false;
	}
	
	/**
	 * Method which returns a boolean indicating whether any
	 * unit in a set of armies is missing a leg.
	 * 
	 * Separate method, instead of inline in process(), so
	 * this can be moved into SPlayer easily if other code
	 * needs to do leglessness (sp?) checks.
	 */
	private boolean armiesContainLeggedUnit(Vector<SArmy> armies) {
		
		//loop though all units in all armies
		for (SArmy army : armies) {
			Iterator units = army.getUnits().iterator();
			while (units.hasNext()) {
				
				//check RL/LL on a standard mek
				SUnit unit = (SUnit) units.next();
				if (unit.getType() == Unit.MEK) {
					if (unit.getEntity().getInternal(Mech.LOC_LLEG) <= 0)
						return true;
					if (unit.getEntity().getInternal(Mech.LOC_RLEG) <= 0)
						return true;
				}
				
				//check all 4 legs on a quad
				else if (unit.getType() == Unit.QUAD) {
					if (unit.getEntity().getInternal(Mech.LOC_LLEG) <= 0)
						return true;
					if (unit.getEntity().getInternal(Mech.LOC_RLEG) <= 0)
						return true;
					if (unit.getEntity().getInternal(Mech.LOC_LARM) <= 0)
						return true;
					if (unit.getEntity().getInternal(Mech.LOC_RARM) <= 0)
						return true;
				}
			}
		}
		
		//no 3-hit units found. return false.
		return false;
	}
	
    /**
     * Method which returns a boolean indicating whether any
     * unit in a set of armies has any damage.
     * 
     * Separate method, instead of inline in process(), so
     * this can be moved into SPlayer easily if other code
     * needs to do damaged unit checks.
     */
    private boolean armiesDamagedUnits(Vector<SArmy> armies) {
        
        //loop though all units in all armies
        for (SArmy army : armies) {
            Iterator units = army.getUnits().iterator();
            while (units.hasNext()) {
                
                //check RL/LL on a standard mek
                SUnit unit = (SUnit) units.next();
                if ( UnitUtils.hasArmorDamage(unit.getEntity()) )
                    return true;
                if ( UnitUtils.hasCriticalDamage(unit.getEntity()))
                    return true;
            }
        }
        
        //no 3-hit units found. return false.
        return false;
    }
    
    /**
     * This checks all the units in the army to see if they have partial ammo bins.
     * 
     */
    private boolean armiesPartialAmmoBinUnits(Vector<SArmy> armies) {
        
        //loop though all units in all armies
        for (SArmy army : armies) {
            Iterator units = army.getUnits().iterator();
            while (units.hasNext()) {
                
                SUnit unit = (SUnit) units.next();
                if ( !UnitUtils.hasAllAmmo(unit.getEntity()) )
                    return true;
            }
        }
        
        //no units with partial ammobins found. return false.
        return false;
    }
    
}//end activatecommand class

/**
 * @author urgru
 *
 * private thread. simple minactivetime wait
 * which runs a checkattack for the activating
 * player.
 */
class CheckAttackThread extends Thread {
	
	//vars
	SPlayer p;
	long duration;
	
	public CheckAttackThread(SPlayer p, int duration) {
		super("ActivationThread-" + p.getName());
		this.duration = duration; //set length when thread is spun
		this.p = p;
	}
	
	@Override
	public synchronized void run() {
		try {
			wait(duration+250);//buffer by a quarter second 
			
			//make sure the player is still active (hasnt logged out,
			//been forcedeactivated, attacked or joined a game).
			if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
				CheckAttackCommand ca = new CheckAttackCommand();
				CampaignMain.cm.toUser("<br><font color=\"black\">You have arrived on the front lines!</font>",p.getName(),true);
				ca.process(new StringTokenizer(""), p.getName());    
			}
			
			//ran once. kill the thread by returning.
			return;
		} catch (Exception ex) {
			MMServ.mmlog.errLog(ex);
		}
	}//end run()
}//end CheckAttackThread


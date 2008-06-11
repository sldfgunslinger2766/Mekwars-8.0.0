/*
 * MekWars - Copyright (C) 2006 
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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.Unit;

import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;
import server.MWChatServer.auth.IAuthenticator;

//Syntax sethousebasepilotingskills house#pilotType#Skill$Skill
public class SetHouseBasePilotingSkillsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#pilotType#PilotingSkill$PilotingSkill";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}

            SHouse house;
            int pilotType;
            String skills = "";
            
            try{
                house = CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
                pilotType = Integer.parseInt(command.nextToken());
                skills = command.nextToken()+"$";
            }catch (Exception ex ){
                CampaignMain.cm.toUser("Invalid Syntax: sethousebasepilotskills house#pilotType#PilotingSkill$PilotingSkill", Username);
                CampaignMain.cm.toUser("Invalid unit type:<br>Mek "+Unit.MEK+"<br>Vehicle "+Unit.VEHICLE+"<br>Infantry "+Unit.INFANTRY+"<br>Battle Armor "+Unit.BATTLEARMOR+"<br>ProtoMek "+Unit.PROTOMEK+"<br>Aero "+Unit.AERO, Username);
                return;
            }
    
            if ( house == null )
                return;
            
            if ( pilotType >= Unit.MAXBUILD || pilotType < 0){
                CampaignMain.cm.toUser("Invalid unit type:<br>Mek "+Unit.MEK+"<br>Vehicle "+Unit.VEHICLE+"<br>Infantry "+Unit.INFANTRY+"<br>Battle Armor "+Unit.BATTLEARMOR+"<br>ProtoMek "+Unit.PROTOMEK+"<br>Aero "+Unit.AERO, Username);
                return;
            }

            house.getPilotQueues().setBasePilotSkill(skills, pilotType);
            
            house.updated();
            //log, and inform mods.
            CampaignMain.cm.toUser("You added a piloting skill for unit "+Unit.getTypeClassDesc(pilotType)+" for house "+house.getName()+" to "+skills,Username);
            CampaignMain.cm.doSendModMail("NOTE",Username + " has added a piloting skill for unit "+Unit.getTypeClassDesc(pilotType)+" for house "+house.getName()+" to "+skills+ ".");
		
	}//end process
}
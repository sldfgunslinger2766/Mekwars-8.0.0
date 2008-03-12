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

package server.campaign.commands.mod;

import java.util.LinkedList;
import java.util.StringTokenizer;

import common.Unit;
import common.campaign.pilot.Pilot;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;
import server.MWChatServer.auth.IAuthenticator;

/**
 * Return a human readable string that describes the pilots
 * currently in a player's personal queues.
 */
public class ViewPlayerPersonalPilotQueueCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Player name";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		//get the player you wish to use
        
        if ( !command.hasMoreTokens() ){
            CampaignMain.cm.toUser("Syntax: ViewPlayerPersonalPilotQueue#Name#",Username);
            return;
        }
		SPlayer p = CampaignMain.cm.getPlayer(command.nextToken());
		
		/*
		 * @urgru 1/12/06 - factored string generation out of SPeronalPilotQueue
		 * and into the command class, which was the only class to call the old
		 * SPPQ.listDisposessedPilot() method.
		 */
		boolean hasQueuedPilots = false;
		
		//set up a string buffer to contain the return info
		StringBuilder toReturn = new StringBuilder();
		
        //process MEK pilots first
		for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
			
			LinkedList<Pilot> currList = p.getPersonalPilotQueue().getPilotQueue(Unit.MEK,weightClass);
			if (currList.size() != 0) {
				
				//we have a pilot, so set the bool
				hasQueuedPilots = true;
				
				//add the weight class descrpition to table
				toReturn.append(Unit.getWeightClassDesc(weightClass)+":<UL>");
				
				//add all pilots in the list to the table
				for (int i = 0; i < currList.size(); i++) {
					
					//mek, so show gunnery AND piloting
					Pilot currPil = currList.get(i);
					toReturn.append("<LI>#"+currPil.getPilotId() + " "
							+ currPil.getName() +" (" + currPil.getGunnery() + "/"
							+ currPil.getPiloting());
					
					//append skill descriptions
					String skills = currPil.getSkillString(true);
					if (skills != null && skills.trim().length() != 0) {
						toReturn.append(", ");
						toReturn.append(skills);
					}
					
					//close the description block.
					toReturn.append(")");
					
					//show hits, if tracking and > 0
					if (currPil.getHits() > 0)
						toReturn.append(" Hits: "+ currPil.getHits());
					
					toReturn.append("</LI>");
				}
				
				//close the weight class table block
				toReturn.append("</UL>");
			}
			
		}
		
        if ( hasQueuedPilots )
            toReturn.insert(0,"<font color=black><u>Mek Pilots</u>:<br>");
        
		//now process the PROTOMEK pilots
        
        StringBuilder toReturnProtos = new StringBuilder();
		for (int weightClass = Unit.LIGHT; weightClass <= Unit.ASSAULT; weightClass++) {
			
			LinkedList<Pilot> currList = p.getPersonalPilotQueue().getPilotQueue(Unit.PROTOMEK,weightClass);
			if (currList.size() != 0) {
				
				//we have a pilot, so set the bool
				hasQueuedPilots = true;
				
				//add the weight class descrpition to table
                toReturnProtos.append(Unit.getWeightClassDesc(weightClass)+":<UL>");
				
				//add all pilots in the list to the table
				for (int i = 0; i < currList.size(); i++) {
					
					//protomek, so we only show the gunnery skill
					Pilot currPil = currList.get(i);
                    toReturnProtos.append("<LI>#"+currPil.getPilotId() + " "
							+ currPil.getName() +" (" + currPil.getGunnery());
					
					//append skill descriptions
					String skills = currPil.getSkillString(true);
					if (skills != null && skills.trim().length() != 0) {
                        toReturnProtos.append(", ");
                        toReturnProtos.append(skills);
					}
					
					//close the description block.
                    toReturnProtos.append(")");
					
					//show hits, if tracking and > 0
					if (currPil.getHits() > 0)
                        toReturnProtos.append(" Hits: "+ currPil.getHits());
					
                    toReturnProtos.append("</LI>");
				}
				
				//close the weight class table block
                toReturnProtos.append("</UL>");
			}
			
		}
		
        if ( toReturnProtos.length() > 1){
            toReturnProtos.insert(0,"<font color=black><u>ProtoMek Pilots</u>:<br>");
            toReturn.append(toReturnProtos);
        }

        toReturn.append("</font>");
		
		if (hasQueuedPilots)
			CampaignMain.cm.toUser("SM|"+toReturn.toString(),Username,false);
		else
			CampaignMain.cm.toUser("SM|"+p.getName()+" doesn't have any reserve pilots at the moment.",Username,false);
		
        CampaignMain.cm.doSendModMail("NOTE",Username+" has viewed "+p.getName()+"'s pilot queue");
	}
}
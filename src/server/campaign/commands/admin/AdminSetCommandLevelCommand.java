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

package server.campaign.commands.admin;

import java.util.Hashtable;
import java.util.StringTokenizer;
import server.campaign.commands.Command;
import server.campaign.CampaignMain;
import server.MWChatServer.auth.IAuthenticator;


public class AdminSetCommandLevelCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		
		Hashtable commandTable = CampaignMain.cm.getServerCommands();
		
		String commandName = command.nextToken().toUpperCase();
		int commandLevel = Integer.parseInt(command.nextToken());
		
		if ( commandTable.containsKey(commandName) )
		    ((Command)commandTable.get(commandName)).setExecutionLevel(commandLevel);
		else{
		    CampaignMain.cm.toUser("Command "+commandName+" not found!",Username,true);
		    return;
		}

		CampaignMain.cm.toUser("Command level changed on "+commandName.toLowerCase()+" to "+commandLevel,Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has changed the command level for "+commandName.toLowerCase()+" to "+commandLevel);
		
	}
}
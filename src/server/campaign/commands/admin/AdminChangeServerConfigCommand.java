/*
 * MekWars - Copyright (C) 2004 
 * 
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

/**
 * @author jtighe
 * This Command is used bye server admins to change config items on the fly
 * while the server is still running.
 * 
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;
import server.campaign.util.scheduler.MWScheduler;

public class AdminChangeServerConfigCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "config#arg";
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
		
		//get config var and new setting
		String config = command.nextToken();
		String arg = command.nextToken();
		
		//make setting change
		CampaignMain.cm.getConfig().setProperty(config,arg);
		
		// Check for Schedule changes here
		if (config.equalsIgnoreCase("Christmas_StartDate")) {
			
		} else if (config.equalsIgnoreCase("Christmas_EndDate")) {
			
		} else if (config.equalsIgnoreCase("Scheduler_FactionSave")) {
			
		} else if (config.equalsIgnoreCase("Scheduler_PlayerActivity_flu")) {
			MWScheduler.getInstance().rescheduleAllActivePlayers();
		} else if (config.equalsIgnoreCase("Scheduler_PlayerActivity_comps")) {
			MWScheduler.getInstance().rescheduleAllActivePlayers();
		}
		
		//NOTE:
		//NO MODMAIL for setting changes. Server Config GUI would spam too much.
		
	}//end process
}
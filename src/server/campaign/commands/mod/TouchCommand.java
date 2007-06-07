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

import java.util.StringTokenizer;
import server.campaign.commands.Command;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.MWChatServer.auth.IAuthenticator;


public class TouchCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		String player = command.nextToken();
		SPlayer p = CampaignMain.cm.getPlayer(player);
		if (p.getDutyStatus() != SPlayer.STATUS_LOGGEDOUT) {
			CampaignMain.cm.toUser(p.getName()+" is already on-line and doesn't need a pfile update.", Username);
			return;
		}
		
		p.setLastOnline(System.currentTimeMillis());
		p.setSave(true);
		
		CampaignMain.cm.toUser("You touched " + p.getName() + ".",Username,true);
		//server.MMServ.mmlog.modLog(Username + " touched " + p.getName() + ".");
		CampaignMain.cm.doSendModMail("NOTE",Username + " touched " + p.getName() + ".");
		
	}
}
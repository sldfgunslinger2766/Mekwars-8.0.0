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

import java.util.StringTokenizer;
import java.util.Enumeration;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SArmy;


public class CheckArmyLinkCommand implements Command {
	
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
		
		SPlayer p = null;
		int armyid = -1;
		SArmy army = null;
		
		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
			armyid = Integer.parseInt(command.nextToken());
			army = p.getArmy(armyid);
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c checkarmylink#name#armyid",Username,true);
			return;
		}
		
		if (p.getDutyStatus() != SPlayer.STATUS_FIGHTING && CampaignMain.cm.getServer().getUserLevel(Username) < IAuthenticator.ADMIN) {
			CampaignMain.cm.toUser("You may only check links in fighting players' amries.",Username,true);
			return;
		}
		
		StringBuilder toSend = new StringBuilder("Link info for " + p.getName() + "'s Army #" + armyid + ":");
		if (army.getC3Network().size() < 1) {
			toSend.append(" No Linked C3.");
			CampaignMain.cm.toUser(toSend.toString(),Username,true);
		} else {
			Enumeration c3Units = army.getC3Network().keys();
			while (c3Units.hasMoreElements()){
				Integer c3U = (Integer)c3Units.nextElement();
				Integer c3M = army.getC3Network().get(c3U);
				toSend.append("<br>Unit " + c3U + " is linked to unit " + c3M + ".");
			}
			CampaignMain.cm.toUser(toSend.toString(),Username,true);
		}
	}
}
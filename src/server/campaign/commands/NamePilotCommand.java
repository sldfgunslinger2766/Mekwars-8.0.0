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

import common.util.StringUtils;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;


public class NamePilotCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
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
		
		int unitid = -1;
		String name = "";
		
		try {
			unitid = Integer.parseInt((String)command.nextElement());
			name = (String)command.nextElement();
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper syntx. Try: /c namepilot#unitid#newname",Username,true);
			return;
		}
		
		//check validity of name
		if (name.length() > 30)
			name = name.substring(0,30);
		
		if (StringUtils.hasBadChars(name, true).trim().length() > 0 ) {
			CampaignMain.cm.toUser(StringUtils.hasBadChars(name, true).trim(),Username);
			return;
		} 
		
		//check player
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("AM:Null player while naming pilot. Report to an admin.",Username,true);
			return;
		}
		
		//fetch unit
		SUnit u = p.getUnit(unitid);
		if (u == null) {
			CampaignMain.cm.toUser("AM:Could not find a unit with ID#" + unitid + ".",Username,true);
			return;
		}
		
		//fetch pilot
		SPilot pilot = (SPilot)u.getPilot();
		if (pilot == null) {
			CampaignMain.cm.toUser("AM:Unit #" + unitid + " has a null pilot. Report this to an admin.",Username,true);
			return;
		}
		
		//make sure pilot isn't Vacant (99/99)
		if (pilot.getName().toLowerCase().startsWith("vacant")) {
			CampaignMain.cm.toUser("AM:There is no pilot in that unit! It is vacant!",Username,true);
			return;
		}
		
		//checks passed. change name,
		pilot.setName(name);
		CampaignMain.cm.toUser("AM:The pilot of the " + u.getModelName() + " (#" + unitid + ") was renamed. New name: " + name + ".",Username,true);
		CampaignMain.cm.toUser("PL|UU|"+ u.getId() + "|" + u.toString(true),Username,false);
		
	}//end process()
}//end NamePilotCommand
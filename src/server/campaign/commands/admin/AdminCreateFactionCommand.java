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
 * Created on 14.04.2004
 *
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;
import server.campaign.commands.Command;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.MWChatServer.auth.IAuthenticator;


/**
 * @author Helge Richter
 */
public class AdminCreateFactionCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "name#color(hex)#basegunner#basePilot#Abbreviation";
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
		
		try{
			String name = command.nextToken();
			String color = command.nextToken();
			int baseGunner = Integer.parseInt(command.nextToken());
			int basePilot = Integer.parseInt(command.nextToken());
			String Abb = command.nextToken();
			
			SHouse newfaction = new SHouse(CampaignMain.cm.getData().getUnusedHouseID(),name,"#" + color,baseGunner,basePilot,Abb); 
			newfaction.updated();
			
			CampaignMain.cm.addHouse(newfaction);
			CampaignMain.cm.toUser("Faction created!",Username,true);
			CampaignMain.cm.doSendModMail("NOTE",Username + " has created faction " + newfaction.getName());
		}
		catch(Exception ex){
			CampaignMain.cm.toUser("Invalid Syntax: /AdminCreateFaction Name#Color(hex)#BaseGunner#BasePilot#Abberviation",Username,true);
			return;
		}
	}
}
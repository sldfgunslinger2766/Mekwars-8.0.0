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

import java.util.StringTokenizer;

import common.util.Position;

import server.campaign.commands.Command;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.MWChatServer.auth.IAuthenticator;

public class AdminMovePlanetCommand implements Command {
	
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
		
		SPlanet p = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
        
        if ( p == null )
            return;
        double x = 0;
        double y = 0;
        
        try{
            x = Double.parseDouble(command.nextToken());
            y = Double.parseDouble(command.nextToken());
        }catch (Exception ex){
            CampaignMain.cm.toUser("Invalid Syntax: adminmoveplanet#name#x#y",Username);
            return;
        }
        
        p.setPosition(new Position(x,y));
        p.updated();

        CampaignMain.cm.doSendModMail("NOTE",Username + " has moved planet " + p.getName()+" to "+x+","+y);
		CampaignMain.cm.toUser("Planet Moved",Username);
	}
}
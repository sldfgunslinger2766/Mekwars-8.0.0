/*
 * MekWars - Copyright (C) 2007 
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
 * 
 * Command Saves the server config to its defined file
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;
import server.campaign.commands.Command;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.MWChatServer.auth.IAuthenticator;

public class AdminSaveFactionConfigsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		String faction = "";
        
		try{
		    faction = command.nextToken();
		}
		catch (Exception ex){
		    CampaignMain.cm.toUser("Invalid syntax. Try: AdminSaveFactionConfigs#faction",Username,true);
		    return;
		}
		
		SHouse h = CampaignMain.cm.getHouseFromPartialString(faction,Username);
		
		if ( h == null )
		    return;

		if(CampaignMain.cm.isUsingMySQL())
			h.saveConfigFileToDB();
		else
			h.saveConfigFile();
		h.setUsedMekBayMultiplier(Float.parseFloat(h.getConfig("UsedPurchaseCostMulti")));
		CampaignMain.cm.toUser("Status saved!",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has saved "+faction+"'s configs");
		
	}//end process
}
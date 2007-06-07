/*
 * MekWars - Copyright (C) 2006 
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

import server.campaign.CampaignMain;

public class ListServerOpFlagsCommand implements Command {
	
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
		
        StringBuilder result = new StringBuilder();
    
        result.append("<font color=\"black\">");
        result.append("Server Op Flags<br>");
        result.append("<table><tr><th>Key</th>");
        result.append("<th>Value</th>");

        for ( String key : CampaignMain.cm.getData().getPlanetOpFlags().keySet() ){
            result.append("<tr>");
            result.append("<td>");
            result.append(key);
            result.append("</td><td>");
            result.append(CampaignMain.cm.getData().getPlanetOpFlags().get(key));
            result.append("</td>");
            result.append("</tr>");
        }
        result.append("</table></font>");
        CampaignMain.cm.toUser(result.toString(), Username);
	}
}

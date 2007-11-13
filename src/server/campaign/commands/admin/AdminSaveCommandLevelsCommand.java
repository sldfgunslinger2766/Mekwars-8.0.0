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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import server.MWServ;
import server.campaign.commands.Command;
import server.campaign.CampaignMain;
import server.MWChatServer.auth.IAuthenticator;


public class AdminSaveCommandLevelsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	@SuppressWarnings("unchecked")
    public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		TreeMap commandTable = new TreeMap(CampaignMain.cm.getServerCommands());
        
        try{
		    
		    File fp = new File("./data/commands");
		    if ( !fp.exists() )
		        fp.mkdir();
		    
			FileOutputStream out = new FileOutputStream("./data/commands/commands.dat");
			PrintStream p = new PrintStream(out);
			
            String commandName = "";
			for(Iterator i = commandTable.keySet().iterator(); i.hasNext(); commandName = (String)i.next() )
			{
				Command commandMethod = CampaignMain.cm.getServerCommands().get(commandName);
                if ( commandName == null || commandMethod == null)
                    continue;
				p.println(commandName.toUpperCase()+"#"+commandMethod.getExecutionLevel());
			}	
		}
		catch (Exception ex){
		    MWServ.mwlog.errLog(ex);
		    MWServ.mwlog.errLog("Unable to save command levels");
		}
		CampaignMain.cm.toUser("Command levels saved!",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has saved the command levels to file.");
		
	}
}
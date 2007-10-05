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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;

import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;
import server.MWChatServer.auth.IAuthenticator;
@SuppressWarnings({"unchecked","serial"})
public class SingASongCommand implements Command {
	
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
		
		String startHouse = "";
		String request = "";
		SHouse faction = null;
		
		if ( command.hasMoreTokens() )
			request = command.nextToken();
		
		if ( command.hasMoreTokens() )
			startHouse = command.nextToken();
		
		if ( request.equalsIgnoreCase("list") ){
			listSongs(Username);
			return;
		}
		
        String song = getSong(request);
        
        if ( song == null ){
            listSongs(Username);
            return;
        }
            
		if ( !startHouse.equals("") )
			faction = CampaignMain.cm.getHouseFromPartialString(startHouse,null);
		
		CampaignMain.cm.doSendToAllOnlinePlayers(Username + " forces you all to sing "+request, true);
		StringTokenizer songLyrics = new StringTokenizer(song,"#");
		
		try{
			while ( songLyrics.hasMoreTokens() ){
				String songLine ="";
				
				if ( faction != null ){
					for (SPlayer player : faction.getAllOnlinePlayers().values() ){
						if ( player.getDutyStatus() < SPlayer.STATUS_RESERVE)
							continue;
						if ( CampaignMain.cm.getServer().isAdmin(player.getName()) )
							continue;
						if ( player.getName().equalsIgnoreCase("Spork") )
							continue;
						songLine = songLyrics.nextToken();
						CampaignMain.cm.doSendToAllOnlinePlayers(player.getName()+"|"+songLine,true);
						Thread.sleep(1000);
					}
				}
				Iterator factions = CampaignMain.cm.getData().getAllHouses().iterator();  
				while (factions.hasNext()){
                    faction = (SHouse) factions.next();
                    for (SPlayer player : faction.getAllOnlinePlayers().values() ){
						if ( player.getDutyStatus() < SPlayer.STATUS_RESERVE)
							continue;
						if (CampaignMain.cm.getServer().isAdmin(player.getName()))
							continue;
						if ( player.getName().equalsIgnoreCase("Spork") )
							continue;
						songLine = songLyrics.nextToken();
						CampaignMain.cm.doSendToAllOnlinePlayers(player.getName()+"|"+songLine,true);
						Thread.sleep(1000);
					}
				}
				
			}
		}
		catch (Exception ex){
			
		}
		
	}
	
	public void listSongs(String user){
		File songList = new File("./data/songs.txt");
		CampaignMain.cm.toUser("SM|Current song List",user,false);
        CampaignMain.cm.toUser("SM|teapot",user,false);
		try{
			FileInputStream fis = new FileInputStream(songList);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()){
				StringTokenizer song = new StringTokenizer(dis.readLine(),"|");
				CampaignMain.cm.toUser("SM|"+song.nextToken(),user,false);
			}
		}
		catch(Exception ex){
			//No song list found;
			return;
		}
		
	}
	
	public String getSong(String songName){
		
		//default song of torcher --Torren
		String Song = "I'm a little teapot!#Short and Stout#Here is my handle#Here is my Spout#When I get all steamed up#Hear me shout!#Tip me over#And pour me out!";
		if ( songName.equalsIgnoreCase("teapot") )
			return Song;
		File songList = new File("./data/songs.txt");
		try{
			FileInputStream fis = new FileInputStream(songList);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()){
				StringTokenizer song = new StringTokenizer(dis.readLine(),"|");
				if ( song.nextToken().equalsIgnoreCase(songName) ){
					Song = song.nextToken();
					return Song;
				}
			}
		}
		catch(Exception ex){
            return null;
		}
		
		return null;
	}
	
}
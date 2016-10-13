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

package server.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import server.MWServ;
import server.campaign.CampaignMain;
import common.CampaignData;

public class TrackerThread extends Thread {
	
	//VARIABLE
	MWServ serv;
	
	//CONSTRUCTOS
	public TrackerThread(MWServ s) {
	    super("Tracker Thread");
		serv = s;
		CampaignData.mwlog.infoLog("Created TrackerThread");
	}
	
	//METHODS
	
	@Override
	public synchronized void run() {
		
		CampaignData.mwlog.infoLog("TrackerThread running.");
		
		//core info
		String name = serv.getConfigParam("SERVERNAME");
		String link = serv.getConfigParam("TRACKERLINK");
		String desc = serv.getConfigParam("TRACKERDESC");
		
		//ip of tracker
		String trackerAddress = serv.getConfigParam("TRACKERADDRESS");
		
		/*
		 * Immediately send core info to tracker.
		 */
		Socket sock = null;
		try {
			CampaignData.mwlog.infoLog("TrackerThread attempting to send ServerStart information.");
			sock = new Socket(trackerAddress, 13731);//fixed port
			PrintWriter pw = new PrintWriter(sock.getOutputStream());
			
			pw.println("SS%" + name + "%" + link + "%" + MWServ.SERVER_VERSION + "%" + desc);
			pw.flush();
			pw.close();
			CampaignData.mwlog.infoLog("TrackerThread sent server start information.");
		} catch (Exception e) {
			CampaignData.mwlog.infoLog("TrackerThread could not contact tracker. Shutting down.");
			CampaignData.mwlog.errLog("Could not contact tracker. Shutting down trackerthread.");
			CampaignData.mwlog.errLog(e);
			return;
		} finally {
			try {
				sock.close();
			} catch (IOException e) {
				CampaignData.mwlog.errLog(e);
			}
		}
		
		/*
		 * socket is closed server side after this
		 * line is read from a buffered stream.
		 */ 
		
		/*
		 * now that the first run is done, start a forever-long
		 * phone home loop which updates tracker with player
		 * counts, game counts, etc. every 10 minutes.
		 */
		try {
			while (true) {
				
				//10 minute wait between updates

				try {
					Thread.sleep(600000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					break;
				}
				
				//set up substrings
				//name - already saved from ServerStart
				int playersOnline = serv.userCount(false);
				int gamesInProgress = 0;
				int gamesCompleted = 0;
				
				//get campaign main. if not null, get game info.
				CampaignMain campaign = serv.getCampaign();
				if (campaign != null) {
					gamesInProgress = campaign.getOpsManager().getRunningOps().size();
					gamesCompleted = campaign.getGamesCompleted();
					campaign.setGamesCompleted(0);
				}
				
				//string to send to tracker
				String toSend = "PH%" + name + "%" + playersOnline + "%" + gamesInProgress + "%" + gamesCompleted;
				
				//set up a socket to the tracker and send this record
				try {
					CampaignData.mwlog.infoLog("TrackerThread attempting to send PhoneHome information.");
					sock = new Socket(trackerAddress, 13731);//fixed port
					PrintWriter pw = new PrintWriter(sock.getOutputStream());
					
					pw.println(toSend);
					pw.flush();
					pw.close();
					CampaignData.mwlog.infoLog("TrackerThread sent PH% information.");
				} catch (Exception e) {
					CampaignData.mwlog.infoLog("TrackerThread could not reach tracker for PH%.");
					CampaignData.mwlog.errLog("Could not contact tracker.");
					CampaignData.mwlog.errLog(e);
				}
			}//end (forever)	
		}
		catch (Exception ex) {
			CampaignData.mwlog.errLog(ex);
		}
	}
}
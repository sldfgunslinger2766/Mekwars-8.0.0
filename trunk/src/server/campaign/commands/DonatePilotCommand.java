package server.campaign.commands;

import java.util.StringTokenizer;

import common.campaign.pilot.Pilot;

import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.pilot.SPilot;

/**
 * @author Torren (Jason Tighe)
 */
public class DonatePilotCommand implements Command {
	
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
		
		SPlayer p =  CampaignMain.cm.getPlayer(Username);
		SHouse house = p.getMyHouse();
		
		int donationsAllowed = Integer.parseInt(house.getConfig("DonationsAllowed"));
		if (donationsAllowed <= 0) {
			CampaignMain.cm.toUser("Donations are not allowed on this server.",Username,true);
			return;
		}
		
        
        if ( p.mayAcquireWelfareUnits() ){
            CampaignMain.cm.toUser("You may not donate any of your pilots while you are on welfare.",Username,true);
            
            return;
        }

        if (p.getMyHouse().isNewbieHouse()) {
			CampaignMain.cm.toUser("SOL Players are not allowed to donate pilots, sorry!",Username,true);
			return;
		}
		
		int unitType = Integer.parseInt(command.nextToken());
		int weightClass = Integer.parseInt(command.nextToken());
		int pilotLocation = Integer.parseInt(command.nextToken());

		Pilot pilot = p.getPersonalPilotQueue().getPilot(unitType,weightClass,pilotLocation);

		if ( pilot == null ){
			CampaignMain.cm.toUser("Unable to find pilot!",Username,true);
			return;
		}
		int mechdonateprize = 0;
		if (Boolean.parseBoolean(house.getConfig("DonatingCostsBills"))) {
				mechdonateprize = Integer.parseInt(house.getConfig("CostToBuyNewPilot")) / 2;
				int infToDonate = weightClass * 2;
				if ( (p.getMoney() >= mechdonateprize) && (p.getInfluence() >= infToDonate)) {
					p.addMoney( -mechdonateprize);
					p.addInfluence( -infToDonate);
				}
				else if (p.getUnits().size() < 4 ) {
					CampaignMain.cm.toUser("HQ has allowed you to retrain this pilot, at a reduced rate, due to your current situation.", Username, true);
					if ( p.getMoney() >= mechdonateprize )
						p.addMoney( -mechdonateprize);
					else {
						mechdonateprize = p.getMoney();
						p.addMoney( -mechdonateprize);
					}
					p.addInfluence( -infToDonate);
				}
				else {
					CampaignMain.cm.toUser("You can't afford to retrain this pilot. You need "  + CampaignMain.cm.moneyOrFluMessage(true,true,mechdonateprize) + " and " + CampaignMain.cm.moneyOrFluMessage(false,false,infToDonate)+".", Username, true);
					//send the pilot back to the players queue.
					p.getPersonalPilotQueue().addPilot(pilot,weightClass);
					return;
				}
			}
		
		//set up strign to send user
		String toUser = "You've sent pilot "+pilot.getName()+" back to the faction for more training";
		if (mechdonateprize > 0)
			toUser += ". Your faction charges you " + CampaignMain.cm.moneyOrFluMessage(true,true,mechdonateprize) + " for the transfer";
		toUser += ".";

		p.getMyHouse().getPilotQueues().addPilot(unitType,(SPilot)pilot);
        CampaignMain.cm.toUser("PL|RPPPQ|"+unitType+"|"+weightClass+"|"+pilotLocation,Username,false);
		CampaignMain.cm.doSendHouseMail(p.getMyHouse(), "NOTE", p.getName() + " donated a " + pilot.getName().trim() + " to the faction pools!");

		
	}
}


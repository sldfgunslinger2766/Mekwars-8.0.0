/*
 * MekWars - Copyright (C) 2005  
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

import server.MMServ;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.operations.Operation;
import server.campaign.operations.OpsChickenThread;
import server.campaign.operations.ShortOperation;

/**
 * DefendCommand is analagous to the Task system's
 * "join" command - it allows a player to register
 * himself as the defender for an attack.
 */
public class DefendCommand implements Command {
	
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
		
		//get the Op ID and the Army ID
		int opID = -1;
		int armyID = -1;
		int teamNumber = -1;
		
		try {
			opID = Integer.parseInt(command.nextToken());
			armyID = Integer.parseInt(command.nextToken());
			teamNumber = Integer.parseInt(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c defend#attack number#army number#team number",Username,true);
			return;
		}
		
		//get the player
		SPlayer dp = CampaignMain.cm.getPlayer(Username);
		if (dp == null) {
			CampaignMain.cm.toUser("Null player. Report this immediately!",Username,true);
			return;
		}
		
		//check the attack
		ShortOperation so = CampaignMain.cm.getOpsManager().getRunningOps().get(opID);
		if (so == null) {
			CampaignMain.cm.toUser("Defend failed. Attack #" + opID + " does not exist.",Username,true);
			return;
		}
		
		//check the army
		SArmy da = dp.getArmy(armyID);
		if (da == null) {
			CampaignMain.cm.toUser("Defend failed. Army #" + armyID + " does not exist.",Username,true);
			return;
		}
		
		//don't let players defend multiple games
		if(dp.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("You are already fighting!", Username, true);
			return;
		}
		
		//check the player's activity
		if (dp.getDutyStatus() != SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("Defend failed. You must be active to defend against an attack.",Username,true);
			return;
		}
		
		//make sure the operation is accepting defenders
		if (so.getStatus() != ShortOperation.STATUS_WAITING) {
			
			if (so.getStatus() == ShortOperation.STATUS_FINISHED) {
				CampaignMain.cm.toUser("Defend failed. Attack #" + opID + " is finished.",Username,true);
				return;
			}
			
			//else, neither waiting nor finished. assume running.
			CampaignMain.cm.toUser("Defend failed. Attack #" + opID + " is already defended.",Username,true);
			return;

		}
		
		/*
		 * Make sure the defender can actually defend this attack. If the player
		 * is in this chickenTree, with this army, we can assume that he/it have
		 * already passed validation.
		 * 
		 * If not, he's probably trying to defend illegally. Run him through the
		 * validator and give him failure reasons. If he doesn't fail he probably
		 * activated after the attack, and we should let him defend ...
		 */
		OpsChickenThread pThread = so.getChickenThreads().get(dp.getName().toLowerCase());
		Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
		if (pThread == null || !pThread.getArmies().contains(da)) {
			
			//check to see if the attacking army is on the da's oplist
			SPlayer attacker = CampaignMain.cm.getPlayer(so.getAttackers().firstKey());
			SArmy aa = attacker.getArmy(so.getAttackers().get(so.getAttackers().firstKey()));

			boolean isAnOpponent = false;
			for (SArmy currA : da.getOpponents()) {
				if (currA.equals(aa)) {
					isAnOpponent = true;
					break;
				}
			}
			
			if (!isAnOpponent) {
				CampaignMain.cm.toUser("Defend failed. Army #" + da.getID() + " is not an opponent "
						+ "for the army in Attack #" + so.getShortID() +". BV's do not match.",Username,true);
				return;
			}
			
			//is an opponent (BV wise), so check for an op match
			String s = CampaignMain.cm.getOpsManager().validateShortDefense(dp, da, o,null);
			if (s != null && !s.trim().equals("")) {
				CampaignMain.cm.toUser("Defend failed " + s,Username,true);
				return;
			} 
		}
		
		
		if ( teamNumber > 0 ) {
			int bv = 0;
			String message = "That team is already full pick another one!";
			if ( o.getBooleanValue("TeamOperation") && o.getBooleanValue("TeamsMustBeSameFaction") ) {
				teamNumber = so.getFactionTeam(dp.getHouseFightingFor().getName());
				bv = da.getBV();
				message = "You are not able to join that team to defend!";
			}
			if ( !so.checkTeam(teamNumber,bv,false) ) {
				CampaignMain.cm.toUser(message, Username);
				CampaignMain.cm.toUser(so.getChickenThreads().get(Username.toLowerCase()).generateAttackDialogCall(),Username,false);
				return;
			}
			
			dp.setTeamNumber(teamNumber);
			CampaignMain.cm.toUser("PL|STN|"+teamNumber, Username,false);
			CampaignMain.cm.toUser("You've been assigned to team #"+teamNumber+".", Username);
		}else if ( o.getBooleanValue("RandomTeamDetermination") && o.getBooleanValue("TeamOperation") ) {
			int numberOfTeams = Math.max(2,Math.min(8,o.getIntValue("NumberOfTeams")));

			for (int team = 1; team <= numberOfTeams; team++ ) {
				if ( so.checkTeam(team) ) {
					teamNumber = team;
					break;
				}
			}
			CampaignMain.cm.toUser("PL|STN|"+teamNumber, Username,false);
			CampaignMain.cm.toUser("You've been assigned to team #"+teamNumber+".", Username);
		}
		
		/*
		 * At this point, we can assume that we have a valid army
		 * 
		 * - Add the defender to the ShortOperation.
		 * - Remove the defender from any other ShortOperations
		 *   he is involved in. This stops any running chicken
		 *   threads and may cancel cancel other attacks.
		 */
		CampaignMain.cm.getOpsManager().removePlayerFromAllAttackerLists(dp, so, true);
		CampaignMain.cm.getOpsManager().removePlayerFromAllDefenderLists(dp, so, true);
		
		//If you join the attackers team then you will be added to the attackers array.
		
		if ( teamNumber > 0 && so.getAttackersTeam() == teamNumber)
			so.addAttacker(dp, da, "");
		else
			so.addDefender(dp, da, "");//add defender

		/*
		 * We can assume that the player has enough money/etc to defend
		 * the game, or he would not have passed the checks. This is a
		 * VERY VERY dangerous step. It is possible for people to have
		 * enough when they go active and then drop under (ie - buy a
		 * unit). This is a massive hole, as the returns when an op is
		 * terminated are flat. This means a player can defend, transfer
		 * and then cancel to get huge stacks of cash ...
		 * 
		 * This is why defender costs for non-modifying operations are
		 * so strongly discouraged in the DefaultOperation comments.
		 */
		int money = o.getIntValue("DefenderCostMoney");
		int flu = o.getIntValue("DefenderCostInfluence");
		int rp = o.getIntValue("DefenderCostReward");
		
		String toSend = "You are now defending Attack #" + opID;
		
		boolean hasCost = false;
		
		if (money > 0) {
			dp.addMoney(-money);
			toSend += "(" + CampaignMain.cm.moneyOrFluMessage(true, true, money);
			hasCost = true;
		}
		if (flu > 0) {
			dp.addInfluence(-flu);
			if(hasCost)
				toSend += ", ";
			else
				toSend += "(";
			toSend += CampaignMain.cm.moneyOrFluMessage(false, true, flu);
			hasCost = true;
		}
		if (rp > 0) {
			dp.addReward(-rp);
			if(hasCost)
				toSend += ", ";
			else
				toSend += "(";
			toSend += "-" + rp + " RP";
			hasCost = true;
		}
		
		if (hasCost)
			toSend += ").";
		else
			toSend += ".";
		
		//tell the defender that he has succesfully joined the attack.
		MMServ.mmlog.gameLog("Defend: " + so.getShortID() + "/" + dp.getName() + " w. Army #" + da.getID());
		CampaignMain.cm.toUser(toSend,Username,true);
        
        if ( o.getBooleanValue("FreeForAllOperation") ){
            CampaignMain.cm.toUser(dp.getName()+" has joined the operation, as a defender. <a href=\"MEKWARS/c commenceoperation#" + opID + "#CONFIRM\">Click here to commence</a>",so.getInitiator().getName(),true);
        }
		
	}//end process
	
}//end DefendCommand
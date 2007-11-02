/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * OperationsManager should instantiate one StandardResolver
 * and pass it (Short/Long)Operations as they are completed.
 */
package server.campaign.operations;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import megamek.common.IEntityRemovalConditions;
import megamek.common.Mech;

import common.Unit;
import common.UnitFactory;
import common.campaign.pilot.skills.PilotSkill;
import common.util.UnitUtils;

import server.MWServ;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.mercenaries.MercHouse;
import server.campaign.pilot.SPilot;
import server.campaign.util.ELORanking;

public class ShortResolver {

	// IVARS

	/*
	 * These variables are NOT true instance variables. They're set when a
	 * resolve() is called - in a specific order - and used in place of gigantic
	 * paramater lists for each method called by a resolve(). They are reset
	 * with each new resolve().
	 */
	TreeMap<String, SPlayer> allPlayers;

	TreeMap<String, SArmy> allArmies;

	TreeMap<String, String> metaStrings;// You Win! messages/meta outcomes.

	TreeMap<String, String> unitStrings;// salvage, level up and pilot recovery
										// information.

	TreeMap<String, String> payStrings;// personal payment info (CBills, etc)

	TreeMap<String, String> longStrings;// long status if part of a long

	TreeMap<String, Integer> unitCosts;// salvage costs, etc.

	TreeMap<String, Integer> lossCompensation;// reimbursement for destroyed
												// or enemy-salvaged units.

	TreeMap<Integer, OperationEntity> salvagableUnits;

	TreeMap<Integer, OperationEntity> destroyedUnits;

	TreeMap<Integer, OperationEntity> livingUnits;

	TreeMap<Integer, SPilot> pilots;

	TreeMap<String, OpsScrapThread> scrapThreads;// threads are put into
													// OpsMan @ end.

	boolean attackersWon = false;

	boolean defendersWon = false;

	int currentBV = 0;

	int loserBV = 0;

	int buildingsLeft = -1;

	int buildingsDestroyed = 0;

	String completeFinishedInfoString = "";

	String incompleteFinishedInfoString = "";

	String newsFeedTitle = "";

	String newsFeedBody = "";

	private boolean drawGame = false;

	private boolean saveStats = false;

	private boolean freeForAll = false;

	private ShortOperation shortOp = null;

	// CONSTRUCTORS
	public ShortResolver() {
		// contents
	}

	// METHODS
	/**
	 * Method which clears or nulls all of the holder variables. Private. Called
	 * when a resolve() is finished.
	 */
	private void resetVariables() {

		allPlayers = null;
		allArmies = null;

		metaStrings = null;
		unitStrings = null;
		payStrings = null;
		longStrings = null;

		unitCosts = null;
		lossCompensation = null;

		salvagableUnits = null;
		destroyedUnits = null;
		livingUnits = null;
		pilots = null;

		scrapThreads = null;

		attackersWon = false;
		defendersWon = false;

		currentBV = 0;
		loserBV = 0;

		buildingsLeft = -1;
		buildingsDestroyed = 0;

		completeFinishedInfoString = "";
		incompleteFinishedInfoString = "";
		newsFeedTitle = "";
		newsFeedBody = "";

		drawGame = false;
		saveStats = false;
		freeForAll = false;

	}// end resetVariables()

	/**
	 * Method which takes in an autoreport string, reads operation
	 * configurations, and sets post-game outcomes as needed.
	 * 
	 * This method does initial processing of the report string, then farms work
	 * out to several private methods for salvage, payment, etc.
	 * 
	 * Using the discrete private methods makes it a bit simpler to track all of
	 * the paramaters and variables in play ... also allows for automagical
	 * disconnection resolution.
	 */
	public void resolveShortAttack(Operation o, ShortOperation so, String report) {

		this.shortOp = so;
		// return if the game is waiting, already reporting or already finished.
		if (so.getStatus() != ShortOperation.STATUS_INPROGRESS)
			return;

		if (so.getAutoReport() == null)
			so.setAutoReport(report);

		// reset any old trees/variables/etc
		this.resetVariables();

		saveStats = !o.getBooleanValue("NoStatisticsMode");
		freeForAll = o.getBooleanValue("FreeForAllOperation");
		// master tokenizer
		StringTokenizer reportTokenizer = new StringTokenizer(report, "#");

		/*
		 * Process the winner and loser lists. These should, generally, align
		 * perfectly with the attacker/defender maps; however, doing it this way
		 * makes arena and grand melee style operations theoretically possible.
		 * 
		 * so.getWinners() = new TreeMap<String, SPlayer>(); so.getLosers() =
		 * new TreeMap<String, SPlayer>();
		 */

		/*
		 * Set up a Map with all of the players from the game. ShortOperation
		 * has a method which returns all player names; however, we want a map
		 * populated with real SPlayer pointers from the outset in order to
		 * avoid continual calls to CampaignMain's getPlayer() method.
		 */
		allPlayers = new TreeMap<String, SPlayer>();
		allArmies = new TreeMap<String, SArmy>();

		TreeMap<String, Integer> soIdentifiers = so.getAllPlayersAndArmies();
		for (String currName : soIdentifiers.keySet()) {

			SPlayer currP = CampaignMain.cm.getPlayer(currName);
			SArmy currA = currP.getArmy(soIdentifiers.get(currName));

			allPlayers.put(currName, currP);
			allArmies.put(currName, currA);
		}

		// First token is a winner list, deliminted by *'s.
		StringTokenizer winnerTokenizer = new StringTokenizer(reportTokenizer
				.nextToken(), "*");

		/*
		 * loop through so.getWinners() and add each to the winner tree.
		 */
		while (winnerTokenizer.hasMoreElements()) {
			String nextName = winnerTokenizer.nextToken().toLowerCase();
			if (nextName.equalsIgnoreCase("DRAW"))
				drawGame = true;
			if (!so.getWinners().containsKey(nextName))
				so.getWinners().put(nextName.toLowerCase(),
						allPlayers.get(nextName.toLowerCase()));
		}

		/*
		 * It is safe to assume that that all players who weren't
		 * so.getWinners() lost. Compare the allPlayers tree to the
		 * so.getWinners() tree and add those from all who aren't already in
		 * so.getWinners() to the so.getLosers() map.
		 */
		for (String currName : allPlayers.keySet()) {
			if (!so.getWinners().containsKey(currName)
					&& !so.getLosers().containsKey(currName))
				so.getLosers().put(currName, allPlayers.get(currName));
		}

		/*
		 * Before establishing which units are saveable/killed and moving
		 * anything between players or changing the pilots in units, save the
		 * so.getLosers()'s BV. User to set the level at which he's paid out.
		 */
		loserBV = 0;
		for (SArmy currArmy : allArmies.values()) {
			if (so.getLosers().containsKey(
					currArmy.getPlayerName().toLowerCase()))
				loserBV += currArmy.getBV();
		}

		// return if there is no winner. terminate the game.
		if (so.getWinners().size() == 0) {
			for (String pname : allPlayers.keySet())
				CampaignMain.cm.toUser("Reporting error: Game had no winner.",
						pname, true);
			// set to reporting status
			so.changeStatus(ShortOperation.STATUS_REPORTING);
			CampaignMain.cm.getOpsManager().terminateOperation(so,
					OperationManager.TERM_REPORTINGERROR, null);
			return;
		}

		// set to reporting status
		so.changeStatus(ShortOperation.STATUS_REPORTING);

		// break units in living/salvagable/dead, etc.
		this.possibleSalvageFromReport(reportTokenizer, so);

		/*
		 * assemble the winner and loser strings, and the final status info
		 * string.
		 * 
		 * NOTE: This is where meta-impacts are applied. See method for more
		 * detailed comments on assignment of conquest %, thefts, etc.
		 */
		this.assembleDescriptionStrings(o, so);

		/*
		 * put together the salvage strings, and move units around. also
		 * determine cost of player's salvage. save these costs so they may be
		 * used to adjust players' paystrings.
		 */
		this.assembleSalvageStrings(o, so);

		/*
		 * Put together the payment strings, and pay the players. Adjust the
		 * actual game pay by salvage & tech/bay costs.
		 */
		this.assemblePaymentStrings(o, so, null);

		/*
		 * Check to see if this resolves a long operation.
		 */
		// if (so.getLongID() >= 0)
		// this.checkStatusOfRelatedLong();
		/*
		 * Unlock all participating armies.
		 */
		for (SArmy currA : allArmies.values()) {
			currA.setLocked(false);
			CampaignMain.cm.toUser("PL|SAL|" + currA.getID() + "#" + false,
					currA.getPlayerName(), false);
		}

		/*
		 * Send messages to the players, remove them from fighting status, and
		 * inform them of any immunity they may have received.
		 */
		for (SPlayer currPlayer : allPlayers.values()) {

			// send messages
			String currName = currPlayer.getName().toLowerCase();
			String toSend = metaStrings.get(currName)
					+ unitStrings.get(currName) + payStrings.get(currName);// +
																			// longStrings.get(currName);
			CampaignMain.cm.toUser(toSend, currName, true);
			// Reset all Playes Team Number.
			CampaignMain.cm.toUser("PL|STN|" + -1, currName, false);

			// stick the result into the human readable result log, per
			// RFE1479311.
			MWServ.mwlog.resultsLog(toSend);

			// deal with scrapThread for player, if he has one.
			if (scrapThreads.containsKey(currName)) {

				// stop existing thread(s)
				if (CampaignMain.cm.getOpsManager().getScrapThreads()
						.containsKey(currName))
					CampaignMain.cm.getOpsManager().getScrapThreads().get(
							currName).stopScrap();

				// start new thread
				Integer maxScrapPay = unitCosts.get(currName);
				if (maxScrapPay == null)
					maxScrapPay = new Integer(0);
				scrapThreads.get(currName).setMaxPayment(maxScrapPay);
				scrapThreads.get(currName).start();

				// move to manager
				CampaignMain.cm.getOpsManager().getScrapThreads().put(currName,
						scrapThreads.remove(currName));
			}

			this.contractFinishedInfo(currPlayer);

			// refresh all armies and review all legal operations
			for (SArmy currA : currPlayer.getArmies()) {
				currA.setBV(0);// force BV recalculation
				CampaignMain.cm.toUser("PL|SAD|" + currA.toString(true, "%"),
						currA.getPlayerName(), false);
				CampaignMain.cm.getOpsManager().checkOperations(currA, true);
			}

			currPlayer.resetWeightedArmyNumber();

			// set immunity && make unbusy
			CampaignMain.cm.getIThread().addImmunePlayer(currPlayer);
			if (so.isFromReserve())
				currPlayer.setFighting(false, true);
			else
				currPlayer.setFighting(false);

			currPlayer.checkForPromotion();
			/*
			 * Servers with AR generally force people to deactivate and repair.
			 * We set immunity above, despite the fact that players will never
			 * need it to avoid games, so they can scrap units without cost and
			 * reset in SOL.
			 */
			if (!so.isFromReserve()
					&& CampaignMain.cm.getBooleanConfig("ForcedDeactivation")) {
				currPlayer.setActive(false);
				CampaignMain.cm
						.toUser(
								"You've left the front lines to repair and refit, and are now in reserve.",
								currPlayer.getName());
			}

			CampaignMain.cm.sendPlayerStatusUpdate(currPlayer, true);
		}

		/*
		 * Set the finished strings for the ShortOperation.
		 */
		so.setCompleteFinishedInfo(completeFinishedInfoString);
		so.setIncompleteFinishedInfo(incompleteFinishedInfoString);

		/*
		 * Set the game to finished staus and bump the game counter.
		 */
		so.changeStatus(ShortOperation.STATUS_FINISHED);
		CampaignMain.cm.addGamesCompleted(1);
		if (CampaignMain.cm.isUsingCyclops())
			CampaignMain.cm.getMWCC().opConclude(so);
		if (o.getBooleanValue("ReportOpToNewsFeed"))
			CampaignMain.cm.addToNewsFeed(newsFeedTitle, newsFeedBody);

	}// end resolveShortAttack

	/**
	 * Method which resolves a short operation in the absence of an autoreport -
	 * namely, when one player disconnected from a game for an inordinately long
	 * period of time.
	 * 
	 * Instead of processing a complete autoreport, this resolution pathway
	 * rebuilds the state of the game at the time of the disconnection using a
	 * TreeMap which contains the status of all units removed from the game up
	 * to that point (destroyed, salvage, etc).
	 * 
	 */
	public void resolveShortAttack(Operation o, ShortOperation so,
			String winnerName, String loserName) {

		// reset any old trees/variables/etc
		this.resetVariables();

		// get the players
		SPlayer winner = CampaignMain.cm.getPlayer(winnerName);
		SPlayer loser = CampaignMain.cm.getPlayer(loserName);

		// return if the game is waiting
		if (so.getStatus() != ShortOperation.STATUS_INPROGRESS)
			return;

		// set to reporting status
		so.changeStatus(ShortOperation.STATUS_REPORTING);

		/*
		 * Disconnect resolution is used only for games involving 2 players, and
		 * our winner and loser are fed to us here.
		 * 
		 * so.getWinners() = new TreeMap<String, SPlayer>(); so.getLosers() =
		 * new TreeMap<String, SPlayer>();
		 */

		if (winner != null)
			so.getWinners().put(winnerName.toLowerCase(), winner);
		if (loser != null)
			so.getLosers().put(loserName.toLowerCase(), loser);

		// return if there is no winner. terminate the game.
		if (so.getWinners().size() == 0) {
			CampaignMain.cm.toUser("Autoreporting error: Game had no winner.",
					loserName, true);
			CampaignMain.cm.getOpsManager().terminateOperation(so,
					OperationManager.TERM_REPORTINGERROR, null);
			return;
		}

		// return if there is no loser. terminate the game.
		if (so.getLosers().size() == 0) {
			CampaignMain.cm.toUser("Autoreporting error: Game had no loser.",
					winnerName, true);
			CampaignMain.cm.getOpsManager().terminateOperation(so,
					OperationManager.TERM_REPORTINGERROR, null);
			return;
		}

		/*
		 * Set up a Map with all of the players from the game. We know that only
		 * 2 players are involved; however, the allPlayers and allArmies maps
		 * are used in the salvage method and elsewhere, so this is needed to
		 * conform and use the same processing methods as an autoreport.
		 */
		allPlayers = new TreeMap<String, SPlayer>();
		allArmies = new TreeMap<String, SArmy>();

		allPlayers.put(winnerName.toLowerCase(), winner);
		allPlayers.put(loserName.toLowerCase(), loser);

		SArmy winnerA = winner.getArmy(so.getAllPlayersAndArmies().get(
				winnerName.toLowerCase()));
		SArmy loserA = loser.getArmy(so.getAllPlayersAndArmies().get(
				loserName.toLowerCase()));
		if (winnerA == null || winnerA.getPlayerName().trim().length() == 0) {
			CampaignMain.cm
					.toUser(
							"Autoreporting error: Winner army null or had empty owner name.",
							winnerName, true);
			CampaignMain.cm.getOpsManager().terminateOperation(so,
					OperationManager.TERM_REPORTINGERROR, null);
			return;
		}

		if (loserA == null || loserA.getPlayerName().trim().length() == 0) {
			CampaignMain.cm
					.toUser(
							"Autoreporting error: Loser army null or had empty owner name.",
							winnerName, true);
			CampaignMain.cm.getOpsManager().terminateOperation(so,
					OperationManager.TERM_REPORTINGERROR, null);
			return;
		}

		allArmies.put(winnerName.toLowerCase(), winnerA);
		allArmies.put(loserName.toLowerCase(), loserA);

		// break units in living/salvagable/dead, etc.
		this.possibleSalvageFromInProgressInfo(so, loser);

		/*
		 * assemble the winner and loser strings, and the final status info
		 * string.
		 * 
		 * NOTE: This is where meta-impacts are applied. See method for more
		 * detailed comments on assignment of conquest %, thefts, etc.
		 */
		this.assembleDescriptionStrings(o, so);

		/*
		 * put together the salvage strings, and move units around. also
		 * determine cost of player's salvage. save these costs so they may be
		 * used to adjust players' paystrings.
		 */
		this.assembleSalvageStrings(o, so);

		/*
		 * Put together the payment strings, and pay the players. Adjust the
		 * actual game pay by the salvage costs.
		 */
		this.assemblePaymentStrings(o, so, loser);

		/*
		 * Check to see if this resolves a long operation.
		 */
		// if (so.getLongID() >= 0)
		// this.checkStatusOfRelatedLong();
		/*
		 * Unlock all participating armies. This may result in some oddities for
		 * players who are no logner connected, but its not going to kill the
		 * server.
		 */
		for (SArmy currA : allArmies.values()) {
			currA.setLocked(false);
			CampaignMain.cm.toUser("PL|SAL|" + currA.getID() + "#" + false,
					currA.getPlayerName(), false);
		}

		/*
		 * Send messages to the winner, remove them from fighting status, and
		 * inform him of any immunity he may have received.
		 */

		// send message
		String winName = winner.getName().toLowerCase();
		String toSend = "Time expired. GAME RESOLVED AUTOMATICALLY.<br>"
				+ metaStrings.get(winName) + unitStrings.get(winName)
				+ payStrings.get(winName);// + longStrings.get(winName);
		CampaignMain.cm.toUser(toSend, winName, true);

		// stick the result into the human readable result log, per RFE1479311.
		MWServ.mwlog.resultsLog(toSend);

		// update operations and set unbusy. we know the loser isn't
		// online, so we only need to send to the winner.
		for (SArmy currA : winner.getArmies()) {
			currA.setBV(0);
			CampaignMain.cm.toUser("PL|SAD|" + currA.toString(true, "%"), currA.getPlayerName(), false);
			CampaignMain.cm.getOpsManager().checkOperations(currA, true);
		}

		// start scrap thread
		if (scrapThreads.containsKey(winName)) {
			Integer maxScrapPay = unitCosts.get(winName);
			if (maxScrapPay == null || maxScrapPay < 0)
				maxScrapPay = new Integer(0);
			scrapThreads.get(winName).setMaxPayment(maxScrapPay);
			scrapThreads.get(winName).start();
			if (CampaignMain.cm.getOpsManager().getScrapThreads().containsKey(
					winName))
				CampaignMain.cm.getOpsManager().getScrapThreads().get(winName)
						.stopScrap();

			// move the thread to the manager
			CampaignMain.cm.getOpsManager().getScrapThreads().put(winName,
					scrapThreads.remove(winName));
		}

		winner.resetWeightedArmyNumber();

		// set immunity && make unbusy
		CampaignMain.cm.getIThread().addImmunePlayer(winner);
		if (so.isFromReserve())
			winner.setFighting(false, true);// return AFR players to reserve
		else
			winner.setFighting(false);

		/*
		 * Servers with AR generally force people to deactivate and repair. We
		 * set immunity above, despite the fact that players will never need it
		 * to avoid games, so they can scrap units without cost and reset in
		 * SOL.
		 */
		if (!so.isFromReserve()
				&& CampaignMain.cm.getBooleanConfig("ForcedDeactivation")) {
			winner.setActive(false);
			CampaignMain.cm.toUser(
							"You've left the front lines to repair and refit, and are now in reserve.",
							winner.getName());
		}

		// send the status update to all players
		CampaignMain.cm.sendPlayerStatusUpdate(winner, true);

		/*
		 * Send the message to the loser/disconnector. The player is offline, so
		 * their status and immunity time are not concerns at the moment.
		 */
		String loseName = loser.getName().toLowerCase();
		toSend = "You were disconnected too long. GAME RESOLVED AUTOMATICALLY.<br>"
				+ metaStrings.get(loseName)
				+ unitStrings.get(loseName)
				+ payStrings.get(loseName);// + longStrings.get(loseName);
		CampaignMain.cm.toUser(toSend, loseName, true);

		winner.setSave();
		loser.setSave();
		// stick the result into the human readable result log, per RFE1479311.
		MWServ.mwlog.resultsLog(toSend);

		/*
		 * Set the finished strings for the ShortOperation.
		 */
		so.setCompleteFinishedInfo(completeFinishedInfoString);
		so.setIncompleteFinishedInfo(incompleteFinishedInfoString);

		/*
		 * Set the game to finished staus and bump the game counter.
		 */
		so.changeStatus(ShortOperation.STATUS_FINISHED);
		CampaignMain.cm.addGamesCompleted(1);

		// send to cyclops, if using Guibod-style info tracking
		if (CampaignMain.cm.isUsingCyclops())
			CampaignMain.cm.getMWCC().opConclude(so);

		// send to news feed, if the server ops believe this is a "meaningful"
		// game
		if (o.getBooleanValue("ReportOpToNewsFeed"))
			CampaignMain.cm.addToNewsFeed(newsFeedTitle, newsFeedBody);
		winner.checkForPromotion();
		winner.setSave();
		loser.checkForPromotion();
		loser.setSave();
		
	}

	/**
	 * Method which determines the payment which should be given to each player,
	 * adjusts the payment for salvage costs, and triggers the pay of techs.
	 */
	private void assemblePaymentStrings(Operation o, ShortOperation so,
			SPlayer disconnector) {

		// create the tree
		payStrings = new TreeMap<String, String>();

		// load the base payments. these will be used as starting points for
		// players.
		int baseAttackerMoneyPay = o.getIntValue("BaseAttackerPayCBills");
		int baseAttackerFluPay = o.getIntValue("BaseAttackerPayInfluence");

		int baseDefenderMoneyPay = o.getIntValue("BaseDefenderPayCBills");
		int baseDefenderFluPay = o.getIntValue("BaseDefenderPayInfluence");

		int baseAttackerExperiencePay = o
				.getIntValue("BaseAttackerPayExperience");
		int baseDefenderExperiencePay = o
				.getIntValue("BaseDefenderPayExperience");

		int baseAttackerRewardPointsPay = o.getIntValue("RPForAttacker");
		int baseDefenderRewardPointsPay = o.getIntValue("RPForDefender");

		// adjust the base values for total BV, if configured to do so.
		int attackerTotalBVMoneyAdjustment = o
				.getIntValue("AttackerPayBVforCBill");
		if (attackerTotalBVMoneyAdjustment > 0)
			baseAttackerMoneyPay += Math.floor(so.getStartingBV()
					/ attackerTotalBVMoneyAdjustment);

		int defenderTotalBVMoneyAdjustment = o
				.getIntValue("DefenderPayBVforCBill");
		if (defenderTotalBVMoneyAdjustment > 0)
			baseDefenderMoneyPay += Math.floor(so.getStartingBV()
					/ defenderTotalBVMoneyAdjustment);

		// do the total influence adjustments
		int attackerTotalBVFluAdjustment = o
				.getIntValue("AttackerPayBVforInfluence");
		if (attackerTotalBVFluAdjustment > 0)
			baseAttackerFluPay += Math.floor(so.getStartingBV()
					/ attackerTotalBVFluAdjustment);

		int defenderTotalBVFluAdjustment = o
				.getIntValue("DefenderPayBVforInfluence");
		if (defenderTotalBVFluAdjustment > 0)
			baseDefenderFluPay += Math.floor(so.getStartingBV()
					/ defenderTotalBVFluAdjustment);

		// do the exp adjustments
		int attackerTotalBVXPAdjustment = o
				.getIntValue("AttackerPayBVforExperience");
		if (attackerTotalBVXPAdjustment > 0)
			baseAttackerExperiencePay += Math.floor(so.getStartingBV()
					/ attackerTotalBVXPAdjustment);

		int defenderTotalBVXPAdjustment = o
				.getIntValue("DefenderPayBVforExperience");
		if (defenderTotalBVXPAdjustment > 0)
			baseDefenderExperiencePay += Math.floor(so.getStartingBV()
					/ defenderTotalBVXPAdjustment);

		// Building Payments.
		// make sure that it wasn't a building map that they fought on and the
		// SO screwed up and set building ops happen.
		if (buildingsLeft > -1 && buildingsLeft <= so.getTotalBuildings()) {
			int totalBuildings = so.getTotalBuildings();
			int minBuildings = so.getMinBuildings();
			buildingsDestroyed = totalBuildings - buildingsLeft;

			// attacker won but didn't get to destroy all the buildings.
			if (attackerisWinner(so))
				buildingsDestroyed = Math.max(buildingsDestroyed, o
						.getIntValue("AttackerMinBuildingsIfAttackerWins"));

			// attacker only gets paid if they destroyed the minimum amount of
			// buildings.
			if (buildingsDestroyed >= minBuildings) {
				int attackerBuildingXPAdjustment = o
						.getIntValue("AttackerExpPerBuilding");
				if (attackerBuildingXPAdjustment != 0)
					baseAttackerExperiencePay += (attackerBuildingXPAdjustment * buildingsDestroyed);

				int attackerBuildingFluAdjustment = o
						.getIntValue("AttackerFluPerBuilding");
				if (attackerBuildingFluAdjustment != 0)
					baseAttackerFluPay += (attackerBuildingFluAdjustment * buildingsDestroyed);

				int attackerBuildingRPAdjustment = o
						.getIntValue("AttackerRPPerBuilding");
				if (attackerBuildingRPAdjustment != 0)
					baseAttackerRewardPointsPay += (attackerBuildingRPAdjustment * buildingsDestroyed);

				int attackerBuildingMoneyAdjustment = o
						.getIntValue("AttackerMoneyPerBuilding");
				if (attackerBuildingMoneyAdjustment != 0)
					baseAttackerMoneyPay += (attackerBuildingMoneyAdjustment * buildingsDestroyed);
			}// end if buildingsDestroyed !== minBuildings

			// defender gets paid if their are any buildings left
			int defenderBuildingXPAdjustment = o
					.getIntValue("DefenderExpPerBuilding");
			if (defenderBuildingXPAdjustment != 0)
				baseDefenderExperiencePay += (defenderBuildingXPAdjustment * buildingsLeft);

			int defenderBuildingFluAdjustment = o
					.getIntValue("DefenderFluPerBuilding");
			if (defenderBuildingFluAdjustment != 0)
				baseDefenderFluPay += (defenderBuildingFluAdjustment * buildingsLeft);

			int defenderBuildingRPAdjustment = o
					.getIntValue("DefenderRPPerBuilding");
			if (defenderBuildingRPAdjustment != 0)
				baseDefenderRewardPointsPay += (defenderBuildingRPAdjustment * buildingsLeft);

			int defenderBuildingMoneyAdjustment = o
					.getIntValue("DefenderMoneyPerBuilding");
			if (defenderBuildingMoneyAdjustment != 0)
				baseDefenderMoneyPay += (defenderBuildingMoneyAdjustment * buildingsLeft);
		}

		/*
		 * Loop through all players in the set. Draw a name from the keyset and
		 * get the latest copy of the SPlayer from campaign main. This is less
		 * than ideal. Once we can guarantee that all SPlayers are updated as
		 * players leave/join. we can use the players directly.
		 * 
		 * Loading ops values in the loop looks inefficient; however, for 1 v 1
		 * games in loop loads are actually more efficient than preloading -
		 * youre going to take a combination of 2 payment paths one time each,
		 * not all 4. Break even in team 2 v 2's, would be better off
		 * prefetching with large melees.
		 */
		int discoPayPercent = CampaignMain.cm
				.getIntegerConfig("DisconnectionPayPercentage");
		for (String currName : allPlayers.keySet()) {

			// load the player
			SPlayer currP = CampaignMain.cm.getPlayer(currName);
			// Reset all players Team Number
			CampaignMain.cm.toUser("PL|STN|" + -1, currName, false);

			/*
			 * If the player disconnected, tell him that he gets no payment or
			 * reduced payment, then continue normally.
			 */
			if (disconnector != null
					&& disconnector.getName().toLowerCase().equals(currName)) {

				// if the disco pay is 0, no pay
				String modString = "";
				if (discoPayPercent <= 0)
					modString = "You received no pay for the game because you disconnected.";
				else if (discoPayPercent > 0)
					modString = "You received reduced pay for the game because you disconnected.";

				if (discoPayPercent > 100)
					discoPayPercent = 100;

				payStrings.put(currName, modString);
			}

			// payment amounts
			int earnedMoney = 0;
			int earnedFlu = 0;
			int earnedXP = 0;
			int earnedRP = 0;

			/*
			 * Check to see if all should be paid as so.getWinners(). Note that
			 * salvage has already happened, as have meta impacts (% exchanges
			 * and unit thefts, for example). At thie point it is safe to move
			 * ALL so.getLosers() into the so.getWinners() tree for payment
			 * purposes.
			 */
			boolean payAllAsWinners = o.getBooleanValue("PayAllAsWinners");
			if (payAllAsWinners) {
				so.getWinners().putAll(so.getLosers());
				so.getLosers().clear();
			}

			/*
			 * Payment breaks into 4 primary situations, each of which has its
			 * own configurable modifiers.
			 * 
			 * 1 - winning attacker 2 - winning defender 3 - losing attacker 4 -
			 * losing defender
			 * 
			 * First determine whether player is an attacker or defender and set
			 * the base. Then check his winner/loser status and modify his
			 * payments.
			 */

			// if it was a building op report the damage done.
			String buildingReport = "";
			if (so.getAttackers().containsKey(currName)) {

				// set base pay
				earnedMoney += baseAttackerMoneyPay;
				earnedFlu += baseAttackerFluPay;
				earnedXP += baseAttackerExperiencePay;
				earnedRP += baseAttackerRewardPointsPay;

				if (so.getWinners().containsKey(currName)) {

					earnedRP += o.getIntValue("RPForWinner");
					earnedMoney += o
							.getIntValue("AttackerWinModifierCBillsFlat");
					earnedFlu += o
							.getIntValue("AttackerWinModifierInfluenceFlat");
					earnedXP += o
							.getIntValue("AttackerWinModifierExperienceFlat");

					// check percent modifiers
					double moneyMod = 1 + o
							.getDoubleValue("AttackerWinModifierCBillsPercent") / 100;
					double fluMod = 1 + o
							.getDoubleValue("AttackerWinModifierInfluencePercent") / 100;
					double expMod = 1 + o
							.getDoubleValue("AttackerWinModifierExperiencePercent") / 100;
					if (moneyMod > 0)
						earnedMoney *= moneyMod;
					if (fluMod > 0)
						earnedFlu *= fluMod;
					if (expMod > 0)
						earnedXP *= expMod;

				} else if (so.getLosers().containsKey(currName)) {

					earnedRP += o.getIntValue("RPForLoser");
					earnedMoney -= o
							.getIntValue("AttackerLossModifierCBillsFlat");
					earnedFlu -= o
							.getIntValue("AttackerLossModifierInfluenceFlat");
					earnedXP -= o
							.getIntValue("AttackerLossModifierExperienceFlat");

					// check percent modifiers
					double moneyMod = 1 - o
							.getDoubleValue("AttackerLossModifierCBillsPercent") / 100;
					double fluMod = 1 - o
							.getDoubleValue("AttackerLossModifierInfluencePercent") / 100;
					double expMod = 1 - o
							.getDoubleValue("AttackerLossModifierExperiencePercent") / 100;
					if (moneyMod > 0)
						earnedMoney *= moneyMod;
					if (fluMod > 0)
						earnedFlu *= fluMod;
					if (expMod > 0)
						earnedXP *= expMod;

					if (o.getBooleanValue("OnlyGiveRPtoWinners"))
						earnedRP = 0;
				}
				if (buildingsLeft > -1) {
					int totalBuildings = so.getTotalBuildings();
					int minBuildings = so.getMinBuildings();

					if (buildingsDestroyed == 0)
						buildingReport = "<b>Mission Report: Failed</b><br>You did not manage to destroy a single enemy facility!<br>";
					else if (buildingsDestroyed == totalBuildings)
						buildingReport = "<b>Mission Report: Success</b><br>You managed to destroy all of the enemies facilities!<br>";
					else if (buildingsDestroyed >= minBuildings)
						buildingReport = "<b>Mission Report: Success</b><br>You managed to destroy "
								+ buildingsDestroyed
								+ " out of "
								+ totalBuildings + " facilities!<br>";
					else
						buildingReport = "<b>Mission Report: Failed</b><br>You only managed to destroy "
								+ buildingsDestroyed
								+ " out of "
								+ totalBuildings + " facilities!<br>";
				}
			}// end if(is attacker)

			else if (so.getDefenders().containsKey(currName)) {

				// set base pay
				earnedMoney += baseDefenderMoneyPay;
				earnedFlu += baseDefenderFluPay;
				earnedXP += baseDefenderExperiencePay;
				earnedRP += baseDefenderRewardPointsPay;

				if (so.getWinners().containsKey(currName)) {

					earnedRP += o.getIntValue("RPForWinner");
					earnedMoney += o
							.getIntValue("DefenderWinModifierCBillsFlat");
					earnedFlu += o
							.getIntValue("DefenderWinModifierInfluenceFlat");
					earnedXP += o
							.getIntValue("DefenderWinModifierExperienceFlat");

					// check percent modifiers
					double moneyMod = 1 + o
							.getDoubleValue("DefenderWinModifierCBillsPercent") / 100;
					double fluMod = 1 + o
							.getDoubleValue("DefenderWinModifierInfluencePercent") / 100;
					double expMod = 1 + o
							.getDoubleValue("DefenderWinModifierExperiencePercent") / 100;
					if (moneyMod > 0)
						earnedMoney *= moneyMod;
					if (fluMod > 0)
						earnedFlu *= fluMod;
					if (expMod > 0)
						earnedXP *= expMod;

				} else if (so.getLosers().containsKey(currName)) {

					earnedRP += o.getIntValue("RPForLoser");
					earnedMoney -= o
							.getIntValue("DefenderLossModifierCBillsFlat");
					earnedFlu -= o
							.getIntValue("DefenderLossModifierInfluenceFlat");
					earnedXP -= o
							.getIntValue("DefenderLossModifierExperienceFlat");

					// check percent modifiers
					double moneyMod = 1 - o
							.getDoubleValue("DefenderLossModifierCBillsPercent") / 100;
					double fluMod = 1 - o
							.getDoubleValue("DefenderLossModifierInfluencePercent") / 100;
					double expMod = 1 - o
							.getDoubleValue("DefenderLossModifierExperiencePercent") / 100;
					if (moneyMod > 0)
						earnedMoney *= moneyMod;
					if (fluMod > 0)
						earnedFlu *= fluMod;
					if (expMod > 0)
						earnedXP *= expMod;

					if (o.getBooleanValue("OnlyGiveRPtoWinners"))
						earnedRP = 0;
				}

				if (buildingsLeft > -1) {
					int totalBuildings = so.getTotalBuildings();
					int minBuildings = so.getMinBuildings();

					if (buildingsDestroyed == 0)
						buildingReport = "<b>Mission Report: Success</b><br>You to stop the enemy from destroing any facilites!<br>";
					else if (buildingsDestroyed == totalBuildings)
						buildingReport = "<b>Mission Report: Failed</b><br>You managed to allow the enemy to destroy all our facilites!<br>";
					else if (buildingsDestroyed >= minBuildings)
						buildingReport = "<b>Mission Report: Failed</b><br>You managed to allow the enemy to destroy "
								+ buildingsDestroyed
								+ " out of "
								+ totalBuildings + " facilities!<br>";
					else
						buildingReport = "<b>Mission Report: Success</b><br>The enemy destroyed only "
								+ buildingsDestroyed
								+ " out of "
								+ totalBuildings + " facilities!<br>";
				}

			}// end else if(is defender)

			/*
			 * Adjust the payments for disconnection status, if necessary.
			 */
			if (disconnector != null
					&& disconnector.getName().toLowerCase().equals(currName)) {
				earnedMoney = (earnedMoney * discoPayPercent) / 100;
				earnedFlu = (earnedFlu * discoPayPercent) / 100;
				earnedXP = (earnedXP * discoPayPercent) / 100;
				earnedRP = (earnedRP * discoPayPercent) / 100;
			}

			/*
			 * Now that we know what homeboy is getting paid, we need to check
			 * eligibility for payment (SOL games) and adjust the cash for
			 * repair and salvage costs.
			 */
			int salvageAndRepairCosts = 0;
			if (unitCosts.containsKey(currName)
					&& !CampaignMain.cm.isUsingAdvanceRepair())
				salvageAndRepairCosts = unitCosts.get(currName);

			// load his loss compensation, if any.
			int battleLossCompensation = 0;
			if (lossCompensation.containsKey(currName))
				battleLossCompensation = lossCompensation.get(currName);

			// Check to make sure the battle did enough damage to reward
			// everything.
			// Only the loser is affected and, for the time being, only money is
			// reduced.
			double minBVDifference = o
					.getDoubleValue("MinBVDifferenceForFullPay");
			/*MWServ.mwlog.debugLog("Loser BV: Current: "+currentBV+" Starting BV: "+loserBV+" minBVDiff: "+minBVDifference);
			MWServ.mwlog.debugLog("Total BV Lost: "+(1.0 - ((double) currentBV / (double) loserBV)));
			MWServ.mwlog.debugLog("Money Earned: "+earnedMoney);*/
			if (minBVDifference > 0
					&& (1.0 - ((double) currentBV / (double) loserBV)) < minBVDifference
					&& disconnector == null
					&& so.getLosers().containsKey(currName)) {
				int minBVvPenaltyMod = o
						.getIntValue("BVFailurePaymentModifier");
				earnedMoney = (earnedMoney * minBVvPenaltyMod) / 100;
				// earnedFlu = (earnedFlu * minBVvPenaltyMod)/100;
				// earnedXP = (earnedXP * minBVvPenaltyMod)/100;
				// earnedRP = (earnedRP * minBVvPenaltyMod)/100;
			}
			//MWServ.mwlog.debugLog("Money Earned: "+earnedMoney);

			/*
			 * Determine how much to play the players technicians (or, if using
			 * AR, the bay rental fee).
			 */
			int techPayment = 0;
			if (o.getBooleanValue("PayTechsForGame"))
				techPayment = currP.getCurrentTechPayment();

			/*
			 * Put together the actual string to show our bold hero, and add the
			 * earned amounts to their accounts.
			 */
			StringBuilder toSave = new StringBuilder();

			// Prepend the building report, if there is one.
			if (buildingReport.length() > 1)
				toSave.append(buildingReport);

			// Don't allow 0 base pay
			earnedMoney = Math.max(earnedMoney, 0);

			int actualPay = 0;
			String techFiringWarning = "";
			String unmaintainedUnitWarning = "";

			boolean hasMoneyGain = false;// CBills
			boolean hasOtherGain = false;// XP, Flu, RP

			if (earnedMoney > 0 || salvageAndRepairCosts > 0
					|| battleLossCompensation > 0 || techPayment > 0) {

				// if salvage costs are greater than the amount earned, reduce
				// them.
				if (salvageAndRepairCosts > 0
						&& salvageAndRepairCosts > earnedMoney) {
					salvageAndRepairCosts = earnedMoney;
					unitCosts.put(currName, salvageAndRepairCosts);
				}

				// combine earned money, loss compensation and salvage costs.
				actualPay = earnedMoney + battleLossCompensation
						- salvageAndRepairCosts;

				// check to see if the player can afford to pay his techs.
				int availMoney = Math.max(currP.getMoney() + actualPay,0);

				/*
				 * If the tech payment is greater than the amount the player has
				 * available, including both the pay from this game and pre-game
				 * funds, some of the techs will quit or bays will be seized by
				 * their owners.
				 */
				int techsLost = 0;
				if (techPayment > availMoney) {
					techsLost = currP.doFireUnpaidTechnicians(techPayment
							- availMoney);
					techPayment = availMoney;
				}

				// Put together a notification string.
				if (techsLost > 0) {
					techFiringWarning += "You weren't able to pay all of your technicians. ";
					techFiringWarning += (techsLost == 1) ? "One" : techsLost;
					techFiringWarning += " quit in protest.";
				}

				/*
				 * If the player loses techs, set some units to be unmaintained
				 * and store a warning string to add to the final message.
				 */
				if (techsLost > 0 && currP.getTechnicians() < 0) {

					int numberUnmaintained = currP.setRandomUnmaintained();
					if (numberUnmaintained > 0) {

						unmaintainedUnitWarning = "<i><u>WARNING:</u></i> ";
						unmaintainedUnitWarning += (numberUnmaintained == 1) ? " A unit is now"
								: " Units are now";

						unmaintainedUnitWarning += " unmaintained. You must [<a href=\"MEKWARS/c deactivate\">DEACTIVATE</a>] in order to prevent";
						unmaintainedUnitWarning += (numberUnmaintained == 1) ? " an autoscrap."
								: " autoscraps.";
					}
				}

				// subtract tech payment from actual pay
				actualPay -= techPayment;

				toSave.append("Net Pay: "
						+ CampaignMain.cm.moneyOrFluMessage(true, false,
								actualPay, true) + " (");
				toSave.append("Gross Pay: "
						+ CampaignMain.cm.moneyOrFluMessage(true, false,
								earnedMoney, true) + ", ");
				if (battleLossCompensation > 0)
					toSave.append("Battle Loss Comp: +"
							+ CampaignMain.cm.moneyOrFluMessage(true, false,
									battleLossCompensation) + ", ");
				if (salvageAndRepairCosts > 0)
					toSave.append("Salvage & Repair: -"
							+ CampaignMain.cm.moneyOrFluMessage(true, false,
									salvageAndRepairCosts) + ", ");
				if (techPayment > 0 && CampaignMain.cm.isUsingAdvanceRepair())
					toSave.append("Bay Rental: -"
							+ CampaignMain.cm.moneyOrFluMessage(true, false,
									techPayment) + ", ");
				else if (techPayment > 0)
					toSave.append("Techs: -"
							+ CampaignMain.cm.moneyOrFluMessage(true, false,
									techPayment) + ", ");

				int commaIndex = toSave.lastIndexOf(", ");
				toSave = toSave.replace(commaIndex, commaIndex + 2, ")");

				currP.addMoney(actualPay);
				hasMoneyGain = true;
			}

			StringBuilder tempBuilder = new StringBuilder();
			if (earnedFlu > 0) {
				tempBuilder.append(CampaignMain.cm.moneyOrFluMessage(false,
						false, earnedFlu));
				currP.addInfluence(earnedFlu);
				hasOtherGain = true;
			}

			if (earnedXP > 0) {

				if (hasOtherGain)
					tempBuilder.append(", ");

				tempBuilder.append(earnedXP + "XP");
				currP.addExperience(earnedXP, false);
				checkMercContracts(currP, ContractInfo.CONTRACT_EXP, earnedXP);
				hasOtherGain = true;
			}

			if (earnedRP > 0) {

				if (hasOtherGain)
					tempBuilder.append(", ");

				tempBuilder.append(earnedRP + "RP");
				currP.addReward(earnedRP);
				hasOtherGain = true;
			}

			// add XP/Flu/RP messages toSave.
			if (tempBuilder.length() > 0) {

				tempBuilder.append(".");

				// try to remove the last instance of ", "
				int lastComma = tempBuilder.lastIndexOf(", ");
				if (lastComma >= 0)
					tempBuilder.replace(lastComma, lastComma + 1, " and");

				toSave.append("<br>You earned " + tempBuilder.toString());
			}

			if (hasMoneyGain || hasOtherGain) {

				// add link to use RP, if earned.
				if (earnedRP > 0)
					toSave.append(" [<a href=\"MWUSERP\">Use RP</a>]");

				// add firing and maint info
				if (techFiringWarning.length() > 0)
					toSave.append("<br>" + techFiringWarning);
				if (unmaintainedUnitWarning.length() > 0)
					toSave.append("<br>" + unmaintainedUnitWarning);

				// set the player's payString
				payStrings.put(currName, toSave.toString());
			}

			else {// no money, XP, Flu or RP so set a "no pay" message
				payStrings
						.put(currName,
								"You received no pay (money, experience, etc.) for this game.");
			}

		}// end for(each player name)

		/*
		 * Ratings should only change in 1 vs 1 games.
		 */
		boolean opEffectsElo = o.getBooleanValue("CountGameForRanking");
		if (allPlayers.size() == 2 && opEffectsElo && !drawGame) {

			SPlayer wp = so.getWinners().get(so.getWinners().firstKey());
			SPlayer lp = so.getLosers().get(so.getLosers().firstKey());

			double oldWinnerRating = wp.getRating();
			double oldLoserRating = lp.getRating();

			/*
			 * Tasks used to give a larger rating change for big games, but it
			 * was an arbitrary unit count ...
			 * 
			 * TODO: Re-approach weighting.
			 */
			wp.setRating(ELORanking.getNewRatingWinner(oldWinnerRating,
					oldLoserRating, 8));
			lp.setRating(ELORanking.getNewRatingLoser(oldWinnerRating,
					oldLoserRating, 8));

			if (!CampaignMain.cm.getBooleanConfig("HideELO")) {

				DecimalFormat myFormatter = new DecimalFormat("###.##");
				double winnerRating = (wp.getRating() - oldWinnerRating);
				double loserRating = (lp.getRating() - oldLoserRating);
				String winRatingStr = myFormatter.format(winnerRating);
				String LoseRatingStr = myFormatter.format(loserRating);

				String winToUser = "Your new Rating is "
						+ wp.getRatingRounded() + " (+" + winRatingStr + ")";
				String loseToUser = "Your new Rating is "
						+ lp.getRatingRounded() + " (" + LoseRatingStr + ")";

				// get the old strings
				String ls = payStrings.get(lp.getName().toLowerCase());
				String ws = payStrings.get(wp.getName().toLowerCase());

				// pass the rating strings on, and store
				payStrings.put(lp.getName().toLowerCase(), loseToUser + "<br>"
						+ ls);
				payStrings.put(wp.getName().toLowerCase(), winToUser + "<br>"
						+ ws);
			}
		}// end if(should adjust ELO)

		/*
		 * Add clickable defection link for SOL players, if they have enough XP
		 * to leave the training faction.
		 */
		for (String currName : allPlayers.keySet()) {

			SPlayer currP = CampaignMain.cm.getPlayer(currName);
			if (!currP.getMyHouse().isNewbieHouse())
				continue;

			if (currP.getExperience() >= CampaignMain.cm
					.getIntegerConfig("MinEXPforDefecting")) {
				String oldString = payStrings
						.get(currP.getName().toLowerCase());
				String toAdd = "You have enough XP to leave the training faction. Click to [<a href=\"MWSOLDEFECT\">defect</a>].";
				payStrings.put(currP.getName().toLowerCase(), oldString
						+ "<br>" + toAdd);
			}
		}

	}// end assemblePaymentStrings

	/**
	 * Method which determines player salvage and generates unique strings.
	 * Salvage costs are also determined and saved here; however, they are saved
	 * for use in the mathod which generates payStrings rather than shown
	 * directly to the players.
	 * 
	 * Determination as to which units are salvageable, which pilots survived
	 * the game, etc. should already have been made by one of two methods: -
	 * possibleSalvageFromReport(), or - possibleSalvageFromDisconnection
	 */
	private void assembleSalvageStrings(Operation o, ShortOperation so) {

		// set up the units cost tree and string tree
		unitStrings = new TreeMap<String, String>();
		unitCosts = new TreeMap<String, Integer>();
		lossCompensation = new TreeMap<String, Integer>();
		scrapThreads = new TreeMap<String, OpsScrapThread>();

		/*
		 * Immediately check to see if this is a no-destruction game. If so,
		 * move ALL units from salvaged into living and clear out the pilots
		 * hash.
		 */
		if (o.getBooleanValue("NoDestructionMode")) {
			livingUnits.putAll(salvagableUnits);
			livingUnits.putAll(destroyedUnits);
			salvagableUnits.clear();
			destroyedUnits.clear();
			pilots.clear();
		}

		/*
		 * First, dispose of the living units. Iterate through the survivors,
		 * adding experience to each and trying to level up those which belong
		 * to so.getWinners().
		 */
		for (OperationEntity currEntity : livingUnits.values()) {

			// load the player and unit
			String ownerName = currEntity.getOwnerName().toLowerCase();
			SPlayer owner = CampaignMain.cm.getPlayer(ownerName);

			/*
			 * Null owners boggle my mind. Lets throw an error and look for some
			 * kind of pattern. Skip to the next unit and allow normal
			 * resolution.
			 */
			if (owner == null) {
				MWServ.mwlog
						.errLog("Null _owner_ while processing post-game salvage for "
								+ " Attack #"
								+ so.getShortID()
								+ ". Needed to find Player: "
								+ ownerName
								+ " Unit #"
								+ currEntity.getID()
								+ "/Type: "
								+ currEntity.getType());
				continue;
			}

			SUnit currU = owner.getUnit(currEntity.getID());

			// apply battle damage if there is any to be applied.
			try {
				if (!o.getBooleanValue("NoDestructionMode")
						&& currEntity.getUnitDamage().trim().length() > 0)
					UnitUtils.applyBattleDamage(currU.getEntity(), currEntity
							.getUnitDamage(), false);
			} catch (Exception ex) {
				MWServ.mwlog.errLog("Unable to apply damage to unit "
						+ currU.getModelName());
				MWServ.mwlog.errLog(ex);
			}

			// If damaged is transfered from Game to campaign then save it the
			// pilot
			if (CampaignMain.cm.getBooleanConfig("AllowPilotDamageToTransfer")) {
				if (o.getBooleanValue("NoDestructionMode")) {
					currU.getPilot().setHits(0);
					currEntity.setPilothits(0);
				} else {
					int hits = CampaignMain.cm
							.getIntegerConfig("AmountOfDamagePerPilotHit")
							* currEntity.getPilothits();
					currU.getPilot().setHits(hits);
				}
			}

			String append = calculatePilotEXP(o, so, currEntity, owner, true);
			if (unitStrings.containsKey(ownerName)) {
				String s = unitStrings.get(ownerName);
				unitStrings.put(ownerName, s + "The " + currU.getModelName()
						+ " survived the battle" + append + ".<br>");
			} else {
				unitStrings.put(ownerName, "The " + currU.getModelName()
						+ " survived the battle" + append + ".<br>");
			}

			// we may assume that a living unit earned kills, XP, or -somehow-
			// changed. the number
			// of units whic hlive through a game without any change in their
			// info is exceedingly
			// small. As such, send a unit update.
			CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|"
					+ currU.toString(true), ownerName, false);

		}// end foreach(living unit)

		/*
		 * Now that we've handled the living units, look at the salvageable
		 * units and determine who ends up with the unit. Need to forumulate 2
		 * or 3 Strings. 1 to tell the original player what happened to the
		 * unit, 1 to tell all other players what happened to the unit, and 1
		 * (if the owner is not the same as the original owner) to tell the new
		 * owner of his prize ...
		 * 
		 * Also, salvaged units *may* earn kills if their pilots are still alive
		 * and they are returned to their original owners. In this case, the
		 * pilots get kill XP, but do not gain any game-play XP.
		 */
		boolean winnerAlwaysSalvagesOwn = o
				.getBooleanValue("WinnerAlwaysSalvagesOwnUnits");

		int winnerSalvagePercent = 0;
		int salvageAdjustment = 0;
		if (attackersWon) {
			winnerSalvagePercent = o.getIntValue("BaseAttackerSalvagePercent");
			salvageAdjustment = o.getIntValue("AttackerSalvageAdjustment");
		} else {
			winnerSalvagePercent = o.getIntValue("BaseDefenderSalvagePercent");
			salvageAdjustment = o.getIntValue("DefenderSalvageAdjustment");
		}

		for (OperationEntity currEntity : salvagableUnits.values()) {

			// load the original player and the unit
			String oldOwnerName = currEntity.getOwnerName().toLowerCase();
			SPlayer oldOwner = CampaignMain.cm.getPlayer(oldOwnerName);
			SUnit currU = oldOwner.getUnit(currEntity.getID());

			// apply battle damage if there is any to be applied.
			try {
				if (currEntity.getUnitDamage().trim().length() > 0)
					UnitUtils.applyBattleDamage(currU.getEntity(), currEntity
							.getUnitDamage(), false);
			} catch (Exception ex) {
				MWServ.mwlog.errLog("Unable to apply damage to unit "
						+ currU.getModelName());
				MWServ.mwlog.errLog(ex);
			}

			/*
			 * See the comments for setupPilotString for array information. This
			 * is a fragile array - needs specific objects in specific indices,
			 * or reporting will fail.
			 */
			Object[] pilotinformation = this.setupPilotStringForUnit(
					currEntity, currU, so.getTargetWorld());

			/*
			 * If the so.getWinners() always recover their own salvage, pull it
			 * from the queue and send a recovery message to the original owner.
			 * Also check to see if the pilot survived.
			 */
			if (winnerAlwaysSalvagesOwn
					&& so.getWinners().containsKey(oldOwnerName)) {

				String toOwner = " You recovered your " + currU.getModelName()
						+ ". ";
				String toOthers = oldOwner.getColoredName() + " recovered his "
						+ currU.getModelName() + ". ";

				/*
				 * Check the pilot's death. If he passed away, call the death
				 * handler and append the result to the owners' pilotString.
				 */
				Boolean pilotLived = (Boolean) pilotinformation[0];
				if (!pilotLived.booleanValue()) {
					String deathString = this.handleDeadPilot(oldOwner, currU,
							currEntity, so);
					String workingS = (String) pilotinformation[1];
					pilotinformation[1] = workingS + deathString;
				}

				/*
				 * loop through all players, adding the unit's outcome to their
				 * salvage message trees. Send the owner his own message and set
				 * the other message for remaining players. Even though this is
				 * a winner salvaging his unit, we need to check captors - a
				 * pilot may have been picked up by an enemy on the field.
				 */
				for (String currName : allPlayers.keySet()) {

					SPlayer captor = (SPlayer) pilotinformation[4];
					String toSet = toOthers + pilotinformation[3];// default
																	// to
																	// otherstring
					if (currName.equals(oldOwnerName))
						toSet = toOwner + pilotinformation[1];// set the
																// "owner"
																// string
					else {
						if (captor != null
								&& currName.equals(captor.getName()
										.toLowerCase()))
							toSet = toOthers + pilotinformation[2];
						else
							toSet = toOthers + pilotinformation[3];
					}

					// if already part of the tree
					if (unitStrings.containsKey(currName)) {
						String workingS = unitStrings.get(currName);
						unitStrings.put(currName, workingS + toSet + "<br>");
					}

					// put the name in the tree
					else
						unitStrings.put(currName, toSet + "<br>");

				}// end foreach(name in allplayers)

				/*
				 * Add the cost to the player's tree.
				 */
				int costToRepair = getSalvageCost(oldOwner, currU, currEntity,
						o, so);

				if (costToRepair < 0 || CampaignMain.cm.isUsingAdvanceRepair())
					costToRepair = 0;

				if (unitCosts.containsKey(oldOwnerName)) {
					Integer oldCost = unitCosts.get(oldOwnerName);
					unitCosts.put(oldOwnerName, oldCost + costToRepair);
				} else {
					unitCosts.put(oldOwnerName, costToRepair);
				}

				/*
				 * Check to see if the player already has a scrap thread. If so,
				 * add this unit as an entry. If not, create a new
				 * OpsScrapThread for the player.
				 */
				if (scrapThreads.containsKey(oldOwnerName)) {
					scrapThreads.get(oldOwnerName).addScrappableUnit(
							currU.getId(), costToRepair);
				} else {
					OpsScrapThread ost = new OpsScrapThread(oldOwnerName);
					ost.addScrappableUnit(currU.getId(), costToRepair);
					scrapThreads.put(oldOwnerName, ost);
				}

				// winner recovered own unit. send update.
				CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|"
						+ currU.toString(true), oldOwnerName, false);

				/*
				 * Nothing more to do with this particular unit in the for
				 * each(OperationEntity : salvagable) loop. continue to next
				 * unit.
				 */
				continue;

			}// end for winnerSalvage

			/*
			 * Either the salvage is mixed (such that defenders can gain
			 * attackers units, even in a loss, much to Kai's consternation), or
			 * we're dealing with a loser's unit. Either way, make a roll to
			 * determine who gains the unit itself.
			 */

			// Holder. Put a new owner here, or leave
			// null if original player recovers unit
			SPlayer newOwner = null;

			// roll is under winner %. winner gets the unit.
			if (CampaignMain.cm.getRandomNumber(100) < winnerSalvagePercent) {

				// previous owner wasn't a winner. pick a new owner
				if (!so.getWinners().containsKey(oldOwnerName))
					newOwner = selectRandomWinner();

				// decrease the so.getWinners() chance to get the next unit
				if (salvageAdjustment != 0)
					winnerSalvagePercent -= salvageAdjustment;
				if (winnerSalvagePercent < 0)
					winnerSalvagePercent = 0;
			}

			// roll is under loser % and loser gets the unit.
			else {

				// previous owner wasnt a loser. pick a new owner.
				if (!so.getLosers().containsKey(oldOwnerName))
					newOwner = selectRandomLoser();

				// increase the winner's chance to get next unit
				if (salvageAdjustment != 0)
					winnerSalvagePercent += salvageAdjustment;
				if (winnerSalvagePercent > 100)
					winnerSalvagePercent = 100;
			}

			/*
			 * Check the pilot's death. If he passed away, add the relevant info
			 * to the owner's string.
			 * 
			 * Only check for a replacement pilot if the unit is returning to
			 * its original owner!
			 */
			Boolean pilotLived = (Boolean) pilotinformation[0];
			if (!pilotLived.booleanValue() && newOwner == null) {
				String deathString = this.handleDeadPilot(oldOwner, currU,
						currEntity, so);
				String workingS = (String) pilotinformation[1];
				workingS += deathString;
			}

			// If the pilot is alive, and has no unit ...
			if (pilotLived.booleanValue() && newOwner != null)
				this.handleDispossesedPilot(oldOwner, currU);

			// setup the strings to show players
			String toOriginalOwner = "";
			String toNewOwner = "";
			String toOthers = "";

			/*
			 * If the new owner is null, we know that the unit was returned to
			 * its previous owner.
			 */
			if (newOwner == null) {
				toOriginalOwner = " You recovered your " + currU.getModelName()
						+ ". ";
				toOthers = oldOwner.getColoredName() + " recovered his "
						+ currU.getModelName() + ". ";
			} else {
				toOriginalOwner = " " + newOwner.getColoredName()
						+ " recovered your " + currU.getModelName() + ". ";
				toNewOwner = " You recovered " + oldOwner.getColoredName()
						+ "'s " + currU.getModelName() + ". ";
				toOthers = " " + newOwner.getColoredName() + " recovered "
						+ oldOwner.getColoredName() + "'s "
						+ currU.getModelName() + ". ";
			}

			/*
			 * Add the salvage strings to the tree.
			 */
			for (String currName : allPlayers.keySet()) {

				/*
				 * determine which string to send. default to the "other"
				 * message, but check to see if the name is the same as the
				 * owner.
				 */
				SPlayer captor = (SPlayer) pilotinformation[4];
				String toSet = toOthers + pilotinformation[3];// default to
																// otherstring
				if (currName.equals(oldOwnerName))
					toSet = toOriginalOwner + pilotinformation[1];// set the
																	// "owner"
																	// string
				else if (newOwner != null
						&& currName.equals(newOwner.getName().toLowerCase())) {
					if (captor != null
							&& currName.equals(captor.getName().toLowerCase()))
						toSet = toNewOwner + pilotinformation[2];
					else
						toSet = toNewOwner + pilotinformation[3];
				} else {
					if (captor != null
							&& currName.equals(captor.getName().toLowerCase()))
						toSet = toOthers + pilotinformation[2];
					else
						toSet = toOthers + pilotinformation[3];
				}

				// if already part of the tree
				if (unitStrings.containsKey(currName)) {
					String workingS = unitStrings.get(currName);
					unitStrings.put(currName, workingS + toSet + "<br>");
				}

				// put the name in the tree
				else
					unitStrings.put(currName, toSet + "<br>");

			}// end foreach(name in allplayers)

			/*
			 * If the newowner is not null, move the unit from its old master to
			 * the new. This will clear out the pilot if PPQs are in place.
			 * 
			 * If PPQs are not being used, get a new pilot for the unit out of
			 * the newOwner's house queue.
			 * 
			 * No PL|UU sent - unit is sent in string form by SPlayer.addUnit()
			 */
			if (newOwner != null) {

				// check for compensation from the original owner's faction.
				int compensation = determineLossCompensation(oldOwner, currU,
						true);
				if (compensation > 0) {
					if (lossCompensation.containsKey(oldOwnerName)) {
						Integer oldCost = lossCompensation.get(oldOwnerName);
						lossCompensation.put(oldOwnerName, oldCost
								+ compensation);
					} else {
						lossCompensation.put(oldOwnerName, compensation);
					}
				}

				// note change of ownership on cyclops
				if (CampaignMain.cm.isUsingCyclops())
					CampaignMain.cm.getMWCC().unitChangeOwnerShip(
							Integer.toString(currU.getId()),
							newOwner.getName(),
							newOwner.getMyHouse().getName(),
							so.getOpCyclopsID(), "Salvage");

				// do the actual unit move
				oldOwner.removeUnit(currU.getId(), false);
				String newPilot = this.handleDeadPilot(newOwner, currU,
						currEntity, so);
				newOwner.addUnit(currU, true);// add unit is same as a PL|UU
												// in most respects ...

				// add new pilot info to this unit's string
				String workingS = unitStrings.get(newOwner.getName()
						.toLowerCase());
				workingS = workingS.substring(0, workingS.length() - 4);// remove
																		// "<br>"
				unitStrings.put(newOwner.getName().toLowerCase(), workingS
						+ newPilot + "<br>");
			}

			/*
			 * Old owner. If the pilot lived throughthe game and grant him his
			 * kill XP, but do not attempt to level up.
			 */
			else {// old owner

				// if the pilot lived, check for kills.
				if (pilotLived) {
					String append = calculatePilotEXP(o, so, currEntity,
							oldOwner, false);
					if (unitStrings.containsKey(oldOwnerName)) {
						String s = unitStrings.get(oldOwnerName);
						unitStrings.put(oldOwnerName, s + "The "
								+ currU.getModelName() + " survived the battle"
								+ append + ".<br>");
					} else {
						unitStrings.put(oldOwnerName, "The "
								+ currU.getModelName() + " survived the battle"
								+ append + ".<br>");
					}
				}// end if(pilot lived)

				// whether pilot lived or not, send a PL|UU
				CampaignMain.cm.toUser("PL|UU|" + currU.getId() + "|"
						+ currU.toString(true), oldOwnerName, false);

			}// end else (newowner == null, so stay with old)

			/*
			 * Add the cost to the player's tree.
			 */
			int costToRepair = 0;
			String nameKey = "";

			if (newOwner != null) {
				costToRepair = getSalvageCost(newOwner, currU, currEntity, o,
						so);
				nameKey = newOwner.getName().toLowerCase();
			} else {
				costToRepair = getSalvageCost(oldOwner, currU, currEntity, o,
						so);
				nameKey = oldOwnerName;
			}

			// don't charge if using AR, or allow negative repair fees
			if (costToRepair < 0 || CampaignMain.cm.isUsingAdvanceRepair())
				costToRepair = 0;

			// add to unit costs tree
			if (unitCosts.containsKey(nameKey)) {
				Integer cost = unitCosts.get(nameKey);
				cost += costToRepair;
			} else {
				unitCosts.put(nameKey, costToRepair);
			}

			// add to scrap threads
			if (scrapThreads.containsKey(nameKey)) {
				scrapThreads.get(nameKey).addScrappableUnit(currU.getId(),
						costToRepair);
			} else {
				OpsScrapThread ost = new OpsScrapThread(nameKey);
				ost.addScrappableUnit(currU.getId(), costToRepair);
				scrapThreads.put(nameKey, ost);
			}

		}// end foreach(savlageable unit)

		/*
		 * Finally, and most pleasuably, we get to DESTROY things!
		 */
		for (OperationEntity currEntity : destroyedUnits.values()) {

			// load the original player and the unit
			String oldOwnerName = currEntity.getOwnerName().toLowerCase();
			SPlayer oldOwner = CampaignMain.cm.getPlayer(oldOwnerName);
			SUnit currU = oldOwner.getUnit(currEntity.getID());

			String append = calculatePilotEXP(o, so, currEntity, oldOwner,
					false);
			// setup string.
			String toOwner = "Your " + currU.getModelName()
					+ " was utterly destroyed " + append;
			String toOthers = oldOwner.getColoredName() + "'s "
					+ currU.getModelName() + " was utterly destroyed. ";

			// we know the unit is obliterated. how about the pilot?
			Object[] pilotinformation = this.setupPilotStringForUnit(
					currEntity, currU, so.getTargetWorld());

			/*
			 * Check the pilot's death. If he passed away, do nothing. There's
			 * no unit to put a new pilot into.
			 * 
			 * If the pilot lived, put him in the house queue or the PPQ, as
			 * appropriate.
			 */
			Boolean pilotLived = (Boolean) pilotinformation[0];
			if (pilotLived.booleanValue())
				this.handleDispossesedPilot(oldOwner, currU);

			/*
			 * Added name of player and unit that destroyed unit for better
			 * tracking on cyclops. MM does report this and we can use it.
			 * @torren
			 */
			if (CampaignMain.cm.isUsingCyclops())
				CampaignMain.cm.getMWCC().unitDestroy(
						Integer.toString(currU.getId()), "cored",
						so.getOpCyclopsID(), "", "");

			// check for compensation from the faction.
			int compensation = determineLossCompensation(oldOwner, currU, false);
			if (compensation > 0) {
				if (lossCompensation.containsKey(oldOwnerName)) {
					Integer oldCost = lossCompensation.get(oldOwnerName);
					lossCompensation.put(oldOwnerName, oldCost + compensation);
				} else {
					lossCompensation.put(oldOwnerName, compensation);
				}
			}

			// remove the unit from the player's hangar.
			oldOwner.removeUnit(currU.getId(), false);
			// Remove it from the database
			if (CampaignMain.cm.isUsingMySQL())
				CampaignMain.cm.MySQL.deleteUnit(currU.getDBId());

			/*
			 * Tell the player if his pilot was killed, saved or captured.
			 * Others are not informed of events, but Captors are given info on
			 * their pickups.
			 */
			for (String currName : allPlayers.keySet()) {

				/*
				 * determine which string to send. default to the "other"
				 * message, but check to see if the name is the same as the
				 * owner.
				 */
				SPlayer captor = (SPlayer) pilotinformation[4];
				String toSet = toOthers + pilotinformation[3];// default to
																// otherstring
				if (currName.equals(oldOwnerName))
					toSet = toOwner + pilotinformation[1];// set the "owner"
															// string
				else {
					if (captor != null
							&& currName.equals(captor.getName().toLowerCase()))
						toSet = toOthers + pilotinformation[2];
					else
						toSet = toOthers + pilotinformation[3];
				}

				// if already part of the tree
				if (unitStrings.containsKey(currName)) {
					String workingS = unitStrings.get(currName);
					unitStrings.put(currName, workingS + toSet + "<br>");
				}

				// put the name in the tree
				else
					unitStrings.put(currName, toSet + "<br>");

			}// end foreach(name in allplayers)

		}// end foreach(destroyed unit)
	}// end this.assembleSalvageStrings()

	/**
	 * Method which generates general info strings (who attacked whom, which
	 * faction won, etc) for the players and a finished status description for
	 * public viewing.
	 * 
	 * After these general strings are put together, the need to check Operation
	 * values and apply meta effects (% transfers, unit thefts, etc) is checked.
	 * 
	 * If the given ShortOperation is capable of impacting factions (isn't a
	 * mult-faction melee, isn't tied to a long operation), the necessary values
	 * are loaded from the passed Operation and checked/applied.
	 * 
	 * Some have commented that this is too procedural and not very OO; however,
	 * it works. Anyone who cares to beautify the process is more than welcome
	 * to do so.
	 * 
	 * @urgru 8.26.05
	 */
	private void assembleDescriptionStrings(Operation o, ShortOperation so) {

		try {// main try for the whole function
			// setup metas
			metaStrings = new TreeMap<String, String>();

			/*
			 * Before we make strings, we need to determine the outcomes.
			 * Determine whether the attacking or defending team was the winner.
			 */
			SPlayer aWinner = so.getWinners().get(so.getWinners().firstKey());
			SPlayer aLoser = so.getLosers().get(so.getLosers().firstKey());
			// Draw Game
			if (drawGame) {
				attackersWon = false;
				defendersWon = false;
			} else if (attackerisWinner(so)) {
				attackersWon = true;
				defendersWon = false;
			} else {
				defendersWon = true;
				attackersWon = false;
			}

			/*
			 * Throw the entire meta-outcome out if the winner or defender trees
			 * include players from multiple factions - that would mean it was
			 * an arena or a duel.
			 * 
			 * These loops are heinously inefficient - (O)n^n - but we can
			 * pretty safely assume that n will never be larger than 4.
			 */
			boolean attackersPolluted = false;
			boolean defendersPolluted = false;
			if (!drawGame) {
				for (SPlayer wp : so.getWinners().values()) {
					for (SPlayer compareP : so.getWinners().values()) {
						if (!compareP.getHouseFightingFor().equals(
								wp.getHouseFightingFor()))
							attackersPolluted = true;
					}
					if (attackersPolluted) {
						break;
					}
				}
			}
			for (SPlayer lp : so.getLosers().values()) {
				for (SPlayer compareP : so.getLosers().values()) {
					if (!compareP.getHouseFightingFor().equals(
							lp.getHouseFightingFor())) {
						defendersPolluted = true;
						break;
					}
					if (defendersPolluted) {
						break;
					}
				}
			}

			/*
			 * If there is pollution of the lists (not mono-faction) put
			 * together a melee string which shows the "teams" in play.
			 * 
			 * The code below is actually very simple, although the loops used
			 * to generate some of the strings don't read well and make it
			 * overly long.
			 * 
			 * NOTE: Complete and Incomplete info for Polluted games is
			 * IDENTICAL b/c there is no faction affiliation to attach to the
			 * outcome.
			 */
			if (attackersPolluted || defendersPolluted) {

				try {

					/*
					 * Set up the finished info. Template: "Player A defeated
					 * Player B and Player C (Arena)."
					 * 
					 * Also set up the generic strings at this time. The
					 * generics are used when generating the messages sent to
					 * individual players. Template: "You defeated Player B and
					 * Player C (Arena)", where "Player B and Player C" is the
					 * genetic loser string.
					 * 
					 * In p
					 */
					String genericWinnerString = "";
					int marker = 0;
					int total = so.getWinners().size();
					if (!drawGame) {
						for (SPlayer wp : so.getWinners().values()) {
							genericWinnerString += wp.getName();
							marker++;
							if (marker + 1 == total)
								genericWinnerString += " and ";
							else if (marker + 1 < total)
								genericWinnerString += ", ";
						}
					}

					completeFinishedInfoString += genericWinnerString;
					completeFinishedInfoString += " defeated ";

					String genericLoserString = "";
					marker = 0;
					total = so.getLosers().size();
					for (SPlayer lp : so.getLosers().values()) {
						genericLoserString += lp.getName();
						marker++;
						if (marker + 1 == total)
							genericLoserString += " and ";
						else if (marker + 1 < total)
							genericLoserString += ", ";
					}

					completeFinishedInfoString += genericLoserString + " ("
							+ so.getName() + ").<br>";
					newsFeedTitle = completeFinishedInfoString.substring(0,
							completeFinishedInfoString.indexOf("<br>"));
					newsFeedBody = newsFeedTitle;

					/*
					 * Because the winner/loser lists are polluted, we need to
					 * show all of the players names instead of simply showing
					 * the victorious faction. This requires some unappealing
					 * embedded loops.
					 */
					int numberOfWinners = so.getWinners().size();
					if (!drawGame) {
						for (SPlayer wp : so.getWinners().values()) {

							// if only one winner, send him a singular victory
							// message.
							if (numberOfWinners == 1)
								metaStrings.put(wp.getName().toLowerCase(),
										"You have defeated "
												+ genericLoserString + " ("
												+ so.getName() + ").<br>");

							// else, send a plural message. ugly, but we need to
							// not send the player his own name.
							else {
								int i = 0;
								int numToList = numberOfWinners - 1;
								String toSet = "You worked with ";
								for (SPlayer loopP : so.getWinners().values()) {
									if (!wp.equals(loopP)) {
										toSet += loopP.getName();
										i++;
										if (i + 1 == numToList)
											toSet += " and ";
										else if (marker + 1 < numToList)
											toSet += ", ";
									} else {
										i++;
									}
								}

								toSet += " to defeat " + genericLoserString
										+ " (" + so.getName() + ").";
								metaStrings.put(wp.getName().toLowerCase(),
										toSet);
							}// end else(multiple so.getWinners()/plural
								// victory string)
						}// end (foreach winner)
					}
					/*
					 * Although the loser metastrings should show complete
					 * lists, same as the so.getWinners(), "you worked with
					 * player B to get your ass kicked" sounds wrong. Need to
					 * give this some thought and revise with a clearer message.
					 * 
					 * For now, just say "You were defeated..."
					 */
					for (SPlayer lp : so.getLosers().values()) {
						if (drawGame) {
							String toSet = "The match on "
									+ so.getTargetWorld()
											.getNameAsColoredLink()
									+ " ended in a draw! (" + so.getName()
									+ ").<br>";
							metaStrings.put(lp.getName().toLowerCase(), toSet);
						} else
							metaStrings.put(lp.getName().toLowerCase(),
									"You were defeated by "
											+ genericWinnerString + " ("
											+ so.getName() + ").<br>");
					}

					// copy the complete finished info to the incomplete, as
					// they should be the same in a polluted game
					incompleteFinishedInfoString = completeFinishedInfoString;

					/*
					 * We don't want polluted games to have any impact on the
					 * player factions. Return.
					 */
					return;
				} catch (Exception ex) {
					MWServ.mwlog.errLog(ex);
				}
			}// end if(polluted)

			/*
			 * If the winner and loser trees are nor polluted, generate 1 v 1 or
			 * faction strings, as appropriate. These are similar to the
			 * polluted meta strings; however, faction names replace individual
			 * player names in large games.
			 * 
			 * Templates: "You defeated Player A on Solaris VII (TestAttack)."
			 * "Your team was defeated by Davion on New Avalon (Conquer)." "Your
			 * team defeated Liao on Sian (Raid)."
			 * 
			 * These should be fleshed out with more descriptive verbage in the
			 * future; however, this should suffice for the time being ...
			 */
			try {
				int numWinners = so.getWinners().size();
				int numLosers = so.getLosers().size();
				if (!drawGame) {
					for (SPlayer wp : so.getWinners().values()) {

						String toSet = "";
						if (numWinners > 1)
							toSet += "Your team ";
						else
							toSet += "You ";

						toSet += " defeated ";

						if (numLosers > 1)
							toSet += aLoser.getHouseFightingFor()
									.getColoredNameAsLink();
						else
							toSet += aLoser.getColoredName()
									+ aLoser.getHouseFightingFor()
											.getColoredAbbreviation(true);

						toSet += " on "
								+ so.getTargetWorld().getNameAsColoredLink()
								+ " (" + so.getName() + ").";
						metaStrings.put(wp.getName().toLowerCase(), toSet
								+ "<br>");
					}
				}

				for (SPlayer lp : so.getLosers().values()) {

					String toSet = "";
					if (drawGame) {
						toSet += "The game ended in a draw!<br>";
						toSet += so.getTargetWorld().getNameAsColoredLink()
								+ " (" + so.getName() + ").";
					} else {
						if (numLosers > 1)
							toSet += "Your team was defeated by ";
						else
							toSet += "You were defeated by ";

						if (numWinners > 1)
							toSet += aWinner.getHouseFightingFor()
									.getColoredNameAsLink();
						else
							toSet += aWinner.getColoredName()
									+ aWinner.getHouseFightingFor()
											.getColoredAbbreviation(true);

						toSet += " on "
								+ so.getTargetWorld().getNameAsColoredLink()
								+ " (" + so.getName() + ").";
					}
					metaStrings.put(lp.getName().toLowerCase(), toSet + "<br>");
				}

				/*
				 * Set up the beginning of the finishedInfo string. If the short
				 * has no long-backing, actual meta info will be tacked onto
				 * this.
				 * 
				 * Use a faction name in lieu of a player name in team games, or
				 * if the "ShowCompleteGameInfoInNews" option is disabled.
				 */
				if (numWinners > 1
						|| !CampaignMain.cm
								.getBooleanConfig("ShowCompleteGameInfoInNews")) {
					completeFinishedInfoString += aWinner.getHouseFightingFor()
							.getColoredNameAsLink();
					incompleteFinishedInfoString += aWinner
							.getHouseFightingFor().getColoredNameAsLink();
					newsFeedBody = aWinner.getHouseFightingFor().getName();
					newsFeedTitle = aWinner.getHouseFightingFor().getName();
				} else {
					completeFinishedInfoString += aWinner.getColoredName()
							+ aWinner.getHouseFightingFor()
									.getColoredAbbreviation(true);
					incompleteFinishedInfoString += aWinner
							.getHouseFightingFor().getColoredNameAsLink();
					newsFeedBody = aWinner.getName() + "["
							+ aWinner.getHouseFightingFor().getAbbreviation()
							+ "]";
					newsFeedTitle = aWinner.getName() + "["
							+ aWinner.getHouseFightingFor().getAbbreviation()
							+ "]";
				}

				newsFeedTitle += " defeats ";

				String completeLoserTemp = "";
				String incompleteLoserTemp = "";
				String newsFeedTemp = "";
				if (numLosers > 1
						|| !CampaignMain.cm
								.getBooleanConfig("ShowCompleteGameInfoInNews")) {
					completeLoserTemp += aLoser.getHouseFightingFor()
							.getColoredNameAsLink();
					incompleteLoserTemp += aLoser.getHouseFightingFor()
							.getColoredNameAsLink();
					newsFeedTemp = aLoser.getHouseFightingFor().getName();
					newsFeedTitle += aLoser.getHouseFightingFor().getName();
				} else {
					completeLoserTemp += aLoser.getColoredName()
							+ aLoser.getHouseFightingFor()
									.getColoredAbbreviation(true);
					incompleteLoserTemp += aLoser.getHouseFightingFor()
							.getColoredNameAsLink();
					newsFeedTemp = aLoser.getName() + "["
							+ aLoser.getHouseFightingFor().getAbbreviation()
							+ "]";
					newsFeedTitle += aLoser.getName() + "["
							+ aLoser.getHouseFightingFor().getAbbreviation()
							+ "]";
				}

				if (numLosers > 1 && attackersWon) {
					completeFinishedInfoString += " defeated"
							+ completeLoserTemp + "defenders";
					incompleteFinishedInfoString += " defeated"
							+ incompleteLoserTemp + "defenders";
					newsFeedBody += " defeated" + newsFeedTemp + "defenders";
				} else if (numLosers == 1 && attackersWon) {
					completeFinishedInfoString += " attacked and defeated "
							+ completeLoserTemp;
					incompleteFinishedInfoString += " attacked and defeated "
							+ incompleteLoserTemp;
					newsFeedBody += " attacked and defeated " + newsFeedTemp;
				} else if (numLosers > 1 && defendersWon) {
					completeFinishedInfoString += " defeated "
							+ completeLoserTemp + " attackers";
					incompleteFinishedInfoString += " defeated "
							+ incompleteLoserTemp + " attackers";
					newsFeedBody += " defeated " + newsFeedTemp + " attackers";
				} else {// 1 loser, defender won
					completeFinishedInfoString += " defeated an attack by "
							+ completeLoserTemp;
					incompleteFinishedInfoString += " defeated an attack by "
							+ incompleteLoserTemp;
					newsFeedBody += " defeated an attack by " + newsFeedTemp;
				}

				completeFinishedInfoString += " on "
						+ so.getTargetWorld().getNameAsColoredLink() + " ("
						+ so.getName() + ").<br>";
				incompleteFinishedInfoString += " on "
						+ so.getTargetWorld().getNameAsColoredLink() + " ("
						+ so.getName() + ").<br>";

				newsFeedBody += " on " + so.getTargetWorld().getName() + " ("
						+ so.getName() + ").";
				newsFeedTitle += " on " + so.getTargetWorld().getName();
			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

			/*
			 * If this is a component of a long operation, there are no
			 * post-game faction impacts. The status of the long op to which the
			 * ShortOperation must be bound isn't check until after salvage
			 * resolution.
			 */
			if (o.getTypeIndicator() == Operation.TYPE_SHORTANDLONG)
				return;

			/*
			 * Games with polluted lists (arena matches) have been returned, as
			 * have games which are components of LongOperations. Any ShortOp
			 * which remains at this point may be configured to have
			 * faction-impacts.
			 * 
			 * Its entirely possible that none will be enabled (ie - 1 v 1 duel
			 * for cash/ELO); however, still need to check and apply if
			 * necessary.
			 * 
			 * Check to see whether the attacking group or defending group won.
			 * Only load the variables from Operation that we actually need.
			 */
			// metastring is sent to each player, and appended
			// to finishedstring, unless hasLoss is false...
			try {
				boolean hasLoss = false;

				// caps are used by both attackers and defenders, so load them
				// here
				int conquestCap = o.getIntValue("ConquestAmountCap");
				int delayCap = o.getIntValue("DelayAmountCap");
				int ppCaptureCap = o.getIntValue("PPCapptureCap");
				int unitCaptureCap = o.getIntValue("UnitCaptureCap");

				// need winner and loser strings to set in tree
				String winnerMetaString = "";
				String loserMetaString = "";

				// queue updates to send to houses that may be affected by the
				// results.
				StringBuilder winnerHSUpdates = new StringBuilder();
				StringBuilder loserHSUpdates = new StringBuilder();

				// save the planet
				SPlanet target = so.getTargetWorld();

				// ATTACKER VICTORY BLOCK
				if (attackersWon) {
					try {
						/*
						 * Determine the amount of land which should change
						 * hands as a result of the Operation. Check to make
						 * sure there is actually land to gain - an attacker who
						 * already owns 100% of a world should gain nothing.
						 */
						int totalConquest = o
								.getIntValue("AttackerBaseConquestAmount");
						int conquestUnitAdjust = o
								.getIntValue("AttackerConquestUnitAdjustment");
						int conquestBVAdjust = o
								.getIntValue("AttackerConquestBVAdjustment");
						if (conquestUnitAdjust > 0)
							totalConquest += Math.floor(so.getStartingUnits()
									/ conquestUnitAdjust);
						if (conquestBVAdjust > 0)
							totalConquest += Math.floor(so.getStartingBV()
									/ conquestBVAdjust);
						if (totalConquest > conquestCap)
							totalConquest = conquestCap;

						// Drop % to 0 if either group is non-conquer
						if (!aWinner.getMyHouse().isConquerable()
								|| !aLoser.getMyHouse().isConquerable())
							totalConquest = 0;

						// Drop to 0 is the planet is non-conquerable
						if (!so.getTargetWorld().isConquerable())
							totalConquest = 0;

						// make the % adjustment. This zeros totalConquest if
						// there is nothing to gain.
						totalConquest = so.getTargetWorld().doGainInfluence(
								aWinner.getHouseFightingFor(),
								aLoser.getHouseFightingFor(), totalConquest,
								false);

						if (totalConquest > 0) {
							hasLoss = true;
							winnerMetaString += " gained " + totalConquest
									+ " points of "
									+ target.getNameAsColoredLink();
							loserMetaString += " lost " + totalConquest
									+ " points  of "
									+ target.getNameAsColoredLink();
							checkMercContracts(aWinner,
									ContractInfo.CONTRACT_LAND, totalConquest);
						}

						/*
						 * Determine the amount of delay to apply to the
						 * facilities on world. Note that delay is applied to
						 * *all* facilities under Operations, whereas under
						 * Tasks is was applied to one random facility on world.
						 */
						int totalDelay = o
								.getIntValue("AttackerBaseDelayAmount");
						int delayUnitAdjust = o
								.getIntValue("AttackerDelayUnitAdjustment");
						int delayBVAdjust = o
								.getIntValue("AttackerDelayBVAdjustment");
						if (delayUnitAdjust > 0)
							totalDelay += Math.floor(so.getStartingUnits()
									/ delayUnitAdjust);
						if (delayBVAdjust > 0)
							totalDelay += Math.floor(so.getStartingBV()
									/ delayBVAdjust);

						// Building delays for each one the attacker destroyed.
						if (buildingsLeft > -1
								&& buildingsLeft <= so.getTotalBuildings()) {
							if (attackerisWinner(so))
								buildingsDestroyed = Math
										.max(
												buildingsDestroyed,
												o
														.getIntValue("AttackerMinBuildingsIfAttackerWins"));

							if (buildingsDestroyed >= so.getMinBuildings()) {
								int buildingDestructionDelayAmount = o
										.getIntValue("DelayPerBuilding");
								if (buildingDestructionDelayAmount != 0)
									totalDelay += buildingDestructionDelayAmount
											* buildingsDestroyed;
							}

						}

						// not allowed to delay or steal from factories the
						// defender doesn't own
						if (so.getTargetWorld().getOwner() == null
								|| !so.getTargetWorld().getOwner().getName()
										.equalsIgnoreCase(
												aLoser.getMyHouse().getName())) {
							totalDelay = 0;
						}

						if (totalDelay > delayCap)
							totalDelay = delayCap;

						// if no facilities exist to delay, reduce the total to
						// 0.
						if (target.getFactoryCount() == 0)
							totalDelay = 0;

						// do the delay and string setup
						if (totalDelay > 0) {
							Iterator i = target.getUnitFactories().iterator();
							while (i.hasNext()) {
								SUnitFactory currFacility = (SUnitFactory) i
										.next();
								loserHSUpdates.append(currFacility.addRefresh(
										totalDelay, false));
							}

							if (hasLoss) {
								winnerMetaString += ",";
								loserMetaString += ",";
							}
							winnerMetaString += " stopped production on "
									+ target.getNameAsColoredLink() + " for "
									+ totalDelay + " miniticks";
							loserMetaString += " allowed "
									+ aWinner.getHouseFightingFor()
											.getColoredNameAsLink()
									+ " to stop production on "
									+ target.getNameAsColoredLink() + " for "
									+ totalDelay + " miniticks";
							hasLoss = true;
							checkMercContracts(aWinner,
									ContractInfo.CONTRACT_DELAY, totalDelay);
						}

						/*
						 * Determine the number of units stolen. Move the units
						 * between the houses, but also save their model info in
						 * an ArrayList of Strings. The AList will be used later
						 * to generate messages for players and the
						 * finishedInfo.
						 * 
						 * Operations gives all stolen units to the house. None
						 * go to the player. This is a major deviation from the
						 * old Task system; however, giving units to the players
						 * makes very little sense when payments can be
						 * increased and games can result in several different
						 * kinds of outcomes.
						 * 
						 * Also - If there are no units of a proper type (ie -
						 * no factory making units on the world or no units of
						 * types which factories DO make), little it taken.
						 * Rollover curtailments are in place only
						 * ShortOperations. LongOps make provision for rollover
						 * plus-ups, such that the class/type of units taken is
						 * increased if none remain in the target type. This is
						 * to discourage factions from clearing out bays in
						 * response to a multi-game theft operation - not a
						 * situation which arises frequently with shorts. This,
						 * they have downward/reduced spillover.
						 */
						int unitsToCapture = o
								.getIntValue("AttackerBaseUnitsTaken");
						int unitUnitAdjust = o
								.getIntValue("AttackerUnitsUnitAdjustment");
						int unitBVAdjust = o
								.getIntValue("AttackerUnitsBVAdjustment");
						
						if (unitUnitAdjust > 0)
							unitsToCapture += Math.floor(so.getStartingUnits()
									/ unitUnitAdjust);
						if (unitBVAdjust > 0)
							unitsToCapture += Math.floor(so.getStartingBV()
									/ unitBVAdjust);
						if (unitsToCapture > unitCaptureCap)
							unitsToCapture = unitCaptureCap;

						ArrayList<SUnit> capturedUnits = new ArrayList<SUnit>();
						if (unitsToCapture > 0) {

							// for (all unit types, mek preferred)
							int numCaptured = 0;

							/*
							 * Try every factory on the world, at random, until
							 * we've taken what we can. This may mean getting
							 * inf or vehs on a planet than can produce assault
							 * mechs.
							 */
							ArrayList<SUnitFactory> factoriesSearched = new ArrayList<SUnitFactory>();
							while (factoriesSearched.size() < target
									.getFactoryCount()) {

								// get a random factory
								SUnitFactory currFacility = target
										.getRandomUnitFactory();

								// if we've already searched this factory
								// before, skip
								if (factoriesSearched.contains(currFacility))
									continue;

								// we've not searched the facility before. add
								// it to the searchlist
								factoriesSearched.add(currFacility);

								// get the factory's weightclass, then try
								// all types in order of preference.
								int currWeight = currFacility.getWeightclass();

								for (int type = Unit.MEK; type <= Unit.BATTLEARMOR; type++) {

									// skip this type if the facility cannot
									// produce
									if (!currFacility.canProduce(type))
										continue;

									boolean noUnits = false;
									while (!noUnits
											&& numCaptured < unitsToCapture) {
										SUnit captured = aLoser
												.getHouseFightingFor()
												.getEntity(currWeight, type);
										if (captured == null)
											noUnits = true;
										else {
											capturedUnits.add(captured);
											loserHSUpdates.append(aLoser
													.getHouseFightingFor()
													.getHSUnitRemovalString(
															captured));
											winnerHSUpdates.append(aWinner
													.getHouseFightingFor()
													.addUnit(captured, false));
											numCaptured++;
										}
									}// end while(units remain in this
										// factories' pool)
								}// end for(all types)

							}// end while(factories remain)

							// add to metaString if anything was actually taken
							if (numCaptured >= 1) {

								// setup leadin
								if (hasLoss) {
									winnerMetaString += ",";
									loserMetaString += ",";
								}
								hasLoss = true;

								// get the units
								String unitString = "";
								for (SUnit currU : capturedUnits) {
									unitString += currU.getModelName() + ", ";
								}

								// strip the last ", "
								unitString = unitString.substring(0, unitString
										.length() - 2);

								if (numCaptured == 1) {
									winnerMetaString += " captured a unit ["
											+ unitString + "]";
									loserMetaString += " lost a unit ["
											+ unitString + "]";
								} else {
									winnerMetaString += " captured "
											+ numCaptured + " units ["
											+ unitString + "]";
									loserMetaString += " lost " + numCaptured
											+ " units [" + unitString + "]";
								}
							}
							checkMercContracts(aWinner,
									ContractInfo.CONTRACT_UNITS, numCaptured);
						}// end if(unitsToCapture > 0)


						/*
						 * Force factories to build units 
						 */
						
						unitsToCapture = o
								.getIntValue("AttackerBaseFactoryUnitsTaken");
						unitUnitAdjust = o
								.getIntValue("AttackerFactoryUnitsUnitAdjustment");
						unitBVAdjust = o
								.getIntValue("AttackerFactoryUnitsBVAdjustment");

						if (unitUnitAdjust > 0)
							unitsToCapture += Math.floor(so.getStartingUnits()
									/ unitUnitAdjust);
						if (unitBVAdjust > 0)
							unitsToCapture += Math.floor(so.getStartingBV()
									/ unitBVAdjust);
						if (unitsToCapture > unitCaptureCap)
							unitsToCapture = unitCaptureCap;

						capturedUnits = new ArrayList<SUnit>();
						if (unitsToCapture > 0) {

							// for (all unit types, mek preferred)
							int numCaptured = 0;

							/*
							 * Try every factory on the world, at random, until
							 * we've taken what we can. This may mean getting
							 * inf or vehs on a planet than can produce assault
							 * mechs.
							 */
							ArrayList<UnitFactory> factoriesSearched = new ArrayList<UnitFactory>(target.getUnitFactories());
							while (factoriesSearched.size() > 0) {

								// get a random factory
								SUnitFactory currFacility = (SUnitFactory)factoriesSearched.remove(CampaignMain.cm.getRandomNumber(factoriesSearched.size()));

								// if we've already searched this factory
								/* before, skip
								if (factoriesSearched.contains(currFacility))
									continue;

								// we've not searched the facility before. add
								// it to the searchlist
								factoriesSearched.add(currFacility);
								*/
								for (int type = Unit.MEK; type <= Unit.BATTLEARMOR; type++) {

									// skip this type if the facility cannot
									// produce
									if (!currFacility.canProduce(type))
										continue;

									boolean noUnits = false;
									while (!noUnits && numCaptured < unitsToCapture) {
										SPilot pilot = new SPilot("Vacant",99,99);
										SUnit captured = currFacility.getMechProduced(type,pilot);
										if (captured == null)
											noUnits = true;
										else {
											capturedUnits.add(captured);
											loserHSUpdates.append(aLoser
													.getHouseFightingFor()
													.getHSUnitRemovalString(
															captured));
											winnerHSUpdates.append(aWinner
													.getHouseFightingFor()
													.addUnit(captured, false));
											numCaptured++;
										}
									}// end while(units remain in this
									// factories' pool)
								}// end for(all types)

							}// end while(factories remain)

							// add to metaString if anything was actually taken
							if (numCaptured >= 1) {

								// setup leadin
								if (hasLoss) {
									winnerMetaString += ",";
									loserMetaString += ",";
								}
								hasLoss = true;

								// get the units
								String unitString = "";
								for (SUnit currU : capturedUnits) {
									unitString += currU.getModelName() + ", ";
								}

								// strip the last ", "
								unitString = unitString.substring(0, unitString
										.length() - 2);

								if (numCaptured == 1) {
									winnerMetaString += " force produced a unit ["
											+ unitString + "]";
									loserMetaString += " had a unit force produced ["
											+ unitString + "]";
								} else {
									winnerMetaString += " force produced "
											+ numCaptured + " units ["
											+ unitString + "]";
									loserMetaString += " had " + numCaptured
											+ " units force produced [" + unitString + "]";
								}
							}
							checkMercContracts(aWinner,ContractInfo.CONTRACT_UNITS, numCaptured);
						}// end if(unitsToCapture > 0)
				
				/*
						 * Determine the number and type of components to steal
						 * from target faction. Much like Unit theft, this is
						 * handled in a different manner than it was under the
						 * Task system.
						 * 
						 * If a defined target is given (specific factory), take
						 * only from that target. If nothing is there, no
						 * rollover. If no factory is given, select random
						 * factories until the PP amount is reached or all
						 * options from the world are exhausted.
						 */
						double ppToCaptureDoub = o.getDoubleValue("AttackerBasePPAmount");
						double ppUnitAdjust = o.getDoubleValue("AttackerPPUnitAdjustment");
						double ppBVAdjust = o.getDoubleValue("AttackerPPBVAdjustment");

						if ( ppUnitAdjust > 0 )
							ppToCaptureDoub *= so.getStartingUnits() / ppUnitAdjust;
						
						if ( ppBVAdjust > 0 )
							ppToCaptureDoub *= so.getStartingBV() / ppBVAdjust;

						// convert ppToCapture into an int
						int ppToCapture = (int) Math.min(ppToCaptureDoub,ppCaptureCap);

						// not allowed to delay or steal from factories the
						// defender doesn't own
						if (so.getTargetWorld().getOwner() == null
								|| !so.getTargetWorld().getOwner().getName()
										.equalsIgnoreCase(
												aLoser.getMyHouse().getName())) {
							ppToCapture = 0;
						}

						/*
						 * Try every factory on the world, at random, until
						 * we've taken what we can. This may mean getting inf or
						 * veh pp on a planet than can produce meks.
						 * 
						 * The while loop terminates without running if the
						 * ppToCapture is 0.
						 */
						int ppCaptured = 0;
						ArrayList<UnitFactory> factoriesSearched = new ArrayList<UnitFactory>(target.getUnitFactories());
						while (factoriesSearched.size() > 0
								&& ppCaptured < ppToCapture) {

							// get a random factory
							SUnitFactory currFacility = (SUnitFactory)factoriesSearched.remove(CampaignMain.cm.getRandomNumber(factoriesSearched.size()));

							// if we've already searched this factory before,
							// skip
							//if (factoriesSearched.contains(currFacility))
								//continue;

							// searching the factory.
							//factoriesSearched.add(currFacility);

							// get the factory's weightclass, then try
							// all types in order of preference.
							int currWeight = currFacility.getWeightclass();

							for (int type = Unit.MEK; type <= Unit.BATTLEARMOR; type++) {

								// skip this type if the facility cannot produce
								if (!currFacility.canProduce(type))
									continue;

								boolean noPP = false;
								while (!noPP && ppCaptured < ppToCapture) {
									int ppAvailable = aLoser
											.getHouseFightingFor().getPP(
													currWeight, type);
									if (ppAvailable <= 0)
										noPP = true;
									else {
										int toTake = ppToCapture;
										if (ppToCapture > ppAvailable)
											toTake = ppAvailable;
										loserHSUpdates.append(aLoser
												.getHouseFightingFor().addPP(
														currWeight, type,
														-toTake, false));
										winnerHSUpdates.append(aWinner
												.getHouseFightingFor().addPP(
														currWeight, type,
														toTake, false));
										ppCaptured += toTake;

										if (hasLoss) {
											winnerMetaString += ",";
											loserMetaString += ",";
										}

										winnerMetaString += " stole "
												+ toTake
												+ " "
												+ Unit
														.getWeightClassDesc(currWeight)
												+ " "
												+ Unit.getTypeClassDesc(type)
												+ " components";
										loserMetaString += " lost "
												+ toTake
												+ " "
												+ Unit
														.getWeightClassDesc(currWeight)
												+ " "
												+ Unit.getTypeClassDesc(type)
												+ " components";
										hasLoss = true;
									}
								}// end while(units remain in this factories'
									// pool)
							}// end for(all types)
						}// end while(factories remain)
						checkMercContracts(aWinner,
								ContractInfo.CONTRACT_COMPONENTS, ppCaptured);

						// Start Destruction of Units and Components

						int unitDestructionCap = o.getIntValue("UnitDestructionCap");
						int unitsToDestroy = o.getIntValue("BaseUnitsDestroyed");
						int unitUnitDestroyAdjust = o.getIntValue("DestroyedUnitsBVAdjustment");
						int unitBVDestroyAdjust = o.getIntValue("DestroyedUnitsUnitAdjustment");

						if (unitUnitDestroyAdjust > 0 )
							unitsToDestroy += so.getStartingUnits() / unitUnitDestroyAdjust;
						
						if (unitBVDestroyAdjust > 0 )
							unitsToDestroy += so.getStartingBV() / unitBVDestroyAdjust;
						
						unitsToDestroy = Math.min(unitsToDestroy, unitDestructionCap);

						ArrayList<SUnit> opdestroyedUnits = new ArrayList<SUnit>();
						if (unitsToDestroy > 0) {

							// for (all unit types, mek preferred)
							int numDestroyed = 0;

							/*
							 * Try every factory on the world, at random, until
							 * we've taken what we can. This may mean getting
							 * inf or vehs on a planet than can produce assault
							 * mechs.
							 */
							factoriesSearched.clear();
							factoriesSearched.addAll(target.getUnitFactories());
							while (factoriesSearched.size() > 0 ) {

								// get a random factory
								SUnitFactory currFacility = (SUnitFactory)factoriesSearched.remove(CampaignMain.cm.getRandomNumber(factoriesSearched.size()));

								// if we've already searched this factory
								// before, skip
								//if (factoriesSearched.contains(currFacility))
									//continue;

								// we've not searched the facility before. add
								// it to the searchlist
								//factoriesSearched.add(currFacility);

								// get the factory's weightclass, then try
								// all types in order of preference.
								int currWeight = currFacility.getWeightclass();

								for (int type = Unit.MEK; type <= Unit.BATTLEARMOR; type++) {

									// skip this type if the facility cannot
									// produce
									if (!currFacility.canProduce(type))
										continue;

									boolean noUnits = false;
									while (!noUnits
											&& numDestroyed < unitsToDestroy) {
										SUnit destroyed = aLoser
												.getHouseFightingFor()
												.getEntity(currWeight, type);
										if (destroyed == null)
											noUnits = true;
										else {
											opdestroyedUnits.add(destroyed);
											loserHSUpdates.append(aLoser
													.getHouseFightingFor()
													.getHSUnitRemovalString(
															destroyed));
											// aWinner.getHouseFightingFor().addUnit(captured);
											numDestroyed++;
										}
									}// end while(units remain in this
										// factories' pool)
								}// end for(all types)

							}// end while(factories remain)

							// add to metaString if anything was actually taken
							if (numDestroyed >= 1) {

								// setup leadin
								if (hasLoss) {
									winnerMetaString += ",";
									loserMetaString += ",";
								}
								hasLoss = true;

								// get the units
								String unitString = "";
								for (SUnit currU : opdestroyedUnits) {
									unitString += currU.getModelName() + ", ";
								}

								// strip the last ", "
								unitString = unitString.substring(0, unitString
										.length() - 2);

								if (numDestroyed == 1) {
									winnerMetaString += " destroyed a unit ["
											+ unitString + "]";
									loserMetaString += " lost a unit ["
											+ unitString + "]";
								} else {
									winnerMetaString += " destroyed "
											+ numDestroyed + " units ["
											+ unitString + "]";
									loserMetaString += " lost " + numDestroyed
											+ " units [" + unitString + "]";
								}
							}
							checkMercContracts(aWinner,
									ContractInfo.CONTRACT_UNITS, numDestroyed);
						}// end if(unitsToDestroy > 0)

						/*
						 * Determine the number and type of components to
						 * destroy from target faction. Much like Unit
						 * destruction, this is handled in a different manner
						 * than it was under the Task system.
						 * 
						 * If a defined target is given (specific factory), take
						 * only from that target. If nothing is there, no
						 * rollover. If no factory is given, select random
						 * factories until the PP amount is reached or all
						 * options from the world are exhausted.
						 */
						double ppDestructionCap = o.getDoubleValue("PPDestructionCap");
						double ppToDestroyDoub = o.getDoubleValue("BasePPDestroyed");
						double ppUnitDestroyAdjust = o.getDoubleValue("DestroyedPPBVAdjustment");
						double ppBVDestroyAdjust = o.getDoubleValue("DestroyedPPUnitAdjustment");
						
						if ( ppUnitDestroyAdjust > 0)
							ppToDestroyDoub *= so.getStartingUnits() / ppUnitDestroyAdjust;
						
						if ( ppBVDestroyAdjust > 0)
							ppToDestroyDoub *= so.getStartingBV() / ppBVDestroyAdjust;
						
						

						// convert ppToCapture into an int
						int ppToDestroy = (int) Math.min(ppToDestroyDoub,ppDestructionCap);

						// not allowed to delay or steal from factories the
						// defender doesn't own
						if (so.getTargetWorld().getOwner() == null
								|| !so.getTargetWorld().getOwner().getName()
										.equalsIgnoreCase(
												aLoser.getMyHouse().getName())) {
							ppToDestroy = 0;
						}

						/*
						 * Try every factory on the world, at random, until
						 * we've taken what we can. This may mean getting inf or
						 * veh pp on a planet than can produce meks.
						 * 
						 * The while loop terminates without running if the
						 * ppToDestroy is 0.
						 */
						int ppDestroyed = 0;
						factoriesSearched.clear();
						factoriesSearched.addAll(target.getUnitFactories());
						while (factoriesSearched.size() > 0
								&& ppDestroyed < ppToDestroy) {

							// get a random factory
							SUnitFactory currFacility = (SUnitFactory)factoriesSearched.remove(CampaignMain.cm.getRandomNumber(factoriesSearched.size()));

							// get the factory's weightclass, then try
							// all types in order of preference.
							int currWeight = currFacility.getWeightclass();

							for (int type = Unit.MEK; type <= Unit.BATTLEARMOR; type++) {

								// skip this type if the facility cannot produce
								if (!currFacility.canProduce(type))
									continue;

								boolean noPP = false;
								while (!noPP && ppDestroyed < ppToDestroy) {
									int ppAvailable = aLoser
											.getHouseFightingFor().getPP(
													currWeight, type);
									if (ppAvailable <= 0)
										noPP = true;
									else {
										int toDestroy = ppToDestroy;
										if (ppToDestroy > ppAvailable)
											toDestroy = ppAvailable;
										loserHSUpdates.append(aLoser
												.getHouseFightingFor().addPP(
														currWeight, type,
														-toDestroy, false));
										// aWinner.getHouseFightingFor().addPP(currWeight,type,
										// toTake);
										ppDestroyed += toDestroy;

										if (hasLoss) {
											winnerMetaString += ",";
											loserMetaString += ",";
										}

										winnerMetaString += " destroyed "
												+ toDestroy
												+ " "
												+ Unit
														.getWeightClassDesc(currWeight)
												+ " "
												+ Unit.getTypeClassDesc(type)
												+ " components";
										loserMetaString += " lost "
												+ toDestroy
												+ " "
												+ Unit
														.getWeightClassDesc(currWeight)
												+ " "
												+ Unit.getTypeClassDesc(type)
												+ " components";
										hasLoss = true;
									}
								}// end while(units remain in this factories'
									// pool)
							}// end for(all types)
						}// end while(factories remain)
						checkMercContracts(aWinner,
								ContractInfo.CONTRACT_COMPONENTS, ppDestroyed);
					} catch (Exception ex) {
						MWServ.mwlog.errLog(ex);
					}

				}// end if(attackerWon)

				// DEFENDER VICTORY BLOCK
				else if (defendersWon) {
					try {
						/*
						 * Determine the amount of land which should change
						 * hands as a result of the Operation.
						 */
						int totalConquest = o
								.getIntValue("DefenderBaseConquestAmount");
						int conquestUnitAdjust = o
								.getIntValue("DefenderConquestUnitAdjustment");
						int conquestBVAdjust = o
								.getIntValue("DefenderConquestBVAdjustment");
						if (conquestUnitAdjust > 0)
							totalConquest += Math.floor(so.getStartingUnits()
									/ conquestUnitAdjust);
						if (conquestBVAdjust > 0)
							totalConquest += Math.floor(so.getStartingBV()
									/ conquestBVAdjust);
						if (totalConquest > conquestCap)
							totalConquest = conquestCap;

						// Drop % to 0 if either group is non-conquer
						if (!aWinner.getMyHouse().isConquerable()
								|| !aLoser.getMyHouse().isConquerable())
							totalConquest = 0;

						// Drop % to 0 if the planet is non-conquerable
						if (!so.getTargetWorld().isConquerable())
							totalConquest = 0;

						// make the % adjustment. This zeros totalConquest if
						// there is nothing to gain.
						totalConquest = so.getTargetWorld().doGainInfluence(
								aWinner.getHouseFightingFor(),
								aLoser.getHouseFightingFor(), totalConquest,
								false);

						if (totalConquest > 0) {
							hasLoss = true;
							winnerMetaString += " gained " + totalConquest
									+ " points of "
									+ target.getNameAsColoredLink();
							loserMetaString += " lost " + totalConquest
									+ " points of "
									+ target.getNameAsColoredLink();
							checkMercContracts(aWinner,
									ContractInfo.CONTRACT_LAND, totalConquest);
						}

						/*
						 * Determine the amount of refresh to add to the
						 * facilities on world. This is an inverse of what the
						 * attacker was trying to get - bonus miniticks for the
						 * factories.
						 */
						int totalRefreshBoost = o
								.getIntValue("DefenderBaseDelayAmount");
						int delayUnitAdjust = o
								.getIntValue("DefenderDelayUnitAdjustment");
						int delayBVAdjust = o
								.getIntValue("DefenderDelayBVAdjustment");
						if (delayUnitAdjust > 0)
							totalRefreshBoost += Math.floor(so
									.getStartingUnits()
									/ delayUnitAdjust);
						if (delayBVAdjust > 0)
							totalRefreshBoost += Math.floor(so.getStartingBV()
									/ delayBVAdjust);
						if (totalRefreshBoost > delayCap)
							totalRefreshBoost = delayCap;

						// if no facilities exist to refresh, reduce the total
						// to 0.
						if (target.getFactoryCount() == 0)
							totalRefreshBoost = 0;

						// not allowed to boost production or decrease factory
						// delays if the defender doesn't own
						if (so.getTargetWorld().getOwner() == null
								|| !so.getTargetWorld().getOwner().getName()
										.equalsIgnoreCase(
												aWinner.getMyHouse().getName())) {
							totalRefreshBoost = 0;
						}

						// do the refresh and string setup
						if (totalRefreshBoost > 0) {
							Iterator i = target.getUnitFactories().iterator();
							while (i.hasNext()) {
								SUnitFactory currFacility = (SUnitFactory) i
										.next();
								winnerHSUpdates.append(currFacility.addRefresh(
										totalRefreshBoost, false));
							}

							if (hasLoss) {
								winnerMetaString += ",";
								loserMetaString += ",";
							}
							winnerMetaString += " sped up production on "
									+ target.getNameAsColoredLink() + " by "
									+ totalRefreshBoost + " miniticks";
							loserMetaString += " encouraged the workers on "
									+ target.getNameAsColoredLink()
									+ " to speed up the production lines for "
									+ totalRefreshBoost + "miniticks";
							hasLoss = true;
							checkMercContracts(aWinner,
									ContractInfo.CONTRACT_DELAY,
									totalRefreshBoost);

						}

						/*
						 * NOTE: This is the point where unit thefts appear in
						 * the attacker win block. Defenders cannot steal or
						 * insta-build units, so its been removed.
						 */

						/*
						 * Determine the number and type of components to
						 * produce. The defender cannot choose which type of
						 * components should be produced. They will always be
						 * generated at random from the factories available on
						 * world.
						 * 
						 * We only need to do this if the target actually has
						 * factories. If it doesnt, the attacker was wasting his
						 * time anyway ...
						 */
						if (target.getFactoryCount() > 0) {

							double ppToGenerateDoub = o
									.getDoubleValue("DefenderBasePPAmount");
							double ppUnitAdjust = o
									.getDoubleValue("DefenderPPUnitAdjustment");
							double ppBVAdjust = o
									.getDoubleValue("DefenderPPBVAdjustment");
							if (ppUnitAdjust > 0)
								ppToGenerateDoub *= so.getStartingUnits()
										/ ppUnitAdjust;
							if (ppBVAdjust > 0)
								ppToGenerateDoub *= so.getStartingBV()
										/ ppBVAdjust;
							if (ppToGenerateDoub > ppCaptureCap)
								ppToGenerateDoub = ppCaptureCap;

							// convert ppToGenerate into an int
							int ppToGenerate = (int) ppToGenerateDoub;

							// not allowed to boost production or decrease
							// factory delays if the defender doesn't own
							if (so.getTargetWorld().getOwner() == null
									|| !so.getTargetWorld().getOwner()
											.getName().equalsIgnoreCase(
													aWinner.getMyHouse()
															.getName())) {
								ppToGenerate = 0;
							}

							// Only generate PP if your suppose to
							if (ppToGenerate > 0) {
								// get a random factory
								SUnitFactory currFacility = target
										.getRandomUnitFactory();

								for (int type = Unit.MEK; type <= Unit.BATTLEARMOR; type++) {

									// skip this type if the facility cannot
									// produce
									if (!currFacility.canProduce(type))
										continue;

									// it can produce. add some PP.
									winnerHSUpdates.append(aWinner
											.getHouseFightingFor().addPP(
													currFacility
															.getWeightclass(),
													type, ppToGenerate, false));

									// tell the players
									if (hasLoss) {
										winnerMetaString += ",";
										loserMetaString += ",";
									}

									winnerMetaString += " generated an extra "
											+ ppToGenerate
											+ " "
											+ Unit
													.getWeightClassDesc(currFacility
															.getWeightclass())
											+ " " + Unit.getTypeClassDesc(type)
											+ " components";
									loserMetaString += " inspired the workers on "
											+ target.getNameAsColoredLink()
											+ " to manufacture "
											+ ppToGenerate
											+ " "
											+ Unit
													.getWeightClassDesc(currFacility
															.getWeightclass())
											+ " "
											+ Unit.getTypeClassDesc(type)
											+ " components";
									hasLoss = true;
									checkMercContracts(aWinner,
											ContractInfo.CONTRACT_COMPONENTS,
											ppToGenerate);
								}
							}// end if ppToGenerate > 0
						}// end if(defender has factories on world)
					} catch (Exception ex) {
						MWServ.mwlog.errLog(ex);
					}

				}// end elseif(defenderWon)

				// only futz with the metastring additions if there is actual
				// loss.
				if (hasLoss) {

					/*
					 * Strip the the last instance of ", " and insert " and " in
					 * order to have a properly formatted metastring. After
					 * correcting the format, append to the finishedInfo and add
					 * to player's entries in the metaStrings tree.
					 */
					String meta1 = "";
					String meta2 = "";
					int winnerIndex = winnerMetaString.lastIndexOf(", ");
					int loserIndex = loserMetaString.lastIndexOf(", ");
					if (winnerIndex >= 0) {
						meta1 = winnerMetaString.substring(0, winnerIndex);
						meta2 = winnerMetaString.substring(winnerIndex + 2,
								winnerMetaString.length());
						winnerMetaString = meta1 + " and " + meta2 + ".";
					}

					completeFinishedInfoString += " "
							+ aWinner.getHouseFightingFor()
									.getColoredNameAsLink() + winnerMetaString;
					incompleteFinishedInfoString += " "
							+ aWinner.getHouseFightingFor()
									.getColoredNameAsLink() + winnerMetaString;

					newsFeedBody += " "
							+ aWinner.getHouseFightingFor().getName()
							+ winnerMetaString;

					if (loserIndex >= 0) {
						meta1 = loserMetaString.substring(0, loserIndex);
						meta2 = loserMetaString.substring(loserIndex + 2,
								loserMetaString.length());
						loserMetaString = meta1 + " and " + meta2 + ".";
					}

					if (!drawGame) {
						for (String currName : so.getWinners().keySet()) {
							String currMeta = metaStrings.get(currName);
							metaStrings.put(currName, currMeta + " You've "
									+ winnerMetaString + "<br>");
						}
					}
					for (String currName : so.getLosers().keySet()) {
						String currentMeta = metaStrings.get(currName);
						metaStrings.put(currName, currentMeta + " You've "
								+ loserMetaString + "<br>");
					}
				}

				// all done. send updates to effected houses.
				if (winnerHSUpdates.length() > 0)
					CampaignMain.cm.doSendToAllOnlinePlayers(aWinner
							.getHouseFightingFor(), "HS|" + winnerHSUpdates,
							false);
				if (loserHSUpdates.length() > 0)
					CampaignMain.cm.doSendToAllOnlinePlayers(aLoser
							.getHouseFightingFor(), "HS|" + loserHSUpdates,
							false);

			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
		}
	}// end this.assembleMetaStrings()

	/**
	 * Method which parses salvage from the report and saves outcome strings for
	 * the players.
	 */
	private void possibleSalvageFromReport(StringTokenizer reportTokenizer,
			ShortOperation so) {

		try {// main catch, make new lists and a new pilots hash.

			salvagableUnits = new TreeMap<Integer, OperationEntity>();
			destroyedUnits = new TreeMap<Integer, OperationEntity>();
			livingUnits = new TreeMap<Integer, OperationEntity>();
			pilots = new TreeMap<Integer, SPilot>();
			int destroyed = 0;
			Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
			int fledSalvageChance = o.getIntValue("FledUnitSalvageChance");
			int fledScrappedChance = o.getIntValue("FledUnitScrappedChance");
			int pushedSalvageChance = o.getIntValue("PushedUnitSalvageChance");
			int pushedScrappedChance = o.getIntValue("PushedUnitScrappedChance");

			try {
				// loop through all units in the string
				while (reportTokenizer.hasMoreTokens()) {

					String currentUnit = reportTokenizer.nextToken();

					// MechWarrior
					if (currentUnit.startsWith("MW*")) {

						StringTokenizer mwTokenizer = new StringTokenizer(currentUnit, "*");
						mwTokenizer.nextToken();// burn the second token. always
												// -1. unused.

						int originalID = Integer.parseInt(mwTokenizer.nextToken());
						int pickUpID = Integer.parseInt(mwTokenizer.nextToken());
						boolean isDead = Boolean.parseBoolean(mwTokenizer.nextToken());

						SPilot mw = SPilot.getMekWarrior(originalID, pickUpID);
						mw.setDeath(isDead);

						pilots.put(originalID, mw);// key to host unit
					}

					// check for builings left this is tacked onto the end of
					// the results string.
					else if (currentUnit.startsWith("BL*"))
						buildingsLeft = Integer.parseInt(currentUnit.substring(3));

					// Combat Unit. Make and store an OperationEntity.
					else {

						OperationEntity oEntity = new OperationEntity(currentUnit);
						SUnit unit = CampaignMain.cm.getPlayer(oEntity.getOwnerName()).getUnit(oEntity.getID());

						// if the player doesnt own the unit, its probably
						// autoartillery. continue to next loop.
						if (unit == null)
							continue;

						if ( (fledSalvageChance > 0 || fledScrappedChance > 0) 
								&& oEntity.getRemovalReason() == IEntityRemovalConditions.REMOVE_IN_RETREAT ){
							if ( CampaignMain.cm.getRandomNumber(100) <= fledSalvageChance ){
								oEntity.setRemovalReason(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
								oEntity.setSalvage(true);
							}else if ( CampaignMain.cm.getRandomNumber(100) <= fledScrappedChance ){
								oEntity.setRemovalReason(IEntityRemovalConditions.REMOVE_STACKPOLE);
							}
						}
						else if ( (pushedSalvageChance > 0 || pushedScrappedChance > 0) 
								&& oEntity.getRemovalReason() == IEntityRemovalConditions.REMOVE_PUSHED ){
							if ( CampaignMain.cm.getRandomNumber(100) <= fledSalvageChance ){
								oEntity.setRemovalReason(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
								oEntity.setSalvage(true);
							}else if ( CampaignMain.cm.getRandomNumber(100) <= fledScrappedChance ){
								oEntity.setRemovalReason(IEntityRemovalConditions.REMOVE_STACKPOLE);
							}
						}
						
						if (oEntity.isLiving()) {
							livingUnits.put(oEntity.getID(), oEntity);
							destroyed = 0;
							if (so.getLosers().containsKey(oEntity.getOwnerName().toLowerCase()))
								currentBV += unit.getBV();
						} else if (oEntity.isSalvagable()) {
							salvagableUnits.put(oEntity.getID(), oEntity);
							destroyed = 1;
							/*
							 * I've changed the code so that only the BV of
							 * living units is reported. if the unit was
							 * destroyed(but not cored) or forced salvage they
							 * do not get a BV count. Might tweak this later
							 * --Torren.
							 */
							// currentBV += unit.getBV();
						} else {
							destroyedUnits.put(oEntity.getID(), oEntity);
							destroyed = 1;
						}

						/*
						 * Count all real games for mechstats. Do NOT count
						 * disco resolutions as they are not meritorious.
						 */
						if (saveStats) {
							SUnit currU = CampaignMain.cm.getPlayer(oEntity.getOwnerName()).getUnit(oEntity.getID());
							if (so.getWinners().containsKey(oEntity.getOwnerName().toLowerCase())) {
								CampaignMain.cm.addMechStat(currU.getUnitFilename(), currU.getWeightclass(), 1, 1, 0, destroyed);
							} else if (so.getLosers().containsKey(oEntity.getOwnerName().toLowerCase())) {
								CampaignMain.cm.addMechStat(currU.getUnitFilename(), currU.getWeightclass(), 1, 0, 0, destroyed);
							}
						}// end save stats

					}

				}// end while(more unit strings)

				// final game BV determined. save to the short op for use in /c
				// modgames.
				so.setFinishingBV(currentBV);

			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

			/*
			 * Check offboard units. Units which are deployed off map will
			 * always return as surviving, which is rather unfair. There are
			 * configurable options to allow destructive overrun to mitigate
			 * this advantage.
			 * 
			 * Base chance of overrun is modified by the distance an artillery
			 * unit is placed off of the board, yeilding a final overrun %.
			 * party, not only the winner.
			 */
			int baseChance = CampaignMain.cm
					.getIntegerConfig("ArtilleryOffBoardOverRun");
			int captureChance = CampaignMain.cm
					.getIntegerConfig("OffBoardChanceOfCapture");

			/*
			 * Need to use an iterator here b/c it allows for a chance to remove
			 * units (over run) from living units, which foreach does not.
			 */
			Iterator i = livingUnits.values().iterator();
			try {
				while (i.hasNext()) {

					OperationEntity currO = (OperationEntity) i.next();

					// so.getWinners() don't get overrun
					if (so.getWinners().containsKey(
							currO.getOwnerName().toLowerCase()))
						continue;

					// if a unit isn't actually offboard, no check
					if (currO.getOffBoardRange() <= 0)
						return;

					// integer which reresents this OEntity's likelyhood of
					// being overrun
					int currOverrunChance = baseChance
							- currO.getOffBoardRange();

					// check for overrun
					if (CampaignMain.cm.getRandomNumber(100) <= currOverrunChance) {

						// its been overrun. remove from living units.
						i.remove();

						// if capture roll passes, set as salvageable
						if (CampaignMain.cm.getRandomNumber(100) < captureChance) {
							currO.setRemovalReason(megamek.common.IEntityRemovalConditions.REMOVE_SALVAGEABLE);
							salvagableUnits.put(currO.getID(), currO);
						} else {// destroy the piece
							if (currO.getType() == Unit.MEK
									|| currO.getType() == Unit.QUAD)
								currO.setCTint(0);
							currO.setSalvage(false);
							destroyedUnits.put(currO.getID(), currO);
						}
					}// end if
				}// end while(More To Check)
			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
		}

	}// end possibleSalvageFromReport

	/**
	 * Method which builds possible salvage from a Short Operation's death tree.
	 * Since the information stored while a game is in progress is less exact
	 * than a full autoreport, some assumptions must be made.
	 * 
	 * By setting up accurate living, salvage and dead unit treemaps, this
	 * method allows the resolver to use its remaining methods (salvage
	 * assignment, etc) as usual.
	 */
	public void possibleSalvageFromInProgressInfo(ShortOperation so,
			SPlayer loser) {
		try {
			// make new lists and a new pilots hash.
			salvagableUnits = new TreeMap<Integer, OperationEntity>();
			destroyedUnits = new TreeMap<Integer, OperationEntity>();
			livingUnits = new TreeMap<Integer, OperationEntity>();
			pilots = new TreeMap<Integer, SPilot>();
			try {
				// populate the units trees using removed units. some may end up
				// in living.
				for (OperationEntity currEntity : so.getUnitsInProgress()
						.values()) {

					// if the player doesnt own the unit, its probably
					// autoartillery. continue to next loop.
					if (CampaignMain.cm.getPlayer(currEntity.getOwnerName())
							.getUnit(currEntity.getID()) == null)
						continue;

					if (currEntity.isLiving())
						livingUnits.put(currEntity.getID(), currEntity);
					else if (currEntity.isSalvagable())
						salvagableUnits.put(currEntity.getID(), currEntity);
					else
						destroyedUnits.put(currEntity.getID(), currEntity);
				}
			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

			/*
			 * loop through all units from all armies. any unit which was never
			 * removed from play (ie - isnt already in the units treemaps) is
			 * presumed to be alive for reporting purposes.
			 */
			try {
				for (SArmy currArmy : allArmies.values()) {
					for (Unit currUnit : currArmy.getUnits()) {
						int currID = currUnit.getId();
						if (livingUnits.get(currID) == null
								&& salvagableUnits.get(currID) == null
								&& destroyedUnits.get(currID) == null) {
							OperationEntity oe = new OperationEntity(currArmy
									.getPlayerName(), currID, 0, 1, 1, true);
							livingUnits.put(currID, oe);
						}
					}// end for(all units in currArmy)
				}// end for(all armies)
			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

			/*
			 * Populate the pilots hashtable, using pilots from the
			 * ShortOperation's inProgress TreeMap.
			 */
			pilots.putAll(so.getPilotsInProgress());

			/*
			 * Build a vector of units which are living and belong to the player
			 * who dropped from the server.
			 */
			ArrayList<OperationEntity> dropLivingUnits = new ArrayList<OperationEntity>();
			try {
				for (OperationEntity currE : livingUnits.values()) {
					if (currE.getOwnerName().toLowerCase().equals(
							loser.getName().toLowerCase()))
						dropLivingUnits.add(currE);
				}
			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

			/*
			 * If the dropper doesnt have any living units, we move on and do
			 * not apply a penalty. If he had pieces survive the game, check
			 * server settings to see if additional units should be destroyed or
			 * set as salvage.
			 */
			try {
				if (dropLivingUnits.size() > 0) {

					int unitsToDestroy = CampaignMain.cm
							.getIntegerConfig("DisconnectionAddUnitsDestroyed");
					int unitsToSalvage = CampaignMain.cm
							.getIntegerConfig("DisconnectionAddUnitsSalvage");

					// DESTROY! RAWR!
					int unitsDestroyed = 0;
					while (unitsDestroyed < unitsToDestroy
							&& dropLivingUnits.size() > 0) {

						OperationEntity randomEntity = dropLivingUnits
								.remove(CampaignMain.cm.getRandomNumber(
										dropLivingUnits.size()));

						// destroy the unit, killing pilot
						randomEntity
								.setRemovalReason(megamek.common.IEntityRemovalConditions.REMOVE_STACKPOLE);
						randomEntity.setSalvage(false);// for vehs
						livingUnits.remove(randomEntity.getID());
						destroyedUnits.put(randomEntity.getID(), randomEntity);

						unitsDestroyed++;
					}

					// Bonus Salvage. Note that this can destroy infantry.
					int unitsSalvaged = 0;
					while (unitsSalvaged < unitsToSalvage
							&& dropLivingUnits.size() > 0) {

						OperationEntity randomEntity = dropLivingUnits
								.remove(CampaignMain.cm.getRandomNumber(
										dropLivingUnits.size()));

						// if the entity if a mech, eject the pilot
						if (randomEntity.getType() == Unit.MEK
								|| randomEntity.getType() == Unit.QUAD)
							pilots.put(randomEntity.getID(), SPilot
									.getMekWarrior(randomEntity.getID(), -1));

						// make the unit salvageable
						randomEntity
								.setRemovalReason(megamek.common.IEntityRemovalConditions.REMOVE_SALVAGEABLE);
						randomEntity.setSalvage(true);// for vehs
						livingUnits.remove(randomEntity.getID());
						salvagableUnits.put(randomEntity.getID(), randomEntity);

						unitsSalvaged++;
					}

				}// end if(no units to shift)
			} catch (Exception ex) {
				MWServ.mwlog.errLog(ex);
			}

		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
		}

	}// end possibleSalvageFromInProgressInfo

	/**
	 * Method which cleans up when a pilot dies. Determines whether or not
	 * peronal queues are in use, re-BV's the unit and the armies in which it is
	 * resident, and returns a string detailing the outcome.
	 */
	private String handleDeadPilot(SPlayer owner, SUnit unit,
			OperationEntity entity, ShortOperation op) {
		// holder string
		String toReturn = "";

		try {
			// if PPQs are on and the unit is a mek, vacate
			boolean personalQueues = CampaignMain.cm
					.getBooleanConfig("AllowPersonalPilotQueues");
			boolean isPilotChangeable = (entity.getType() == Unit.MEK
					|| entity.getType() == Unit.QUAD || entity.getType() == Unit.PROTOMEK);
			if (CampaignMain.cm.isUsingCyclops())
				CampaignMain.cm.getMWCC().pilotKill((SPilot) unit.getPilot(),
						op.getOpCyclopsID());
			if (CampaignMain.cm.isUsingMySQL())
				CampaignMain.cm.MySQL.deletePilot(((SPilot) unit.getPilot())
						.getDBId());
			if (isPilotChangeable && personalQueues) {
				SPilot pilot = new SPilot("Vacant", 99, 99);
				unit.setPilot(pilot);
			}

			else {

				unit.setPilot(owner.getMyHouse().getNewPilot(unit.getType()));
				if (entity.getType() == Unit.MEK
						|| entity.getType() == Unit.QUAD
						|| entity.getType() == Unit.PROTOMEK)
					toReturn += " New Pilot: " + unit.getPilot().getName();
				else if (entity.getType() == Unit.VEHICLE)
					toReturn += " New Crew:";
				else if (entity.getType() == Unit.BATTLEARMOR
						|| entity.getType() == Unit.INFANTRY)
					toReturn += " New Squad:";
				toReturn += " " + newPilotDescription(unit);
			}
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return toReturn;
		}

		return toReturn;
	}

	/**
	 * Method which handles living, dispossessed pilots. Called from the salvage
	 * method. Very simple. Broken out to slice repetetive code.
	 */
	private void handleDispossesedPilot(SPlayer owner, SUnit unit) {

		try {

			boolean personalQueues = CampaignMain.cm
					.getBooleanConfig("AllowPersonalPilotQueues");
			boolean isPilotChangeable = (unit.getType() == Unit.MEK
					|| unit.getType() == Unit.QUAD || unit.getType() == Unit.PROTOMEK);

			// if PPQs are on, insert the pilot in the owner's queue
			if (isPilotChangeable && personalQueues) {
				owner.getPersonalPilotQueue().addPilot(unit.getPilot(),
						unit.getWeightclass());
				CampaignMain.cm.toUser("PL|AP2PPQ|" + unit.getType() + "|"
						+ unit.getWeightclass() + "|"
						+ ((SPilot) unit.getPilot()).toFileFormat("#", true),
						owner.getName(), false);
				owner.getPersonalPilotQueue().checkQueueAndWarn(
						owner.getName(), unit.getType(), unit.getWeightclass());
				unit.setPilot(new SPilot("Vacant", 99, 99));
			}

			// else, add it to the owner's house.
			else
				owner.getHouseFightingFor().addDispossessedPilot(unit, false);
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
		}
	}

	/**
	 * Method which checks the status of a pilot. Called when checking on
	 * salvaged and destroyed units.
	 * 
	 * If the pilot is non-null, can check the is dead field for status and
	 * reason for showing on field.
	 * 
	 * If the pilot is null, death is presumed. Can derive the cause of death
	 * from the status of his unit.
	 * 
	 * This method returns an array which has very specific object ordering, as
	 * follows:
	 * 
	 * 0 - Boolean pilotIsDead; indicates status of pilot 1 - String
	 * ownerString; to send to the original owner 2 - String captorString; to
	 * send to capturing player 3 - String otherString; to send to player who
	 * are neither original owners nor captors 4 - SPlayer captor; player who
	 * has captured a pilot
	 */
	private Object[] setupPilotStringForUnit(OperationEntity currEntity,
			SUnit currUnit, SPlanet target) {

		// get the pilot
		SPilot mw = pilots.get(currEntity.getID());

		// setup the return array
		Object[] toReturn = new Object[5];
		toReturn[0] = new Boolean(false);
		toReturn[1] = "";
		toReturn[2] = "";
		toReturn[3] = "";
		toReturn[4] = null;

		try {
			// pilot isn't null. see if they lived through the game.
			if (mw != null) {

				if (mw.isDead()) {
					toReturn[1] = currUnit.getPilot().getName()
							+ " ejected, but was killed on the ground.";
					toReturn[3] = " The pilot was killed.";
					return toReturn;
				}

				// pilot isn't dead. check to see if it was picked up.
				// pickup only matters if it was by a living unit.
				SPlayer pickupPlayer = null;
				OperationEntity pickupEntity = livingUnits.get(mw
						.getPickedUpID());
				if (pickupEntity != null)
					pickupPlayer = CampaignMain.cm.getPlayer(pickupEntity
							.getOwnerName());
				if (pickupPlayer != null) {

					// if the player is fighting for the same house, simply
					// return the pilot
					if (currEntity.getOwner().getHouseFightingFor().equals(
							pickupPlayer.getHouseFightingFor())) {
						toReturn[0] = new Boolean(true);
						toReturn[1] = currUnit.getPilot().getName()
								+ " ejected and was picked up by a friendly unit.";
						toReturn[3] = "The pilot survived.";
						return toReturn;
					}

					// player isn't friendly, so we can set the messages to the
					// owner ...
					toReturn[1] = currUnit.getPilot().getName()
							+ " ejected, but was captured by enemy forces.";
					toReturn[3] = "The pilot was captured.";
					toReturn[4] = pickupPlayer;

					// check to see if the pilot defects
					int captureChance = CampaignMain.cm
							.getIntegerConfig("ChanceToConvertCapturedPilots");

					// captured. check to see if the pilot defects and joins the
					// queues.
					if (CampaignMain.cm.getRandomNumber(100) < captureChance) {
						if (CampaignMain.cm
								.getBooleanConfig("AllowPersonalPilotQueues")) {
							pickupPlayer.getPersonalPilotQueue().addPilot(
									currUnit.getPilot(),
									currUnit.getWeightclass());
							CampaignMain.cm.toUser("PL|AP2PPQ|"
									+ currUnit.getType()
									+ "|"
									+ currUnit.getWeightclass()
									+ "|"
									+ ((SPilot) currUnit.getPilot())
											.toFileFormat("#", true),
									pickupPlayer.getName(), false);
							pickupPlayer.getPersonalPilotQueue()
									.checkQueueAndWarn(pickupPlayer.getName(),
											currUnit.getType(),
											currUnit.getWeightclass());
						} else
							pickupPlayer.getHouseFightingFor()
									.addDispossessedPilot(currUnit, true);

						toReturn[0] = new Boolean(false);// although the
															// pilot is
															// technically
															// alive, treat as
															// dead
						toReturn[2] = currUnit.getPilot().getName()
								+ " was captured by your forces and has decided to join "
								+ pickupPlayer.getMyHouse()
										.getColoredNameAsLink() + ".";
						return toReturn;
					}

					// captured, but did not defect
					toReturn[2] = currUnit.getPilot().getName()
							+ " was captured by your forces and transferred to HQ for interrogation.";
					return toReturn;
				}// end if(non-null pickup)

				/*
				 * The pickup was null, but the pilot was not. Check to see if
				 * the pilot managed to survive or was captured on the field.
				 */

				// allow so.getWinners() to recover all of their pilots outright
				if (shortOp.getWinners().containsKey(
						currEntity.getOwnerName().toLowerCase())) {
					toReturn[0] = new Boolean(true);
					toReturn[1] = currUnit.getPilot().getName()
							+ " was picked up by a recovery team.";
					toReturn[3] = "The pilot survived.";
					return toReturn;
				}

				// if not a winner, give the pilot a chance to make it back to
				// base
				int survivalChance = target.getInfluence().getInfluence(
						currEntity.getOwner().getHouseFightingFor().getId());

				// always give them a fighting chance.
				int minChance = CampaignMain.cm
						.getIntegerConfig("BasePilotSurvival");
				if (survivalChance < minChance)
					survivalChance = minChance;

				// plus up his survival if he's got ill wilderness traning? huh?
				if (currUnit.getPilot().getSkills().has(
						PilotSkill.SurvivalistSkillID))
					survivalChance += 20;

				// survived. let the player know and return.
				if (CampaignMain.cm.getRandomNumber(100) < survivalChance) {
					toReturn[0] = new Boolean(true);
					toReturn[1] = ((SPilot) currUnit.getPilot())
							.getPilotRescueMessage(currUnit);
					toReturn[3] = "The pilot survived";
					return toReturn;
				}

				// didn't survive. can set up the message to owner and select a
				// random pickup player
				pickupPlayer = this.selectRandomWinner();
				toReturn[1] = ((SPilot) currUnit.getPilot())
						.getPilotCaptureMessageToOwner(currUnit);
				toReturn[3] = "The pilot was captured.";
				toReturn[4] = pickupPlayer;

				// didn't survive the hike back. make the normal conversion
				// check.
				int captureChance = CampaignMain.cm
						.getIntegerConfig("ChanceToConvertCapturedPilots");

				// captured. check to see if the pilot defects and joins the
				// queues.
				if (CampaignMain.cm.getRandomNumber(100) < captureChance) {
					if (CampaignMain.cm
							.getBooleanConfig("AllowPersonalPilotQueues")) {
						pickupPlayer.getPersonalPilotQueue().addPilot(
								currUnit.getPilot(), currUnit.getWeightclass());
						CampaignMain.cm.toUser("PL|PPQ|"
								+ pickupPlayer.getPersonalPilotQueue()
										.toString(true),
								pickupPlayer.getName(), false);
					} else
						pickupPlayer.getHouseFightingFor()
								.addDispossessedPilot(currUnit, true);
					toReturn[0] = new Boolean(false);// pilot technically
														// lives, but we say
														// that he is dead.
					toReturn[2] = ((SPilot) currUnit.getPilot())
							.getPilotCaptureAndDefectedMessage(currUnit,
									pickupPlayer.getMyHouse());
					return toReturn;
				}

				// captured, but did not defect
				toReturn[2] = ((SPilot) currUnit.getPilot())
						.getPilotCaptureAndRemovedMessage(currUnit);
				return toReturn;

			}// end (mw != null)

			/*
			 * The pilot is null. This generally means that the pilot is dead;
			 * however, it may also mean that the unit was killed in a way which
			 * doesnt trigger an ejection - generally 3 engine crits - of the
			 * unit simply doesn't have an ejectable pilot (proto, BA).
			 */

			// pilots of non-mech units cannot survive destruction
			if (currEntity.getType() == Unit.PROTOMEK) {
				toReturn[1] = currUnit.getPilot().getName()
						+ " died when the unit was destroyed.";
				toReturn[3] = "The pilot was killed.";
				return toReturn;
			}

			else if (currEntity.getType() == Unit.VEHICLE) {

				/*
				 * If the vehicle is salvageable and the crew is alive, we have
				 * to test for winner and loser, same as for a pilot trapped in
				 * an engine-destroyed mek.
				 */
				if (currEntity.isSalvage() && !currEntity.isCrewDead()) {

					/*
					 * Winners always keep their crews. If survival checks for
					 * trapped pilots and crews are turned off, the crew is
					 * always returned to its owner.
					 */
					if (shortOp.getWinners().containsKey(
							currEntity.getOwnerName().toLowerCase())
							|| !CampaignMain.cm
									.getBooleanConfig("DownPilotsMustRollForSurvival")) {
						toReturn[0] = new Boolean(true);
						toReturn[1] = "The crew survived.";
						toReturn[3] = "The crew survived.";
						return toReturn;
					}

					// loser and trapped crews need to make a survival roll.
					int survivalChance = target.getInfluence()
							.getInfluence(
									currEntity.getOwner().getHouseFightingFor()
											.getId());

					// always give them a fighting chance. no survivalist
					// adjustment.
					int minChance = CampaignMain.cm
							.getIntegerConfig("BasePilotSurvival");
					if (survivalChance < minChance)
						survivalChance = minChance;
					survivalChance -= CampaignMain.cm
							.getIntegerConfig("TrappedInMechSurvivalMod");

					// survived. let the player know and return.
					if (CampaignMain.cm.getRandomNumber(100) < survivalChance) {
						toReturn[0] = new Boolean(true);
						toReturn[1] = "The crew survived.";
						toReturn[3] = "The crew survived.";
					} else {
						toReturn[0] = new Boolean(false);
						toReturn[1] = "The crew didn't make it back to base.";
						toReturn[3] = "The crew was killed.";
					}
				}

				/*
				 * If the vehicle is salvageable and the crew is dead, set an
				 * appropriate message and resolve salvage normally.
				 */
				else if (currEntity.isSalvage() && currEntity.isCrewDead()) {
					toReturn[1] = "The crew of the " + currUnit.getModelName()
							+ " was killed in action.";
					toReturn[3] = "The crew was killed.";
				}

				/*
				 * The vehicle was destroyed outright.
				 */
				else {
					toReturn[1] = "The crew of the " + currUnit.getModelName()
							+ " died when the unit was destroyed.";
					toReturn[3] = "The crew was killed.";
				}

				return toReturn;
			}

			else if (currEntity.getType() == Unit.BATTLEARMOR
					|| currEntity.getType() == Unit.INFANTRY) {
				toReturn[1] = "The " + currUnit.getModelName()
						+ " squad was declared MIA.";
				toReturn[3] = "The squad was killed.";
				return toReturn;
			}

			// first, see if the pilot has too many wounds.
			if (currEntity.getPilothits() >= 6) {
				toReturn[1] = currUnit.getPilot().getName()
						+ " was fatally wounded.";
				toReturn[3] = "The pilot was killed.";
				return toReturn;
			}

			// see if the location containing the cockpit was obliterated (HD or
			// CT)
			if (currEntity.getHDint() <= 0
					&& currEntity.getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED
					&& currEntity.getRemovalReason() != IEntityRemovalConditions.REMOVE_EJECTED) {
				toReturn[1] = currUnit.getPilot().getName() + " died when the "
						+ currUnit.getModelName() + "'s head was destroyed.";
				toReturn[3] = "The pilot was killed.";
				return toReturn;
			} else if ((currEntity.getCTint() <= 0 || currEntity
					.getRemovalReason() == IEntityRemovalConditions.REMOVE_DEVASTATED)
					&& currEntity.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
				toReturn[1] = currUnit.getPilot().getName() + " died when the "
						+ currUnit.getModelName()
						+ "'s center torso was destroyed.";
				toReturn[3] = "The pilot was killed.";
				return toReturn;
			}

			// If damaged is transfered from Game to campaign then save it the
			// pilot
			if (CampaignMain.cm.getBooleanConfig("AllowPilotDamageToTransfer")) {
				currUnit.getPilot().setHits(currEntity.getPilothits());
			}

			/*
			 * Winners should always recover their engine-killed pilots, and
			 * pilots should always be returned if the survival roll is disabled
			 * by an admin.
			 */
			if (shortOp.getWinners().containsKey(
					currEntity.getOwnerName().toLowerCase())
					|| !CampaignMain.cm
							.getBooleanConfig("DownPilotsMustRollForSurvival")) {
				toReturn[0] = new Boolean(true);
				toReturn[1] = ((SPilot) currUnit.getPilot())
						.getPilotRescueMessage(currUnit);
				toReturn[3] = "The pilot survived.";
				return toReturn;
			}

			/*
			 * If pilots in downed units need to roll for survival, check them.
			 * This code is virtually identical to the capture block when a
			 * pilot is left alive on field and could probably be factored out
			 * into a seperate checksurvival method...
			 */

			// if not a winner, give the pilot a chance to make it back to base
			int survivalChance = target.getInfluence().getInfluence(
					currEntity.getOwner().getHouseFightingFor().getId());

			// always give them a fighting chance.
			int minChance = CampaignMain.cm
					.getIntegerConfig("BasePilotSurvival");
			if (survivalChance < minChance)
				survivalChance = minChance;

			// plus up his survival if he's got ill wilderness traning? huh?
			if (currUnit.getPilot().getSkills().has(
					PilotSkill.SurvivalistSkillID))
				survivalChance += 20;

			/*
			 * Unique to in-mech pilots (engine kills). Penalty for being in a
			 * stationary unit when the capture crews come around and sweep the
			 * field.
			 */
			survivalChance -= CampaignMain.cm
					.getIntegerConfig("TrappedInMechSurvivalMod");

			// survived. let the player know and return.
			if (CampaignMain.cm.getRandomNumber(100) < survivalChance) {
				toReturn[0] = new Boolean(true);
				toReturn[1] = ((SPilot) currUnit.getPilot())
						.getPilotRescueMessage(currUnit);
				toReturn[3] = "The pilot survived.";
				return toReturn;
			}

			// didn't survive. can set up the message to owner and select a
			// random pickup player
			SPlayer pickupPlayer = this.selectRandomWinner();
			toReturn[1] = ((SPilot) currUnit.getPilot())
					.getPilotCaptureMessageToOwner(currUnit);
			toReturn[3] = "The pilot was captured.";
			toReturn[4] = pickupPlayer;

			// didn't survive the hike back. make the normal conversion check.
			int captureChance = CampaignMain.cm
					.getIntegerConfig("ChanceToConvertCapturedPilots");

			// captured. check to see if the pilot defects and joins the queues.
			if (CampaignMain.cm.getRandomNumber(100) < captureChance) {
				if (CampaignMain.cm
						.getBooleanConfig("AllowPersonalPilotQueues")) {
					pickupPlayer.getPersonalPilotQueue().addPilot(
							currUnit.getPilot(), currUnit.getWeightclass());
					CampaignMain.cm.toUser("PL|PPQ|"
							+ pickupPlayer.getPersonalPilotQueue().toString(
									true), pickupPlayer.getName(), false);
				} else
					pickupPlayer.getHouseFightingFor().addDispossessedPilot(
							currUnit, true);
				toReturn[0] = new Boolean(false);// technically alive, but
													// dead for New Pilot
													// purposes
				toReturn[2] = ((SPilot) currUnit.getPilot())
						.getPilotCaptureAndDefectedMessage(currUnit,
								pickupPlayer.getMyHouse());
				return toReturn;
			}

			// captured, but did not defect
			toReturn[2] = ((SPilot) currUnit.getPilot())
					.getPilotCaptureAndRemovedMessage(currUnit);
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return toReturn;
		}

		return toReturn;

	}// end setupPilotString

	/**
	 * Method which returns the cost of salvaging a given unit. A unit costs
	 * less to salvage after a headkill, more to salvage if torsos are
	 * destroyed, etc.
	 */
	private int getSalvageCost(SPlayer owner, SUnit u, OperationEntity oe,
			Operation o, ShortOperation so) {

		float repairCost = 0;
		int attackerMod = o.getIntValue("BVToBoostAttackerSalvageCost");
		int defenderMod = o.getIntValue("BVToBoostDefenderSalvageCost");

		// if attacker, true. else, defender.
		boolean isAttacker = false;
		try {
			if (so.getAttackers().containsKey(owner.getName().toLowerCase())
					&& attackerMod > 0) {
				repairCost = u.getBV() / attackerMod;
				isAttacker = true;
			}

			if (so.getDefenders().containsKey(owner.getName().toLowerCase())
					&& defenderMod > 0)
				repairCost = u.getBV() / defenderMod;

			// head kills & ejections are easier to repair than most. reduce
			// cost.
			if (oe.getHDint() <= 0)
				repairCost *= 0.7;
			else if (oe.getRemovalReason() == IEntityRemovalConditions.REMOVE_EJECTED)
				repairCost *= 0.6;

			// if a leg is cleaved, its going to be a bit harder to repair
			if (oe.getLLint() <= 0)
				repairCost *= 1.05;
			if (oe.getRLint() <= 0)
				repairCost *= 1.05;

			// capturing an enemy unit will be slightly more expensive then
			// recovering one's own
			if (oe.getOwnerName().toLowerCase().equals(
					owner.getName().toLowerCase()))
				repairCost *= 0.75;
			else
				repairCost *= 1.15;

			// pull ops adjustments
			if (isAttacker)
				repairCost = repairCost
						* o.getFloatValue("AttackerSalvageCostModifier");
			else
				repairCost = repairCost
						* o.getFloatValue("DefenderSalvageCostModifier");

		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return Math.round(repairCost);
		}

		return Math.round(repairCost);
	}

	/**
	 * Method which determines how much, if any, compensation a faction will
	 * give a player after a unit is lost (to either irreparable damage or an
	 * enemy) in a game. Flat/Percentage Caps and unit type adjustments allow
	 * admins to give greater amounts (relatively) for smaller units, meks, and
	 * so on.
	 */
	private int determineLossCompensation(SPlayer p, SUnit u, boolean salvage) {

		// start with the base compensation
		float compensation = CampaignMain.cm
				.getFloatConfig("BaseUnitLossPayment");

		// store the cost of a similar unit from the faction
		int newPrice = p.getMyHouse().getPriceForUnit(u.getWeightclass(),
				u.getType());

		// add a multiple of a similar new unit's cost from the player's faction
		compensation += newPrice
				* CampaignMain.cm.getFloatConfig("NewCostMultiUnitLossPayment");

		/*
		 * If the unit was salvaged by an an enemy, instead of destroyed, check
		 * for a salvage multiplier. We will usually want to reduce the payment
		 * (giving away equipment, etc), but some servers may want to boost the
		 * payout to encourage non-destructive play.
		 */
		if (salvage)
			compensation *= CampaignMain.cm
					.getFloatConfig("SalvageMultiToUnitLossPayment");

		/*
		 * Apply specific type multipliers. Can be used to reduce the
		 * compensation for a class of units. Useful, for example, to give hih
		 * compensation for Light Meks, but reduced compensation for light
		 * vehicles.
		 */
		if (u.getType() == Unit.MEK || u.getType() == Unit.QUAD)
			compensation *= CampaignMain.cm
					.getFloatConfig("MekMultiToUnitLossPayment");
		else if (u.getType() == Unit.VEHICLE)
			compensation *= CampaignMain.cm
					.getFloatConfig("VehMultiToUnitLossPayment");
		else if (u.getType() == Unit.PROTOMEK)
			compensation *= CampaignMain.cm
					.getFloatConfig("ProtoMultiToUnitLossPayment");
		else if (u.getType() == Unit.BATTLEARMOR)
			compensation *= CampaignMain.cm
					.getFloatConfig("BAMultiToUnitLossPayment");
		else if (u.getType() == Unit.INFANTRY)
			compensation *= CampaignMain.cm
					.getFloatConfig("InfMultiToUnitLossPayment");

		// check the compensation caps. 1st check reduces compensation to a
		// portion of a new unit's cost.
		float newMultiMax = newPrice
				* CampaignMain.cm
						.getFloatConfig("NewCostMultiMaxUnitLossPayment");
		if (newMultiMax > 0 && compensation > newMultiMax)
			compensation = newMultiMax;

		// check the compensation against a flat cap.
		int flatMax = CampaignMain.cm
				.getIntegerConfig("FlatMaxUnitLossPayment");
		if (compensation > flatMax)
			compensation = flatMax;

		// don't penalize players for deaths.
		if (compensation < 0)
			compensation = 0;

		// return a rounded int
		return Math.round(compensation);

	}

	/**
	 * Method which selects a random player from the winner tree. Used to assign
	 * salvage and on-field pilot captures.
	 */
	private SPlayer selectRandomWinner() {

		try {
			if (shortOp.getWinners().size() <= 0)
				return null;
			else if (shortOp.getWinners().size() == 1) {
				String key = shortOp.getWinners().firstKey();
				return CampaignMain.cm.getPlayer(key);
			}

			/*
			 * We have a tree with more than one person. Get the size, generate
			 * a random, then iterate until we hit the random number, returning
			 * the SPlayer @ stop.
			 */
			int random = CampaignMain.cm.getRandomNumber(
					shortOp.getWinners().size());
			int current = 0;

			for (String wn : shortOp.getWinners().keySet()) {
				if (random == current)
					return CampaignMain.cm.getPlayer(wn);
				// else
				current++;
			}

			return null;
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return null;
		}

	}

	/**
	 * Method which selects a random player from the so.getLosers() tree. Used
	 * to assign salvage which is gained from so.getWinners() (used rarely).
	 */
	private SPlayer selectRandomLoser() {

		try {
			if (shortOp.getLosers().size() <= 0)
				return null;
			else if (shortOp.getLosers().size() == 1) {
				String key = shortOp.getLosers().firstKey();
				return CampaignMain.cm.getPlayer(key);
			}

			/*
			 * We have a tree with more than one person. Get the size, generate
			 * a random, then iterate until we hit the random number, returning
			 * the SPlayer @ stop.
			 */
			int random = CampaignMain.cm.getRandomNumber(
					shortOp.getLosers().size());
			int current = 0;
			for (String ln : shortOp.getLosers().keySet()) {
				if (random == current)
					return CampaignMain.cm.getPlayer(ln);
				// else
				current++;
			}

			return null;
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return null;
		}

	}

	private String newPilotDescription(SUnit u) {
		String result = " ";
		try {
			result = u.getPilot().getName() + " ";
			if (u.getType() == Unit.MEK || u.getType() == Unit.VEHICLE)
				result = "[" + u.getPilot().getGunnery() + "/"
						+ u.getPilot().getPiloting();
			else
				result = u.getModelName() + " [" + u.getPilot().getGunnery();
			if (!u.getPilot().getSkillString(true).equals(" "))
				result += u.getPilot().getSkillString(true);
			result += "]";
			return result;
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return result;
		}

	}

	private void checkMercContracts(SPlayer player, int contractType, int amount) {

		if (!player.getMyHouse().isMercHouse())
			return;

		ContractInfo contract = (((MercHouse) player.getMyHouse())
				.getContractInfo(player));

		if (contract == null)
			return;

		if (contract.getType() != contractType)
			return;

		contract.setEarnedAmount(contract.getEarnedAmount() + amount);

	}

	private void contractFinishedInfo(SPlayer player) {

		if (!player.getMyHouse().isMercHouse())
			return;

		ContractInfo contract = (((MercHouse) player.getMyHouse())
				.getContractInfo(player));

		if (contract == null)
			return;

		int duration = contract.getContractDuration();

		if (duration <= contract.getEarnedAmount()) {
			int payoff = contract.getPayment() / 2;
			player.addMoney(payoff);

			CampaignMain.cm.toUser("You completed your contract with "
					+ contract.getEmployingHouse().getName()
					+ " and received the final payment ("
					+ CampaignMain.cm.moneyOrFluMessage(true, true, payoff,
							false) + ").", player.getName(), true);
			CampaignMain.cm
					.toUser(
							player.getName()
									+ " completed his contract and received his final payment.",
							contract.getOfferingPlayer().getName(), true);
			((MercHouse) player.getMyHouse()).endContract(player);
		}
	}

	private boolean attackerisWinner(ShortOperation so) {
		try {
			for (String player : so.getAttackers().keySet()) {
				if (so.getWinners().containsKey(player))
					return true;
			}
			return false;
		} catch (Exception ex) {
			MWServ.mwlog.errLog(ex);
			return false;
		}

	}

	private String calculatePilotEXP(Operation o, ShortOperation so,
			OperationEntity currEntity, SPlayer owner, boolean allowLevelUp) {
		StringBuffer append = new StringBuffer("");
		String ownerName = owner.getName().toLowerCase();
		SUnit currU = owner.getUnit(currEntity.getID());
		int unitXP = o.getIntValue("BaseUnitXP");
		int totalUnitsAsjusment = o.getIntValue("UnitXPUnitsAdjustment");
		int totalBVAdjustment = o.getIntValue("UnitXPBVAdjustment");
		if (totalUnitsAsjusment > 0)
			unitXP += Math.floor(so.getStartingUnits() / totalUnitsAsjusment);
		if (totalBVAdjustment > 0)
			unitXP += Math.floor(so.getStartingBV() / totalBVAdjustment);

		// load simple XP bonuses
		int winnerXP = o.getIntValue("WinnerBonusUnitXP");
		int defenderXP = o.getIntValue("DefenderBonusUnitXP");
		int flatKillXP = o.getIntValue("KillBonusUnitXP");

		// running total of the XP to add to this unit
		int totalXPforUnit = unitXP;

		// add defender/winner XP
		if (so.getWinners().containsKey(ownerName))
			totalXPforUnit += winnerXP;
		if (so.getDefenders().containsKey(ownerName))
			totalXPforUnit += defenderXP;

		// check the unit for kills. if it has kills, check their quality
		int realKills = 0;
		boolean countKills = !o.getBooleanValue("NoStatisticsMode");
		if (currEntity.getKills().size() > 0 && countKills) {

			// Look for ID matches of all killed units.
			for (Integer id : currEntity.getKills()) {

				// load the killed unit
				OperationEntity killed = salvagableUnits.get(id);
				if (killed == null)
					killed = destroyedUnits.get(id);

				/*
				 * if we cant find it, go to the net kill in the loop. auto-arty
				 * and pilots were weeded out in the possibleSalvageFromReport
				 * method and will return null here.
				 */
				if (killed == null)
					continue;

				// if the owner doesnt have it, its probably an autoassigned
				// unit or mechwarrior.
				SUnit killedUnit = currEntity.getOwner().getUnit(
						currEntity.getID());
				if (killedUnit == null)
					continue;

				// if the killed units owner was on the same team as (or is!)
				// the killer, no XP.
				if (so.getWinners().containsKey(ownerName)
						&& so.getWinners().containsKey(
								killed.getOwnerName().toLowerCase()))
					continue;
				if (so.getLosers().containsKey(ownerName)
						&& so.getLosers().containsKey(
								killed.getOwnerName().toLowerCase())
						&& !drawGame && !freeForAll)
					continue;

				// add BV bonus XP
				totalXPforUnit += flatKillXP;
				int bvForBonusXP = o.getIntValue("KillBonusXPforBV");
				if (bvForBonusXP > 0)
					totalXPforUnit += Math.floor(killedUnit.getBV()
							/ bvForBonusXP);

				// add the kill
				realKills++;
			}

			// add the kills to the pilot and unit
			currU.getPilot().addKill(realKills);

		}// end if(has kills)

		// add the earned XP to the unit
		boolean housePilotGetsXP = o.getBooleanValue("HousePilotsGainXP");
		boolean solPilotGetsXP = o.getBooleanValue("SOLPilotsGainXP");

		// reduce XP to 0 for units should should not get XP
		if (owner.getMyHouse().isNewbieHouse() && !solPilotGetsXP)
			totalXPforUnit = 0;
		if (!owner.getMyHouse().isNewbieHouse() && !housePilotGetsXP)
			totalXPforUnit = 0;

		// on the off chance that XP somehow freakishly became < 0
		if (totalXPforUnit < 0)
			totalXPforUnit = 0;

		if (currU.getPilot().getSkills().has(PilotSkill.QuickStudyID))
			totalXPforUnit = (int) (totalXPforUnit * 1.05);

		// set experience
		currU.getPilot().setExperience(
				currU.getPilot().getExperience() + totalXPforUnit);

		// if the player is a winner, check for level up
		if (so.getWinners().containsKey(ownerName) && allowLevelUp) {
			boolean solPilotCanLevel = o
					.getBooleanValue("SOLPilotsCheckLevelUp");
			boolean housePilotCanLevel = o
					.getBooleanValue("HousePilotsCheckLevelUp");
			if ((owner.getMyHouse().isNewbieHouse() && solPilotCanLevel)
					|| (!owner.getMyHouse().isNewbieHouse() && housePilotCanLevel))
				append.append(((SPilot) currU.getPilot())
						.checkForPilotSkillImprovement(currU, owner));
		}

		// if the unit failes to level, tell the owner about earned XP.
		if (append.toString().trim().equals(""))
			append.append(". " + currU.getPilot().getName() + " gained "
					+ totalXPforUnit + " XP");

		if (realKills == 1)
			append.append(". Earned 1 kill");
		else if (realKills > 1)
			append.append(". Earned " + realKills + " kills");

		return append.toString();
	}// calculatePilotEXP
}// end ShortResolver

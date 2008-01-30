/*
 * MekWars - Copyright (C) 2005 
 * 
 * original author: N. Morris (urgru@users.sourceforge.net)
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

package server.campaign.market2;

import java.util.Iterator;
import java.util.TreeMap;

import server.campaign.CampaignMain;

/**
 * Classic MMNET-style sealed bid auction.
 * @author urgru
 */
public final class HighestSealedBidAuction implements IAuction {
	
	/**
	 * Winner is simply the highest offering person who can
	 * afford to pay. This, codewise, is a truncated Vickrey
	 * Auction. Same mechanism to find highest bidder, but no
	 * downward adjustment.
	 */
	public MarketBid getWinner(MarketListing listing) {
		
		MarketBid winningBid = null;
		
		/*
		 * Assemble a TreeMap of the bids. This is a simple
		 * inversion of the name/bid tree in the listing.
		 */
		TreeMap<MarketBid,String> orderedBids = new TreeMap<MarketBid,String>();
		TreeMap<String, MarketBid> placedBids = listing.getAllBids();
        for (String bidderName : placedBids.keySet())
			orderedBids.put(placedBids.get(bidderName), bidderName);
		
		/*
		 * Now, loop through the ordered bids until we find someone
		 * who can actually AFFORD to pay for the unit at this point.
		 */
		Iterator<MarketBid> i = orderedBids.keySet().iterator();
        
		while (i.hasNext()) {
			MarketBid currBid = i.next();
			IBuyer potentialWinner = CampaignMain.cm.getPlayer(currBid.getBidderName());
			if (potentialWinner == null)
				potentialWinner = CampaignMain.cm.getHouseFromPartialString(currBid.getBidderName(),null);
			
			//if we get a null buyer (someone unenrolled?), continue to next.
			if (potentialWinner == null)
				continue;
			
			//if the buyer can no longer afford his bid, move on
			if (potentialWinner.getMoney() < currBid.getAmount()) {
				if (potentialWinner.isHuman()) {//let a human know ...
				CampaignMain.cm.toUser("The " + listing.getListedModelName() 
						+ " from the BM could have been yours! Unfortunately, you don't have the "
						+ CampaignMain.cm.moneyOrFluMessage(true,true,currBid.getAmount())+" you "
						+ "offered.",currBid.getBidderName(),true);
				}
				continue;
			}
			
			//we found someone who can afford the unit. joy!
			winningBid = currBid;
			break;
		}
		
		/*
		 * If winningBid is still null, we had no valid winner. Just
		 * return a null, and let Market.java sort it out from there.
		 */
		if (winningBid == null)
			return null;
		
		/*
		 * Let everyone else know they they lost, and what the winner paid.
		 */
		while (i.hasNext()) {
			MarketBid losingBid = i.next();
			IBuyer loser = CampaignMain.cm.getPlayer(losingBid.getBidderName());
			if (loser == null)
				loser = CampaignMain.cm.getHouseFromPartialString(losingBid.getBidderName(),null);
			if (loser != null && loser.isHuman()) {
				CampaignMain.cm.toUser("You didn't get the  " + listing.getListedModelName()
						+ " for " + CampaignMain.cm.moneyOrFluMessage(true,true,losingBid.getAmount())+". The "
						+ "winner paid " + CampaignMain.cm.moneyOrFluMessage(true,true,winningBid.getAmount())
						+".",losingBid.getBidderName(),true);
			}	
		}//end while(losers remain)
		
		//return the winner.
		return winningBid;
		
	}//end getWinner

}//end HighestSealedBidAuction.java
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

/*
 * Created on 09.22.2005
 * Torren
 *
 */
package server.campaign.pilot.skills;


import megamek.common.Entity;

import server.campaign.CampaignMain;
import server.campaign.SHouse;

import common.Unit;
import common.campaign.pilot.Pilot;

/**
 *  Pilots with the Quick Study skill gain a 5% bonus to all
 *  XP earned.
 * @author Torren (Jason Tighe)
 */
@SuppressWarnings({"unchecked","serial"})
public class QuickStudySkill extends SPilotSkill {
	
	public QuickStudySkill(int id) {
		super(id, "Quick Study", "QS");
		this.setDescription("Pilots with the Quick Study skill gain a 5% bonus to all XP earned.");
	}
	
	public QuickStudySkill() {
		//TODO: replace with ReflectionProvider
	}
	
	@Override
	public int getChance(int unitType, Pilot p) {
		if (p.getSkills().has(this))
			return 0;
		
		String chance = "chancefor"+this.getAbbreviation()+"for"+Unit.getTypeClassDesc(unitType);
		
		SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());
		
		if ( house == null )
			return CampaignMain.cm.getIntegerConfig(chance);
		
		return Integer.parseInt(house.getConfig(chance));
	}
	@Override
	public int getBVMod(Entity unit){
		return 0;
	}
}
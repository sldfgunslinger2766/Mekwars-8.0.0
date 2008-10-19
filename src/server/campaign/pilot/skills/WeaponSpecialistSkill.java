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

/*
 * Created on 18.04.2004
 *
 */
package server.campaign.pilot.skills;

import java.util.Enumeration;

import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.WeaponType;

import java.util.Hashtable;

import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.pilot.SPilot;

import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.Pilot;


/**
 * A pilot who specializes in a particular weapon receives a -2 to hit modifier on all attacks with that weapon.
 * @@author Torren (Jason Tighe)
 */

public class WeaponSpecialistSkill extends SPilotSkill {
	
	public WeaponSpecialistSkill(int id) {
		super(id, "Weapon Specialist", "WS");
		this.setDescription("A pilot who specializes in a particular weapon receives a -2 to hit modifier on all attacks with that weapon.");
	}
	
	public WeaponSpecialistSkill() {
		//TODO: replace with ReflectionProvider
	}
	
	@Override
	public int getChance(int unitType, Pilot pilot) {
		if (pilot.getSkills().has(this))
			return 0;
		
		String chance = "chancefor"+this.getAbbreviation()+"for"+Unit.getTypeClassDesc(unitType);
		
		SHouse house = CampaignMain.cm.getHouseFromPartialString(pilot.getCurrentFaction());
		
		if ( house == null )
			return CampaignMain.cm.getIntegerConfig(chance);
		
		return Integer.parseInt(house.getConfig(chance));
	}
	
	@Override
	public void modifyPilot(Pilot pilot) {
		pilot.addMegamekOption(new MegaMekPilotOption("weapon_specialist",true));
		//pilot.setBvMod(pilot.getBVMod() +  0.02);
	}
	
	@Override
	public int getBVMod(Entity unit){
		return 0;
	}
	
	@Override
	public int getBVMod(Entity unit, SPilot pilot){
		double totalWeaponBV = 0;
		double weaponSpecialistBVBaseMod = megamek.common.Pilot.getBVSkillMultiplier(unit.getCrew().getGunnery()-2, unit.getCrew().getPiloting());
		
		for (Mounted weapons : unit.getWeaponList()){
			WeaponType weapon = (WeaponType)weapons.getType();
			
			if ( weapon.getName().equalsIgnoreCase(pilot.getWeapon()) ) {
				totalWeaponBV += weapon.getBV(unit);
			}
		}
		return (int)(totalWeaponBV * weaponSpecialistBVBaseMod);
	}
	
	public void assignWeapon(Entity entity, Pilot pilot){
		Hashtable<String, Boolean> uniqueWeapons = new Hashtable<String, Boolean>();
		String bannedWeapons = CampaignMain.cm.getConfig("BannedWSWeapons");
		for (Mounted m : entity.getWeaponList()) {
			if ( bannedWeapons.indexOf(m.getDesc()) >= 0)
				continue;
			uniqueWeapons.put(m.getName(),new Boolean(true));
		}
		
		int selectedWeapon = 0;
		
		if (uniqueWeapons.size() == 0)
			return;
		
		if (uniqueWeapons.size() > 1)
			selectedWeapon = CampaignMain.cm.getRandomNumber(uniqueWeapons.size());
		
		
		for (Enumeration<String> e = uniqueWeapons.keys(); e.hasMoreElements(); selectedWeapon--) {
			String weaponName = e.nextElement();
			if ( selectedWeapon == 0){
				pilot.setWeapon(weaponName);
				break;
			}
		}
		
	}
}

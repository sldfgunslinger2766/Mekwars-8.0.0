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

import common.Unit;
import common.campaign.pilot.Pilot;
import megamek.common.Entity;
import server.campaign.CampaignMain;
import server.campaign.SHouse;

/**
 *  Pilots with the gifted Skill recive an extra 5% chance to
 *  gain a skill when they fail to level Piloting or Gunnery
 *  after a win.
 * @@author Torren (Jason Tighe)
 */

public class GiftedSkill extends SPilotSkill {
   public GiftedSkill(int id) {
      super(id, "Gifted", "GT");
      setDescription("Pilots receive an extra 5% chance to gain a skill when they fail to level Piloting or Gunnery after a win.");
  }

  public GiftedSkill() {
  	//TODO: replace with ReflectionProvider
  }


  @Override
public void modifyPilot(Pilot p) {
	}

	@Override
	public int getChance(int unitType, Pilot p) {
    	if (p.getSkills().has(this)) {
            return 0;
        }

       	String chance = "chancefor"+getAbbreviation()+"for"+Unit.getTypeClassDesc(unitType);

		SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());

		if ( house == null ) {
            return CampaignMain.cm.getIntegerConfig(chance);
        }

		return house.getIntegerConfig(chance);
    }

	@Override
	public int getBVMod(Entity unit){
        return 0;
    }

}

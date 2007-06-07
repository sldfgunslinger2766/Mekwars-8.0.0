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
 */
package server.campaign.pilot.skills;

//import common.Unit;
import megamek.common.Entity;
import server.campaign.CampaignMain;
import server.campaign.SHouse;

import common.Unit;
import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;

/**
 * Reduces the bay-consume of the unit.
 * @author Helge Richter
 */
public class AstechSkill extends SPilotSkill {

    public AstechSkill(int id) {
        super(id, "Astech", "AT");
        setDescription("Reduces the number of techs needed to repair a unit by 1");
    }
    
    public AstechSkill(){
    	//TODO: Remove when no longer necessary
    }

    @Override
	public void modifyPilot(Pilot p) {
        if ( !CampaignMain.cm.isUsingAdvanceRepair() )
            p.setBayModifier(p.getBayModifier() - 1);
	}
    
    @Override
	public int getBVMod(Entity unit){
        return 0;
    }
    
	@Override
	public int getChance(int unitType, Pilot p) {
    	if (p.getSkills().has(PilotSkill.AstechSkillID))
    		return 0;
    	
    	String chance = "chancefor"+this.getAbbreviation()+"for"+Unit.getTypeClassDesc(unitType);

		SHouse house = CampaignMain.cm.getHouseFromPartialString(p.getCurrentFaction());
		
		if ( house == null )
			return CampaignMain.cm.getIntegerConfig(chance);
		
		return Integer.parseInt(house.getConfig(chance));
	}
	
    @Override
	public void addToPilot(Pilot pilot) {
        //this.setLevel(-1);
        pilot.getSkills().add(this);
    }

    /**
     * @param level The level to set.
     */
    @Override
	public void setLevel(int level) {
        
        if ( CampaignMain.cm.isUsingAdvanceRepair() ){
            if ( level == -1 )
                super.setLevel(0);
            //if ( level > super.getLevel() )
            else
                super.setLevel(level);
        }
        else{
            super.setLevel(-1);
        }
    }
}
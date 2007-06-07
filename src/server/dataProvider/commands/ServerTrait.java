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

package server.dataProvider.commands;

import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import common.CampaignData;
import common.util.BinWriter;

import server.dataProvider.ServerCommand;
import server.campaign.CampaignMain;
import server.campaign.SHouse;

/**
 * Retrieve all planet information (if the data cache is lost at client side)
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class ServerTrait implements ServerCommand {

    /**
     * @see server.dataProvider.ServerCommand#execute(java.util.Date,
     *      java.io.PrintWriter, common.CampaignData)
     */
    public void execute(Date timestamp, BinWriter out, CampaignData data)
            throws Exception {
        String factionName = "common";
        Vector traits = CampaignMain.cm.getFactionTraits(factionName);
        out.println(factionName,"TraitLine");
        out.println(traits.size(),"TraitLine");
        for ( int i = 0; i < traits.size(); i++){
            out.println((String)traits.elementAt(i),"TraitLine");
        }
        
        Iterator factions = CampaignMain.cm.getData().getAllHouses().iterator();
        
        while ( factions.hasNext() ){
            SHouse faction = (SHouse) factions.next();
            factionName = faction.getName().toLowerCase();
            traits = CampaignMain.cm.getFactionTraits(factionName);
            out.println(factionName,"TraitLine");
            out.println(traits.size(),"TraitLine");
            for ( int i = 0; i < traits.size(); i++){
                out.println((String)traits.elementAt(i),"TraitLine");
            }
        }
    }
}

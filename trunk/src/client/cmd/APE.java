/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package client.cmd;

import java.awt.Dimension;
import java.util.StringTokenizer;

import common.AdvanceTerrain;

import client.MWClient;

/**
 * @@author Torren (Jason Tighe)
 * 
 * Used for Advance Planet Environments.
 * 
 */

public class APE extends Command {

	/**
	 * @see Command#Command(MMClient)
	 */
	public APE(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
		AdvanceTerrain aTerrain = new AdvanceTerrain(st.nextToken());
		int xsize = Integer.parseInt(st.nextToken());
		int ysize = Integer.parseInt(st.nextToken());
		/*if (Boolean.parseBoolean(client.getserverConfigs("UseStaticMaps")).booleanValue() ){
		    int xboard = Integer.parseInt(st.nextToken());
		    int yboard = Integer.parseInt(st.nextToken());
		    client.setEnvironment(pe, new Dimension(xsize,ysize),new Dimension(xboard,yboard));
		}
		
		else*/
		mwclient.setAdvanceTerrain(aTerrain, new Dimension(xsize,ysize));
	}
}

/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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

package admin.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import common.Equipment;
import common.util.SpringLayoutHelper;
import common.util.UnitUtils;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

import client.MWClient;

public final class ComponentDisplayDialog extends JDialog implements ActionListener, MouseListener, KeyListener, ChangeListener{
	
	//store the client backlink for other things to use
	private static final long serialVersionUID = 8839724432360797850L;
	private MWClient mwclient = null;
	
	public final static int WEAPON_TYPE = 0;
	public final static int MISC_TYPE = 1;
	public final static int AMMO_TYPE = 2;
	public final static int SYSTEM = 8;
	
	private final static String okayCommand = "Add";
	private final static String cancelCommand = "Close";
	
	private String windowName = "Component Display Dialog";
	
	//BUTTONS
	private final JButton okayButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Close");	
	
	//STOCK DIALOUG AND PANE
	private JDialog dialog;
	private JOptionPane pane;
	private JScrollPane MasterPanel = new JScrollPane();
	
	private int displayType = 0;
	
	//Text boxes
	JTabbedPane ConfigPane = new JTabbedPane();
	
	public ComponentDisplayDialog(MWClient c, int type) {
		
		super(c.getMainFrame(),"Component Display Dialog", true);
		
		//save the client
		this.mwclient = c;
		
		//stored values.
		this.displayType = type;
		
		//Set the tooltips and actions for dialouge buttons
		okayButton.setActionCommand(okayCommand);
		cancelButton.setActionCommand(cancelCommand);
		
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		okayButton.setToolTipText("Save");
		cancelButton.setToolTipText("Exit without saving changes");
		
		ConfigPane = new JTabbedPane();
		ConfigPane.addMouseListener(this);
		
		
		//Pull data from the server.
		this.mwclient.getBlackMarketSettings();
		
		//CREATE THE PANELS
		
		if ( displayType == WEAPON_TYPE){
			loadWeaponPanel();
			windowName += " (Weapons)";
		}
		else if ( displayType == AMMO_TYPE){
			loadAmmoPanel();
			windowName += " (Ammo)";
		}
		else{
			loadMiscPanel();
			windowName += " (Misc)";
		}
		
        for ( int pos = ConfigPane.getComponentCount()-1; pos >= 0; pos-- ){
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel);
            
        }

		// Set the user's options
		Object[] options = { okayButton, cancelButton };
		
		Dimension dim = new Dimension(100,200);
		
		ConfigPane.setMaximumSize(dim);
		
		// Create the pane containing the buttons
		pane = new JOptionPane(ConfigPane,JOptionPane.PLAIN_MESSAGE,JOptionPane.DEFAULT_OPTION, null, options, null);
		
		pane.setMaximumSize(dim);
		MasterPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		MasterPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		
		MasterPanel.setMaximumSize(dim);
		
		// Create the main dialog and set the default button
		dialog = pane.createDialog(MasterPanel, windowName);
		dialog.getRootPane().setDefaultButton(cancelButton);
		
		dialog.setMaximumSize(dim);
		dialog.setLocation(Math.max(mwclient.getMainFrame().getLocation().x,mwclient.getMainFrame().getLocation().x+((mwclient.getMainFrame().getWidth()/2)-(dialog.getWidth()/2))),Math.max(mwclient.getMainFrame().getLocation().y+(mwclient.getMainFrame().getHeight()/2)-dialog.getHeight()/2,mwclient.getMainFrame().getLocation().y));
		//Show the dialog and get the user's input
		dialog.setModal(true);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if ( command.equals(okayCommand)){
            for ( int pos = ConfigPane.getComponentCount()-1; pos >= 0; pos-- ){
                JPanel panel = (JPanel) ConfigPane.getComponent(pos);
                findAndSaveConfigs(panel);
            }

            transmitSettings();
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+ "c AdminSaveBlackMarketConfigs");
			dialog.dispose();
			return;
		}
		else if (command.equals(cancelCommand)) {
			//mwclient.getPlayer().resetRepairs();
			dialog.dispose();
		}
	}
	
	public void mouseExited(MouseEvent e){
	}
	
	public void mousePressed(MouseEvent e){
		
	}
	
	public void  mouseEntered(MouseEvent e){
		
	}
	
	public void mouseClicked(MouseEvent arg0) {
		
	}
	
	public void mouseReleased(MouseEvent arg0) {
		
	}
	
	private void loadWeaponPanel(){
		loadWeaponPanelType(TechConstants.T_IS_LEVEL_1);
		loadWeaponPanelType(TechConstants.T_IS_LEVEL_2);
		loadWeaponPanelType(TechConstants.T_IS_LEVEL_3);
		loadWeaponPanelType(TechConstants.T_CLAN_LEVEL_2);
		loadWeaponPanelType(TechConstants.T_CLAN_LEVEL_3);
	}
	
	private void loadAmmoPanel(){
		loadAmmoPanelType(TechConstants.T_IS_LEVEL_1);
		loadAmmoPanelType(TechConstants.T_IS_LEVEL_2);
		loadAmmoPanelType(TechConstants.T_IS_LEVEL_3);
		loadAmmoPanelType(TechConstants.T_CLAN_LEVEL_2);
		loadAmmoPanelType(TechConstants.T_CLAN_LEVEL_3);
	}

	
	private void loadAmmoPanelType(int tech) {
		Enumeration list = EquipmentType.getAllTypes();

		int count = 0;
		int tabNumber = 0;
		JPanel panel = new JPanel(new SpringLayout());
		JTextField textField = null;
		Dimension dim = new Dimension(50,10);
        JPanel masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.X_AXIS));
		panel.add(new JLabel("Component"));
		panel.add(new JLabel("Min. Cost"));
		panel.add(new JLabel("Max. Cost"));
		panel.add(new JLabel("Min. Parts"));
		panel.add(new JLabel("Max. Parts"));
		panel.add(new JLabel("Component"));
		panel.add(new JLabel("Min. Cost"));
		panel.add(new JLabel("Max. Cost"));
		panel.add(new JLabel("Min. Parts"));
		panel.add(new JLabel("Max. Parts"));

		String tabPrefix = "";
		switch (tech){
			case TechConstants.T_IS_LEVEL_2:
				tabPrefix = "IS Level 2-";
				break;
			case TechConstants.T_IS_LEVEL_3:
				tabPrefix = "IS Level 3-";
				break;
			case TechConstants.T_CLAN_LEVEL_2:
				tabPrefix = "Clan Level 2-";
				break;
			case TechConstants.T_CLAN_LEVEL_3:
				tabPrefix = "Clan Level 3-";
				break;
			default:
				tabPrefix = "IS Level 1-";
				break;
			}

		while ( list.hasMoreElements() ){
			Object eq = list.nextElement();
			
			if ( !(eq instanceof AmmoType) )
				continue;
			
			if ( ((AmmoType)eq).getTechLevel() != tech ) {
				//This is done for Unknown and all tech level. Make them all IS Level 1
				if ( tech == TechConstants.T_IS_LEVEL_1 && ((AmmoType)eq).getTechLevel() > tech )
					continue;
				if ( tech != TechConstants.T_IS_LEVEL_1  )
					continue;

			}
			
			String name = ((AmmoType)eq).getName();
			String intName = ((AmmoType)eq).getInternalName();
			panel.add(new JLabel(name));
			
			textField = new JTextField("0");
			textField.setName(intName+"|mincost");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The min. cost for this item on the BM");
			panel.add(textField);
			
			textField = new JTextField("0");
			textField.setName(intName+"|maxcost");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The max. cost for this item on the BM");
			panel.add(textField);
			
			textField = new JTextField("0");
			textField.setName(intName+"|minparts");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The min. number of items that will be on the BM");
			panel.add(textField);
			
			textField = new JTextField("0");
			textField.setName(intName+"|maxparts");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The max. number of items that will be on the BM");
			panel.add(textField);
			
			if ( ++count % 40 == 0 ) {
				panel.addMouseListener(this);
				panel.setAutoscrolls(true);
				SpringLayoutHelper.setupSpringGrid(panel,10);
				masterBox.add(panel);
				
				tabNumber++;
				ConfigPane.addTab(tabPrefix+tabNumber,null,panel,tabPrefix+tabNumber);
				panel =  new JPanel(new SpringLayout());
				panel.add(new JLabel("Component"));
				panel.add(new JLabel("Min. Cost"));
				panel.add(new JLabel("Max. Cost"));
				panel.add(new JLabel("Min. Parts"));
				panel.add(new JLabel("Max. Parts"));
				panel.add(new JLabel("Component"));
				panel.add(new JLabel("Min. Cost"));
				panel.add(new JLabel("Max. Cost"));
				panel.add(new JLabel("Min. Parts"));
				panel.add(new JLabel("Max. Parts"));
			}
		}

		if ( panel.getComponentCount() > 0 ) {
			tabNumber++;
			SpringLayoutHelper.setupSpringGrid(panel,10);
			ConfigPane.addTab(tabPrefix+tabNumber,null,panel,tabPrefix+tabNumber);
		}
		
        MasterPanel.add(ConfigPane);

	}
	
	private void loadWeaponPanelType(int tech) {
		Enumeration list = EquipmentType.getAllTypes();

		int count = 0;
		int tabNumber = 0;
		JPanel panel = new JPanel(new SpringLayout());
		JTextField textField = null;
		Dimension dim = new Dimension(50,10);

		panel.add(new JLabel("Component"));
		panel.add(new JLabel("Min. Cost"));
		panel.add(new JLabel("Max. Cost"));
		panel.add(new JLabel("Min. Parts"));
		panel.add(new JLabel("Max. Parts"));
		panel.add(new JLabel("Component"));
		panel.add(new JLabel("Min. Cost"));
		panel.add(new JLabel("Max. Cost"));
		panel.add(new JLabel("Min. Parts"));
		panel.add(new JLabel("Max. Parts"));

		String tabPrefix = "";
		switch (tech){
			case TechConstants.T_IS_LEVEL_2:
				tabPrefix = "IS Level 2-";
				break;
			case TechConstants.T_IS_LEVEL_3:
				tabPrefix = "IS Level 3-";
				break;
			case TechConstants.T_CLAN_LEVEL_2:
				tabPrefix = "Clan Level 2-";
				break;
			case TechConstants.T_CLAN_LEVEL_3:
				tabPrefix = "Clan Level 3-";
				break;
			default:
				tabPrefix = "IS Level 1-";
				break;
			}

		while ( list.hasMoreElements() ){
			Object eq = list.nextElement();
			
			if ( !(eq instanceof WeaponType) )
				continue;
			
			if ( ((WeaponType)eq).getTechLevel() != tech ) {
				//This is done for Unknown and all tech level. Make them all IS Level 1
				if ( tech == TechConstants.T_IS_LEVEL_1 && ((WeaponType)eq).getTechLevel() > tech )
					continue;

				if ( tech != TechConstants.T_IS_LEVEL_1  )
					continue;
			}
			
			String name = ((WeaponType)eq).getName();
			String intName = ((WeaponType)eq).getInternalName();
			panel.add(new JLabel(name));
			
			textField = new JTextField("0");
			textField.setName(intName+"|mincost");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The min. cost for this item on the BM");
			panel.add(textField);
			
			textField = new JTextField("0");
			textField.setName(intName+"|maxcost");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The max. cost for this item on the BM");
			panel.add(textField);
			
			textField = new JTextField("0");
			textField.setName(intName+"|minparts");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The min. number of items that will be on the BM");
			panel.add(textField);
			
			textField = new JTextField("0");
			textField.setName(intName+"|maxparts");
			textField.setMaximumSize(dim);
			textField.setToolTipText("The max. number of items that will be on the BM");
			panel.add(textField);
			
			if ( ++count % 40 == 0 ) {
				panel.addMouseListener(this);
				SpringLayoutHelper.setupSpringGrid(panel,10);
				tabNumber++;
				ConfigPane.addTab(tabPrefix+tabNumber,null,panel,tabPrefix+tabNumber);
		
				panel =  new JPanel(new SpringLayout());
				panel.add(new JLabel("Component"));
				panel.add(new JLabel("Min. Cost"));
				panel.add(new JLabel("Max. Cost"));
				panel.add(new JLabel("Min. Parts"));
				panel.add(new JLabel("Max. Parts"));
				panel.add(new JLabel("Component"));
				panel.add(new JLabel("Min. Cost"));
				panel.add(new JLabel("Max. Cost"));
				panel.add(new JLabel("Min. Parts"));
				panel.add(new JLabel("Max. Parts"));
			}
		}
		
		if ( panel.getComponentCount() > 0 ) {
			tabNumber++;
			SpringLayoutHelper.setupSpringGrid(panel,10);
			ConfigPane.addTab(tabPrefix+tabNumber,null,panel,tabPrefix+tabNumber);
		}

		MasterPanel.add(ConfigPane);

	}

	private void loadMiscPanelType(int tech) {
		Enumeration list = EquipmentType.getAllTypes();

		int count = 0;
		int tabNumber = 0;
		JPanel panel = new JPanel(new SpringLayout());
		JTextField textField = null;
		Dimension dim = new Dimension(50,10);

		panel.add(new JLabel("Component"));
		panel.add(new JLabel("Min. Cost"));
		panel.add(new JLabel("Max. Cost"));
		panel.add(new JLabel("Min. Parts"));
		panel.add(new JLabel("Max. Parts"));
		panel.add(new JLabel("Component"));
		panel.add(new JLabel("Min. Cost"));
		panel.add(new JLabel("Max. Cost"));
		panel.add(new JLabel("Min. Parts"));
		panel.add(new JLabel("Max. Parts"));

		String tabPrefix = "";
		switch (tech){
			case TechConstants.T_IS_LEVEL_2:
				tabPrefix = "IS Level 2-";
				break;
			case TechConstants.T_IS_LEVEL_3:
				tabPrefix = "IS Level 3-";
				break;
			case TechConstants.T_CLAN_LEVEL_2:
				tabPrefix = "Clan Level 2-";
				break;
			case TechConstants.T_CLAN_LEVEL_3:
				tabPrefix = "Clan Level 3-";
				break;
			case ComponentDisplayDialog.SYSTEM:
				tabPrefix = "Systems";
				break;
			default:
				tabPrefix = "IS Level 1-";
				break;
			}

		if ( tech == ComponentDisplayDialog.SYSTEM ) {
				String name = Mech.systemNames[Mech.SYSTEM_LIFE_SUPPORT];
				String intName = Mech.systemNames[Mech.SYSTEM_LIFE_SUPPORT];

				panel.add(new JLabel(name));
				
				textField = new JTextField("0");
				textField.setName(intName+"|mincost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxcost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|minparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. number of items that will be on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. number of items that will be on the BM");
				panel.add(textField);

				name = Mech.systemNames[Mech.SYSTEM_SENSORS];
				intName = Mech.systemNames[Mech.SYSTEM_SENSORS];

				panel.add(new JLabel(name));
				
				textField = new JTextField("0");
				textField.setName(intName+"|mincost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxcost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|minparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. number of items that will be on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. number of items that will be on the BM");
				panel.add(textField);

				name = "Actuator";
				intName = "Actuator";

				panel.add(new JLabel(name));
				
				textField = new JTextField("0");
				textField.setName(intName+"|mincost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxcost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|minparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. number of items that will be on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. number of items that will be on the BM");
				panel.add(textField);

				for ( int pos = 0; pos <= Mech.GYRO_HEAVY_DUTY;pos++) {
					name = Mech.getGyroTypeString(pos);
					intName = Mech.getGyroTypeString(pos);

					panel.add(new JLabel(name));

					textField = new JTextField("0");
					textField.setName(intName+"|mincost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxcost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|minparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. number of items that will be on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. number of items that will be on the BM");
					panel.add(textField);
				}

				for ( int pos = 0; pos <= Mech.COCKPIT_DUAL;pos++) {
					name = Mech.getCockpitTypeString(pos);
					intName = Mech.getCockpitTypeString(pos);

					panel.add(new JLabel(name));

					textField = new JTextField("0");
					textField.setName(intName+"|mincost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxcost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|minparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. number of items that will be on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. number of items that will be on the BM");
					panel.add(textField);
				}
				
				for ( int pos = 0; pos <= UnitUtils.CLAN_XXL_ENGINE;pos++) {
					name = UnitUtils.ENGINE_TECH_STRING[pos];
					intName = UnitUtils.ENGINE_TECH_STRING[pos];

					panel.add(new JLabel(name));

					textField = new JTextField("0");
					textField.setName(intName+"|mincost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxcost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|minparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. number of items that will be on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. number of items that will be on the BM");
					panel.add(textField);
				}

				SpringLayoutHelper.setupSpringGrid(panel,10);
				ConfigPane.addTab(tabPrefix,null,panel,tabPrefix);
		}
		else {
			while ( list.hasMoreElements() ){
				Object eq = list.nextElement();
				
				if ( !(eq instanceof MiscType) )
					continue;
				
				if ( ((MiscType)eq).getTechLevel() != tech ) {
					//This is done for Unknown and all tech level. Make them all IS Level 1
					if ( tech == TechConstants.T_IS_LEVEL_1 && ((MiscType)eq).getTechLevel() > tech )
						continue;
					if ( tech != TechConstants.T_IS_LEVEL_1  )
						continue;
				}
				
				String name = ((MiscType)eq).getName();
				String intName = ((MiscType)eq).getInternalName();
				
				if ( name.equalsIgnoreCase("standard")) {
					name = "Armor (STD)";
					intName = "Armor (STD)";
				}
				panel.add(new JLabel(name));
				
				textField = new JTextField("0");
				textField.setName(intName+"|mincost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxcost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|minparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. number of items that will be on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. number of items that will be on the BM");
				panel.add(textField);
				
				if ( name.equalsIgnoreCase("Armor (STD)")) {
					count++;
					name = "IS (STD)";
					intName = "IS (STD)";
					panel.add(new JLabel(name));
					
					textField = new JTextField("0");
					textField.setName(intName+"|mincost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxcost");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. cost for this item on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|minparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The min. number of items that will be on the BM");
					panel.add(textField);
					
					textField = new JTextField("0");
					textField.setName(intName+"|maxparts");
					textField.setMaximumSize(dim);
					textField.setToolTipText("The max. number of items that will be on the BM");
					panel.add(textField);
				}				
				if ( ++count % 40 == 0 ) {
					panel.addMouseListener(this);
					SpringLayoutHelper.setupSpringGrid(panel,10);
					
					tabNumber++;
					ConfigPane.addTab(tabPrefix+tabNumber,null,panel,tabPrefix+tabNumber);
					
					panel =  new JPanel(new SpringLayout());
					panel.add(new JLabel("Component"));
					panel.add(new JLabel("Min. Cost"));
					panel.add(new JLabel("Max. Cost"));
					panel.add(new JLabel("Min. Parts"));
					panel.add(new JLabel("Max. Parts"));
					panel.add(new JLabel("Component"));
					panel.add(new JLabel("Min. Cost"));
					panel.add(new JLabel("Max. Cost"));
					panel.add(new JLabel("Min. Parts"));
					panel.add(new JLabel("Max. Parts"));
				}
			}
			
			if ( tech == TechConstants.T_IS_LEVEL_1 ) {
				String name = "Ammo Bin";
				String intName = "Ammo Bin";

				panel.add(new JLabel(name));

				textField = new JTextField("0");
				textField.setName(intName+"|mincost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxcost");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. cost for this item on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|minparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The min. number of items that will be on the BM");
				panel.add(textField);
				
				textField = new JTextField("0");
				textField.setName(intName+"|maxparts");
				textField.setMaximumSize(dim);
				textField.setToolTipText("The max. number of items that will be on the BM");
				panel.add(textField);

			}

			if ( panel.getComponentCount() > 0 ) {
				tabNumber++;
				SpringLayoutHelper.setupSpringGrid(panel,10);
				ConfigPane.addTab(tabPrefix+tabNumber,null,panel,tabPrefix+tabNumber);
			}
		}
		MasterPanel.add(ConfigPane);
		
	}

	private void loadMiscPanel(){
		loadMiscPanelType(TechConstants.T_IS_LEVEL_1);
		loadMiscPanelType(TechConstants.T_IS_LEVEL_2);
		loadMiscPanelType(TechConstants.T_IS_LEVEL_3);
		loadMiscPanelType(TechConstants.T_CLAN_LEVEL_2);
		loadMiscPanelType(TechConstants.T_CLAN_LEVEL_3);
		loadMiscPanelType(SYSTEM);
	}
	
	public void keyTyped(KeyEvent arg0) {
		// Auto-generated method stub
	}
	
	public void keyPressed(KeyEvent arg0) {
		// Auto-generated method stub
	}
	
	public void keyReleased(KeyEvent arg0) {
		// Auto-generated method stub
	}
	
	public void stateChanged(ChangeEvent arg0) {
		// Auto-generated method stub
	}
	

    /**
     * This Method tunnels through all of the panels to find the textfields
     * and checkboxes. Once it find one it grabs the Name() param of the object
     * and uses that to find out what the setting should be from the
     * mwclient.getserverConfigs() method.
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel){
        String key = null;
        
        for ( int fieldPos = panel.getComponentCount()-1; fieldPos >= 0; fieldPos--){
            
            Object field = panel.getComponent(fieldPos);
            
            if ( field instanceof JPanel)
                findAndPopulateTextAndCheckBoxes((JPanel)field);
            else if ( field instanceof JTextField){
                JTextField textBox = (JTextField)field;
                
                key = textBox.getName();
                if ( key == null )
                    continue;
                
                textBox.setMaximumSize(new Dimension(100,10));
                try{
                	StringTokenizer keys = new StringTokenizer(key,"|");
                	
                	Equipment equipment = mwclient.getBlackMarketEquipmentList().get(keys.nextToken());
                	
                	if ( equipment == null )
                		textBox.setText("0");
                	else {
                		String type = keys.nextToken();
                		
                		if ( type.equalsIgnoreCase("mincost") )
                			textBox.setText(Double.toString(equipment.getMinCost()));
                		else if ( type.equalsIgnoreCase("maxcost"))
                			textBox.setText(Double.toString(equipment.getMaxCost()));
                		else if ( type.equalsIgnoreCase("minparts"))
                			textBox.setText(Integer.toString(equipment.getMinProduction()));
                		else 
                			textBox.setText(Integer.toString(equipment.getMaxProduction()));
                			
                	}
                }catch(Exception ex){
                    textBox.setText("N/A");
                }
            }   
        }
    }

    public void transmitSettings() {
    	
    	for ( String key : mwclient.getBlackMarketEquipmentList().keySet() ) {
    		Equipment bme = mwclient.getBlackMarketEquipmentList().get(key);
    		
	        if ( bme.isUpdated() )
	        	mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+ "c AdminSetBlackMarketSetting#"+key+"#"+bme.getMinCost()+"#"+bme.getMaxCost()+"#"+bme.getMinProduction()+"#"+bme.getMaxProduction());
    	}

    }
    
    /**
     * This method will tunnel through all of the panels of the config UI
     * to find any changed text fields. The data is saved to the Equipment Hashmap
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel){
        String key = null;
        String value = null;
        for ( int fieldPos = panel.getComponentCount()-1; fieldPos >= 0; fieldPos--){
            
            Object field = panel.getComponent(fieldPos);

            //found another JPanel keep digging!
            if ( field instanceof JPanel )
                findAndSaveConfigs((JPanel)field);
            else if ( field instanceof JTextField){
                JTextField textBox = (JTextField)field;
                
                value = textBox.getText();
                key = textBox.getName();
                
                if ( key == null || value == null )
                    continue;
        
                StringTokenizer keys = new StringTokenizer(key,"|");
                
                String internalName = keys.nextToken();
                
                Equipment equipment = mwclient.getBlackMarketEquipmentList().get(internalName);
                
                if ( equipment == null ) {
                	equipment = new Equipment();
                	equipment.setEquipmentInternalName(key);
                }

                String fieldKey = keys.nextToken();
                
        		if ( fieldKey.equalsIgnoreCase("mincost") )
        			equipment.setMinCost(Double.parseDouble(value));
        		else if ( fieldKey.equalsIgnoreCase("maxcost"))
        			equipment.setMaxCost(Double.parseDouble(value));
        		else if ( fieldKey.equalsIgnoreCase("minparts"))
        			equipment.setMinProduction(Integer.parseInt(value));
        		else 
        			equipment.setMaxProduction(Integer.parseInt(value));

        		mwclient.getBlackMarketEquipmentList().put(internalName, equipment);
        		
        		//reduce bandwidth only send things that have changed.
                /*if ( !mwclient.getserverConfigs(key).equalsIgnoreCase(value) )
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+ "c AdminChangeBlackMarketConfig#"+key+"#"+value+"#CONFIRM");*/
            }
        }

    }

}//end ComponentDisplayDialog.java

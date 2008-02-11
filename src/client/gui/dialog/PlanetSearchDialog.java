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
 * 
 * Portions of this dialog derived from work done by Imanuel Schultz. Original
 * part of MegaMekNET's client.gui.actions pacakge as SearchPlanetActionListener.java.
 * See http://www.sourceforge.net/projects/megameknet for more info.
 */

package client.gui.dialog;

//awt imports
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

//util imports
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

//swing imports
import javax.swing.BoxLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

//MekWars imports
import client.MWClient;
import client.gui.InnerStellarMap;
import common.util.SpringLayoutHelper;
import common.Planet;

/*
 * Base dialog, derived from MMNET's SearchPlanetListener, allows players
 * to search for planets using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 * 
 * @urgru 5.2.05
 */
@SuppressWarnings({"unchecked","serial"})
public class PlanetSearchDialog extends JDialog implements ActionListener {

	//variables
	private final InnerStellarMap map;
	private final Collection planets;
	private final TreeSet planetNames;
	
	private JList matchingPlanetsList;
	private JScrollPane scrollPane;//holds the JList
	private JTextField nameField;//input field
	private final JButton okayButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");	
	private final String okayCommand = "Okay";
	
	//constructor
	public PlanetSearchDialog(InnerStellarMap map, MWClient mwclient) {
		
		/*
		 * NOTE: variables are final in order to
		 * allow access by caretUpdate()
		 */
		
		//super, and variable saves
		super(mwclient.getMainFrame(),"Planet Search", true);//dummy frame as owner
		this.map = map;
		this.planets = mwclient.getData().getAllPlanets();
		
		//setup the a list of names to feed into a list
		planetNames = new TreeSet();//tree to alpha sort
		for (Iterator it = planets.iterator(); it.hasNext();)
            planetNames.add(((Planet)it.next()).getName());
		final Object[] allPlanetNames = planetNames.toArray();
		
		//construct the planet name list
		matchingPlanetsList = new JList(allPlanetNames);
		matchingPlanetsList.setVisibleRowCount(20);
		matchingPlanetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		//the name field, for user input. caretUpdate
		//does most of the work to update list contents
		nameField = new JTextField();//field for user input
		nameField.addCaretListener(new CaretListener(){
			public void caretUpdate(CaretEvent e) {
				new Thread() {
					@Override
					public void run(){
						String text = nameField.getText();
						if (text == null || text.equals("")) {
							matchingPlanetsList.setListData(allPlanetNames);
							return;
						}
						ArrayList possiblePlanets = new ArrayList();
						text = text.toLowerCase();
						for (Iterator it = planetNames.iterator(); it.hasNext();) {
							String curPlanet = ((String)it.next());
							if (curPlanet.toLowerCase().indexOf(text) != -1)
								possiblePlanets.add(curPlanet);
						}
						matchingPlanetsList.setListData(possiblePlanets.toArray());
						
						/*
						 * Try to select a planet with a STARTING string which matched
						 * the seach index. If none is available, use the first planet.
						 * 
						 * Hacky, but functional. @urgru 5.2.05
						 */
						boolean shouldContinue = true;
						int element = 0;
						Iterator it = possiblePlanets.iterator();
						while (it.hasNext() && shouldContinue) {
							String name = (String)it.next();
							if (name.toLowerCase().startsWith(text)) {
								matchingPlanetsList.setSelectedIndex(element);
								shouldContinue = false;
							}
							element++;
						}
						
						//looped through without finding a starting match. set 0.
						if (shouldContinue) {
							matchingPlanetsList.setSelectedIndex(0);
						}
						
					}
				}.start();
			}
		});
		
		//put the list in a scroll pane
		scrollPane = new JScrollPane(matchingPlanetsList);
		scrollPane.setAlignmentX(LEFT_ALIGNMENT);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		//set up listeners for the buttons
		okayButton.setActionCommand(okayCommand);
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		//do some formatting. rawr.
		JPanel springPanel = new JPanel(new SpringLayout());
		springPanel.add(nameField);
		springPanel.add(scrollPane);
		SpringLayoutHelper.setupSpringGrid(springPanel,2,1);
		
		JPanel buttonFlow = new JPanel();
		buttonFlow.add(okayButton);
		buttonFlow.add(cancelButton);
		
		JPanel generalLayout = new JPanel();
		generalLayout.setLayout(new BoxLayout(generalLayout, BoxLayout.Y_AXIS));
		generalLayout.add(springPanel);
		generalLayout.add(buttonFlow);
		this.getContentPane().add(generalLayout);
		this.pack();
		
		this.checkMinimumSize();
		this.setResizable(true);
		
		//set a default button
		this.getRootPane().setDefaultButton(okayButton);
		
		//center the dialog.
		this.setLocationRelativeTo(null);
       
	}
	
	
	/**
	 * OK or CANCEL buttons pressed. Handle any
	 * changes and then close the dialouge.
	 */
	public void actionPerformed(ActionEvent event) {
		
		String command = event.getActionCommand();
		
		if (command.equals(okayCommand)) {
			 String selectedPlanet = (String)matchingPlanetsList.getSelectedValue();
		        if (selectedPlanet == null)
		        	selectedPlanet = nameField.getText();
		        if (selectedPlanet == null || selectedPlanet.equals(""))
		        	return;
		        if (matchingPlanetsList.getModel().getSize() == 1)
		        	selectedPlanet = (String)matchingPlanetsList.getModel().getElementAt(0);
		        for (Iterator it = planets.iterator(); it.hasNext();) {
		        	Planet planet = (Planet)it.next();
		        	if (selectedPlanet.equals(planet.getName())) {
		        		map.setSelectedPlanet(planet);
		        		map.activate(planet, true);
		        		map.saveMapSelection(planet);
		        		this.dispose();
		        		return;
		        	}
		        }
		        JOptionPane.showMessageDialog(null,"Unknown Planet");
		}
		
		//dispose of the dialog
		this.dispose();
		
	}//end actionPerformed
	
	private void checkMinimumSize() {
		
		Dimension curDim = this.getSize();
		
		int height = 0;
		int width = 0;
		boolean shouldRedraw = false;
		
		if (curDim.getWidth() < 300) {
			width = 300;
			shouldRedraw = true;
		} else
			width = (int)curDim.getWidth();
		
		if (curDim.getHeight() < 300) {
			height = 300;
			shouldRedraw = true;
		} else
			height = (int)curDim.getHeight();
		
		if (shouldRedraw) {
			this.setSize(new Dimension(width, height));
		}
		
	}//end checkMinimumSize

}
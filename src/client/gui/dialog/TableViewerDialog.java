/*
 * MekWars - Copyright (C) 2004, 2005 
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

package client.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

import common.CampaignData;
import common.House;
import common.Unit;
import common.campaign.pilot.Pilot;

import client.MWClient;
import client.campaign.CUnit;
import common.util.SpringLayoutHelper;
import common.util.UnitUtils;
import client.gui.TableSorter;
import client.util.CUnitComparator;

public class TableViewerDialog extends JFrame implements ItemListener {

    /**
     * 
     */
    private static final long serialVersionUID = -5449211786198003020L;
    // ivars
    JComboBox weightClassCombo;
    JComboBox factionCombo;
    JComboBox unitTypeCombo;

    JLabel factionLabel = new JLabel("Faction: ", SwingConstants.RIGHT);
    JLabel typeLabel = new JLabel("Type: ", SwingConstants.RIGHT);
    JLabel weightLabel = new JLabel("Class: ", SwingConstants.RIGHT);
    JLabel percentageLabel = new JLabel("Total Percentage: ", SwingConstants.CENTER);

    Object[] factionArray;
    String[] unitTypeArray = { "Mek", "Vehicle", "BattleArmor", "Infantry", "ProtoMek", "Aero" };
    String[] weightClassArray = { "Light", "Medium", "Heavy", "Assault" };

    int factionSort = 0;
    int unitSort = 0;
    int weightSort = 0;

    JTable generalTable = new JTable();
    JScrollPane generalScrollPane = new JScrollPane();

    JButton closeButton = new JButton("Close");
    JButton refreshButton = new JButton("Reload Data");

    // model and whatnot for refreshing
    TableViewerModel tvModel;

    // maps and sorts
    TreeMap<Object, TableUnit> currentUnits;
    TableUnit[] sortedUnits = {};// sorts generated from the map.

    MWClient mwclient;

    // constructor
    public TableViewerDialog(MWClient client) {
        super("Table Browser");

        mwclient = client;
        currentUnits = new TreeMap<Object, TableUnit>();
        generalScrollPane = new JScrollPane();

        // alpha sorted faction array. hacky and evil.
        TreeSet<String> factionNames = new TreeSet<String>();// tree to alpha
        // sort
        Iterator<House> i = mwclient.getData().getAllHouses().iterator();
        while (i.hasNext()) {
            House house = i.next();

            if (house.getId() > -1) {
                factionNames.add(house.getName());
            }
        }
        factionNames.add("Common");
        factionArray = factionNames.toArray();

        // CONSTRUCT GUI
        // make combo boxes
        weightClassCombo = new JComboBox(weightClassArray);
        factionCombo = new JComboBox(factionArray);
        unitTypeCombo = new JComboBox();
        
        for ( int type = Unit.MEK; type < Unit.MAXBUILD; type++ ){
            unitTypeCombo.addItem(Unit.getTypeClassDesc(type));
        }

        // set max combo heights
        Dimension comboDim = new Dimension();
        comboDim.setSize(factionCombo.getMinimumSize().getWidth() * 1.5, factionLabel.getMinimumSize().getHeight() + 2);

        factionCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        factionCombo.setMaximumSize(comboDim);
        weightClassCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        weightClassCombo.setMaximumSize(comboDim);
        unitTypeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        unitTypeCombo.setMaximumSize(comboDim);

        // put the combos and their labels into a spring
        JPanel comboPanel = new JPanel(new SpringLayout());
        comboPanel.add(factionLabel);
        comboPanel.add(factionCombo);
        comboPanel.add(typeLabel);
        comboPanel.add(unitTypeCombo);
        comboPanel.add(weightLabel);
        comboPanel.add(weightClassCombo);
        SpringLayoutHelper.setupSpringGrid(comboPanel, 3, 2);

        /*
         * Load preserved combo selection settings.
         */
        // In the absence of a saved faction, load the player's own.
        try {
            String previousItem = mwclient.getConfigParam("TABLEVIEWERFACTION");
            factionCombo.setSelectedItem(previousItem);
            factionSort = factionCombo.getSelectedIndex();
        } catch (Exception e) {
            factionCombo.setSelectedItem(mwclient.getPlayer().getHouse());
        }

        // If type/weight data are missing, select the first item in the combo.
        try {
            String previousItem = mwclient.getConfigParam("TABLEVIEWERTYPE");
            unitTypeCombo.setSelectedItem(previousItem);
            unitSort = unitTypeCombo.getSelectedIndex();
        } catch (Exception e) {
            unitTypeCombo.setSelectedIndex(-1);
        }

        try {
            String previousItem = mwclient.getConfigParam("TALEVIEWERWEIGHT");
            weightClassCombo.setSelectedItem(previousItem);
            weightSort = weightClassCombo.getSelectedIndex();
        } catch (Exception e) {
            weightClassCombo.setSelectedIndex(-1);
        }

        // add listeners to the combo boxes
        factionCombo.addItemListener(this);
        unitTypeCombo.addItemListener(this);
        weightClassCombo.addItemListener(this);

        // allow the close button to actually close things ...
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshButton_ActionPerformed();
            }
        });

        // set up the BM-style table
        tvModel = new TableViewerModel(mwclient, currentUnits, sortedUnits);
        TableSorter sorter = new TableSorter(tvModel, client, TableSorter.SORTER_BUILDTABLES);
        generalTable.setModel(sorter);

        // make it possible to double click for unit info
        generalTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TableUnit u = getUnitAtRow(generalTable.getSelectedRow());
                    if (u == null) {
                        return;
                    }

                    Entity theEntity = u.getEntity();
                    theEntity.loadAllWeapons();

                    JFrame InfoWindow = new JFrame();
                    MechDetailDisplay MechDetailInfo = new MechDetailDisplay();

                    InfoWindow.getContentPane().add(MechDetailInfo);
                    InfoWindow.setSize(220, 400);
                    InfoWindow.setResizable(false);

                    MechDetailInfo.displayEntity(theEntity, theEntity.calculateBattleValue(), mwclient.getConfig().getImage("CAMO"));
                    InfoWindow.setTitle(u.getModelName());
                    InfoWindow.setLocationRelativeTo(mwclient.getMainFrame());// center
                    // it
                    InfoWindow.setVisible(true);
                }
            }
        });// end addMouseListener();

        // set the proper cell renderers
        for (int j = 0; j < tvModel.getColumnCount(); j++) {
            generalTable.getColumnModel().getColumn(j).setCellRenderer(tvModel.getRenderer());
        }

        // add sort listener to column heads
        sorter.addMouseListenerToHeaderInTable(generalTable);

        // allow only single selections
        generalTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        /*
         * Unlike the BM table, the BuildTableTable (huh?) doesn't need a
         * ListSelectionListener. No buttons to activate/deactivate and no
         * images to update w/ proper .gifs.
         */

        // make the table double buffered
        generalTable.setDoubleBuffered(true);

        // add the table to the scroll pane
        generalScrollPane.setToolTipText("Click on column header to sort.");
        generalScrollPane.setViewportView(generalTable);
        generalScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0), BorderFactory.createLineBorder(Color.BLACK, 1)));

        // make a box layout to hold the combos and table
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        JPanel buttonPanel = new JPanel(new SpringLayout());
        
        // center the percentage label
        percentageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        percentageLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        percentageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // add combo boxes scrollPane and percentage info to the boxpanel
        boxPanel.add(comboPanel);
        boxPanel.add(generalScrollPane);
        boxPanel.add(percentageLabel);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        SpringLayoutHelper.setupSpringGrid(buttonPanel, 2);
        
        boxPanel.add(buttonPanel);
            
        // give the box a small border
        boxPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // add the box to the main panel
        this.getContentPane().add(boxPanel);

        // load the default tables/units
        this.loadTables();
        this.refresh();
        this.setLocationRelativeTo(mwclient.getMainFrame());
        this.setVisible(true);
    }

    // refresh
    public void refresh() {
        tvModel.refreshModel();
        generalTable.setPreferredSize(new Dimension(generalTable.getWidth(), generalTable.getRowHeight() * (generalTable.getRowCount())));
        generalTable.revalidate();
    }

    // Override show to center on screen.
    @Override
    public void setVisible(boolean show) {

        this.pack();
        this.setLocationRelativeTo(mwclient.getMainFrame());

        this.setSize(720, 575);
        setResizable(false);

        super.setVisible(show);
    }

    // methods
    public TableUnit getUnitAtRow(int row) {

        String filename = (String) generalTable.getModel().getValueAt(row, TableViewerModel.FILENAME);
        if (filename != null)
            return (TableUnit) currentUnits.get(filename);

        // else
        return null;
    }

    /**
     * Method to conform with ItemListener. Takes item events from the combo
     * boxes and triggers table loads.
     */
    public void itemStateChanged(ItemEvent i) {

        /*
         * Do not re-load tables and units if there is no actual change in the
         * selection.
         */
        JComboBox source = (JComboBox) i.getSource();
        if (source == unitTypeCombo && unitSort == unitTypeCombo.getSelectedIndex())
            return;
        else if (source == weightClassCombo && weightSort == weightClassCombo.getSelectedIndex())
            return;
        else if (source == factionCombo && factionSort == factionCombo.getSelectedIndex())
            return;

        // fails passed. reload the tables.
        loadTables();

        // save the current sort modes locally
        unitSort = unitTypeCombo.getSelectedIndex();
        weightSort = weightClassCombo.getSelectedIndex();
        factionSort = factionCombo.getSelectedIndex();

        // save the new sort to the config
        mwclient.getConfig().setParam("TABLEVIEWERFACTION", (String) factionCombo.getSelectedItem());
        mwclient.getConfig().setParam("TABLEVIEWERTYPE", (String) unitTypeCombo.getSelectedItem());
        mwclient.getConfig().setParam("TALEVIEWERWEIGHT", (String) weightClassCombo.getSelectedItem());
        mwclient.getConfig().saveConfig();
        mwclient.setConfig();

        // refresh the display
        this.refresh();
    }

    /**
     * Helper which checks strings to see if they end with a known-good unit
     * file extension.
     */
    public boolean hasValidExtension(String l) {
        String lc = l.toLowerCase();
        if (lc.endsWith(".blk") || lc.endsWith(".mtf") || lc.endsWith(".hmp") || lc.endsWith(".xml") || lc.endsWith(".hmv") || lc.endsWith(".mep") || lc.endsWith(".mul"))
            return true;
        // else
        return false;
    }

    /**
     * Helper which takes a File entry and returns an input stream. Handles
     * errors, etc. to reduce clutter in loadTables().
     */
    public InputStream getEntryInputStream(File bf) {
        InputStream is = null;
        try {
            is = new FileInputStream(bf);
            return is;
        } catch (IOException io) {
            return null;
        }
    }

    /**
     * Helper which loops through a table, ignoring filenames and tablenames.
     * Returns total table weighting for use when analyzing names.
     */
    public int getTotalWeightForTable(File bf) {

        int totalweight = 0;

        try {
            FileInputStream fis = new FileInputStream(bf);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()) {
                // read the line and remove excess whitespace
                String l = dis.readLine();

                if (l == null || l.trim().length() == 0)
                    continue;

                l = l.trim();
                l = l.replaceAll("\\s+", " ");
                if (l.indexOf(" ") == 0)
                    l = l.substring(1, l.length());

                StringTokenizer ST = new StringTokenizer(l);
                totalweight += Integer.parseInt((String) ST.nextElement());
            }
            fis.close();
            dis.close();
        } catch (Exception e) {
            // nothing
        }

        // System.out.println("totalweight of current table: " + totalweight);
        return totalweight;
    }

    /**
     * Helper method which reads a given layer of tables. Extracted from
     * loadTables to reduce repetition; however, doing do actually makes each
     * check (inparticular, the first and last map levels) more complex than
     * they would otherwise.
     */
    public void doTableLayer(TreeMap<String, Double> curr, TreeMap<String, Double> next, String add, File buildTablePath, boolean commonOverride) {

        /*
         * Set up an iterator of target tables. Note that the first level (base
         * table) is put into a dummy treemap in order to have an iterator.
         */
        Iterator<String> it = curr.keySet().iterator();
        while (it.hasNext()) {
            String currTableName = it.next();

            // get zip entry for the new file.
            File tableEntry = null;
            if (commonOverride)
                tableEntry = new File(buildTablePath.getPath() + File.separatorChar + "Common" + add);
            else
                tableEntry = new File(buildTablePath.getPath() + File.separatorChar + currTableName + add);

            if (!tableEntry.exists()) {
                if (commonOverride)
                    tableEntry = new File((buildTablePath.getPath() + File.separatorChar + "Common" + add).toLowerCase());
                else
                    tableEntry = new File((buildTablePath.getPath() + File.separatorChar + currTableName + add).toLowerCase());
            }

            // ignore missing links
            if (tableEntry.exists()) {

                /*
                 * Loop through the target table once to determine the total
                 * weighting of all entries. This total is used to determine the
                 * fractional values of each line on a second pass.
                 */
                int totaltableweight = this.getTotalWeightForTable(tableEntry);

                /*
                 * InputStream and Buffered reader for a second pass through the
                 * file.
                 */
                InputStream is = this.getEntryInputStream(tableEntry);
                BufferedReader dis = new BufferedReader(new InputStreamReader(is));

                /*
                 * TableMultiplier is used to determine the relative value of
                 * each entry. For example, if the Orion ONI-1K appears on a
                 * target table at 50%, and the tableweight is .10 (aka - 10%),
                 * the ONI's actual frequncy is 5%.
                 */
                double tablemultiplier = ((Double) curr.get(currTableName)).doubleValue();
                // System.out.println("TableMultiplier for " + currTableName +
                // ": " + tablemultiplier);

                try {
                    while (dis.ready()) {

                        // read the line. make sure it's not empty.
                        String l = dis.readLine();
                        if (l == null || l.trim().length() == 0)
                            continue;

                        // remove excess whitespace
                        l = l.trim();
                        l = l.replaceAll("\\s+", " ");
                        if (l.indexOf(" ") == 0)
                            l = l.substring(1, l.length());

                        /*
                         * All lines should have weights. Set up a
                         * StringTokenizer and grab common data before seperate
                         * file/table work is done.
                         */
                        StringTokenizer ST = new StringTokenizer(l);
                        double weight = Double.parseDouble((String) ST.nextElement());

                        /*
                         * Determine whether this line is a cross-linked table
                         * or an actual unit file. Assume a valid file if the
                         * entry ends with a known unit file extension. If no
                         * known extension is present, assume a crosslinked
                         * table.
                         */
                        if (this.hasValidExtension(l) && weight != 0) {

                            String Filename = "";
                            while (ST.hasMoreElements()) {
                                Filename += ST.nextToken();
                                if (ST.hasMoreElements())
                                    Filename += " ";
                            }

                            /*
                             * Now that we have a filename, create a TableUnit.
                             * Check for duplication before adding to
                             * currentUnits. If the file in question is a dupe,
                             * simply add its frequency to that of the existing
                             * unit.
                             */
                            double frequency = (weight / totaltableweight) * tablemultiplier;

                            if (Filename.toLowerCase().endsWith(".mul")) {

                                Vector<Entity> loadedUnits = null;
                                File entityFile = new File("data/armies/" + Filename);

                                try {
                                    loadedUnits = EntityListFile.loadFrom(entityFile);
                                    loadedUnits.trimToSize();
                                    frequency /= loadedUnits.size();
                                } catch (Exception ex) {
                                    CampaignData.mwlog.errLog("Unable to load file " + entityFile.getName());
                                    CampaignData.mwlog.errLog(ex);
                                    continue;
                                }

                                for (Entity en : loadedUnits) {
                                    TableUnit tu = new TableUnit(en, frequency);
                                    if (tu == null) {
                                        continue;
                                    }

                                    TableUnit eu = (TableUnit) currentUnits.get(tu.getRealFilename());// existing
                                    // unit
                                    if (eu != null) {
                                        eu.addFrequencyFrom(tu);
                                    } else {
                                        currentUnits.put(tu.getRealFilename(), tu);
                                    }

                                    /*
                                     * Add this table as a source.
                                     */
                                    eu = (TableUnit) currentUnits.get(tu.getRealFilename());// existing
                                    // unit
                                    if (eu.getTables().get(currTableName) == null) {
                                        eu.getTables().put(currTableName, new Double(frequency));
                                    } else {
                                        Double currFreq = (Double) eu.getTables().get(currTableName);
                                        Double newFreq = new Double(currFreq.doubleValue() + frequency);
                                        eu.getTables().remove(currTableName);
                                        eu.getTables().put(currTableName, newFreq);
                                    }

                                }
                            } else {
                                TableUnit tu = new TableUnit(Filename, frequency);
                                if (tu == null) {
                                    continue;
                                }

                                TableUnit eu = (TableUnit) currentUnits.get(Filename);// existing
                                // unit
                                if (eu != null) {
                                    eu.addFrequencyFrom(tu);
                                } else {
                                    currentUnits.put(Filename, tu);
                                }

                                /*
                                 * Add this table as a source.
                                 */
                                eu = (TableUnit) currentUnits.get(Filename);// existing
                                // unit
                                if (eu.getTables().get(currTableName) == null) {
                                    eu.getTables().put(currTableName, new Double(frequency));
                                } else {
                                    Double currFreq = (Double) eu.getTables().get(currTableName);
                                    Double newFreq = new Double(currFreq.doubleValue() + frequency);
                                    eu.getTables().remove(currTableName);
                                    eu.getTables().put(currTableName, newFreq);
                                }
                            }
                        } else if (weight != 0) {// is a crosslink table
                            String crossTableName = "";
                            while (ST.hasMoreElements()) {
                                crossTableName += ST.nextToken();
                                if (ST.hasMoreElements())
                                    crossTableName += " ";
                            }

                            /*
                             * Put the crosslink into the map, if another layer
                             * exists. Check for duplication. If next is null
                             * there are no more crosslink hops to be mode,
                             * which means sorting would be a waste of time.
                             */
                            if (next != null) {
                                if (next.containsKey("crossTableName")) {
                                    Double d = (Double) next.get(crossTableName);
                                    double newTableWeight = d.doubleValue() + ((weight / totaltableweight) * tablemultiplier);
                                    next.remove(crossTableName);
                                    next.put(crossTableName, new Double(newTableWeight));
                                } else {
                                    next.put(crossTableName, new Double((weight / totaltableweight) * tablemultiplier));
                                }
                            }
                        }
                    }
                    is.close();// close input stream
                    dis.close();// close buffer
                } catch (Exception e) {
                    return;
                }

            }// end if(Entry != null)
        }// end while(more tables in iterator)
    }// end doTableLayer()

    /**
     * Method which loads tables and TableUnits, based on current ComboBox
     * selections. This is the beef of the class ...
     */
    public void loadTables() {

        // System.out.println("loadTables() called");

        String factionString = "";
        String addOnString = "";

        /*
         * First, determine faction.
         */
        factionString += (String) factionCombo.getSelectedItem();
        // System.out.println("Faction String: " + factionString);

        /*
         * Next, determine the weightclass.
         */
        addOnString += "_" + (String) weightClassCombo.getSelectedItem();

        /*
         * Finally, determine the type of unit to look at.
         */
        String type = (String) unitTypeCombo.getSelectedItem();
        if (!type.equals("Mek"))
            addOnString += type;

        // always look for a .txt
        addOnString += ".txt";
        // System.out.println("AddOn String: " + addOnString);

        /*
         * Look for the build tables
         */
        File buildTablePath = null;
        // System.out.println("Attempting to find ./data/buildtables/standard");
        buildTablePath = new File("./data/buildtables/standard");
        if (!buildTablePath.exists()) {
            CampaignData.mwlog.errLog("Could not find build tables.");
            return;
        }

        /*
         * Reset currentUnits.
         */
        // System.out.println("Clearing currentUnits");
        currentUnits.clear();

        /*
         * Found the zip. Now extract an appropriate entry. Try normal casing
         * and lowercasing/
         */
        boolean overrideWithCommon = false;
        // System.out.println("Attempting to find base table entry.");
        File tableEntry = new File(buildTablePath.getPath() + File.separatorChar + factionString + addOnString);
        if (tableEntry == null) {
            // System.out.println("Failed to find base table entry. Retrying in
            // lower case.");
            tableEntry = new File((buildTablePath.getPath() + File.separatorChar + factionString + addOnString).toLowerCase());
        }

        /*
         * Server defaults to common if a table isnt present. For example, if
         * Davion_AssaultBattleArmor isn't present,
         * Common_AssaultBattleArmor.txt is used instead. So, check that here as
         * well.
         */
        if (tableEntry == null) {
            // System.out.println("Didn't find Faction table in lower case
            // either. Retrying with Common.");
            overrideWithCommon = true;
            tableEntry = new File(buildTablePath.getPath() + File.separatorChar + "Common" + addOnString);
        }

        /*
         * If cased common is also null, try lower case. If this fails, return.
         */
        if (tableEntry == null) {
            // System.out.println("Didn't find Common table with standard
            // casing. Retrying in lower case.");
            tableEntry = new File((buildTablePath.getPath() + File.separatorChar + "Common" + addOnString).toLowerCase());
        }

        if (tableEntry == null) {
            // System.out.println("Didn't find Common table with lowercase.
            // Returning.");
            return;
        }

        /*
         * A clutch of treemaps. These are used to store info on crosslinked
         * tables. Note that linkage hops could extend in perpetuity, so
         * stopping after 3 hops will generate some minor rounding errors.
         */
        TreeMap<String, Double> crossMap1 = new TreeMap<String, Double>();
        TreeMap<String, Double> crossMap2 = new TreeMap<String, Double>();
        // TreeMap<String, Double> crossMap3 = new TreeMap<String, Double>();

        /*
         * Original Table. A dummy treemap is used here in order to pass a
         * treemap to the doTableLayer method. The initial table is the only
         * value and carries a 100% weight.
         */
        TreeMap<String, Double> temp = new TreeMap<String, Double>();
        temp.put(factionString, new Double(100.0));// using 100 makes things
        // %'s instead of decimals
        // ...
        // System.out.println("this.doTableLayer - base");
        this.doTableLayer(temp, crossMap1, addOnString, buildTablePath, overrideWithCommon);

        while (true) {
            // 1st cross-linkages (2nd degree)
            // System.out.println("this.doTableLayer - map1");
            this.doTableLayer(crossMap1, crossMap2, addOnString, buildTablePath, false);

            if (crossMap2.size() < 1)
                break;

            crossMap1 = crossMap2;
            crossMap2 = new TreeMap<String, Double>();
        }
        // 2nd layer cross linkages (3rd degree)
        // System.out.println("this.doTableLayer - map2");
        // this.doTableLayer(crossMap2, crossMap3, addOnString, buildTablePath,
        // false);

        // 3rd layer cross linkages (4th degree)
        // System.out.println("this.doTableLayer - map3");
        // this.doTableLayer(crossMap3, null, addOnString, buildTablePath,
        // false);

        /*
         * Table, and 3 degrees of seperation, processed as well as possible.
         * Holes may exist if linked tables are given bad pointers on the
         * tables, or if linkages are pervasive and 3 hops are insufficient to
         * cover most of the crosstalk.
         */

        /*
         * Update the total percentage counter.
         */
        double totalPercent = 0;
        Iterator<TableUnit> it = currentUnits.values().iterator();
        while (it.hasNext()) {
            TableUnit currUnit = it.next();
            totalPercent += currUnit.getFrequency();
        }
        DecimalFormat myFormatter = new DecimalFormat("###.#####");
        percentageLabel.setText("Total Percentage: " + myFormatter.format(totalPercent) + "%");
    }

    // inner classes
    /*
     * TableViewerModel is a model extention which sets up proper table viewer
     * sorting columns - name, weight, model, % frequency, etc. Modeled along
     * the BlackMarketModel from client.gui
     */
    static class TableViewerModel extends AbstractTableModel {

        /**
         * 
         */
        private static final long serialVersionUID = 4544580878221428391L;
        // IVARS
        // static ints
        public final static int UNIT = 0;// model/name
        public final static int WEIGHT = 1;
        public final static int BATTLEVALUE = 2;
        public final static int FREQUENCY = 3;
        public final static int FILENAME = 4;

        TreeMap<Object, TableUnit> currentUnits;
        TableUnit[] sortedUnits;

        int currentSortMode = TableViewerModel.FREQUENCY;

        // column name array
        String[] columnNames = { "Unit", "Weight", "BV", "Frequency" };

        // client reference
        MWClient mwclient;

        // CONSTRUCTOR
        public TableViewerModel(MWClient c, TreeMap<Object, TableUnit> current, TableUnit[] sorted) {
            this.mwclient = c;
            currentUnits = current;
            sortedUnits = sorted;
        }

        // column count, for AbstractModel
        public int getColumnCount() {
            return columnNames.length;
        }

        // rowcount, for AbstractModel
        public int getRowCount() {
            return sortedUnits.length;
        }

        // override naming
        @Override
        public String getColumnName(int col) {
            return (columnNames[col]);
        }

        // isEditable, overridden from AbstractModel
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void setSortMode(int sortMode) {
            currentSortMode = sortMode;
        }

        /*
         * getRenderer, overridden from AbstractModel in order to use custom
         * renderer.
         */
        public TableViewerModel.TableViewerRenderer getRenderer() {
            return new TableViewerRenderer();
        }

        /*
         * refresh model in order to draw new contents, reorder existin
         * contents.
         */
        public void refreshModel() {
            sortedUnits = new TableUnit[] {};
            sortedUnits = this.sortUnits(currentSortMode);
            this.fireTableDataChanged();
        }

        // getValueAt, for AbstractModel
        public Object getValueAt(int row, int col) {

            // invalid row
            if (row < 0 || row >= sortedUnits.length)
                return "";

            TableUnit currU = (TableUnit) sortedUnits[row];

            if (currU == null)
                return "";

            switch (col) {
            case UNIT:

                try {
                    if (currU.getType() == Unit.MEK && currU.getEntity() != null && !currU.getEntity().isOmni())
                        return "<html><body>" + currU.getEntity().getChassis() + ", " + currU.getModelName();
                    // else
                    return "<html><body>" + currU.getModelName();
                } catch (Exception ex) {
                    CampaignData.mwlog.errLog(ex);
                    return "";
                }
            case WEIGHT:
                return new Integer((int) currU.getEntity().getWeight());

            case BATTLEVALUE:

                return new Integer(currU.getEntity().calculateBattleValue());

            case FREQUENCY:
                DecimalFormat myFormatter = new DecimalFormat("##0.00");
                return myFormatter.format(currU.getFrequency()) + "%";

            case FILENAME:
                return currU.getRealFilename();

            }

            return "";
        }

        /*
         * Method which sorts the units in currentUnits.
         */
        public TableUnit[] sortUnits(int sortMode) {

            // a comparator
            CUnitComparator comparator = null;

            switch (sortMode) {
            case TableViewerModel.UNIT:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQSORT_NAME);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.WEIGHT:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQSORT_WEIGHTTONS);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.BATTLEVALUE:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQSORT_BV);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.FREQUENCY:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                Arrays.sort(sortedUnits, new Comparator<TableUnit>() {
                    public int compare(TableUnit o1, TableUnit o2) {

                        try {
                            TableUnit t1 = o1;
                            TableUnit t2 = o2;
                            Double d1 = 0.0;
                            Double d2 = 0.0;
                            if (t1 != null) {
                                d1 = new Double(t1.getFrequency());
                            }
                            if (t2 != null) {
                                d2 = new Double(t2.getFrequency());
                            }
                            return d1.compareTo(d2);
                        } catch (Exception ex) {
                            CampaignData.mwlog.errLog(ex);
                            return 0;
                        }
                    }
                });
                return sortedUnits;

            }// end switch

            // failsafe return
            return new TableUnit[] {};
        }

        /*
         * TableViewerRenderer ... This needs quite a bit more polish ...
         */
        private class TableViewerRenderer extends DefaultTableCellRenderer {

            /**
             * 
             */
            private static final long serialVersionUID = -8249928200262506117L;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component d = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                JLabel c = new JLabel(); // use a new label for everything
                // (should be made better later)
                c.setOpaque(true);

                if (row >= currentUnits.size() || row < 0)
                    return c;

                if (table.getModel().getValueAt(row, column) != null) {
                    c.setText(table.getModel().getValueAt(row, column).toString());
                }
                c.setToolTipText("");

                // get the unit from the tree
                Object unit = table.getModel().getValueAt(row, TableViewerModel.FILENAME);
                TableUnit currU = (TableUnit) currentUnits.get(unit);

                if (currU == null) {
                    return null;
                }

                // set up description
                StringBuilder description = new StringBuilder();

                if (currU.getType() == Unit.MEK && !currU.getEntity().isOmni())
                    description.append("<html><body><u>" + currU.getEntity().getChassis() + ", " + currU.getModelName() + "</u><br>");
                else
                    description.append("<html><body><u>" + currU.getModelName() + "</u><br>");

                // show the percent frequency for each table
                description.append("Sources:");

                Iterator<String> i = currU.getTables().keySet().iterator();
                DecimalFormat formatter = new DecimalFormat("##0.0##");
                while (i.hasNext()) {
                    String tableName = i.next();
                    Double freq = currU.getTables().get(tableName);
                    description.append("<br>- " + tableName + ": " + formatter.format(freq.doubleValue()) + "%");
                }

                c.setToolTipText(description.toString());

                if (isSelected) {
                    c.setForeground(d.getForeground());
                    c.setBackground(d.getBackground());
                    return c;
                }

                // always a white background
                c.setBackground(Color.white);

                return c;
            }
        }

    }// end TableViewerModel class

    /**
     * TableUnit is a CUnit with added stat-tracking for ongoing frequency
     * calculations. Much like the BMUnit; however, the TableUnit is less
     * complex.
     */
    static class TableUnit extends CUnit {

        // IVARS
        double frequency;
        String realFilename;
        TreeMap<String, Double> tables;

        // CONSTRUCTOR
        public TableUnit(String fn, double f) {
            super();

            /*
             * Since the TableUnit has no data string to set things with,
             * hardflag necessary values.
             */
            setUnitFilename(fn.trim());
            setPilot(new Pilot("Autopilot", 4, 5));

            /*
             * Try to get an entity from the unit cache, given a filename. This
             * makes it possible to use the build table viewer with unzipped
             * file structures (like that in use on the new MMNET). If this
             * fails, use normal server-style zip loading.
             */
            try {

                // remove .MTF, .blk, etc.
                String modfn = fn.trim();
                modfn = modfn.substring(0, modfn.length() - 4);

                // get the unit from the summary cache
                MechSummary ms = MechSummaryCache.getInstance().getMech(modfn);
                UnitEntity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

            } catch (Exception e) {
                // CampaignData.mwlog.errLog(e);
                createEntityFromFileNameWithCache(fn.trim());// make the
                // entity
            }

            realFilename = fn;
            frequency = f;

            tables = new TreeMap<String, Double>();
        }

        public TableUnit(Entity en, double f) {
            super();

            realFilename = UnitUtils.getEntityFileName(en);


            setUnitFilename(realFilename);
            setPilot(new Pilot("Autopilot", 4, 5));

            // get the unit from the summary cache
            UnitEntity = en;

            
            frequency = f;

            tables = new TreeMap<String, Double>();
        }

        // METHODS
        public double getFrequency() {
            return frequency;
        }

        public void addFrequencyFrom(TableUnit u) {
            frequency += u.getFrequency();
        }

        public String getRealFilename() {
            return realFilename;
        }

        public TreeMap<String, Double> getTables() {
            return tables;
        }

        private void createEntityFromFileNameWithCache(String fn) {

            UnitEntity = UnitUtils.createEntity(fn);

            if (UnitEntity == null) {
                createEntityFromFilename(fn);
            }
        }

        /**
         * Tries to setUnitEntity from a filename w/ extension. This used to be
         * the default way of getting units, but CUnit was changed to use the
         * MegaMek summary cache. Because the table viewer reads the tables the
         * same way the server does, it needs a server-style loading cascade,
         * ugly as it may be :-(
         */
        private void createEntityFromFilename(String fn) {

            UnitEntity = null;
            try {
                UnitEntity = new MechFileParser(new File("./data/mechfiles/Meks.zip"), fn).getEntity();
            } catch (Exception e) {
                try {
                    UnitEntity = new MechFileParser(new File("./data/mechfiles/Vehicles.zip"), fn).getEntity();
                } catch (Exception ex) {
                    try {
                        UnitEntity = new MechFileParser(new File("./data/mechfiles/Infantry.zip"), fn).getEntity();
                    } catch (Exception exc) {
                        try {
                            CampaignData.mwlog.errLog("Error loading unit: " + fn + ". Try replacing with OMG.");
                            // MechSummary ms =
                            // MechSummaryCache.getInstance().getMech("Error
                            // OMG-UR-FD");
                            UnitEntity = UnitUtils.createOMG();// new
                            // MechFileParser(ms.getSourceFile(),
                            // ms.getEntryName()).getEntity();
                            // UnitEntity = new MechFileParser (new
                            // File("./data/mechfiles/Meks.zip"),"Error
                            // OMG-UR-FD.hmp").getEntity();
                        } catch (Exception exepe) {
                            CampaignData.mwlog.errLog("Error unit failed to load. Exiting.");
                            System.exit(1);
                        }
                    }
                }
            }

            setType(getEntityType(UnitEntity));
            this.getC3Type(UnitEntity);
        }

    }// end TableUnit

    public void refreshButton_ActionPerformed() {

        int userLevel = mwclient.getUserLevel();
        
        refreshButton.setEnabled(false);
        if (userLevel >= mwclient.getData().getAccessLevel("AdminRequestBuildTable")) {
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminRequestBuildTable#list#true");
        } 
        else if (userLevel >= mwclient.getData().getAccessLevel("RequestBuildTable")) {
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c RequestBuildTable#list#true");
        } 

        mwclient.setWaiting(true);
        while (mwclient.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        this.loadTables();
        this.refresh();
        refreshButton.setEnabled(true);
    }
}// end TableViewerDialog class

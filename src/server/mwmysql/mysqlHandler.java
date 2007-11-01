package server.mwmysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import server.MWServ;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.pilot.SPilot;

import common.CampaignData;
import common.Unit;

public class mysqlHandler{
  private MWmysql MySQLCon = null;
  private planetHandler ph = null;
  private factoryHandler fh = null;
  private PilotHandler pih = null;
  private UnitHandler uh = null;
  private FactionHandler fah = null;
  private PlayerHandler plh = null;
  private PhpBBConnector phpBBCon = null;

  private final int currentDBVersion = 8;
  
  public void closeMySQL(){
	  MySQLCon.close();
	  if(CampaignMain.cm.isSynchingBB())
		  phpBBCon.close();
  }
  
  public void clearArmies(int userID) {
	  try {
		  PreparedStatement ps = MySQLCon.con.prepareStatement("DELETE from playerarmies WHERE playerID = ?");
		  ps.setInt(1, userID);
		  ps.executeUpdate();
		  ps.close();
	  } catch (SQLException e) {
		 MWServ.mwlog.dbLog("SQLException in mysqlHandler.clearArmies: " + e.getMessage());
	  } 	  
  }
  
  public void deleteArmy(int userID, int armyID) {
	  try {
		  PreparedStatement ps = MySQLCon.con.prepareStatement("DELETE from playerarmies WHERE playerID = ? AND armyID = ?");
		  ps.setInt(1, userID);
		  ps.setInt(2, armyID);
		  ps.executeUpdate();
		  ps.close();
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.deleteArmy: " + e.getMessage());
	  }
  }
  
  public void addUserToForum(String name, String pass) {
	  phpBBCon.addToForum(name, pass);
  }
  
  public void backupDB() {
	  MySQLCon.backupDB();
  }
  
  public PreparedStatement getPreparedStatement(String sql) {
	  try {
		  return MySQLCon.con.prepareStatement(sql);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getPreparedStatement: " + e.getMessage());
		  return null;
	  }
  }
  
  public PreparedStatement getPreparedStatement(String sql, int autoGeneratedKeys) {
	  try {
		  return MySQLCon.con.prepareStatement(sql, autoGeneratedKeys);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getPreparedStatement(String, Int): " + e.getMessage());
		  return null;
	  }
  }
  
  public PreparedStatement getPreparedStatement(String sql, int[] columnIndexes) {
	  try {
		  return MySQLCon.con.prepareStatement(sql, columnIndexes);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getPreparedStatement(String, int[]): " + e.getMessage());
		  return null;
	  }
  }

  public PreparedStatement getPreparedStatement(String sql, String[] columnNames) {
	  try {
		  return MySQLCon.con.prepareStatement(sql, columnNames);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getPreparedStatement(String, String[]): " + e.getMessage());
		  return null;
	  }
  }

  public PreparedStatement getPreparedStatement(String sql, int resultSetType, int resultSetConcurrency) {
	  try {
		  return MySQLCon.con.prepareStatement(sql, resultSetType, resultSetConcurrency);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getPreparedStatement(String, int, int): " + e.getMessage());
		  return null;
	  }
  }
  
  public PreparedStatement getPreparedStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
	  try {
		  return MySQLCon.con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getPreparedStatement(String, int, int, int): " + e.getMessage());
		  return null;
	  }
  }
  
  public Statement getStatement() {
	  try {
		  return MySQLCon.con.createStatement();
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getStatement: " + e.getMessage());
		  return null;
	  }
  }
  
  public Statement getStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
	  try {
		  return MySQLCon.con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getStatement(int, int, int): " + e.getMessage());
		  MWServ.mwlog.dbLog("resultSetType: " + resultSetType);
		  MWServ.mwlog.dbLog("resultSetConcurrency: " + resultSetConcurrency);
		  MWServ.mwlog.dbLog("resultSetHoldability: " + resultSetHoldability);
		  return null;
	  }
  }
    
  public Statement getStatement(int resultSetType, int resultSetConcurrency) {
	  try {
		  return MySQLCon.con.createStatement(resultSetType, resultSetConcurrency);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.getStatement(int, int): " + e.getMessage());
		  MWServ.mwlog.dbLog("resultSetType: " + resultSetType);
		  MWServ.mwlog.dbLog("resultSetConcurrency: " + resultSetConcurrency);
		  return null;
	  }
  }
  
  public void deleteFactory(int FactoryID){
    fh.deleteFactory(FactoryID);
  }

  public void deletePlanetFactories(String planetName){
    fh.deletePlanetFactories(planetName);
  }

  public void loadFactories(SPlanet planet){
    fh.loadFactories(planet);
  }
  
  public int countPlanets() {
	  return ph.countPlanets();
  }
  
  public void loadPlanets(CampaignData data) {
	  ph.loadPlanets(data);
  }
  
  public void saveInfluences(SPlanet planet) {
	  ph.saveInfluences(planet);
  }
  
  public void saveEnvironments(SPlanet planet) {
	  ph.saveEnvironments(planet);
  }
  
  public void savePlanetFlags(SPlanet planet) {
	  ph.savePlanetFlags(planet);
  }
  
  public void deletePlanet(int PlanetID) {
	  ph.deletePlanet(PlanetID);
  }
  
  public void loadFactionPilots(SHouse h) {
	  try {
		  ResultSet rs = null;
		  Statement stmt = MySQLCon.con.createStatement();

		  for (int x = Unit.MEK; x < Unit.MAXBUILD; x++) {
			  h.getPilotQueues().setFactionID(h.getDBId());
			  rs = stmt.executeQuery("SELECT pilotID from pilots WHERE factionID = " + h.getId() + " AND pilotType= " + x);
			  while(rs.next()) {
				  SPilot p = pih.loadPilot(rs.getInt("pilotID"));
				  h.getPilotQueues().loadPilot(x, p);
			  }			  
		  }
		  if(rs!=null)
			  rs.close();
		  stmt.close();
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQL Error in mysqlHandler.loadFactionPilots: " + e.getMessage());
	  }
  }
  
  public void deleteFactionPilots(int factionID) {
	  pih.deleteFactionPilots(factionID);
  }
  
  public void deleteFactionPilots(int factionID, int type) {
	  pih.deleteFactionPilots(factionID, type);
  }
  
  public void deletePlayerPilots(int playerID) {
	  pih.deletePlayerPilots(playerID);
  }
  
  public void deletePlayerPilots(int playerID, int unitType, int unitWeight) {
	  pih.deletePlayerPilots(playerID, unitType, unitWeight);
  }
  
  public void deletePilot(int pilotID) {
	  pih.deletePilot(pilotID);
  }
  
  public SPilot loadUnitPilot(int unitID) {
	  return pih.loadUnitPilot(unitID);
  }
  
  public SPilot loadPilot(int pilotID) {
	  return pih.loadPilot(pilotID);
  }
  
  public void linkPilotToUnit(int pilotID, int unitID) {
	  pih.linkPilotToUnit(pilotID, unitID);
  }
  
  public void linkPilotToFaction(int pilotID, int factionID) {
	  pih.linkPilotToFaction(pilotID, factionID);
  }
  
  public void linkPilotToPlayer(int pilotID, int playerID) {
	  pih.linkPilotToPlayer(pilotID, playerID);
  }
  
  public void unlinkUnit(int unitID) {
	  uh.unlinkUnit(unitID);
  }
  
  public void linkUnitToPlayer(int unitID, int playerID) {
	  uh.linkUnitToPlayer(unitID, playerID);
  }
  
  public void linkUnitToFaction(int unitID, int factionID){
	  uh.linkUnitToFaction(unitID, factionID);
  }
  
  public void deleteUnit(int unitID) {
	  uh.deleteUnit(unitID);
  }
  
  public void loadFactions(CampaignData data) {
	  fah.loadFactions(data);
  }

  public int countFactions() {
	  return fah.countFactions();
  }
  
  public int countPlayers() {
	  return plh.countPlayers();
  }
  
  public int getPlayerIDByName(String name) {
	  return plh.getPlayerIDByName(name);
  }
  
  public void setPlayerPassword(int ID, String password) {
	  plh.setPassword(ID, password);
  }
  
  public void setPlayerAccess(int ID, int level) {
	  plh.setPlayerAccess(ID, level);
  }
  
  public boolean matchPassword(String playerName, String pass) {
	  return plh.matchPassword(playerName, pass);
  }
  
  public boolean playerExists(String name) {
	  return plh.playerExists(name);
  }
  
  public void deletePlayer(SPlayer p) {
	  plh.deletePlayer(p);
  }
  
  public void purgeStalePlayers(long days) {
	  plh.purgeStalePlayers(days);
  }
  
  public int getDBVersion() {
	  Statement stmt = null;
	  ResultSet rs = null;
	  try {
		  stmt = getStatement();
		  rs = stmt.executeQuery("SELECT config_value from config WHERE config_key = 'mekwars_database_version'");
		  if(rs.next()) {
			  return rs.getInt("config_value");
		  }
		  rs.close();
		  stmt.close();
		  return 0;
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQL Error in mysqlHandler.getDBVersion: " + e.getMessage());
		  return 0;
	  }
  }
  
  private boolean databaseIsUpToDate() {
	  if(getDBVersion() == currentDBVersion){
		  MWServ.mwlog.dbLog("Database up to date");
		  return true;
	  }
	  MWServ.mwlog.dbLog("Database is an incorrect version!  Please update.");
	  MWServ.mwlog.dbLog("Current Version: " + currentDBVersion + "   --   Your version: " + getDBVersion());
	  return false;
  }
  
  public void checkAndUpdateDB() {
	  if(databaseIsUpToDate())
		  return;
	  MWServ.mwlog.dbLog("Database out of date");
	  MWServ.mwlog.mainLog("Database out of date.  Shutting down to avoid data corruption.");
	  System.exit(0);

/*

	  int dbVersion = getDBVersion();
	  
	  MWServ.mwlog.dbLog("Updating Database from version " + dbVersion + " to " + currentDBVersion);
	  while (dbVersion != currentDBVersion) {
		  int targetVersion = dbVersion + 1;
		  MWServ.mwlog.dbLog("Starting update: " + dbVersion + " to " + targetVersion);

		  dbVersion = targetVersion;
		  
	  }
*/
  }

  public PreparedStatement BBgetPreparedStatement(String sql) {
	  try {
		  return phpBBCon.con.prepareStatement(sql);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetPreparedStatement: " + e.getMessage());
		  return null;
	  }
  }
  
  public PreparedStatement BBgetPreparedStatement(String sql, int autoGeneratedKeys) {
	  try {
		  return phpBBCon.con.prepareStatement(sql, autoGeneratedKeys);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetPreparedStatement(String, Int): " + e.getMessage());
		  return null;
	  }
  }
  
  public PreparedStatement BBgetPreparedStatement(String sql, int[] columnIndexes) {
	  try {
		  return phpBBCon.con.prepareStatement(sql, columnIndexes);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetPreparedStatement(String, int[]): " + e.getMessage());
		  return null;
	  }
  }

  public PreparedStatement BBgetPreparedStatement(String sql, String[] columnNames) {
	  try {
		  return phpBBCon.con.prepareStatement(sql, columnNames);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetPreparedStatement(String, String[]): " + e.getMessage());
		  return null;
	  }
  }

  public PreparedStatement BBgetPreparedStatement(String sql, int resultSetType, int resultSetConcurrency) {
	  try {
		  return phpBBCon.con.prepareStatement(sql, resultSetType, resultSetConcurrency);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetPreparedStatement(String, int, int): " + e.getMessage());
		  return null;
	  }
  }
  
  public PreparedStatement BBgetPreparedStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
	  try {
		  return phpBBCon.con.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetPreparedStatement(String, int, int, int): " + e.getMessage());
		  return null;
	  }
  }
  
  public Statement BBgetStatement() {
	  try {
		  return phpBBCon.con.createStatement();
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetStatement: " + e.getMessage());
		  return null;
	  }
  }
  
  public Statement BBgetStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
	  try {
		  return phpBBCon.con.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetStatement(int, int, int): " + e.getMessage());
		  MWServ.mwlog.dbLog("resultSetType: " + resultSetType);
		  MWServ.mwlog.dbLog("resultSetConcurrency: " + resultSetConcurrency);
		  MWServ.mwlog.dbLog("resultSetHoldability: " + resultSetHoldability);
		  return null;
	  }
  }
    
  public Statement BBgetStatement(int resultSetType, int resultSetConcurrency) {
	  try {
		  return phpBBCon.con.createStatement(resultSetType, resultSetConcurrency);
	  } catch (SQLException e) {
		  MWServ.mwlog.dbLog("SQLException in mysqlHandler.BBgetStatement(int, int): " + e.getMessage());
		  MWServ.mwlog.dbLog("resultSetType: " + resultSetType);
		  MWServ.mwlog.dbLog("resultSetConcurrency: " + resultSetConcurrency);
		  return null;
	  }
  }
  
  public mysqlHandler(){
    this.MySQLCon = new MWmysql();
    if(CampaignMain.cm.isSynchingBB()) {
    	this.phpBBCon = new PhpBBConnector();
    	phpBBCon.init();
    }
    this.ph = new planetHandler(MySQLCon.con);
    this.fh = new factoryHandler(MySQLCon.con);
    this.pih = new PilotHandler(MySQLCon.con);
    this.uh = new UnitHandler(MySQLCon.con);
    this.fah = new FactionHandler(MySQLCon.con);
    this.plh = new PlayerHandler(MySQLCon.con);
    this.checkAndUpdateDB();
  }
}

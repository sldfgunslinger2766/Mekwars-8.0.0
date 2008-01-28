	/*
	 * MekWars - Copyright (C) 2007 
	 * 
	 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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

	

package server.mwmysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import server.MWServ;
import server.campaign.CampaignMain;

public class MWmysql{
  Connection con = null;

 
  public void close(){
    MWServ.mwlog.dbLog("Attempting to close MySQL Connection");
    try {
    	this.con.close();

    } catch (SQLException e) {
    	MWServ.mwlog.dbLog("SQL Exception: " + e.getMessage());
    	MWServ.mwlog.errLog("SQL Exception:");
    	MWServ.mwlog.errLog(e);
    }
  } 

  public void backupDB(long time) {
	  String fs = System.getProperty("file.separator");
	  Runtime runtime=Runtime.getRuntime();

	  String dateTimeFormat = "yyyy.MM.dd.HH.mm";
      SimpleDateFormat sDF = new SimpleDateFormat(dateTimeFormat);
      Date date = new Date(time);
      String dateTime = sDF.format(date);
      
	  try {
		  if(fs.equalsIgnoreCase("/"))
		  {
			  // It's Unix
			  String[] call={"dump_db.sh", dateTime};
			  runtime.exec(call);
		  } else {
			  // It's Windows
			  String[] call={"dump_db.bat", dateTime};
			  runtime.exec(call);
		  }		  
	  } catch (IOException ex){
		  MWServ.mwlog.dbLog("Error in backupDB: " + ex.toString());
	  }
  }

  public MWmysql(){
    String url = "jdbc:mysql://" + CampaignMain.cm.getServer().getConfigParam("MYSQLHOST") + "/" + CampaignMain.cm.getServer().getConfigParam("MYSQLDB") + "?user=" + CampaignMain.cm.getServer().getConfigParam("MYSQLUSER") + "&password=" + CampaignMain.cm.getServer().getConfigParam("MYSQLPASS");
    MWServ.mwlog.dbLog("Attempting MySQL Connection");
    
    try{
      Class.forName("com.mysql.jdbc.Driver");
    }
    catch(ClassNotFoundException e){
      MWServ.mwlog.dbLog("ClassNotFoundException: " + e.getMessage());
    }
    try{
    	con=DriverManager.getConnection(url);
      	if(con != null)
    	  MWServ.mwlog.dbLog("Connection established");
    }
    catch(SQLException ex){
    	MWServ.mwlog.dbLog("SQLException: " + ex.getMessage());
    }
  }
}

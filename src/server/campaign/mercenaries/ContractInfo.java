/* * MekWars - Copyright (C) 2004  *  * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet) * * This program is free software; you can redistribute it and/or modify it * under the terms of the GNU General Public License as published by the Free * Software Foundation; either version 2 of the License, or (at your option) * any later version. * * This program is distributed in the hope that it will be useful, but * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License * for more details. *///urgru @ 11/11/02//Simple class which holds contract data.package server.campaign.mercenaries;import java.io.Serializable;import java.sql.PreparedStatement;import java.sql.ResultSet;import java.sql.SQLException;import java.util.StringTokenizer;import server.MWServ;import server.campaign.CampaignMain;import server.campaign.SPlayer;import server.campaign.SHouse;@SuppressWarnings({"unchecked","serial"})public class ContractInfo implements Serializable {	//simply, obviously, named vars	private int _contractDuration;	private int _earnedAmount;	private int _payment;    private int _type = 0;	private String _contractingHouse;	private String _playerName;	private String _offeringPlayer;	private int DBId = 0;    public static final int CONTRACT_EXP        = 0;    public static final int CONTRACT_LAND       = 1;    public static final int CONTRACT_UNITS      = 2;    public static final int CONTRACT_COMPONENTS = 3;    public static final int CONTRACT_DELAY      = 4;	//constructor	public ContractInfo(int duration, int payment, SHouse contractHouse, String name, int type) {		_contractDuration = duration;		_earnedAmount = 0;		_payment = payment;		_contractingHouse = contractHouse.getName();		_playerName = name;        _type = type;	}//end constructor	//get methods	public int getContractDuration() {		return _contractDuration;	}//end getCon	public int getEarnedAmount() {		return _earnedAmount;	}//end getEarnedAmount    public void setEarnedAmount(int amount){        this._earnedAmount = amount;    }    	public SHouse getEmployingHouse() {		return CampaignMain.cm.getHouseFromPartialString(_contractingHouse,null);	}//end getEmployer	public String getPlayerName() {		return _playerName;	}//end getName	public int getPayment() {		return _payment;	}//end getPayment    public int getType() {        return _type;    }//end getType    public SPlayer getOfferingPlayer() {		return CampaignMain.cm.getPlayer(_offeringPlayer);	}//end get offering player	//cancelation is the only possible major modification, and this is handled elsewhere by blowing	//up the contract. setOfferingPlayer is used in CampaignMain to keep track of contracts before	//offers are accepted, but subsequent to acceptance, offering player is not checked.	public void setOfferingPlayer(SPlayer p) {		_offeringPlayer = p.getName();	}//end setOffer    //Code from the Wiz    public boolean isLegal()    {      if (CampaignMain.cm.getHouseFromPartialString(this._contractingHouse,null) == null)        return false;      if (CampaignMain.cm.getPlayer(this._offeringPlayer) == null)        return false;      if (CampaignMain.cm.getPlayer(this._playerName) == null)        return false;      return true;    }    @Override	public String toString()    {      String result = "CON$";      result += _contractDuration;      result += "$";      result += _earnedAmount;      result += "$";      result += _payment;      result += "$";      result += _contractingHouse;      result += "$";      result += _playerName;      result += "$";      result += _offeringPlayer;      result += "$";      result += _type;      return result;    }    public void toDB() {    	if(getDBId()==0) {    		// It's not in here, so add it    		PreparedStatement ps = CampaignMain.cm.MySQL.getPreparedStatement("INSERT INTO merc_contract_info SET contractDuration = ?, contractEarnedAmount = ?, ContractPayment = ?, contractHouse = ?, contractPlayer = ?, contractOfferingPlayer = ?, contractType = ?", PreparedStatement.RETURN_GENERATED_KEYS);    		ResultSet rs = null;    		try {    			ps.setInt(1, _contractDuration);    			ps.setInt(2, _earnedAmount);    			ps.setInt(3, _payment);    			ps.setString(4, _contractingHouse);    			ps.setString(5, _playerName);    			ps.setString(6, _offeringPlayer);    			ps.setInt(7, _type);    			    			ps.executeUpdate();    			rs = ps.getGeneratedKeys();    			if(rs.next()) {    				setDBId(rs.getInt(1));    			} else {    				MWServ.mwlog.dbLog("Error inserting contract in ContractInfo.toDB():  No key generated");    			}    		} catch(SQLException e) {    			MWServ.mwlog.dbLog("SQLException in ContractInfo.toDB(): " + e.getMessage());    		} finally {    			if(ps != null)					try {						ps.close();					} catch (SQLException e) {						// TODO Auto-generated catch block						e.printStackTrace();					}    			if(rs != null)					try {						rs.close();					} catch (SQLException e) {						// TODO Auto-generated catch block						e.printStackTrace();					}    		}    	} else {    		// It's already in the database, so update it    		PreparedStatement ps = CampaignMain.cm.MySQL.getPreparedStatement("UPDATE merc_contract_info SET contractDuration = ?, contractEarnedAmount = ?, ContractPayment = ?, contractHouse = ?, contractPlayer = ?, contractOfferingPlayer = ?, contractType = ? WHERE contractID = ?");    		try {    			ps.setInt(1, _contractDuration);    			ps.setInt(2, _earnedAmount);    			ps.setInt(3, _payment);    			ps.setString(4, _contractingHouse);    			ps.setString(5, _playerName);    			ps.setString(6, _offeringPlayer);    			ps.setInt(7, _type);    			ps.setInt(8, getDBId());    			    			ps.executeUpdate();     		} catch(SQLException e) {    			MWServ.mwlog.dbLog("SQLException in ContractInfo.toDB(): " + e.getMessage());    		} finally {    			if(ps != null)					try {						ps.close();					} catch (SQLException e) {						// TODO Auto-generated catch block						e.printStackTrace();					}    		}    	}    }        public void fromDB() {    	    }        public void fromString(String s)    {      s = s.substring(4);      StringTokenizer ST = new StringTokenizer(s,"$");      _contractDuration = Integer.parseInt(ST.nextToken());      _earnedAmount = Integer.parseInt((String)ST.nextElement());      _payment = Integer.parseInt((String)ST.nextElement());      _contractingHouse = ST.nextToken();      _playerName = ST.nextToken();      _offeringPlayer = ST.nextToken();      if ( ST.hasMoreElements() )          _type = Integer.parseInt(ST.nextToken());    }    public ContractInfo()    {      //For Serialisation    }    public String getInfo(SPlayer player){        StringBuilder info = new StringBuilder("Contract Status: ");        int duration = getContractDuration();        int performedXP = getEarnedAmount();        info.append(performedXP + " of " + duration + " "+ContractInfo.getContractName(getType())+" performed.<br>");        return info.toString();    }        public static int getContractType(String type){                if ( type.equalsIgnoreCase("land") )            return ContractInfo.CONTRACT_LAND;        if ( type.equalsIgnoreCase("units") )            return ContractInfo.CONTRACT_UNITS;                if ( type.equalsIgnoreCase("components") )            return ContractInfo.CONTRACT_COMPONENTS;        if ( type.equalsIgnoreCase("delay") )            return ContractInfo.CONTRACT_DELAY;        //always exp if nothing else        return ContractInfo.CONTRACT_EXP;    }        public static String getContractName(int type){        String contract = "Exp";                switch(type){        case CONTRACT_LAND:            contract = "Land";            break;        case CONTRACT_UNITS:            contract = "Units";            break;        case CONTRACT_COMPONENTS:            contract = "Components";            break;        case CONTRACT_DELAY:            contract = "Delay";            break;            default:                contract = "Exp";            break;        }                return contract;    }        public int getDBId(){    	return DBId;    }        public void setDBId(int dbID){    	this.DBId = dbID;    }}
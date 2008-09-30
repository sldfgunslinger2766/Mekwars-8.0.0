package client.protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import common.CampaignData;
import common.Equipment;
import common.House;
import common.Influences;
import common.Planet;
import common.util.BinReader;
import common.util.BinWriter;

import client.MWClient;


/**
 * Calls to the data retrieving server and gets data for planets and factions
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class DataFetchClient {
	
	private String hostAddr;
	private String cacheDir;
	private CampaignData data;
	private Map <Integer,Influences>changesSinceLastRefresh;
	private Date lastTimestamp = null;
    //private Date latestTimeStamp = null;
	private int dataPort = 4867;
	private Socket dataSocket = null;
    private int socketDelayTime = 2000;
    
	/**
	 * Constructor. This will not setup the connection. To actually transfer
	 * data use the get*() methods. Remember to set the host address before
	 * calling any get* methods. This cannot be set here, because 
	 * DataFetchClient is used with xstream and persistance and we want the
	 * users to change the address in the config, not the cache file.
	 */
	public DataFetchClient(int dataport, int socketDelayTime) {
		this.dataPort = dataport;
		changesSinceLastRefresh = new HashMap<Integer,Influences>();
        
        if ( socketDelayTime > 0 )
            this.socketDelayTime = socketDelayTime;
        else
            this.socketDelayTime = 2000;
        
	}
	
	/**
	 * get all of the server configs. this is used for Staff
	 */
	public void getServerConfigData(MWClient mwclient) throws IOException {
		
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c getserverconfigs#ALL");

        mwclient.setWaiting(true);
        while ( mwclient.isWaiting() ) {
            
            try {
                Thread.sleep(1000);
            }catch (Exception ex) {
                CampaignData.mwlog.errLog(ex);
            }
        }

	}
	
	/**
	 * Transfer the Black Market Settings
	 * Only called from Admin.ComponentDisplayDialog
	 */
	public void getBlackMarketSettings(MWClient mwclient) throws IOException {
		
		//open the connection to the server, and write out the config
		try {
			
			BinReader in = openConnection("BMSetting");
			
			int count = in.readInt("BMSetting");
			
			//keep reading until there is an error.
			try {
				for (; count > 0 ; count--){
					
					Equipment bme = new Equipment();
					
					bme.setEquipmentInternalName(in.readLine("BMSetting"));
					bme.setMinCost(in.readDouble("BMSetting"));
					bme.setMaxCost(in.readDouble("BMSetting"));
					bme.setMinProduction(in.readInt("BMSetting"));
					bme.setMaxProduction(in.readInt("BMSetting"));
					
					//updated is used for when the data is update in the dialog.
					bme.setUpdated(false);
					mwclient.getBlackMarketEquipmentList().put(bme.getEquipmentInternalName(), bme);
				}
			} catch (Exception e) {
				CampaignData.mwlog.errLog(e);
			}//end catch for read-in
		} 
		
		//failed to open connection. try to load local defaults.
		catch (Exception exe) {
			CampaignData.mwlog.errLog(exe);
		}//end catch(Connection Failure)
	}
	
	/**
     * Transfers server wide banned ammo data from the server to the client.
     *
     */
	public void getBannedAmmoData(MWClient mwclient) throws IOException {
        
        boolean timestampMatch = false;
        File localban = new File(cacheDir + "/banammo.dat");
        if (localban.exists()) {
            
            //get the local timetamp
            String localListTimestamp = "";
            
            try {
                FileInputStream in = new FileInputStream(localban);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                localListTimestamp = br.readLine();
                br.close();
                in.close();
                
                BinReader binreader = openConnection("BannedAmmoTimeStamp");
                String serverTimeStamp = binreader.readLine("BannedAmmoTimeStamp");
                
                CampaignData.mwlog.errLog("Local Ban: "+localListTimestamp+" Server Ban: "+serverTimeStamp );
                if (localListTimestamp.equals(serverTimeStamp) )
                    timestampMatch = true;
            } catch (Exception e) {
                CampaignData.mwlog.errLog("Problems reading timestamp from local banammo.dat.");
            }

        
        }
        
        //clear the hash so we can add all the new stuff --Torren
        mwclient.clearBanAmmo();
        if ( !timestampMatch ){
            BinReader in = openConnection("BannedAmmo");
            String timestamp = "-1";
            try{
                timestamp = in.readLine("BannedAmmo");//TIMESTAMP
                while(true){
                    mwclient.loadBanAmmo(in.readLine("BannedAmmo"));
                }
            }catch(Exception ex){}//Bin empty
            mwclient.saveBannedAmmo(timestamp);
        }else{//load from the banned file.
            try{
                FileInputStream fis = new FileInputStream(mwclient.getCacheDir()+ "/banammo.dat");
                BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                while (dis.ready()) {
                    String line = dis.readLine();
                    mwclient.loadBanAmmo(line);
                }
                dis.close();
                fis.close();
            }catch(Exception ex){
                CampaignData.mwlog.errLog(ex);
            }
        }
        
    }


    /**
     * Transfers server wide banned targeting systems from the server to the client.
     *
     */
    public void getBanTargetingData(MWClient mwclient) throws IOException {
        
        boolean timestampMatch = false;
        File localban = new File(cacheDir + "/bantargeting.dat");
        if (localban.exists()) {
            
            //get the local timetamp
            String localListTimestamp = "";
            
            try {
                FileInputStream in = new FileInputStream(localban);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                localListTimestamp = br.readLine();
                br.close();
                in.close();
                
                BinReader binreader = openConnection("BanTargetingTimeStamp");
                String serverTimeStamp = binreader.readLine("BanTargetingTimeStamp");
                
                CampaignData.mwlog.errLog("Local BanT: "+localListTimestamp+" Server BanT: "+serverTimeStamp );
                if (localListTimestamp.equals(serverTimeStamp) )
                    timestampMatch = true;
            } catch (Exception e) {
                CampaignData.mwlog.errLog("Problems reading timestamp from local bantargeting.dat.");
            }

        
        }
        
        //clear the hash so we can add all the new stuff --Torren
        mwclient.clearBanTargeting();
        if ( !timestampMatch ){          //BanTargeting
            BinReader in = openConnection("BanTargeting");
            String timestamp = "-1";
            try{
                timestamp = in.readLine("BanTargeting");//TIMESTAMP
                mwclient.loadBanTargeting(in.readLine("BanTargeting"));
            }catch(Exception ex){}//Bin empty
            mwclient.saveBannedTargetingSystems(timestamp);
        }else{//load from the banned file.
            try{
                FileInputStream fis = new FileInputStream(mwclient.getCacheDir()+ "/bantargeting.dat");
                BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                dis.readLine();
                mwclient.loadBanTargeting(dis.readLine());
                dis.close();
                fis.close();
            }catch(Exception ex){}
        }
        
    }


    /**
     * Check Server version against client if it doesn't match you can't connect
     *
     */
    public void checkServerVersion(MWClient mwclient) throws IOException {

        boolean mustUpdate = false;
        String clientVersion = MWClient.CLIENT_VERSION;
 
        clientVersion = clientVersion.substring(0,clientVersion.lastIndexOf("."));
        
        BinReader binreader = openConnection("ServerVersion");
        String serverVersion = binreader.readLine("ServerVersion");
        
        serverVersion = serverVersion.substring(0,serverVersion.lastIndexOf("."));
        
        CampaignData.mwlog.errLog("Client Version: "+clientVersion+" Server Version: "+serverVersion);
        mustUpdate = !serverVersion.equalsIgnoreCase(clientVersion);
        
        //If the versions dont match then the client has to update anyways
        if ( !mustUpdate ){
            binreader = openConnection("ForceUpdateKey");
            String forceUpdateKey = binreader.readLine("ForceUpdateKey");
            String clientUpdateKey = mwclient.getConfigParam("UPDATEKEY");
        
            CampaignData.mwlog.errLog("Server Key: "+forceUpdateKey);
            //the server update key starts out blank. So the update only works
            //after a key is set server side.
            if ( forceUpdateKey.trim().length() > 1 )
                mustUpdate = !forceUpdateKey.equals(clientUpdateKey);
        }
        
        if ( mustUpdate ){
            int update = JOptionPane.NO_OPTION;
            if ( !mwclient.isDedicated() ){
                update = JOptionPane.showConfirmDialog(null,"You have an invalid version\n\rof the MekWars Client\n\rWould you like to update now?","Invalid Client update now!",JOptionPane.YES_NO_OPTION);
            
                if ( update == JOptionPane.YES_OPTION ){
                    try{
                        mwclient.goodbye();
                        Runtime runtime = Runtime.getRuntime();
                        String[] call = {"java","-jar","./MekWarsAutoUpdate.jar","PLAYER"};
                        runtime.exec(call);
                        CampaignData.mwlog.errLog("Starting Update!");
                    }catch(Exception ex){
                        CampaignData.mwlog.errLog(ex);
                    }
                }

            }else{//is Ded
                try{
                    mwclient.stopHost();
                    mwclient.goodbye();
                    Runtime runtime = Runtime.getRuntime();
                    String[] call = {"java","-jar","MekWarsAutoUpdate.jar","DEDICATED"};
                    runtime.exec(call);
                }catch(Exception ex){
                    CampaignData.mwlog.errLog(ex);
                }

            }
            
            System.exit(0);
            
        }
    }

    /**
	 * Transfer the server configuration files. Used to
	 * set up verious portions of the GUI, determing proper
	 * Money/Flu names, and more.
	 */
	public void checkForMostRecentOpList() throws IOException {
		
		/* 
		 * Look for an existing OpList.txt in the appropriate
		 * data dir. If it exists, MD5 it and request the MD5 of
		 * its server side analog.
		 * 
		 * If the timestamps don't match, force a refresh from the
		 * data feeder.
		 */
		boolean timestampMatch = false;
		File localList = new File(cacheDir + "/OpList.txt");
		if (localList.exists()) {
			
			//get the local timetamp
			String localListTimestamp = "";
			
			try {
				FileInputStream in = new FileInputStream(cacheDir + "/OpList.txt");
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String tempTime = br.readLine();
				br.close();
				in.close();
				
				localListTimestamp = tempTime.substring(11);//remove "#Timestamp="
			} catch (Exception e) {
				CampaignData.mwlog.errLog("Problems reading timestamp from local OpList.");
			}
			
			//now get the server list's timestamp ...
			BinReader in = openConnection("OpListTimestamp");
			String serverListTimestamp = in.readLine("OpListTimestamp");
			CampaignData.mwlog.errLog("Local OpList: " + localListTimestamp + " Server OpList: " + serverListTimestamp);	
			if (localListTimestamp.equals(serverListTimestamp))
				timestampMatch = true;

		}//end if(localList.exists)
		
		/*
		 * If the MD5s dont match, update
		 */
		if (!timestampMatch) {
			
			//delete the old file, if it exists
			File f = new File(cacheDir + "/OpList.txt");
			if (f.exists())
				f.delete();
			
			//open the connection to the server, and write out the list
			try {
				
				BinReader in = openConnection("OpList");
				FileOutputStream fops = new FileOutputStream(cacheDir + "/OpList.txt");
				PrintStream out = new PrintStream(fops);
				try {
					
					//keep reading new lines until there is an error.
					while (true){
						out.println(in.readLine("ListLine"));
                    }
				
				} catch (Exception e) {
					
					//close the streams
					////in.close();
					out.close();
					fops.close();
					
				}
			} catch (Exception exe) {
				CampaignData.mwlog.errLog(exe);
			}
		}//end if(!md5Match)
	}//end getOpListMD5
	
	/**
	 * Transfer trait data. Used to generate the
	 * Trait dialogs in Help menu.
	 * 
	 * Regardless of the data sent, if there is a
	 * 0% chance for meks to get the trait skill
	 * the help menu will not be shown.
	 * @see CMainFrame.java
	 */
	public void getServerTraitFiles() throws IOException {
		
		try {
			//MMClient.mwClientLog.clientErrLog("- opening connection to datafeed. requesting Trait Files");
			BinReader in = openConnection("ServerTrait");
			
			//keep reading until there is an error.
			try {
				while(true){
					String faction = in.readLine("TraitLine");
					int count = in.readInt("TraitLine");
					FileOutputStream fops = new FileOutputStream(cacheDir + "/"+faction.toLowerCase()+"traitnames.txt");
					PrintStream out = new PrintStream(fops);
					for ( ;count > 0; count--)
						out.println(in.readLine("TraitLine"));
                    out.flush();
					out.close();
                    fops.flush();
					fops.close();
				}
			} catch (Exception e) {
				
				//close the streams
				//in.close();
			}
		} catch (Exception ex){
			CampaignData.mwlog.errLog(ex);
		} 
		
	}
	
	/**
	 * Transfer the whole planet data xml.
	 */
	public CampaignData getAllData() throws IOException {
		BinReader in = openConnection("All");
		CampaignData data = new CampaignData(in);
		//in.close();
		this.data = data;
        
		store();
		
		return data;
	}
	
	/**
	 * Transfer the data from cache.
	 */
	public CampaignData getCacheData(String cachePath) throws IOException {
		BinReader in = new BinReader(new FileReader(cachePath+"/data.dat"));
		CampaignData data = new CampaignData(in);
		in.close();
		this.data = data;
		store();
		
		return data;
	}
	
	/**
	 * Transfer only the differential planets since last timestamp.
	 */
	public boolean getPlanetsUpdate(CampaignData Data) {
		try {
			BinReader in = openConnection("PDiff",60000);
			data = Data;
			if ( data == null )
			{
				CampaignData.mwlog.errLog("data is null getPlanetsUpdate");
				return false;
			}
            try{
   
                data.clearHouses();
            	int size = in.readInt("houses.size");
                for ( int count = 0; count < size; count++){
                	House house = new House(in);
                	data.addHouse(house);
                }
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                lastTimestamp = sdf.parse(in.readLine("lasttimestamp"));
                boolean fullUpdate = in.readBoolean("FullUpdate");

                if ( fullUpdate ) {
                    data.clearPlanets();
                }
                
            	size = in.readInt("planets.size");
                for ( int count = 0; count < size; count++){
                	Planet planet = new Planet();
                	planet.binIn(in, data);
                	data.addPlanet(planet);
                	changesSinceLastRefresh.put(planet.getId(), planet.getInfluence());
                }
   
            }catch(Exception ex){
            	CampaignData.mwlog.errLog(ex);
            }//Bin empty

			/*changesSinceLastRefresh = new HashMap();
			data.decodeMutablePlanets(in, changesSinceLastRefresh);
			String serverMD5 = in.readLine("md5");
			CampaignData.mwlog.infoLog("read MD5 checksum: "+serverMD5);
			MD5OutputStream md5 = new MD5OutputStream();
			BinWriter md5Writer = new BinWriter(new PrintWriter(md5));
			data.binOut(md5Writer);
			md5Writer.close();
			CampaignData.mwlog.infoLog("own checksum: "+md5.getHashString());
			if (!serverMD5.equals(md5.getHashString())) {
				md5.close();
				return false;
			}
			//else
			md5.close();
			//in.close();*/
		} catch (IOException e) {
			CampaignData.mwlog.errLog(e);
			return false;
		} catch (RuntimeException e) {
			CampaignData.mwlog.errLog(e);
			return false;
		}
		//this.data = data;

		store();
		return true;
	}
	
	
	/**
	 * Transfer the Access levels of all the commands
	 * but only save the ones that matchs the users.
	 * 
	 * @author Torren (Jason Tighe)
	 */
	public boolean getAccessLevels(CampaignData Data) {
		try {
			BinReader in = openConnection("CommandAccessLevels");
			
			Data.importAccessLevels(in);
			//in.close();
		} catch (IOException e) {
			CampaignData.mwlog.errLog(e);
			return false;
		} catch (RuntimeException e) {
			CampaignData.mwlog.errLog(e);
			return false;
		}
		return true;
	}
	
    private BinReader openConnection(String cmd) throws IOException {
        return openConnection(cmd,socketDelayTime);
    }
    
	/**
	 * Open a connection to the server.
	 * @return
	 */
	private BinReader openConnection(String cmd, int timeout) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        CampaignData.mwlog.infoLog("Command: "+cmd);
        if ( dataSocket == null
                || dataSocket.isClosed() 
                || dataSocket.isInputShutdown()
                || dataSocket.isOutputShutdown() ){
            this.closeDataConnection();
            CampaignData.mwlog.infoLog("Trying to connect to "+hostAddr+" at port "+dataPort);
            dataSocket = new Socket(hostAddr, dataPort);
            dataSocket.setKeepAlive(true);
        }else{//clean out any old data first.
            dataSocket.getOutputStream().flush();
        }
        dataSocket.setSoTimeout(timeout);
		BinWriter out = new BinWriter(new PrintWriter(dataSocket.getOutputStream()));
		out.println(cmd, "cmd");
		if (lastTimestamp == null)
			out.println("", "lasttimestamp");
		else {
			CampaignData.mwlog.infoLog("writing timestamp "+sdf.format(lastTimestamp));
			out.println(sdf.format(lastTimestamp), "lasttimestamp");
		}
		out.flush();
        BinReader in = null;
        try {
             in = new BinReader(new InputStreamReader(dataSocket.getInputStream()));
             //lastTimestamp = 
             sdf.parse(in.readLine("lasttimestamp"));
		} catch (ParseException e) {
			CampaignData.mwlog.errLog(e);
			CampaignData.mwlog.infoLog("Timestamp could not be parsed.. left unchanged.");
		}catch (SocketException se){
			CampaignData.mwlog.errLog("Socket Exception Error: DataFetchClient");
			CampaignData.mwlog.errLog(se);
            this.closeDataConnection();
            return openConnection(cmd, timeout);
        }catch ( NullPointerException NPE){
            this.closeDataConnection();
            return openConnection(cmd, timeout);
        }
		return in;
	}
	
	/**
	 * @param hostAddr The hostAddr to set.
	 */
	public void setData(String hostAddr, String cacheDir) {
		this.hostAddr = hostAddr;
		this.cacheDir = cacheDir;
	}
	
	/**
	 * Store itself to disk.
	 */
	public void store() {
        
        if ( lastTimestamp != null ){
    		try {
    			FileWriter fw = new FileWriter(cacheDir+"/dataLastUpdated.dat");
                //write the time out in Milliseconds
                //lastTimestamp = latestTimeStamp;
    		
                fw.write(Long.toString(lastTimestamp.getTime()));
                fw.close();
    		} catch (IOException e) {
    			CampaignData.mwlog.errLog(e);
    		}
        }
		try {
			BinWriter binOut = new BinWriter(new PrintWriter(new FileWriter(cacheDir+"/data.dat")));
			data.binOut(binOut);
			binOut.close();
		}
		catch (Exception ex)
		{
			CampaignData.mwlog.errLog(ex);
			CampaignData.mwlog.errLog("Error saving data.");
		}
	}
	
	/**
	 * @return Returns the changesSinceLastRefresh.
	 */
	public Map<Integer,Influences> getChangesSinceLastRefresh() {
		return changesSinceLastRefresh;
	}
	
	/**
	 * @param lastTimestamp The lastTimestamp to set.
	 */
	public void setLastTimestamp(Date lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}
    
    public void closeDataConnection(){
        try{
            if ( dataSocket == null )
                return;
            CampaignData.mwlog.infoLog("Closing Socket.");
            dataSocket.shutdownInput();
            dataSocket.shutdownOutput();
            dataSocket.close();
            dataSocket = null;
        }catch(Exception ex){
            CampaignData.mwlog.errLog(ex);
            dataSocket = null;
        }
        
    }
	
}

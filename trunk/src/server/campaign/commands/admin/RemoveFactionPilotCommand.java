package server.campaign.commands.admin;

import java.util.StringTokenizer;

import common.Unit;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;
import server.MWChatServer.auth.IAuthenticator;

/**
 * @author Torren (Jason Tighe)
 */
public class RemoveFactionPilotCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
        String house = "";
        String type = "";
        String weight = "";
        String position = "";

        try{
            house = command.nextToken();
            type = command.nextToken();
        }catch (Exception ex){
            CampaignMain.cm.toUser("Syntanx RemoveFactionPilot#House#Type[Mek,Vehicle,Infantry,Proto,BattleArmor]/ALL#Position[Not used if ALL is selected].",Username);
            return;
        }
        SHouse h =  CampaignMain.cm.getHouseFromPartialString(house,Username);

        try {
            if ( type.equalsIgnoreCase("all") ){
               if ( weight.equalsIgnoreCase("all") ){
                       h.getPilotQueues().flushQueue();
               }
            }
            else{
                position = command.nextToken();
                if ( position.equalsIgnoreCase("all") ){
                    h.getPilotQueues().getPilotQueue(Unit.getTypeIDForName(type)).clear();
                }
                else if ( position.indexOf("-") > 0){
                    int end = Integer.parseInt(position.substring(0,position.indexOf("-")));
                    int start = Integer.parseInt(position.substring(position.indexOf("-")+1));
                    //search backwards through the queue so you stay ahead of the shrinkinage.
                    for (int pos = start ;pos >= end; pos--){
                        h.getPilotQueues().getPilotQueue(Unit.getTypeIDForName(type)).remove(pos);
                    }
                }
                else
                    h.getPilotQueues().getPilotQueue(Unit.getTypeIDForName(type)).remove(Integer.parseInt(position));
            }
        }catch(Exception ex) {
            CampaignMain.cm.toUser("Syntanx RemoveFactionPilot#House#Type[Mek,Vehicle,Infantry,Proto,BattleArmor]/ALL#Position[Not used if ALL is selected].",Username);
            return;
        }

        h.updated();
        CampaignMain.cm.doSendModMail("NOTE:",Username+" has removed pilots from "+h.getName()+"'s pilot queue");
	}
}


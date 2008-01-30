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

package common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import common.util.BinReader;
import common.util.BinWriter;


/**
 * Represents the influences of different Houses of a planet. This may be
 * used as total influences as well as influence differences between two
 * total influences. 
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
@SuppressWarnings({"unchecked","serial"})
public class Influences implements MutableSerializable {

    /**
     * A hash table with key=House and value=Integer of the influences of the
     * different factions. Only factions greater than 0% are listed.
     */
    private HashMap<Integer,Integer> influences = new HashMap<Integer,Integer>();

    /**
     * Creates a new Influence with a preset table.
     * @param influences
     */
    public Influences(HashMap influences) {
        setInfluence(influences);
    }

    /**
     * Create an empty Influence.
     */
    public Influences() {
    }

    /**
     * Copies the Influence
     */
    public Influences(Influences influences) {
        setInfluence(new HashMap(influences.influences));
    }

    /**
     * Return the influence of a specific faction.
     */
    public int getInfluence(int factionID) {
    	
    	if ( !influences.containsKey(factionID) )
    		return 0;
        int i = influences.get(factionID);
        return i;
    }
    
    /**
     * Return the faction with the most influence.
     */
    public Integer getOwner() {
        
        try{
            TreeSet sset = new TreeSet(new Comparator(){
                public int compare(Object o1, Object o2) {
                    int i1 = ((House)o1).getId();
                    int i2 = ((House)o2).getId();
                    return (i1<i2) ? -1 : (i1==i2?0:1);
                }
            });
            sset.addAll(this.getHouses());
            House[] factions = new House[sset.size()];
            
            int i = 0;
            for (Iterator it = sset.iterator(); it.hasNext();)
                factions[i++] = (House)it.next();
            Arrays.sort(factions, new Comparator(){
                public int compare(Object o1, Object o2) {
                    int i1 = getInfluence((((House)o1).getId()));
                    int i2 = getInfluence((((House)o2).getId()));
                    return (i1>i2) ? -1 : (i1==i2?0:1);
                }});
    
            House faction = factions[0];
            if ( faction == null )
                return null;
            
            //only one owner don't need to see whoes the boss.
            if ( factions.length <= 1)
                return faction.getId();
            
            House faction2 = factions[1];
    
            if ( faction2 != null &&  
                    getInfluence((faction2.getId())) == getInfluence((faction.getId())) )
                return null;
    
            return faction.getId();
        }catch (Exception ex){
            ex.printStackTrace();
            System.err.println("Error in Influenes.getOwner()");
            return null;
        }

    }

    /**
     * Fairly distribute the influence under the factions in the list.
     * 
     * @param factions
     *            All of these factions gain as much as possible influece divided
     *            equal
     * @param gainer
     *            If there is a portion left, one faction get it all. This faction.
     */
    public void setNeutral(List factions, House gainer, int maxInfluence) {
        influences = new HashMap();
        for (int i = 0; i < factions.size(); i++) {
            House h = (House) factions.get(i);
            influences.put((h.getId()), (maxInfluence / factions.size()));
        }
        if (maxInfluence % factions.size() != 0) {
            int bonus = maxInfluence % factions.size();
            if (influences.containsKey((gainer.getId())))
                influences.put((gainer.getId()), (((Integer) influences
                        .get(gainer)).intValue()
                        + bonus));
            else
                influences.put((gainer.getId()), (bonus));
        }
    }

    /**
     * Returns the present factions.
     */
    public Set<House> getHouses() {
    	Set result = new HashSet();
    	Iterator it = influences.keySet().iterator();
    	while (it.hasNext()){
    		House faction =  CampaignData.cd.getHouse(((Integer)it.next()).intValue());
    		result.add(faction);
    	}
        return result;
    }
    
    /**
     * Returns the number of factions with ownership on world.
     */
    public int houseCount() {
    	return influences.size();
    }

    /** 
     * Move influence from one faction to a new faction. Note, that this make sure,
     * that nobody can have more influence than 100% and nobody may drop below 0.
     * If you not want to respect to this, use add() instead.
     */
    public int moveInfluence(House winner, House looser, int amount, int maxInfluence) {
        if (amount == 0) return 0;

        int oldwinnerinfluence = 0;
        int oldlooserinfluence = 0;

        //if (influences.get(winner) != null)
            oldwinnerinfluence = getInfluence(winner.getId());
        //if (influences.get(looser) != null)
            oldlooserinfluence = getInfluence(looser.getId());

        if (oldwinnerinfluence + amount >= maxInfluence)
                amount = maxInfluence - oldwinnerinfluence;
        if (oldlooserinfluence < amount) amount = oldlooserinfluence;

        int winnerInfluence = oldwinnerinfluence + amount;
        int looserInfluence = oldlooserinfluence - amount;

        if (winnerInfluence == 0)
            influences.remove((winner.getId()));
        else
            influences.put((winner.getId()), (winnerInfluence));
        
        if (looserInfluence == 0)
            influences.remove((looser.getId()));
        else
            influences.put((looser.getId()), (looserInfluence));
        return amount;
    }

    /**
     * Sets the whole influences.
     * 
     * @param influences The new influences. Key=TimeUpdateHouse, Value=Integer. 
     */
    public void setInfluence(HashMap influences) {
        this.influences = influences;
    }
    

    /**
     * Returns whether the Influence zone belongs to a so called "hot zone",
     * which means, that it is in a critical sector where ownership is not
     * fully clear.
     * 
     * @return True, if it is a hotZone Planet.
     */
    public boolean isHotZone() {
        int maxflu = 0;
        int secondmaxflu = 0;
        Iterator e = influences.values().iterator();
        while (e.hasNext()) {
            int flu = ((Integer)e.next()).intValue();
            if (maxflu < flu) {
                secondmaxflu = maxflu;
                maxflu = flu;
            } else if (secondmaxflu < flu)
                secondmaxflu = flu;
        }
        return (maxflu - secondmaxflu) < 20;
    }

    /**
     * @see common.MutableSerializable#encodeMutableFields(java.io.OutputStream)
     */
    public void encodeMutableFields(BinWriter out, CampaignData dataProvider) throws IOException {
        out.println(influences.size(), "influences.size");
        for (Iterator it = influences.keySet().iterator(); it.hasNext();) {
            Integer i = (Integer)it.next();
            //House h = (House)it.next();
            
            out.println(i.intValue(), "id");
            out.println(((Integer)influences.get(i)).intValue(), "amount");
        }
    }


    public void decodeMutableFields(BinReader in, CampaignData dataProvider) throws IOException {
        int s = in.readInt("influences.size");
        influences.clear();
        for (int i = 0; i < s; i++) {
            int factionID = in.readInt("id");
            int flu = in.readInt("amount");
            influences.put((factionID),(flu));
        }
    }
    
    /**
     * Outputs itself into an xml-Stream.
     */
    public void xmlOut(PrintWriter out) {
        Iterator inf = getHouses().iterator();
        out.println("\t<influence>");
        while (inf.hasNext()) {
            House h = (House) inf.next();
            out.println("\t\t<inf>");
            out.println("\t\t<faction>" + h.getName() + "</faction>");
            out.println("\t\t<amount>" + getInfluence(h.getId()) + "</amount>");
            out.println("\t\t</inf>");
        }
        out.println("\t</influence>");
    }

    /**
     * Calculates the difference between this and the parameter.
     * @return The influence difference.
     */
    public Influences difference(Influences infNew) {
        HashMap diff = new HashMap();
        Collection other = infNew.getHouses();
        Collection thisone = getHouses();
        for (Iterator it = thisone.iterator(); it.hasNext();) {
            House h = (House) it.next();
            int d = getInfluence(h.getId()) - infNew.getInfluence(h.getId());
            if (d != 0)
                diff.put(h,(d));
        }
        for (Iterator it = other.iterator(); it.hasNext();) {
            House h = (House) it.next();
            if (!thisone.contains(h))
                diff.put(h,(-infNew.getInfluence(h.getId())));
        }
        return new Influences(diff);
    }

    /**
     * Adds the parameter's influence to the own.
     */
    public void add(Influences infNew) {
        for (Iterator it = getHouses().iterator(); it.hasNext();) {
            House h = (House) it.next();
            influences.put((h.getId()),(infNew.getInfluence(h.getId())));
        }
        for (Iterator it = infNew.getHouses().iterator(); it.hasNext();) {
            House h = (House) it.next();
            if (!getHouses().contains(h))
                influences.put((h.getId()),(infNew.getInfluence(h.getId())));
        }
        for (Iterator it = getHouses().iterator(); it.hasNext();) {
            House h = (House) it.next();
            if (getInfluence(h.getId()) == 0)
                influences.remove((h.getId()));
        }
    }

    /**
     * Write itself into the stream.
     */
    public void binOut(BinWriter out) throws IOException {
        Object h[] = influences.keySet().toArray();
        Arrays.sort(h, new Comparator(){
            public int compare(Object o1, Object o2) {
                int i1 = ((Integer)o1).intValue();
                int i2 = ((Integer)o2).intValue();
                return i1 == i2 ? 0 : (i1<i2?-1:1);
            }});
        out.println(h.length, "influence.size");
        for (int i = 0; i < h.length; i++) {
            out.println(((Integer)h[i]).intValue(), "faction");
            out.println(getInfluence(((Integer)h[i]).intValue()), "amount");
        }
    }

    /**
     * Read from a binary stream
     */
    public void binIn(BinReader in, Map<Integer,House> factions) throws IOException {
        influences = new HashMap();
        int size = in.readInt("influence.size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put((hid),(flu));
        }
    }

    public void binIn(BinReader in) throws IOException {
        influences = new HashMap();
        int size = in.readInt("influence.size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put((hid),(flu));
        }
    }

    /**
     * @see common.persistence.MMNetSerializable#binOut(common.persistence.TreeWriter)
     *
    public void binOut(TreeWriter out) {
        Object h[] = influences.keySet().toArray();
        Arrays.sort(h, new Comparator(){
            public int compare(Object o1, Object o2) {
                int i1 = ((Integer)o1).intValue();
                int i2 = ((Integer)o2).intValue();
                return i1 == i2 ? 0 : (i1<i2?-1:1);
            }});
        out.write(h.length, "size");
        for (int i = 0; i < h.length; i++) {
            out.write(((Integer)h[i]).intValue(), "faction");
            out.write(getInfluence(((Integer)h[i]).intValue()), "amount");
        }
    }

    /**
     * @see common.persistence.MMNetSerializable#binIn(common.persistence.TreeReader)
     *
    public void binIn(TreeReader in, CampaignData dataProvider) throws IOException {
        influences = new HashMap();
        int size = in.readInt("size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put((hid),(flu));
        }
    }
    */
    public void removeHouse(House house){
    	influences.remove(house.getId());
    }
    
    public void updateHouse(int id, int amount){
    	influences.put(id, amount);
    }
}
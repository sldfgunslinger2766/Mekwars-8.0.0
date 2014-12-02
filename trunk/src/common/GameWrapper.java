package common;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IPlayer;


public class GameWrapper implements GameInterface {
	
	private final IGame game;
	
	public GameWrapper(IGame game) {
		this.game = game;
	}

	public Enumeration<Entity> getDevastatedEntities() {
		return game.getDevastatedEntities();
	}

	public Enumeration<Entity> getGraveyardEntities() {
		return game.getGraveyardEntities();
	}

	public Iterator<Entity> getEntities() {
		return game.getEntities();
	}

	public Enumeration<Entity> getRetreatedEntities() {
		return game.getRetreatedEntities();
	}

	public List<String> getWinners() {
		ArrayList<String> result = new ArrayList<String>();
		Enumeration<IPlayer> en = game.getPlayers();
		while (en.hasMoreElements()){
			final IPlayer player = en.nextElement();
			if (player.getTeam() == game.getVictoryTeam()){
				result.add(player.getName().trim());
			}
		}
		return result;
	}

	public boolean hasWinner() {
		return game.getVictoryTeam() != IPlayer.TEAM_NONE;
	}


}

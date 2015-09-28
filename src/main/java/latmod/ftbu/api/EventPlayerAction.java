package latmod.ftbu.api;

import latmod.core.util.FastList;
import latmod.ftbu.mod.client.gui.friends.PlayerAction;
import latmod.ftbu.world.*;

public class EventPlayerAction extends EventLM
{
	public final FastList<PlayerAction> actions;
	public final LMPlayerClient player;
	public final boolean isSelf;
	
	public EventPlayerAction(FastList<PlayerAction> l, LMPlayerClient p)
	{
		actions = l;
		player = p;
		isSelf = player.equalsPlayer(LMWorldClient.inst.clientPlayer);
	}
}
package latmod.ftbu.mod.cmd;

import latmod.ftbu.core.LMDimUtils;
import latmod.ftbu.core.cmd.*;
import latmod.ftbu.core.world.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.*;

public class CmdBack extends CommandLM
{
	public CmdBack()
	{ super("back", CommandLevel.ALL); }
	
	public IChatComponent onCommand(ICommandSender ics, String[] args)
	{
		EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
		LMPlayerServer p = LMWorldServer.inst.getPlayer(ep);
		if(p.lastDeath == null) return error(new ChatComponentText("No deathpoint found!")); //LANG
		LMDimUtils.teleportPlayer(ep, p.lastDeath);
		return null;
	}
}
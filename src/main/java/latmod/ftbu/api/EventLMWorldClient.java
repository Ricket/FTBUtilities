package latmod.ftbu.api;

import latmod.ftbu.world.LMWorldClient;

public class EventLMWorldClient extends EventLM
{
	public final LMWorldClient world;
	
	public EventLMWorldClient(LMWorldClient w)
	{ world = w; }
	
	public static class Closed extends EventLMWorldClient
	{
		public Closed(LMWorldClient w)
		{ super(w); }
	}
	
	public static class Joined extends EventLMWorldClient
	{
		public Joined(LMWorldClient w)
		{ super(w); }
	}
}
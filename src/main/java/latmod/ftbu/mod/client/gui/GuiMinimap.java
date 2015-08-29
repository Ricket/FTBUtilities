package latmod.ftbu.mod.client.gui;

import latmod.ftbu.core.client.FTBULang;
import latmod.ftbu.core.gui.*;
import latmod.ftbu.core.net.*;
import latmod.ftbu.core.util.*;
import latmod.ftbu.core.world.LMWorldClient;
import latmod.ftbu.mod.FTBU;
import latmod.ftbu.mod.client.gui.friends.GuiFriends;
import latmod.ftbu.mod.client.minimap.*;
import latmod.ftbu.mod.player.ChunkType;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public class GuiMinimap extends GuiLM implements IClientActionGui
{
	public static final ResourceLocation tex = FTBU.mod.getLocation("textures/gui/minimap.png");
	public static final TextureCoords tex_mouse = new TextureCoords(tex, 156, 0, 32, 32);
	private static final MRenderer mapRenderer = new MRenderer();
	
	public final ButtonLM buttonRefresh, buttonClose;
	public final ItemButtonLM buttonSafe;
	public final MapButton mapButton;
	
	public GuiMinimap()
	{
		super(null, tex);
		xSize = 156;
		ySize = 185;
		hideNEI = true;
		
		mapRenderer.mc = mc;
		mapRenderer.size = 144;
		mapRenderer.tiles = 9;
		mapRenderer.startX = MathHelperLM.chunk(mc.thePlayer.posX) - 4;
		mapRenderer.startY = MathHelperLM.chunk(mc.thePlayer.posZ) - 4;
		
		mapRenderer.renderClaims = true;
		mapRenderer.renderGrid = Minimap.renderGrid.getB();
		mapRenderer.renderPlayers = true;
		mapRenderer.renderWaypoints = true;
		
		final String loading = "Loading...";
		
		buttonRefresh = new ButtonLM(this, 6, 6, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				mapButton.title = loading;
				Minimap m = Minimap.get(mc.thePlayer.dimension);
				m.reloadArea(mc.theWorld, mapRenderer.startX, mapRenderer.startY, mapRenderer.tiles, mapRenderer.tiles);
				gui.playClickSound();
			}
		};
		
		buttonRefresh.title = FTBULang.button_refresh;
		
		buttonClose = new ButtonLM(this, 132, 6, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				gui.playClickSound();
				mc.displayGuiScreen(new GuiFriends(null));
			}
		};
		
		buttonClose.title = FTBULang.button_close;
		
		buttonSafe = new ItemButtonLM(this, 28, 6, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				gui.playClickSound();
				LMNetHelper.sendToServer(new MessageClientGuiAction(MessageClientGuiAction.ACTION_SET_SAFE_CHUNKS, LMWorldClient.inst.clientPlayer.claimSettings.isSafe() ? 0 : 1));
			}
		};
		
		buttonSafe.setItem(new ItemStack(Items.skull, 1, 4));
		
		mapButton = new MapButton(this, 6, 26);
		mapButton.title = loading;
		
		buttonRefresh.onButtonPressed(0);
	}
	
	public void addWidgets(FastList<WidgetLM> l)
	{
		l.add(mapButton);
		l.add(buttonRefresh);
		l.add(buttonClose);
		l.add(buttonSafe);
	}
	
	public void drawBackground()
	{
		super.drawBackground();
		
		GL11.glColor4f(1F, 1F, 1F, 1F);
		setTexture(tex);
		
		mapRenderer.renderX = guiLeft + mapButton.posX;
		mapRenderer.renderY = guiTop + mapButton.posY;
		mapRenderer.render();
		
		if(mapButton.mouseOver())
		{
			GL11.glColor4f(0.1F, 1F, 0.7F, 0.8F);
			tex_mouse.render(this, mapRenderer.renderX + (mapButton.chunkX() - mapRenderer.startX) * 16 - guiLeft, mapRenderer.renderY + (mapButton.chunkZ() - mapRenderer.startY) * 16 - guiTop, 16, 16);
			
			boolean down0 = Mouse.isButtonDown(0);
			boolean down1 = Mouse.isButtonDown(1);
			
			if((down0 || down1) && down0 != down1)
			{
				int cx = mapButton.chunkX();
				int cz = mapButton.chunkZ();
				
				MChunk c = Minimap.get(mc.thePlayer.dimension).getChunk(cx, cz);
				if(c != null && c.type != null && (c.type == ChunkType.WILDERNESS || c.type == ChunkType.CLAIMED_SELF))
				{
					if(c.type == ChunkType.WILDERNESS)
					{
						if(down0)
						{
							c.type = ChunkType.CLAIMED_SELF;
							LMNetHelper.sendToServer(new MessageAreaRequest(cx, cz, mc.thePlayer.dimension, 1));
							LMNetHelper.sendToServer(new MessageClaimChunk(mapRenderer.mc.thePlayer.dimension, cx, cz, true));
							playClickSound();
						}
					}
					else
					{
						if(down1)
						{
							c.type = ChunkType.WILDERNESS;
							LMNetHelper.sendToServer(new MessageAreaRequest(cx, cz, mc.thePlayer.dimension, 1));
							LMNetHelper.sendToServer(new MessageClaimChunk(mapRenderer.mc.thePlayer.dimension, cx, cz, false));
							playClickSound();
						}
					}
				}
			}
		}
		
		GL11.glColor4f(1F, 1F, 1F, 1F);
		
		buttonRefresh.render(Icons.map);
		buttonClose.render(Icons.accept);
		buttonSafe.render();
		
		if(LMWorldClient.inst.clientPlayer.claimSettings.isSafe())
		{
			zLevel = 500;
			GL11.glColor4f(1F, 1F, 1F, 0.75F);
			buttonSafe.render(Icons.close);
			GL11.glColor4f(1F, 1F, 1F, 1F);
			zLevel = 0;
		}
	}
	
	public void drawText(FastList<String> l)
	{
		if(LMWorldClient.inst.clientPlayer == null) return;
		String s = LMWorldClient.inst.clientPlayer.claimedChunks + " / " + LMWorldClient.inst.clientPlayer.maxClaimPower;
		fontRendererObj.drawString(s, guiLeft + xSize - fontRendererObj.getStringWidth(s) - 4, guiTop + ySize - 12, 0xFFFFFFFF);
		super.drawText(l);
	}
	
	public void onLMGuiClosed()
	{
	}
	
	public void onClientDataChanged()
	{
	}
	
	public static class MapButton extends ButtonLM
	{
		public final GuiMinimap guiM;
		
		public MapButton(GuiMinimap g, int x, int y)
		{
			super(g, x, y, mapRenderer.size, mapRenderer.size);
			guiM = g;
		}
		
		public int chunkX()
		{ return mapRenderer.startX + (gui.mouseXR - posX) / 16; }
		
		public int chunkZ()
		{ return mapRenderer.startY + (gui.mouseYR - posY) / 16; }
		
		public void onButtonPressed(int b)
		{
			//LMNetHelper.sendToServer(new MessageClaimChunk(mapRenderer.mc.thePlayer.dimension, chunkX(), chunkZ(), b == 0));
			//gui.playClickSound();
		}
		
		public void addMouseOverText(FastList<String> l)
		{ Minimap.get(mapRenderer.mc.thePlayer.dimension).loadChunk(chunkX(), chunkZ()).getMessage(l, isShiftKeyDown()); }
	}
}
package com.feed_the_beast.ftbu.cmd;

import com.feed_the_beast.ftbl.api.cmd.CommandLM;
import com.feed_the_beast.ftbl.api.cmd.CommandSubBase;
import com.feed_the_beast.ftbu.FTBULang;
import com.feed_the_beast.ftbu.config.FTBUConfigBackups;
import com.feed_the_beast.ftbu.world.backups.Backups;
import com.latmod.lib.BroadcastSender;
import com.latmod.lib.util.LMFileUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;

public class CmdBackup extends CommandSubBase
{
    public static class CmdBackupStart extends CommandLM
    {
        public CmdBackupStart()
        {
            super("start");
        }

        @Override
        public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender ics, @Nonnull String[] args) throws CommandException
        {
            boolean b = Backups.INSTANCE.run(ics);
            if(b)
            {
                FTBULang.backup_manual_launch.printChat(BroadcastSender.INSTANCE, ics.getName());

                if(!FTBUConfigBackups.use_separate_thread.getAsBoolean())
                {
                    Backups.INSTANCE.postBackup();
                }
            }
            else
            {
                FTBULang.backup_already_running.printChat(ics);
            }
        }
    }

    public static class CmdBackupStop extends CommandLM
    {
        public CmdBackupStop()
        {
            super("stop");
        }

        @Override
        public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender ics, @Nonnull String[] args) throws CommandException
        {
            if(Backups.INSTANCE.thread != null)
            {
                Backups.INSTANCE.thread.interrupt();
                Backups.INSTANCE.thread = null;
                FTBULang.backup_stop.printChat(ics);
                return;
            }

            throw FTBULang.backup_not_running.commandError();
        }
    }

    public static class CmdBackupGetSize extends CommandLM
    {
        public CmdBackupGetSize()
        {
            super("getsize");
        }

        @Override
        public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender ics, @Nonnull String[] args) throws CommandException
        {
            String sizeW = LMFileUtils.getSizeS(ics.getEntityWorld().getSaveHandler().getWorldDirectory());
            String sizeT = LMFileUtils.getSizeS(Backups.INSTANCE.backupsFolder);
            FTBULang.backup_size.printChat(ics, sizeW, sizeT);
        }
    }

    public CmdBackup()
    {
        super("backup");
        add(new CmdBackupStart());
        add(new CmdBackupStop());
        add(new CmdBackupGetSize());
    }
}
package tk.hugo4715.golemamc.jump.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import net.golema.database.golemaplayer.GolemaPlayer;
import net.golema.database.golemaplayer.rank.Rank;
import net.md_5.bungee.api.ChatColor;
import tk.hugo4715.golemamc.jump.GolemaJump;
import tk.hugo4715.golemamc.jump.JumpManager.JumpInfo;

public class JumpCmd implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
		if(!(sender instanceof Player))return false;
		
		Player p =(Player)sender;
		
		if(GolemaPlayer.getGolemaPlayer(p).getRankPower() < Rank.ADMINISTRATOR.getPower()){
			GolemaPlayer.getGolemaPlayer(p).sendMessageNoPermission();
			return true;
		}
		
		if(args.length >= 2 && args[0].equalsIgnoreCase("create")){
			return commandCreate(p, args);
		}else if(args.length >= 1 && args[0].equalsIgnoreCase("set")){
			return commandSet(p,args);
		}else if(args.length >= 2 && args[0].equals("leaderboard")){
			return commandLeaderboard(p,args);
		}
		return false;
	}
	
	private boolean commandLeaderboard(Player p, String[] args) {
		String name = args[1];
		
		JumpInfo info = GolemaJump.get().getJumpManager().getJump(name);
		
		if(info == null){
			p.sendMessage(ChatColor.RED + "Ce jump n'existe pas!");
			return true;
		}
		
		GolemaJump.get().getConfig().set("jumps." + name + ".leaderboard", p.getLocation().serialize());
		GolemaJump.get().saveConfig();
		GolemaJump.get().updateLoaderBoards();
		return true;
	}

	private boolean commandSet(Player p, String[] args) {
		if(args.length >= 2){
			String name = args[1];
			
			JumpInfo info = GolemaJump.get().getJumpManager().getJump(name);
			
			if(info == null){
				p.sendMessage(ChatColor.RED + "Ce jump n'existe pas!");
				return true;
			}
			
			if(args.length >= 3 && args[2].equalsIgnoreCase("start")){
				info.setStart(p.getLocation().clone().add(0, 0.1, 0));
				p.sendMessage(ChatColor.GREEN + "Vous avez changé le debut du jump.");
				GolemaJump.get().getJumpManager().save();
			}else if(args.length >= 3 && args[2].equalsIgnoreCase("END")){
				info.setEnd(p.getLocation().clone().add(0, 0.1, 0));
				GolemaJump.get().getJumpManager().save();
				p.sendMessage(ChatColor.GREEN + "Vous avez changé la fin du jump");

			}else if(args.length >= 3 && args[2].equalsIgnoreCase("checkpoint")){
				//no index
				if(args.length == 3){
					info.getCheckpoints().add(p.getLocation().clone().add(0, 0.1, 0));
					p.sendMessage(ChatColor.GREEN + "Checkpoint " + info.getCheckpoints().size() + " ajouté.");
					GolemaJump.get().getJumpManager().save();
				}else{
					
					try {
						int index = Integer.parseInt(args[4]);
						info.getCheckpoints().set(index, p.getLocation().clone().add(0, 0.1, 0));
						p.sendMessage(ChatColor.GREEN + "Checkpoint " + index + " changé.");
					} catch (NumberFormatException e) {
						p.sendMessage(ChatColor.RED + args[4] + " n'est pas un nombre valide.");
					}
					
				}
				
			}
		}
		return true;
	}

	private boolean commandCreate(Player p, String[] args) {
		String name = args[1];
		if(name.length() <= 2 || name.length() > 16){
			p.sendMessage(ChatColor.GREEN + "Ce nom est trop long/court");
			return true;
		}
		
		FileConfiguration c = GolemaJump.get().getConfig();
		
		if(c.contains("jumps." + name)){
			p.sendMessage(ChatColor.RED + "Ce jump existe déja!");
			return true;
		}
		
		JumpInfo info = new JumpInfo(name);
		GolemaJump.get().getJumpManager().getJumps().add(info);
		GolemaJump.get().getJumpManager().save();
		p.sendMessage(ChatColor.GREEN + "vous avez crée le jump " + org.bukkit.ChatColor.BOLD + org.bukkit.ChatColor.YELLOW + info.getName());
		return true;
	}
}

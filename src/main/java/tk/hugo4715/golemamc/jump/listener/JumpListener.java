package tk.hugo4715.golemamc.jump.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.golema.database.api.builder.HologramBuilder;
import net.md_5.bungee.api.ChatColor;
import tk.hugo4715.golemamc.jump.GolemaJump;
import tk.hugo4715.golemamc.jump.JumpManager.JumpInfo;

public class JumpListener implements Listener{
	
	public JumpListener() {
		Bukkit.getPluginManager().registerEvents(this, GolemaJump.get());
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e){
		if(e.getFrom().distanceSquared(e.getTo()) == 0)return;
		
		//if the player is not in jump
		if(!GolemaJump.get().isInJump(e.getPlayer())){
			
			for(JumpInfo info : GolemaJump.get().getJumpManager().getJumps()){
				if(info.getStart().distanceSquared(e.getTo()) < 1){
					GolemaJump.get().startJump(info, e.getPlayer());
					//assume there will never be 2 start point at the same place
					return;
				}
			}
		}else{
			//if the player is in a jump
			
			if(e.getPlayer().isFlying()) {
				e.getPlayer().setFlying(false);
				e.getPlayer().setAllowFlight(false);
				GolemaJump.get().tpToCheckpoint(e.getPlayer());
				e.getPlayer().sendMessage(ChatColor.RED + "Vous ne devez pas voler pendant le parcour!");
			}
			
			
			if(e.getTo().getBlockY() < -1){
				GolemaJump.get().tpToCheckpoint(e.getPlayer());
			}else if(e.getTo().distanceSquared(GolemaJump.get().getJumpsNames().get(e.getPlayer()).getEnd()) < 0.99){
				//finish
				GolemaJump.get().onFinish(e.getPlayer());
			}else{
				for(int i = 0; i < GolemaJump.get().getJumpsNames().get(e.getPlayer()).getCheckpoints().size();i++){
					if(e.getTo().distanceSquared(GolemaJump.get().getJumpsNames().get(e.getPlayer()).getCheckpoints().get(i)) < 0.99){
						GolemaJump.get().onCheckpoint(e.getPlayer(),i);
					}
				}
			}
		}
	}
	
	
	@EventHandler
	public void onClick(PlayerInteractEvent e){
		if(e.getItem() != null && GolemaJump.get().isInJump(e.getPlayer())){
			e.setCancelled(true);
			if(e.getItem().getType().equals(Material.BARRIER)){
				GolemaJump.get().leaveJump(e.getPlayer());
			}else if(e.getItem().getType().equals(Material.FEATHER)){
				GolemaJump.get().reset(e.getPlayer());
			}else if(e.getItem().getType().equals(Material.ANVIL)){
				GolemaJump.get().tpToCheckpoint(e.getPlayer());
			}
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		for(HologramBuilder b : GolemaJump.get().getLeaderboardLines()){
			b.sendToPlayer(e.getPlayer());
		}
		for(JumpInfo info : GolemaJump.get().getJumpManager().getJumps()){
			info.sendHolos(e.getPlayer());
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		if(GolemaJump.get().isInJump(e.getPlayer())) {
			GolemaJump.get().leaveJump(e.getPlayer());
		}
	}
	
	
}

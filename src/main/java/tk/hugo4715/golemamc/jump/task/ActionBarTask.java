package tk.hugo4715.golemamc.jump.task;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import tk.hugo4715.golemamc.jump.GolemaJump;

public class ActionBarTask extends BukkitRunnable {

	@Override
	public void run() {
		for(Player p : GolemaJump.get().getJumpsTimes().keySet()){
			long delta = System.currentTimeMillis()-GolemaJump.get().getJumpsTimes().get(p);
			GolemaJump.get().getActionBarApi().sendActionBar(p,ChatColor.GREEN + DurationFormatUtils.formatDuration(delta, "HH:mm:ss,SSS"));
		}
	}
	
	

}

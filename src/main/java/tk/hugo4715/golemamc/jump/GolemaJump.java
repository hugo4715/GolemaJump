package tk.hugo4715.golemamc.jump;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.golema.database.api.builder.HologramBuilder;
import net.golema.database.api.builder.items.ItemBuilder;
import net.golema.database.api.builder.items.fireworks.FireworkBuilder;
import net.md_5.bungee.api.ChatColor;
import tk.hugo4715.golemamc.jump.JumpManager.JumpInfo;
import tk.hugo4715.golemamc.jump.cmd.JumpCmd;
import tk.hugo4715.golemamc.jump.listener.JumpListener;
import tk.hugo4715.golemamc.jump.task.ActionBarTask;
import tk.hugo4715.golemamc.jump.util.ActionBarAPI;
import tk.hugo4715.golemamc.jump.util.InventoryToBase64;
import tk.hugo4715.golemamc.jump.util.TimeUtil;


public class GolemaJump extends JavaPlugin{

	public static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `jump_times_%jump%` (  `id` INTEGER NOT NULL AUTO_INCREMENT,  `name` VARCHAR(64) NOT NULL,`uuid` VARCHAR(64) UNIQUE NOT NULL,  `best` BIGINT NULL DEFAULT NULL COMMENT 'Store the player''s best time',  `last` BIGINT NULL DEFAULT NULL COMMENT 'Store the player''s last time',  PRIMARY KEY (`id`));"; 
	public static final String SQL_SELECT_BY_UUID = "SELECT * FROM `jump_times_%jump%` WHERE uuid = ?;";
	public static final String SQL_SELECT_BY_BEST = "SELECT * FROM `jump_times_%jump%` ORDER BY best DESC LIMIT ?;";
	public static final String SQL_UPDATE_LAST = "INSERT INTO `jump_times_%jump%` (uuid,name,last) VALUES (?,?,?) ON DUPLICATE KEY UPDATE last = ?;";
	public static final String SQL_UPDATE_BEST = "INSERT INTO `jump_times_%jump%` (uuid,name,best,last) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE last = ?, best = ?;";

	public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "Jump" + ChatColor.GRAY + "] " + ChatColor.GRAY;

	private Map<Player, /* currentTimemillis when the player starts the jump*/ Long> jumpsTimes = Maps.newHashMap();
	private Map<Player, JumpInfo> jumpsNames = Maps.newHashMap();
	private Map<Player,/* checkpoint id*/Integer> checkpoints = Maps.newHashMap();
	private Map<Player,String> savedInvs = Maps.newHashMap();

	private JumpManager jumpManager;
	private ActionBarAPI actionBarApi;
	private Db db;

	private List<HologramBuilder> leaderboardLines = null;
	@Override
	public void onEnable() {
		saveDefaultConfig();

		//mysql related
		this.db = new Db(getConfig().getBoolean("dev"));

		this.jumpManager = new JumpManager();
		this.actionBarApi = new ActionBarAPI(this);

		getCommand("jump").setExecutor(new JumpCmd());

		new JumpListener();
		new ActionBarTask().runTaskTimerAsynchronously(this, 2, 2);
		
		updateLoaderBoards();
	}

	@Override
	public void onDisable() {
		this.saveConfig();
		
	}



	public void startJump(JumpInfo info, Player p){
		//start jump
		this.jumpsTimes.put(p, System.currentTimeMillis());
		this.jumpsNames.put(p, info);
		//save inventory
		GolemaJump.get().getSavedInvs().put(p, InventoryToBase64.toBase64(p.getInventory()));

		p.sendMessage(PREFIX + ChatColor.GRAY + "Vous avez commencé le jump. Bonne chance!");

		p.getInventory().clear();
		p.getInventory().setItem(0, new ItemBuilder().type(Material.FEATHER).name(ChatColor.YELLOW + "Recommmencer").build());
		p.getInventory().setItem(8, new ItemBuilder().type(Material.BARRIER).name(ChatColor.RED + "Quitter").build());
	}

	public void leaveJump(Player p){
		if(jumpsTimes.containsKey(p)){
			Inventory inv = null;
			try {
				inv = InventoryToBase64.fromBase64(savedInvs.get(p));

				p.getInventory().clear();
				for(int i = 0; i < inv.getSize();i++){
					p.getInventory().setItem(i, inv.getItem(i));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			jumpsNames.remove(p);
			jumpsTimes.remove(p);
			checkpoints.remove(p);
			p.sendMessage(PREFIX + ChatColor.GRAY + "Vous avez quitté le jump.");
		}
	}

	public void tpToCheckpoint(Player p){
		if(!jumpsNames.containsKey(p))return;

		if(checkpoints.containsKey(p)){
			p.teleport(jumpsNames.get(p).getCheckpoints().get(checkpoints.get(p)));
		}else{
			this.jumpsTimes.put(p, System.currentTimeMillis());
			p.teleport(jumpsNames.get(p).getStart());
		}
	}

	public void reset(Player p){
		checkpoints.remove(p);
		p.teleport(jumpsNames.get(p).getStart());
		p.getInventory().setItem(4, new ItemStack(Material.AIR));
		jumpsTimes.put(p, System.currentTimeMillis());
	}

	public void onFinish(Player p){
		long deltaMS = System.currentTimeMillis()-getJumpsTimes().get(p);
		p.sendMessage(PREFIX + ChatColor.GRAY + "Vous avez finit le jump en " + ChatColor.YELLOW + TimeUtil.intervalToHumanReadableTime(deltaMS));

		//TODO commit to mysql
		try {
			PreparedStatement ps = getMYsql().getConnection().prepareStatement(SQL_SELECT_BY_UUID.replace("%jump%", jumpsNames.get(p).getName()));
			ps.setString(1, p.getUniqueId().toString());
			ResultSet rs = ps.executeQuery();

			if(rs.next()){
				long best = rs.getLong("best");

				if(best == 0 || deltaMS < best){
					onBestScore(p, deltaMS);
				}else{
					p.sendMessage(PREFIX + ChatColor.YELLOW + "Votre meilleur temps est de " + ChatColor.YELLOW + TimeUtil.intervalToHumanReadableTime(best)); 
					onScore(p,deltaMS);
				}
			}else{
				onBestScore(p, deltaMS);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		//TODO check if best time
		//TODO broadcast if best


		leaveJump(p);
	}

	private void onScore(Player p , long time) {
		//do not send message here
		try {
			PreparedStatement ps = getMYsql().getConnection().prepareStatement(SQL_UPDATE_LAST.replace("%jump%", jumpsNames.get(p).getName()));
			ps.setString(1, p.getUniqueId().toString());
			ps.setString(2, p.getName());
			ps.setLong(3, time);
			ps.setLong(4, time);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public void onBestScore(Player p, long timeMS){

		try {
			PreparedStatement ps = getMYsql().getConnection().prepareStatement(SQL_UPDATE_BEST.replace("%jump%", jumpsNames.get(p).getName()));
			ps.setString(1, p.getUniqueId().toString());
			ps.setString(2, p.getName());
			ps.setLong(3, timeMS);
			ps.setLong(4, timeMS);
			ps.setLong(5, timeMS);
			ps.setLong(6, timeMS);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		p.sendMessage(PREFIX + ChatColor.YELLOW + "Ceci est votre meilleur temps actuel!");
		
		new FireworkBuilder().addRandomColors(3).spawn(p.getEyeLocation());
		
	}

	public void onCheckpoint(Player player, int i) {

		if(!checkpoints.containsKey(player)){
			//give the checkpoint item
			player.getInventory().setItem(4, new ItemBuilder().type(Material.ANVIL).name(ChatColor.YELLOW + "Checkpoint").build());
		}

		if(!checkpoints.containsKey(player) || checkpoints.get(player) < i){
			checkpoints.put(player, i);
			player.sendMessage(ChatColor.GREEN + "Vous avez atteint le checkpoint " + (i+1));
		}

	}
	
	public void updateLoaderBoards() {
		if(leaderboardLines != null){
			for(HologramBuilder h : leaderboardLines){
				h.destroy();
			}
		}
		
		leaderboardLines = Lists.newArrayList();
		
		for(JumpInfo info : getJumpManager().getJumps()){
			if(getConfig().isConfigurationSection("jumps." + info.getName() + ".leaderboard")){
				Location loc = Location.deserialize(getConfig().getConfigurationSection("jumps." + info.getName() + ".leaderboard").getValues(false));
				
				List<HologramBuilder> holos = Lists.newArrayList();
				double y = 0;
				double linesY = 0.25;
				
				try {
					PreparedStatement ps = getMYsql().getConnection().prepareStatement(SQL_SELECT_BY_BEST.replace("%jump%", info.getName()));
					ps.setInt(1, 5);
					
					ResultSet rs = ps.executeQuery();
					int index = 1;
					while(rs.next()){
						holos.add(new HologramBuilder().editLocation(loc.clone().add(0,y, 0)).editMessage(ChatColor.YELLOW + "#" + index + ChatColor.AQUA + rs.getString("name")));
						y+=linesY;
						index++;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				holos.add(new HologramBuilder().editLocation(loc.clone().add(0,y, 0)).editMessage(ChatColor.AQUA + "Scores"));
				leaderboardLines.addAll(holos);
			}
			
		}
	}
	
	
	public List<HologramBuilder> getLeaderboardLines() {
		return leaderboardLines;
	}
	public ActionBarAPI getActionBarApi() {
		return actionBarApi;
	}
	public Map<Player, Long> getJumpsTimes() {
		return jumpsTimes;
	}
	public Map<Player, JumpInfo> getJumpsNames() {
		return jumpsNames;
	}
	public boolean isInJump(Player p){
		return jumpsNames.containsKey(p);
	}
	public JumpManager getJumpManager() {
		return jumpManager;
	}

	public Map<Player, String> getSavedInvs() {
		return savedInvs;
	}

	public Db getMYsql() {
		return db;
	}

	public static GolemaJump get(){
		return getPlugin(GolemaJump.class);
	}

	


}

package tk.hugo4715.golemamc.jump;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.golema.database.api.builder.HologramBuilder;
import net.md_5.bungee.api.ChatColor;

public class JumpManager {

	private List<JumpInfo> jumps = Lists.newArrayList();

	public JumpManager() {
		reload();
	}

	private void reload() {
		jumps.clear();
		FileConfiguration c = GolemaJump.get().getConfig();

		if(c.isConfigurationSection("jumps")){
			for(String name : c.getConfigurationSection("jumps").getKeys(false)){
				jumps.add(new JumpInfo(c.getConfigurationSection("jumps." + name)));
			}
		}
	}
	
	public List<JumpInfo> getJumps() {
		return jumps;
	}
	
	public JumpInfo getJump(String name){
		for(JumpInfo info : getJumps()){
			if(info.getName().equalsIgnoreCase(name))return info;
		}
		return null;
	}
	
	public void save(){
		GolemaJump.get().getConfig().set("jumps", null);
		for(JumpInfo info : jumps){
			GolemaJump.get().getConfig().set("jumps." + info.getName(), info.serialize());
		}
		GolemaJump.get().saveConfig();
	}

	public static class JumpInfo implements ConfigurationSerializable{

		private String name;
		private Location start;
		private List<Location> checkpoints;
		private Location end;
		
		private HologramBuilder startHolo;
		private HologramBuilder endHolo;
		private List<HologramBuilder> checkpointsHolo;
		
		public JumpInfo(ConfigurationSection c) {
			Validate.notNull(c);
			this.name = c.getString("name");
			this.start = Location.deserialize(c.getConfigurationSection("start").getValues(false));
			this.end = Location.deserialize(c.getConfigurationSection("end").getValues(false));
			this.checkpoints = Lists.newArrayList();
			
			if(c.isConfigurationSection("checkpoint")){
				for(String index : c.getConfigurationSection("checkpoint").getKeys(false)){
					this.checkpoints.add(Location.deserialize(c.getConfigurationSection("checkpoint." + index).getValues(false)));
				}
			}
			
			init();
		}
		public JumpInfo(String name) {
			this.name = name;
			this.start = new Location(Bukkit.getWorlds().get(0),0,-1000,0);
			this.end = new Location(Bukkit.getWorlds().get(0),0,-1000,0);
			this.checkpoints = Lists.newArrayList();
			
			init();
		}
		
		private void init(){
			try (Statement ps = GolemaJump.get().getMYsql().getConnection().createStatement()){
				ps.execute(GolemaJump.SQL_CREATE_TABLE.replace("%jump%", getName()));
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			startHolo = new HologramBuilder().editLocation(start.clone().add(0, 0.5, 0)).editMessage(ChatColor.AQUA + "Jump");
			endHolo = new HologramBuilder().editLocation(end.clone().add(0, 0.5, 0)).editMessage(ChatColor.AQUA + "Fin du jump");
			
			checkpointsHolo = Lists.newArrayList();
			for(int i = 0; i < checkpoints.size();i++){
				checkpointsHolo.add(new HologramBuilder().editLocation(checkpoints.get(i).clone().add(0, 0.5, 0)).editMessage(ChatColor.AQUA + "Checkpoint #" + (i+1)));
			}
		}
		
		public String getName() {
			return name;
		}
		public Location getStart() {
			return start;
		}
		public List<Location> getCheckpoints() {
			return checkpoints;
		}
		public Location getEnd() {
			return end;
		}
		public void setCheckpoints(List<Location> checkpoints) {
			this.checkpoints = checkpoints;
		}
		public void setEnd(Location end) {
			this.end = end;
		}
		public void setStart(Location start) {
			this.start = start;
		}
		
		public void sendHolos(Player player) {
			startHolo.sendToPlayer(player);
			endHolo.sendToPlayer(player);
			for(HologramBuilder b : checkpointsHolo){
				b.sendToPlayer(player);
			}
		}
		
		public Map<String, Object> serialize() {
			Map<String,Object> m = Maps.newHashMap();
			m.put("start", start.serialize());
			m.put("end", end.serialize());
			
			for(int i = 0; i < checkpoints.size();i++){
				m.put("checkpoint." + i, checkpoints.get(i).serialize());
			}
			m.put("name", name);
			return m;
		}
		

	}
}

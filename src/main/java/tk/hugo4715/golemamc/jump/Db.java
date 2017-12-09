package tk.hugo4715.golemamc.jump;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.golema.database.GolemaBukkitDatabase;

public class Db {
	
	
	private boolean dev;

	private Connection conn;
	
	public Db(boolean dev) {
		this.dev = dev;
	}
	
	public Connection getConnection(){
		if(!dev){
			//production
			return GolemaBukkitDatabase.INSTANCE.sqlManager.getRessource();
		}else{
			//dev
			if(conn == null){
				try {
					conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db1","root", "");
				} catch (SQLException e) {
					System.out.println("Connection Failed!");
					e.printStackTrace();
					return null;
				}
			}
			
			return conn;
		}
	}
	
	
	
	
}

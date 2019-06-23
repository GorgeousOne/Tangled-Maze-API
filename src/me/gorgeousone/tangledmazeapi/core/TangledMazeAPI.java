package me.gorgeousone.tangledmazeapi.core;

import org.bukkit.plugin.java.JavaPlugin;

import me.gorgeousone.tangledmazeapi.data.Constants;

public class TangledMazeAPI extends JavaPlugin {

	@Override
	public void onEnable() {
		Constants.loadConstants(this);
	}
}
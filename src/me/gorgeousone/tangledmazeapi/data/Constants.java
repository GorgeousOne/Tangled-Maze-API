package me.gorgeousone.tangledmazeapi.data;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import me.gorgeousone.tangledmazeapi.core.TangledMazeAPI;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.TreeSet;

public class Constants {
	
	public static final TreeSet<Material>
			NOT_SOLIDS = new TreeSet<>(),
			REPLACEABLE_SOLIDS = new TreeSet<>();
	
	@SuppressWarnings("unchecked")
	public static void loadConstants(TangledMazeAPI plugin) {
		
		InputStream defConfigStream = plugin.getResource("material_lists.yml");
		YamlConfiguration materialLists = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
		
		for(String materialName : (List<String>) materialLists.getList("not-solid-materials"))
			NOT_SOLIDS.add(Material.valueOf(materialName));

		for(String materialName : (List<String>) materialLists.getList("replaceable-solid-materials"))
			REPLACEABLE_SOLIDS.add(Material.valueOf(materialName));
	}
}
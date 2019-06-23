package me.gorgeousone.tangledmazeapi.util;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.gorgeousone.tangledmazeapi.data.Constants;

public abstract class Utils {

	public static boolean isLikeGround(Material mat) {
		return mat.isSolid() && !Constants.NOT_SOLIDS.contains(mat);
	}
	
	public static boolean canBeOverbuild(Material mat) {
		return !mat.isSolid() || Constants.REPLACEABLE_SOLIDS.contains(mat);
	}

	public static Location nearestSurface(Location loc) {
		
		Location iter = loc.clone();
		
		if(isLikeGround(iter.getBlock().getType())) {
		
			while(iter.getY() <= 255) {
				iter.add(0, 1, 0);
				
				if(!isLikeGround(iter.getBlock().getType())) {
					iter.add(0, -1, 0);
					return iter;
				}
			}
		
		}else {
			
			while(iter.getY() >= 0) {
				iter.add(0, -1, 0);
				
				if(isLikeGround(iter.getBlock().getType())) {
					return iter;
				}
			}
		}
		
		return loc;
	}
	
	public static int nearestSurfaceY(Vec2 loc, int height, World world) {
		
		Location iter = new Location(world, loc.getX(), height, loc.getZ());
		
		if(isLikeGround(iter.getBlock().getType())) {
			
			while(iter.getY() <= 255) {
				
				iter.add(0, 1, 0);
				
				if(!isLikeGround(iter.getBlock().getType())) {
					iter.add(0, -1, 0);
					return iter.getBlockY();
				}
			}
		
		}else {
			
			while(iter.getY() >= 0) {
				
				iter.add(0, -1, 0);
				
				if(isLikeGround(iter.getBlock().getType())) {
					return iter.getBlockY();
				}
			}
		}
		
		return height;
	}
	
	public static int getMaxHeight(List<Location> locs) {
		
		int min = 0;
		
		for(Location point : locs) {
			
			if(point.getBlockY() > min)
				min = point.getBlockY();
		}
		
		return min;
	}

	public static ArrayList<Location> createRectangularVertices(Location vertex0, Location vertex2) {
		
		ArrayList<Location> vertices = new ArrayList<>();
		World world = vertex0.getWorld();
		
		int maxY = Math.max(vertex0.getBlockY(), vertex2.getBlockY());
		
		int minX = Math.min(vertex0.getBlockX(), vertex2.getBlockX()),
			minZ = Math.min(vertex0.getBlockZ(), vertex2.getBlockZ()),
			maxX = Math.max(vertex0.getBlockX(), vertex2.getBlockX()),
			maxZ = Math.max(vertex0.getBlockZ(), vertex2.getBlockZ());
		
		vertices = new ArrayList<>(Arrays.asList(
				Utils.nearestSurface(new Location(world, minX, maxY, minZ)),
				Utils.nearestSurface(new Location(world, maxX, maxY, minZ)),
				Utils.nearestSurface(new Location(world, maxX, maxY, maxZ)),
				Utils.nearestSurface(new Location(world, minX, maxY, maxZ))));
		
		return vertices;
	}
	
	public static void updateBlocksContinuously(List<BlockState> blocksToUpdate, Plugin plugin, ActionListener callback) {
		
		BukkitRunnable builder = new BukkitRunnable() {
			@Override
			public void run() {
				
				long timer = System.currentTimeMillis();
				
				while(!blocksToUpdate.isEmpty()) {
					
					blocksToUpdate.get(0).update(true, false);
					blocksToUpdate.remove(0);
					
					if(System.currentTimeMillis() - timer >= 49)
						return;
				}
				
				this.cancel();
				
				if(callback != null)
					callback.actionPerformed(null);
			}
		};
		
		builder.runTaskTimer(plugin, 0, 1);
	}
}
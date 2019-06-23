package me.gorgeousone.tangledmazeapi.generation;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.gorgeousone.tangledmazeapi.core.Maze;
import me.gorgeousone.tangledmazeapi.util.Directions;
import me.gorgeousone.tangledmazeapi.util.Utils;
import me.gorgeousone.tangledmazeapi.util.Vec2;

/**
 * A class that smoothes the height of walls in a BuildMap and 
 * calculates all BlockStates that need to be updated in order to generate the maze.
 */
@SuppressWarnings("deprecation")
public class BlockGenerator {
	
	protected Random rnd;

	public BlockGenerator() {
		rnd = new Random();
	}
	
	public void generateMazeBlocks(BuildMap buildMap, Plugin plugin, ActionListener callBack) {
		
		cullTrees(buildMap);
		raiseTooLowWalls(buildMap);
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				
				Maze maze = buildMap.getMaze();

				List<MaterialData> wallMaterials = maze.getWallMaterials();
				List<BlockState> blocksToUpdate = new ArrayList<>();
				List<BlockState> backupBlocks = new ArrayList<>();

				for(int x = buildMap.getMinX(); x < buildMap.getMaxX(); x++) {
					for(int z = buildMap.getMinZ(); z < buildMap.getMaxZ(); z++) {
						
						if(buildMap.getType(x, z) != MazeFillType.WALL)
							continue;
						
						for(int height = buildMap.getGroundHeight(x, z) + 1; height <= buildMap.getMazeHeight(x, z); height++) {
							
							Block block = new Location(maze.getWorld(), x, height, z).getBlock();
							
							if(Utils.canBeOverbuild(block.getType())) {
								
								MaterialData rndMaterial = wallMaterials.get(rnd.nextInt(wallMaterials.size()));
								
								BlockState blockToUpdate = block.getState();
								blockToUpdate.setData(rndMaterial);
								
								blocksToUpdate.add(blockToUpdate);
								backupBlocks.add(block.getState());
							}
						}
					}
				}
				
				buildMap.setPreviousBlocks(backupBlocks);
				buildMap.setGeneratedBlocks(blocksToUpdate);
				
				if(callBack != null)
					callBack.actionPerformed(null);
				
			}
		}.runTask(plugin);
	}
	
	/**
	 * A method that lowers wall heights in a BuildMap at points where single spikes of wall would stick out.
	 */
	protected void cullTrees(BuildMap buildMap) {
		
		int wallHeight = buildMap.getMaze().getWallHeight();

		for(int x = buildMap.getMinX(); x < buildMap.getMaxX(); x++) {
			for(int z = buildMap.getMinZ(); z < buildMap.getMaxZ(); z++) {
				
				if(buildMap.getType(x, z) == MazeFillType.NOT_MAZE)
					continue;
				
				Vec2 maxNeighbor = getHeighestNeighbor(x, z, buildMap, null);
				
				int mazeHeight = buildMap.getMazeHeight(x, z);
				int defaultMazeHeight = buildMap.getGroundHeight(maxNeighbor) + wallHeight;
				
				if(mazeHeight <= defaultMazeHeight)
					continue;
				
				int groundDiffToNeighbors = getGroundDiffToNeighbors(buildMap, x, z);
				
				//adapt ground height of path points to surrounding ground height
				if(buildMap.getType(x, z) == MazeFillType.PATH)
					buildMap.setGroundHeight(x, z, buildMap.getGroundHeight(x, z) + groundDiffToNeighbors);
				//adapt wall height of wall points to default wall height or neighbor wall heights
				else
					buildMap.setMazeHeight(x, z, Math.min(defaultMazeHeight, mazeHeight + groundDiffToNeighbors));
			}
		}
	}
	
	/**
	 * A method that raises walls in a BuildMap which are too low to surrounding paths.
	 */
	protected void raiseTooLowWalls(BuildMap buildMap) {
		
		int wallHeight = buildMap.getMaze().getWallHeight();

		for(int x = buildMap.getMinX(); x < buildMap.getMaxX(); x++) {
			for(int z = buildMap.getMinZ(); z < buildMap.getMaxZ(); z++) {
				
				if(buildMap.getType(x, z) == MazeFillType.NOT_MAZE)
					continue;
				
				Vec2 maxNeighbor = getHeighestNeighbor(x, z, buildMap, MazeFillType.PATH);
				
				if(maxNeighbor == null)
					continue;
				
				int maxNeighborsWallHeight = buildMap.getWallHeight(maxNeighbor);
		
				if(maxNeighborsWallHeight <= 0)
					continue;
				
				int mazeHeight = buildMap.getMazeHeight(x, z),
					maxNeighborsGroundHeight = buildMap.getGroundHeight(maxNeighbor);
				
				if(mazeHeight < maxNeighborsGroundHeight + wallHeight)
					buildMap.setMazeHeight(x, z, maxNeighborsGroundHeight + wallHeight);
			}
		}
	}
	
	protected Vec2 getHeighestNeighbor(int x, int z, BuildMap buildMap, MazeFillType limitation) {
		
		Vec2 maxNeighbor = null;
		int maxHeight = 0;
		
		for(Directions dir : Directions.values()) {
			
			Vec2 neighbor = new Vec2(x, z).add(dir.toVec2());
			
			if(!buildMap.contains(neighbor))
				continue;
			
			if(buildMap.getType(neighbor) == MazeFillType.NOT_MAZE || limitation != null &&
			   buildMap.getType(neighbor) != limitation) {
				continue;
			}
			
			int neighborHeight = buildMap.getMazeHeight(neighbor);
			
			if(maxNeighbor == null || neighborHeight > maxHeight) {
				maxNeighbor = neighbor;
				maxHeight = neighborHeight;
			}
		}
		
		return maxNeighbor;
	}

	protected int getGroundDiffToNeighbors(BuildMap buildMap, int x, int z) {
		
		int groundHeight = buildMap.getGroundHeight(x, z);
		int heightDiff = 0;
		int neighborsCount = 0;
		
		for(Directions dir : Directions.values()) {
			
			Vec2 neighbor = new Vec2(x, z).add(dir.toVec2());
			
			if(!buildMap.contains(neighbor) || buildMap.getType(neighbor) == MazeFillType.NOT_MAZE)
				continue;
			
			heightDiff += buildMap.getGroundHeight(neighbor) - groundHeight;
			neighborsCount++;
		}
		
		return heightDiff / neighborsCount;
	}
}
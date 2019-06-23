package me.gorgeousone.tangledmazeapi.core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.gorgeousone.tangledmazeapi.clip.*;
import me.gorgeousone.tangledmazeapi.generation.BlockGenerator;
import me.gorgeousone.tangledmazeapi.generation.BuildMap;
import me.gorgeousone.tangledmazeapi.generation.PathGenerator;
import me.gorgeousone.tangledmazeapi.util.Directions;
import me.gorgeousone.tangledmazeapi.util.Utils;
import me.gorgeousone.tangledmazeapi.util.Vec2;

/**
 * A class to save all information needed to generate a maze.
 * It contains a clip, a list of exits, a list with material data for generating blocks as well as
 * wall width, wall height, path width and path length.
 * Additionally it has methods to add or cut away other clips from the existing clip.
 * 
 * @see #getAddition(Clip)
 * @see #processAction(ClipAction, boolean)
 */

@SuppressWarnings("deprecation")
public class Maze {
	
	private static IllegalStateException notAlterableException = new IllegalStateException("The maze cannot be altered when it is geerated.");
	
	private ActionHistory history;
	private Clip clip;
	private Stack<Vec2> exits;
	private List<MaterialData> wallMaterials;
	private List<BlockState> undoBlocks;
	
	private int wallWidth;
	private int wallHeight;
	private int pathWidth;
	private int pathLength;
	
	private boolean isGenerated;
	private boolean isBeingGenerated;
	
	public Maze(Clip clip) {
		
		setClip(clip);
		history = new ActionHistory();
		exits = new Stack<>();
		undoBlocks = new ArrayList<>();
		
		wallWidth = 1;
		wallHeight = 2;
		pathWidth = 1;
		pathLength = 5;
	}

	public World getWorld() {
		return clip.getWorld();
	}
	
	public boolean hasClip() {
		return clip != null;
	}
	
	public boolean isGenerated() {
		return isGenerated;
	}
	
	public boolean isBeingGenerated() {
		return isBeingGenerated;
	}
	
	public Clip getClip() {
		return clip;
	}
	
	public Maze setClip(Clip clip) {
		
		this.clip = clip;
		return this;
	}
	
	public Stack<Vec2> getExits() {
		
		Stack<Vec2> deepCopy = new Stack<>(); 
		
		for(Vec2 exit : exits)
			deepCopy.push(exit.clone());
		
		return deepCopy;
	}
	
	public boolean hasExits() {
		return !exits.isEmpty();
	}
	
	public Vec2 getMainExit() {
		return hasExits() ? exits.peek().clone() : null;
	}
	
	public ActionHistory getActionHistory() {
		return history;
	}
	
	public int getWallWidth() {
		return wallWidth;
	}
	
	public void setWallWidth(int blocks) {
		wallWidth = Math.max(1, blocks);
	}
	
	public int getWallHeight() {
		return wallHeight;
	}
	
	public void setWallHeight(int blocks) {
		wallHeight = Math.max(1, blocks);
	}
	
	public int getPathWidth() {
		return pathWidth;
	}
	
	public void setPathWidth(int blocks) {
		pathWidth = Math.max(1, blocks);
	}

	public int getPathLength() {
		return pathLength;
	}
	
	public void setPathLength(int blocks) {
		pathLength = Math.max(1, blocks);
	}

	public List<MaterialData> getWallMaterials() {
		return wallMaterials;
	}
	
	public void setWallMaterials(List<MaterialData> materials) {
		wallMaterials = materials;
	}
	
	public List<BlockState> getPreviousBlocks() {
		return undoBlocks;
	}
	
	public boolean exitsContain(Vec2 loc) {
		return exits.contains(loc);
	}

	public boolean canBeExit(Vec2 loc) {
		
		if(!getClip().borderContains(loc))
			return false;
		
		return sealsMaze(loc, Directions.cardinalValues());
	}
	
	public boolean addExit(Vec2 loc) {
		
		if(!exits.contains(loc) && canBeExit(loc)) {
			
			exits.add(loc);
			return true;
		}
			
		return false;
	}
	
	public boolean removeExit(Vec2 loc) {
		
		if(exits.contains(loc)) {
			
			exits.remove(loc);
			return true;
		}
			
		return false;
	}

	public void processAction(ClipAction action, boolean saveToHistory) {
		
		if(isGenerated())
			throw notAlterableException;
		
		for(Vec2 border : action.getRemovedBorder())
			getClip().removeBorder(border);
		
		for(Vec2 fill : action.getRemovedFill().keySet())
			getClip().removeFill(fill);

		getClip().addAllFill(action.getAddedFill());
		
		for(Vec2 border : action.getAddedBorder())
			getClip().addBorder(border);
		
		exits.removeAll(action.getRemovedExits());

		if(saveToHistory)
			getActionHistory().pushAction(action);
	}
	
	/**
	 * Returns a CliptAction to perform a merger of the Clip of this Maze with another Clip.
	 * The method returns null if the other Clip is completely covered by the Clip of the Maze.
	 * 
	 * @see #processAction(ClipAction, boolean)
	 */
	public ClipAction getAddition(Clip otherClip) {
	
		if(!getWorld().equals(otherClip.getWorld()))
			return null;
		
		ClipAction addition = new ClipAction(getClip());

		addProtrudingFill(otherClip, addition);
		
		//return if the shapes is totally covered by the maze
		if(addition.getAddedFill().isEmpty())
			return null;

		addProtrudingBorder(otherClip, addition);
		removeEnclosedBorder(otherClip, addition);
		removeExitsInsideClip(otherClip, addition);
		return addition;
	}
	
	private void addProtrudingFill(Clip otherClip, ClipAction addition) {

		for(Entry<Vec2, Integer> otherFill : otherClip.getFillSet()) {
			
			if(!getClip().contains(otherFill.getKey()))
				addition.addFill(otherFill.getKey(), otherFill.getValue());
		}
	}
	
	private void addProtrudingBorder(Clip otherClip, ClipAction addition) {
	
		for(Vec2 otherBorder : otherClip.getBorder()) {
			
			if(!getClip().contains(otherBorder))
				addition.addBorder(otherBorder);
		}
	}
	
	private void removeEnclosedBorder(Clip otherClip, ClipAction addition) {
		
		for(Vec2 ownBorder : getClip().getBorder()) {
			
			if(otherClip.contains(ownBorder) &&
			  !otherClip.borderContains(ownBorder) ||
			  !sealsMaze(ownBorder, addition, Directions.values()))
				addition.removeBorder(ownBorder);
		}
	}
	
	private void removeExitsInsideClip(Clip otherClip, ClipAction changes) {
	
		for(Vec2 exit : exits) {
			
			if(otherClip.contains(exit))
				changes.removeExit(exit);
		}
	}
	
	/**
	 * Returns a CliptAction to perform a removal of a Clip from the Clip of this Maze.
	 * The method returns null if the other Clip does not intersects the Clip of the Maze.
	 * 
	 * @see #processAction(ClipAction, boolean)
	 */
	public ClipAction getDeletion(Clip clip) {
		
		if(!getWorld().equals(clip.getWorld()))
			return null;
		
		ClipAction deletion = new ClipAction(getClip());
		
		removeOverlappingFill(clip, deletion);
		
		if(deletion.getRemovedFill().isEmpty())
			return null;
		
		//the order of these steps has not to be changed
		addIntersectingBorder(clip, deletion);
		removeExcludedBorder(clip, deletion);
		removeExitsInsideClip(clip, deletion);
		return deletion;
	}
	
	private void removeOverlappingFill(Clip otherClip, ClipAction deletion) {
		
		for(Entry<Vec2, Integer> otherFill : otherClip.getFillSet()) {
			
			if(!otherClip.borderContains(otherFill.getKey()) && getClip().contains(otherFill.getKey()))
				deletion.removeFill(otherFill.getKey(), otherFill.getValue());
		}
	}

	private void addIntersectingBorder(Clip otherClip, ClipAction deletion) {
		
		for(Vec2 otherBorder : otherClip.getBorder()) {
			
			if(!getClip().borderContains(otherBorder) && getClip().contains(otherBorder))
				deletion.addBorder(otherBorder);
		}
	}

	private void removeExcludedBorder(Clip otherClip, ClipAction deletion) {
		
		for(Vec2 ownBorder : getClip().getBorder()) {
			
			if(!otherClip.borderContains(ownBorder) && !sealsMaze(ownBorder, deletion, Directions.values()))
				deletion.removeBorder(ownBorder);
		}
	}

	/**
	 * Returns a CliptAction to perform an expansion at the border of the Clip of this Maze.
	 * The method returns null if the passed location is not border of this Maze.
	 * 
	 * @see #processAction(ClipAction, boolean)
	 */
	public ClipAction getExpansion(Vec2 loc) {
		
		if(!getClip().borderContains(loc))
			return null;
		
		ClipAction expansion = new ClipAction(getClip());
		
		expandBorder(loc, expansion);
		removeIntrusiveBorder(loc, expansion);
		
		return expansion;
	}
	
	private void expandBorder(Vec2 loc, ClipAction expansion) {
		
		expansion.removeBorder(loc);
		
		for(Directions dir : Directions.values()) {
			
			Vec2 neighbor = loc.clone().add(dir.toVec2());
			int height = Utils.nearestSurfaceY(neighbor, getClip().getHeight(loc), getWorld());
			
			if(!getClip().contains(neighbor)) {
				
				expansion.addFill(neighbor, height);
				expansion.addBorder(neighbor);
				
			}else if(exitsContain(neighbor) && !sealsMaze(neighbor, expansion, Directions.cardinalValues()))
				expansion.removeExit(neighbor);
		}
	}
	
	private void removeIntrusiveBorder(Vec2 loc, ClipAction expansion) {
		//look for neighbors, that are now intruding the border unnecessarily
		for(Directions dir : Directions.values()) {
			
			Vec2 neighbor = loc.clone().add(dir.toVec2());

			if(getClip().borderContains(neighbor) && !sealsMaze(neighbor, expansion, Directions.values()))
				expansion.removeBorder(neighbor);
		}
	}

	
	/**
	 * Returns a CliptAction to perform an erasure at the border of the Clip of this Maze.
	 * The method returns null if the passed location is not border of this Maze.
	 * 
	 * @see #processAction(ClipAction, boolean)
	 */
	public ClipAction getErasure(Vec2 loc) {
		
		if(!getClip().borderContains(loc))
			return null;
		
		ClipAction action = new ClipAction(getClip());
		
		action.removeBorder(loc);
		
		reduceBorder(loc, action);
		removeProtrusiveBorder(loc, action);
		return action;
	}
	
	
	private void reduceBorder(Vec2 loc, ClipAction erasure) {
		
		if(exitsContain(loc))
			erasure.removeExit(loc);
		
		erasure.removeBorder(loc);
		erasure.removeFill(loc, getClip().getHeight(loc));
		
		if(!sealsMaze(loc, erasure, Directions.values()))
			return;
		
		for(Directions dir : Directions.values()) {
			Vec2 neighbor = loc.clone().add(dir.toVec2());
			
			if(getClip().contains(neighbor) && !getClip().borderContains(neighbor))
				erasure.addBorder(neighbor);
			
			if(exitsContain(neighbor) && !sealsMaze(neighbor, erasure, Directions.cardinalValues()))
				erasure.removeExit(neighbor);
		}
	}
	
	private void removeProtrusiveBorder(Vec2 loc, ClipAction erasure) {
		//detect outstanding neighbor borders of the block
		for(Directions dir : Directions.values()) {

			Vec2 neighbor = loc.clone().add(dir.toVec2());
			
			//remove the neighbor if it still stands out
			if(getClip().borderContains(neighbor) && !sealsMaze(neighbor, erasure, Directions.values())) {
				
				int height = getClip().getHeight(neighbor);
				erasure.removeBorder(neighbor);
				erasure.removeFill(neighbor, height);
			}
		}
	}
	
	public boolean sealsMaze(Location loc, Directions[] directions) {
		return sealsMaze(new Vec2(loc), directions);
	}

	public boolean sealsMaze(Vec2 loc, Directions[] directions) {
		return sealsMaze(loc, new ClipAction(getClip()), directions);
	}
	
	public boolean sealsMaze(Vec2 loc, ClipAction changes, Directions[] directions) {
		
		boolean touchesFill = false;
		boolean touchesExternal = false;
		
		for(Directions dir : directions) {
			
			Vec2 neighbor = loc.clone().add(dir.toVec2());
			
			if(!changes.clipWillContain(neighbor))
				touchesExternal = true;

			else if(!changes.clipBorderWillContain(getClip(), neighbor))
				touchesFill = true;
			
			if(touchesFill && touchesExternal)
				return true;
		}
		
		return false;
	}

	public void updateHeights() {
		
		if(isGenerated())
			throw notAlterableException;

		for(Entry<Vec2, Integer> fill : getClip().getFillSet())
			getClip().addFill(fill.getKey(), Utils.nearestSurfaceY(fill.getKey(), fill.getValue(), getWorld()));
	}
	
	public Location updateHeight(Block block) {
		
		if(isGenerated())
			throw notAlterableException;
		
		Location updatedBlock = Utils.nearestSurface(block.getLocation());
		Vec2 blockVec = new Vec2(block);
		
		getClip().addFill(blockVec, updatedBlock.getBlockY());
			
		return updatedBlock;
	}
	
	public void buildMaze(PathGenerator pathGenerator, BlockGenerator blockGenerator, Plugin plugin) {
		
		if(!hasClip())
			throw new NullPointerException("No clip has been set for this maze.");
		
		if(isGenerated())
			throw new IllegalStateException("The maze is already generated.");

		if(isBeingGenerated())
			throw new IllegalStateException("The maze is already being generated.");

		if(getExits().isEmpty())
			throw new IllegalStateException("No exit(s) defined for this maze.");
		
		if(getWallMaterials() == null)
			throw new IllegalStateException("No materials defined to build this maze.");
		
		isBeingGenerated = true;

		new BukkitRunnable() {
			
			@Override
			public void run() {
				
				BuildMap buildMap = new BuildMap(Maze.this);
				
				pathGenerator.generateMazePaths(buildMap);
				blockGenerator.generateMazeBlocks(buildMap, plugin, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent event) {
						
						Utils.updateBlocksContinuously(buildMap.getGeneratedBlocks(), plugin, new ActionListener() {
							
							@Override
							public void actionPerformed(ActionEvent e) {
								isBeingGenerated = false;
								isGenerated = true;
								undoBlocks.addAll(buildMap.getUndoBlocks());
							}
						});
					}
				});
			}
		}.runTaskAsynchronously(plugin);
	}
	
	public void unbuild(Plugin plugin) {
		
		if(!isGenerated())
			throw new IllegalStateException("The maze has not been generated yet.");
		
		if(isBeingGenerated())
			throw new IllegalStateException("The maze is still being generated.");
		
		Utils.updateBlocksContinuously(getPreviousBlocks(), plugin, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				
				isGenerated = false;
				undoBlocks.clear();
				updateHeights();
			}
		});
	}
}
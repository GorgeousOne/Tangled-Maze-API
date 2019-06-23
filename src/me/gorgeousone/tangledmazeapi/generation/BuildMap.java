package me.gorgeousone.tangledmazeapi.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.block.BlockState;

import me.gorgeousone.tangledmazeapi.clip.Clip;
import me.gorgeousone.tangledmazeapi.core.Maze;
import me.gorgeousone.tangledmazeapi.util.Vec2;

/**
 * A class used for the generation process of a maze. It stores a MazeFillType,
 * the ground height and the final maze height for each location of the maze.
 * Mostly the PathGenerator accesses it to map path end exit segments.
 * The BlockGenerator later uses it to calculate all blocks that need to be generated.
 */
public class BuildMap {
	
	private Maze maze;
	private MazeFillType[][] shapeMap;
	private int[][] groundHeightMap, mazeHeightMap;
	
	private Vec2 minimum, maximum;
	private Vec2 pathStart;
	
	List<BlockState> previousBlocks = new ArrayList<>();
	List<BlockState> generatedBlocks = new ArrayList<>();
	
	public BuildMap(Maze maze) {
		
		this.maze = maze;
		
		calculateMapSize();
		copyMazeOntoMap();
	}
	
	public Maze getMaze() {
		return maze;
	}
	
	public int getMinX() {
		return minimum.getX();
	}

	public int getMinZ() {
		return minimum.getZ();
	}
	
	public int getMaxX() {
		return maximum.getX();
	}

	public int getMaxZ() {
		return maximum.getZ();
	}
	
	public boolean contains(Vec2 point) {
		return
			point.getX() >= getMinX() && point.getX() < getMaxX() &&
			point.getZ() >= getMinZ() && point.getZ() < getMaxZ();
	}
	
	public MazeFillType getType(int x, int z) {
		return shapeMap[x-getMinX()][z-getMinZ()];
	}
	
	public MazeFillType getType(Vec2 point) {
		return getType(point.getX(), point.getZ());
	}

	public void setType(int x, int z, MazeFillType type) {
		shapeMap[x-getMinX()][z-getMinZ()] = type;
	}

	public void setType(Vec2 point, MazeFillType type) {
		setType(point.getX(), point.getZ(), type);
	}
	
	public int getGroundHeight(int x, int z) {
		return groundHeightMap[x-getMinX()][z-getMinZ()];
	}
	
	public int getGroundHeight(Vec2 point) {
		return getGroundHeight(point.getX(), point.getZ());
	}
	
	public void setGroundHeight(int x, int z, int newY) {
		groundHeightMap[x-getMinX()][z-getMinZ()] = newY;
	}
	
	public void setGroundHeight(Vec2 point, int newY) {
		setGroundHeight(point.getX(), point.getZ(), newY);
	}
	
	public int getMazeHeight(int x, int z) {
		return mazeHeightMap[x-getMinX()][z-getMinZ()];
	}
	
	public int getMazeHeight(Vec2 point) {
		return getMazeHeight(point.getX(), point.getZ());
	}
	
	public int getWallHeight(Vec2 point) {
		return getMazeHeight(point) - getGroundHeight(point);
	}
	
	public void setMazeHeight(int x, int z, int newY) {
		mazeHeightMap[x-getMinX()][z-getMinZ()] = newY;
	}

	public void setMazeHeight(Vec2 point, int newY) {
		setMazeHeight(point.getX(), point.getZ(), newY);
	}
	
	public Vec2 getStart() {
		return pathStart;
	}
	
	public void setStart(Vec2 pathStart) {
		this.pathStart = pathStart;
	}
	
	public List<BlockState> getUndoBlocks() {
		return previousBlocks;
	}

	public void setPreviousBlocks(List<BlockState> preciousBlocks) {
		this.previousBlocks = preciousBlocks;
	}

	public List<BlockState> getGeneratedBlocks() {
		return generatedBlocks;
	}

	public void setGeneratedBlocks(List<BlockState> generatedBlocks) {
		this.generatedBlocks = generatedBlocks;
	}

	public void mapSegment(PathSegment segment, MazeFillType type) {
		
		for(Vec2 point : segment.getFill()) {
			
			if(contains(point))
				setType(point.getX(), point.getZ(), type);
		}
	}
	
	/**
	 * A methods that changes all stored MazeFillTypes to ones the BlockGenerator works with. <br>
	 * (MazeFillType.UNDEFINED becomes WALL, EXIT becomes PATH)
	 */
	public void flip() {
		
		for(int x = getMinX(); x < getMaxX(); x++) {
			for(int z = getMinZ(); z < getMaxZ(); z++) {
				
				MazeFillType fillType = getType(x, z);
				
				if(fillType == MazeFillType.UNDEFINED)
					setType(x, z, MazeFillType.WALL);
					
				else if(fillType == MazeFillType.EXIT)
					setType(x, z, MazeFillType.PATH);
			}
		}
	}
	
	private void calculateMapSize() {
		
		minimum = getMinLoc();
		maximum = getMaxLoc();
		
		shapeMap = new MazeFillType
			[maximum.getX() - minimum.getX()]
			[maximum.getZ() - minimum.getZ()];
		
		groundHeightMap = new int
			[maximum.getX() - minimum.getX()]
			[maximum.getZ() - minimum.getZ()];
		
		mazeHeightMap = new int
			[maximum.getX() - minimum.getX()]
			[maximum.getZ() - minimum.getZ()];
	}
	
	private void copyMazeOntoMap() {
		
		for(int x = getMinX(); x < getMaxX(); x++) {
			for(int z = getMinZ(); z < getMaxZ(); z++) {
				setType(x, z, MazeFillType.NOT_MAZE);
			}
		}
		
		int wallHeight = maze.getWallHeight();
		Clip clip = maze.getClip();
		
		//mark the maze's area in mazeMap as undefined area (open for paths and walls)
		for(Entry<Vec2, Integer> loc : clip.getFillSet()) {
			
			setType(loc.getKey(), MazeFillType.UNDEFINED);
			setGroundHeight(loc.getKey(), loc.getValue());
			setMazeHeight(loc.getKey(), loc.getValue() + wallHeight);
		}
		
		//mark the border in mazeMap as walls
		for(Vec2 loc : maze.getClip().getBorder())
			setType(loc, MazeFillType.WALL);
	}
	
	private Vec2 getMinLoc() {
		
		Vec2 minimum = null;

		for(Vec2 loc : maze.getClip().getFill()) {
			
			if(minimum == null) {
				minimum = loc.clone();
				continue;
			}
			
			if(loc.getX() < minimum.getX())
				minimum.setX(loc.getX());
				
			if(loc.getZ() < minimum.getZ())
				minimum.setZ(loc.getZ());
		}
		
		return minimum;
	}
	
	private Vec2 getMaxLoc() {
		
		Vec2 maximum = null;
		
		for(Vec2 chunk : maze.getClip().getFill()) {
			
			if(maximum == null) {
				maximum = chunk.clone();
				continue;
			}
			
			if(chunk.getX() > maximum.getX())
				maximum.setX(chunk.getX());
				
			if(chunk.getZ() > maximum.getZ())
				maximum.setZ(chunk.getZ());
		}
		
		return maximum.add(1, 1);
	}
}
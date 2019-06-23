package me.gorgeousone.tangledmazeapi.clip.shape;

import java.util.List;

import org.bukkit.Location;

import me.gorgeousone.tangledmazeapi.clip.Clip;
import me.gorgeousone.tangledmazeapi.util.Utils;
import me.gorgeousone.tangledmazeapi.util.Vec2;

/**
 * A class with a method to create a clip in form of a rectangle.
 */

public final class Rectangle {
	
	private Rectangle() {}
	
	public static Clip createClip(Location vertex0, Location vertex2) {
		
		List<Location> vertices = Utils.createRectangularVertices(vertex0, vertex2);
		
		Vec2 minVertex = new Vec2(vertices.get(0));
		Vec2 maxVertex = new Vec2(vertices.get(2)).add(1, 1);
		Clip clip = new Clip(vertex0.getWorld());
		
		int maxY = Utils.getMaxHeight(vertices);
		
		for(int x = minVertex.getX(); x < maxVertex.getX(); x++) {
			for(int z = minVertex.getZ(); z < maxVertex.getZ(); z++) {
				
				Vec2 loc = new Vec2(x, z);
				int height = Utils.nearestSurfaceY(loc, maxY, clip.getWorld());
				
				clip.addFill(loc, height);

				if(isBorder(x, z, minVertex, maxVertex))
					clip.addBorder(loc);
			}
		}
		
		return clip;
	}
	
	private static boolean isBorder(int x, int z, Vec2 minVertex, Vec2 maxVertex) {
		return
			x == minVertex.getX() || x == maxVertex.getX() - 1 ||
			z == minVertex.getZ() || z == maxVertex.getZ() - 1;
	}
}
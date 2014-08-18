package com.graphhopper.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.shapes.BBox;

public class BaseGraphUI {

protected Graph graph;

protected  NodeAccess na;
protected GraphicsWrapper mg;
Random rand;
	    
	 public BaseGraphUI(Graph graph){
		 this.graph=graph;
		 this.na = graph.getNodeAccess();
		  mg = new GraphicsWrapper(graph);
	 }
	 
	 public void phase1(GHBitSet bitset,BBox b, Graphics2D g2,boolean fastPaint){
		 int locs=graph.getNodes();
		 EdgeExplorer explorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES);
         for (int nodeIndex = 0; nodeIndex < locs; nodeIndex++)
         {
             if (fastPaint && rand.nextInt(30) > 1)
                 continue;
             double lat = na.getLatitude(nodeIndex);
             double lon = na.getLongitude(nodeIndex);

             // mg.plotText(g2, lat, lon, "" + nodeIndex);
             if (lat < b.minLat || lat > b.maxLat || lon < b.minLon || lon > b.maxLon)
                 continue;

             EdgeIterator iter = explorer.setBaseNode(nodeIndex);
             while (iter.next())
             {
                 int nodeId = iter.getAdjNode();
                 int sum = nodeIndex + nodeId;
                 if (fastPaint)
                 {
                     if (bitset.contains(sum))
                         continue;

                     bitset.add(sum);
                 }
                 double lat2 = na.getLatitude(nodeId);
                 double lon2 = na.getLongitude(nodeId);

                 mg.plotText(g2, lat2,lon2,""+nodeId);
//                  mg.plotText(g2, lat * 0.9 + lat2 * 0.1, lon * 0.9 + lon2 * 0.1, iter.getName());
                 //mg.plotText(g2, lat * 0.9 + lat2 * 0.1, lon * 0.9 + lon2 * 0.1, "s:" + (int) encoder.getSpeed(iter.getFlags()));
                 //g2.setColor(Color.BLACK);                        
                 mg.plotEdge(g2, lat, lon, lat2, lon2);
                 g2.setColor(Color.BLACK);
             }
         }
	 }
	 
	 public void phase2(BBox b, Graphics2D g2){
		 int locs=graph.getNodes();
		 EdgeExplorer explorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES);
         for (int nodeIndex = 0; nodeIndex < locs; nodeIndex++)
         {
             double lat = na.getLatitude(nodeIndex);
             double lon = na.getLongitude(nodeIndex);

             // mg.plotText(g2, lat, lon, "" + nodeIndex);
             if (lat < b.minLat || lat > b.maxLat || lon < b.minLon || lon > b.maxLon)
                 continue;

             EdgeIterator iter = explorer.setBaseNode(nodeIndex);
             while (iter.next())
             {
                 int nodeId = iter.getAdjNode();
                 int sum = nodeIndex + nodeId;
                 double lat2 = na.getLatitude(nodeId);
                 double lon2 = na.getLongitude(nodeId);

                 mg.plotText(g2, lat2,lon2,""+nodeId);
             }
         }
	 }
	    
	 public void paintGraph(GHBitSet bitset,BBox b, Graphics2D g2,boolean fastPaint){
		 
		 int locs=graph.getNodes();
		 if (fastPaint)
         {
             rand.setSeed(0);
             bitset.clear();
         }
		 phase1(bitset, b, g2, fastPaint);
		 if (!fastPaint)
			 phase2( b, g2);
		 
    	 
    }
}

package com.graphhopper.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Observer;
import java.util.Random;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.LevelGraph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.shapes.BBox;

public class BaseGraphUI {

protected GraphStorage graph;

protected  NodeAccess na;
protected ZoomLevelGraphicsWrapper mg;
Random rand;
HeadlessTilesLayer mapsforgeLayer;

	 public BaseGraphUI(GraphStorage graph){
		 this.graph=graph;
		 this.na = graph.getNodeAccess();
		 
		 BBox graphBounds=graph.getBounds();
         
       
		 
		  mg = new ZoomLevelGraphicsWrapper(graph);
		 // mg.center((graphBounds.maxLat+graphBounds.minLat)/2, (graphBounds.maxLon+graphBounds.minLon)/2);
	        
		  rand = new Random();
		  //mapsforgeLayer=new MapsforgeLayer("/home/bc/Downloads/czech_republic.map");
		  mapsforgeLayer=new HeadlessTilesLayer("http://otile1.mqcdn.com/tiles/1.0.0/map", "/tmp/tiles");
		  
	 }
	 
//	 public void addObserver(Observer observer){
//		 mapsforgeLayer.addObserver(observer);
//	 }
	 
	 public void resize(int width,int height){
		// mapsforgeLayer.setViewportSize(width, height);
		 mg.setViewPort(width, height);
	 }
	 
	 
	 boolean rejectCoords( BBox b,double lat ,double lon,boolean fastPaint){
		 if (fastPaint){
			 return (lat < b.minLat || lat > b.maxLat || lon < b.minLon || lon > b.maxLon);
		 }
		 double deltaLat=b.maxLat-b.minLat;
		 double deltaLon=b.maxLon-b.minLon;
		 return (lat < b.minLat-deltaLat || lat > b.maxLat+deltaLat || lon < b.minLon-deltaLon || lon > b.maxLon+deltaLon);
	 }
	 public void phase0( Graphics2D g2,BBox b) throws InterruptedException{
		 double centerLat=(b.minLat+b.maxLat)/2;
		 double centerLon=(b.minLon+b.maxLon)/2;
		 mapsforgeLayer.paint(g2,mg,2000);
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
            if (rejectCoords(b, lat, lon,fastPaint))
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
	 public String getEdgeLabel( EdgeIterator iter ){
		 return ""+iter.getEdge()+" "+graph.getEncodingManager().getSingle().getSpeed(iter.getFlags())+"km/h";
		 
	 }
	 
	 public void phase2(BBox b, Graphics2D g2){
		 int locs=graph.getNodes();
		 EdgeExplorer explorer = graph.createEdgeExplorer(EdgeFilter.ALL_EDGES);
		 
		 LevelGraph levelGraph=null;
		 if(graph instanceof LevelGraph)
			 levelGraph=(LevelGraph)graph;
		 
         for (int nodeIndex = 0; nodeIndex < locs; nodeIndex++)
         {
             double lat = na.getLatitude(nodeIndex);
             double lon = na.getLongitude(nodeIndex);

             // mg.plotText(g2, lat, lon, "" + nodeIndex);
             if (lat < b.minLat || lat > b.maxLat || lon < b.minLon || lon > b.maxLon)
                 continue;

             EdgeIterator iter = explorer.setBaseNode(nodeIndex);
             while(iter.next()) { // if (iter.getAdjNode()>nodeIndex){
            	 double adjLat=na.getLatitude(iter.getAdjNode());
            	 double adjLon=na.getLongitude(iter.getAdjNode());
            	 mg.plotText(g2, (lat+adjLat)/2,(lon+adjLon)/2,getEdgeLabel(iter),Color.BLACK,Color.LIGHT_GRAY,true);
             }
      
//             if (levelGraph!=null)
//             int level=levelGraph.getLevel(nodeIndex);
                 mg.plotText(g2, lat,lon,""+nodeIndex,Color.RED,Color.LIGHT_GRAY,true);
         }
	 }
	    
	 public void paintGraph(GHBitSet bitset,BBox b, Graphics2D g2,boolean fastPaint) {
		 try {
			phase0(g2,b);
       
			if (fastPaint) {
	             rand.setSeed(0);
	             bitset.clear();
	        }
			phase1(bitset, b, g2, fastPaint);
			if (!fastPaint)
			 phase2( b, g2);
			 
		 } catch (InterruptedException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
	        }
    }
	 public void moveTo(double centerLat,double centerLon){
		 moveTo(centerLat, centerLon,mg.getZoomLevel());
	 }
	 public void moveTo(double centerLat,double centerLon,int zoomLevel){
		 mg.setCenterCoords(centerLat, centerLon);
		 mg.zoom(zoomLevel);
	 }
	 
}

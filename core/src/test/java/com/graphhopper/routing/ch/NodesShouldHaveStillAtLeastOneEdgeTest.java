
package com.graphhopper.routing.ch;

import org.junit.Assert;
import org.junit.Test;

import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.ShortestWeighting;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.GraphBuilder;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.LevelGraphStorage;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

public class NodesShouldHaveStillAtLeastOneEdgeTest {

	
	
	/*
	 *
	 *    4----3      0
	 *    |    |      | 
	 *    |    ^      |
	 *    |    |      |
	 *    5----2------1
	 *                |
	 *                |
	 *                6   
	 */
   protected GraphStorage createNamestiMiruGraph(GraphStorage g)
   {
   	
   	FlagEncoder encoder=new CarFlagEncoder(5, 5, 1);
   	
       

       g.edge(0, 1,2,true);
       g.edge(1, 2,2,true);
       g.edge(1, 6,2,true);
       g.edge(2, 3).setFlags(encoder.setProperties(10, true,false));
       g.edge(3, 4,2,true);
       g.edge(4, 5,2,true);
       g.edge(5, 2,2,true);
       
   	
   	
       return g;
   }
   
   void addTurnRestriction(GraphStorage graph,int from,int via,int to){
       TurnCostStorage turnCostStorage=(TurnCostStorage)graph.getExtendedStorage();
   	FlagEncoder encoder=graph.getEncodingManager().getSingle();
       turnCostStorage.addTurnInfo(via, getEdge(graph, from, via).getEdge(), getEdge(graph, via, to).getEdge(),encoder.getTurnFlags(true,0));
    
       
  }
  
   
   EdgeIteratorState getEdge(GraphStorage graph,int baseNode,int adjNode){
   	EdgeIterator edgeIterator=graph.createEdgeExplorer().setBaseNode(baseNode);
   	
   	while (edgeIterator.next())
   		if (edgeIterator.getAdjNode()==adjNode)
   			return edgeIterator;
   	return null;
   	
   }
   
   /**
    * Check that after CH preparation all nodes belong to at least one edge
    */
	@Test
	public void AllNodesHaveStillAtLeastOneEdge(){
		
		CarFlagEncoder encoder=new CarFlagEncoder(5, 5, 1);
		Weighting weighting=new ShortestWeighting();
		TraversalMode tMode=TraversalMode.EDGE_BASED_2DIR;
		
		
		LevelGraphStorage g = (LevelGraphStorage) new GraphBuilder(new EncodingManager(encoder)).set3D(false).levelGraphCreate();
		createNamestiMiruGraph(g);
		
        PrepareContractionHierarchies prepare = new PrepareContractionHierarchies(encoder, weighting, tMode).setGraph(g);
        prepare.doWork();
        	    
        for(int i=0;i<g.getNodes();i++)
        	Assert.assertTrue("node "+i+" has not edges anymore",g.createEdgeExplorer().setBaseNode(i).next());
	}
	
}

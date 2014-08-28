/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

/**
 * Implements a single source shortest path algorithm
 * http://en.wikipedia.org/wiki/Dijkstra's_algorithm
 * <p/>
 * @author Peter Karich
 */
public class Dijkstra extends AbstractRoutingAlgorithm
{
	private static final Logger logger = LoggerFactory.getLogger(Dijkstra.class);
		
	protected static final int NOT_FOUND = -1;
	
    private TIntObjectMap<EdgeEntry> fromMap;
    private PriorityQueue<EdgeEntry> fromHeap;
    private int visitedNodes;
    protected int to = -1;
    protected EdgeEntry currEdge;
    double limitWeight=Double.MAX_VALUE;
	int limitVisitedNodes=-1;

    public Dijkstra( Graph g, FlagEncoder encoder, Weighting weighting, TraversalMode tMode )
    {
        super(g, encoder, weighting, tMode);
        initCollections(1000);
    }

    protected void initCollections( int size )
    {
        fromHeap = new PriorityQueue<EdgeEntry>(size);
        fromMap = new TIntObjectHashMap<EdgeEntry>(size);
    }

    public void setFrom(int from){
    	 currEdge = createEdgeEntry(from, 0);
         if (!traversalMode.isEdgeBased())
         {
             fromMap.put(from, currEdge);
         }
    }
    @Override
    public Path calcPath( int from, int to )
    {
        checkAlreadyRun();
        setFrom(from);
        return runAlgo(to);
    }
    
    protected Path runAlgo(int to)
    {
    	
    	int node=findEndNode(to);
    	if (node==NOT_FOUND)
    		return createEmptyPath();
    	else {
    		if (node!=to)
    			throw new IllegalStateException("seems we were routed not where we asked for to:"+to+" routed node:"+node);
    		return extractPath();
    	}
    }
   
    public int findEndNode(int to) {
	    
    	
    	
    	 this.to = to;

    	 if (currEdge==null)
 			return NOT_FOUND;
     	
   
        EdgeExplorer explorer = outEdgeExplorer;
        while (true)
        {
            visitedNodes++;
            if (logger.isDebugEnabled()) logger.debug("\n\n\n\n"+visitedNodes);

            int startNode = currEdge.adjNode;
            if (logger.isDebugEnabled()) logger.debug("startNode:"+startNode +" currEdge.weight:"+currEdge.weight);
            EdgeIterator iter = explorer.setBaseNode(startNode);
            while (iter.next())
            {
                if (!accept(iter, currEdge.edge))
                    continue;

                int iterationKey = traversalMode.createTraversalId(iter, false);
                double tmpWeight = weighting.calcWeight(iter, false, currEdge.edge) + currEdge.weight;
                if (Double.isInfinite(tmpWeight))
                    continue;
                if (logger.isDebugEnabled()) logger.debug("  adjNode:"+iter.getAdjNode()+" tmpWeight:"+tmpWeight);
                EdgeEntry nEdge = fromMap.get(iterationKey);
                if (nEdge == null)
                {
                    nEdge = new EdgeEntry(iter.getEdge(), iter.getAdjNode(), tmpWeight);
                    nEdge.parent = currEdge;
                    fromMap.put(iterationKey, nEdge);
                    fromHeap.add(nEdge);
                    
                } else if (nEdge.weight > tmpWeight)
                {
                    fromHeap.remove(nEdge);
                    nEdge.edge = iter.getEdge();
                    nEdge.weight = tmpWeight;
                    nEdge.parent = currEdge;
                    fromHeap.add(nEdge);
                } else
                    continue;

                updateBestPath(iter, nEdge, iterationKey);
            }
            visited(currEdge);

            if (finished())
                break;
            
            if (fromHeap.isEmpty() || (limitVisitedNodes!=-1 && visitedNodes >= limitVisitedNodes))
            {
            	currEdge=null;
                return NOT_FOUND;
            }
            
           

            currEdge = fromHeap.poll();
            if (currEdge == null)
                throw new AssertionError("Empty edge cannot happen");
        }
        if (currEdge == null || !finished())
        	return NOT_FOUND;
        return currEdge.adjNode;
    }

    protected void visited(EdgeEntry edgeEntry) {
    	// placeholder
    }

	
	@Override
    protected boolean finished()
    {
        return currEdge.weight>=limitWeight || currEdge.adjNode == to;
    }

    @Override
    protected Path extractPath()
    {
        if (currEdge == null || !finished())
            return createEmptyPath();
        return extractPath(currEdge);
    }
  
    protected Path extractPath(EdgeEntry edgeEntry)
    {
        return new Path(graph, flagEncoder).setWeight(edgeEntry.weight).setEdgeEntry(edgeEntry).extract();
    }

    
    @Override
    public String getName()
    {
        return "dijkstra";
    }

    @Override
    public int getVisitedNodes()
    {
        return visitedNodes;
    }

	public void clear() {
		initCollections(1000);
		limitWeight=Double.MAX_VALUE; // should this be reset?
		to=-1;
		visitedNodes=0;
		limitVisitedNodes=-1;
		currEdge=null;
	    
    }

	public void close() {
		currEdge=null;
		fromHeap=null;
		fromMap=null;
	    
    }

	public Dijkstra setLimitWeight(double limitWeight) {
		this.limitWeight = limitWeight;
		return this;
	}

	public Dijkstra setLimitVisitedNodes(int limitVisitedNodes) {
		this.limitVisitedNodes = limitVisitedNodes;
		return this;
	}
}

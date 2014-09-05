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
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.LevelGraphStorage;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;

/**
 * Calculates best path in bidirectional way.
 * <p/>
 * 'Ref' stands for reference implementation and is using the normal Java-'reference'-way.
 * <p/>
 * @see DijkstraBidirection for an array based but more complicated version
 * @author Peter Karich
 */
public class DijkstraBidirectionRef extends AbstractBidirAlgo
{
	private static final Logger logger = LoggerFactory.getLogger(DijkstraBidirectionRef.class);
	
    private PriorityQueue<EdgeEntry> openSetFrom;
    private PriorityQueue<EdgeEntry> openSetTo;
    private TLongObjectMap<EdgeEntry> bestWeightMapFrom;
    private TLongObjectMap<EdgeEntry> bestWeightMapTo;
    protected TLongObjectMap<EdgeEntry> bestWeightMapOther;
    protected EdgeEntry currFrom;
    protected EdgeEntry currTo;
    protected PathBidirRef bestPath;
    private boolean updateBestPath = true;

    public DijkstraBidirectionRef( Graph graph, FlagEncoder encoder, Weighting weighting, TraversalMode tMode )
    {
        super(graph, encoder, weighting, tMode);
        initCollections(1000);
    }

    protected void initCollections( int nodes )
    {
        openSetFrom = new PriorityQueue<EdgeEntry>(nodes / 10);
        bestWeightMapFrom = new TLongObjectHashMap<EdgeEntry>(nodes / 10);

        openSetTo = new PriorityQueue<EdgeEntry>(nodes / 10);
        bestWeightMapTo = new TLongObjectHashMap<EdgeEntry>(nodes / 10);
    }

    @Override
    public void initFrom( int from, double dist )
    {
        currFrom = createEdgeEntry(from, dist);
        openSetFrom.add(currFrom);
        if (!traversalMode.isEdgeBased())
        {
            bestWeightMapFrom.put(from, currFrom);
            if (currTo != null)
            {
                bestWeightMapOther = bestWeightMapTo;
                updateBestPath(GHUtility.getEdge(graph, from, currTo.adjNode), currTo, from);
            }
        } else
        {
            if (currTo != null && currTo.adjNode == from)
            {
                finishedFrom = true;
                finishedTo = true;
            }
        }
    }

    @Override
    public void initTo( int to, double dist )
    {
        currTo = createEdgeEntry(to, dist);
        openSetTo.add(currTo);
        if (!traversalMode.isEdgeBased())
        {
            bestWeightMapTo.put(to, currTo);
            if (currFrom != null)
            {
                bestWeightMapOther = bestWeightMapFrom;
                updateBestPath(GHUtility.getEdge(graph, currFrom.adjNode, to), currFrom, to);
            }
        } else
        {
            if (currFrom != null && currFrom.adjNode == to)
            {
                finishedFrom = true;
                finishedTo = true;
            }
        }
    }

    @Override
    protected Path createAndInitPath()
    {
        bestPath = new PathBidirRef(graph, flagEncoder);
        return bestPath;
    }

    @Override
    protected Path extractPath()
    {
        return bestPath.extract();
    }

    @Override
    void checkState( int fromBase, int fromAdj, int toBase, int toAdj )
    {
        if (bestWeightMapFrom.isEmpty() || bestWeightMapTo.isEmpty())
            throw new IllegalStateException("Either 'from'-edge or 'to'-edge is inaccessible. From:" + bestWeightMapFrom + ", to:" + bestWeightMapTo);
    }

    @Override
    public boolean fillEdgesFrom()
    {
    	if (logger.isDebugEnabled()) logger.debug("\nfillEdgesFrom");
        if (openSetFrom.isEmpty())
            return false;

        currFrom = openSetFrom.poll();
        bestWeightMapOther = bestWeightMapTo;
        fillEdges(currFrom, openSetFrom, bestWeightMapFrom, outEdgeExplorer, false);
        visitedCountFrom++;
        return true;
    }

    @Override
    public boolean fillEdgesTo()
    {
    	if (logger.isDebugEnabled()) logger.debug("\nfillEdgesTo");
        if (openSetTo.isEmpty())
            return false;
        currTo = openSetTo.poll();
        bestWeightMapOther = bestWeightMapFrom;
        fillEdges(currTo, openSetTo, bestWeightMapTo, inEdgeExplorer, true);
        visitedCountTo++;
        return true;
    }

    // http://www.cs.princeton.edu/courses/archive/spr06/cos423/Handouts/EPP%20shortest%20path%20algorithms.pdf
    // a node from overlap may not be on the best path!
    // => when scanning an arc (v, w) in the forward search and w is scanned in the reverseOrder 
    //    search, update extractPath = μ if df (v) + (v, w) + dr (w) < μ            
    @Override
    public boolean finished()
    {
        if (finishedFrom || finishedTo)
            return true;

        return currFrom.weight + currTo.weight >= bestPath.getWeight();
    }

    
    
    
    int evaluationCnt=0;
    
    void dumpEdgesFrom(int baseNode){
    	
    	LevelGraphStorage levelGraphStorage=null;
    	if (graph instanceof QueryGraph)
    		if (((QueryGraph)graph).mainGraph instanceof LevelGraphStorage)
    			levelGraphStorage=(LevelGraphStorage)((QueryGraph)graph).mainGraph;
    	EdgeExplorer explorer=graph.createEdgeExplorer();
    	
        {
        	if (logger.isDebugEnabled()) logger.debug("===============");
        	if (logger.isDebugEnabled()) logger.debug("  baseNode:"+baseNode);
        	EdgeIterator iter = explorer.setBaseNode(baseNode);
        	while (iter.next()){
        		if (logger.isDebugEnabled()) logger.debug("  adjNode:"+iter.getAdjNode());
        		if (levelGraphStorage!=null)
        			if (logger.isDebugEnabled()) logger.debug(" chNavigable:"+levelGraphStorage.getChNavigable(iter.getEdge(),iter.getBaseNode(), iter.getAdjNode()));
        	}
        	if (logger.isDebugEnabled()) logger.debug("===============");
        }
    }
    
    void fillEdges( EdgeEntry currEdge, PriorityQueue<EdgeEntry> prioQueue,
            TLongObjectMap<EdgeEntry> shortestWeightMap, EdgeExplorer explorer, boolean reverse )
    {
    	if (logger.isDebugEnabled()) logger.debug("\n\n\n\nevaluation:"+evaluationCnt);
    	currEdge.evaluatedAt=evaluationCnt;
    	
    	int currBaseNode=currEdge.parent!=null?currEdge.parent.adjNode:-1;
        int currNode = currEdge.adjNode;
        if (logger.isDebugEnabled()) logger.debug("currEdge"+" edgeId:"+currEdge.edge+" "+(currEdge.parent!=null?currEdge.parent.adjNode:"?")+" --> "+currEdge.adjNode+" reverse:"+reverse);
        
      
        LevelGraphStorage levelGraphStorage=null;
    	if (graph instanceof QueryGraph)
    		if (((QueryGraph)graph).mainGraph instanceof LevelGraphStorage)
    			levelGraphStorage=(LevelGraphStorage)((QueryGraph)graph).mainGraph;
    	if (levelGraphStorage!=null && currBaseNode!=-1)
  			if (logger.isDebugEnabled()) logger.debug(" chNavigable:"+levelGraphStorage.getChNavigable(currEdge.edge,currBaseNode, currEdge.adjNode));
    
        dumpEdgesFrom(currNode);
        
        
        EdgeIterator iter = explorer.setBaseNode(currNode);
        
    	    
        
//        int originalEnteringEdgeId=PrepareContractionHierarchies.getOriginal(graph,currEdge.edge, currEdge.adjNode, false);
//        if (logger.isDebugEnabled()) logger.debug("originalEnteringEdgeId:"+originalEnteringEdgeId);
        while (iter.next())
        {
        	if (logger.isDebugEnabled()) logger.debug("  adjNode:"+iter.getAdjNode());
            if (!accept(iter, currEdge.edge)){
            	if (logger.isDebugEnabled()) logger.debug("  rejected");
            	continue;
            }
//            int originalExitingEdgeId=PrepareContractionHierarchies.getOriginalEdgeIdClosestToAdjNode(iter);
            
//            // this should be part of the filter
//            if (logger.isDebugEnabled()) logger.debug("originalExitingEdgeId:"+originalExitingEdgeId);
//            if (!traversalMode.hasUTurnSupport() && originalEnteringEdgeId!=EdgeIterator.NO_EDGE && originalEnteringEdgeId==originalExitingEdgeId){
//            	if (logger.isDebugEnabled()) logger.debug("  u turn");
//            	continue;
//            }
            
            
            long iterationKey = traversalMode.createTraversalId(graph,iter, reverse);
            if (logger.isDebugEnabled()) logger.debug("iterationKey:"+iterationKey+" is for edgeId:"+iter.getEdge()+" "+iter.getBaseNode()+" --> "+iter.getAdjNode()+" reverse:"+reverse);
            
            
            double tmpWeight = weighting.calcWeight(iter, reverse, currEdge.edge) + currEdge.weight;
            if (logger.isDebugEnabled()) logger.debug("  tmpWeight:"+tmpWeight);
            if (Double.isInfinite(tmpWeight))
                continue;

            EdgeEntry ee=enQueue(currEdge, prioQueue, shortestWeightMap, iter, iterationKey, tmpWeight); 
            

            if (ee!=null && updateBestPath)
                updateBestPath(iter, ee, iterationKey,tmpWeight);
        }
//        logger.info("evaluation:"+evaluationCnt);
        evaluationCnt++;
    }

	private EdgeEntry enQueue(EdgeEntry currEdge, PriorityQueue<EdgeEntry> prioQueue, TLongObjectMap<EdgeEntry> shortestWeightMap, EdgeIterator iter, long iterationKey, double tmpWeight) {
		
		boolean add=true;
//		  LevelGraphStorage levelGraphStorage=null;
//	    	if (graph instanceof QueryGraph)
//	    		if (((QueryGraph)graph).mainGraph instanceof LevelGraphStorage) {
//	    			levelGraphStorage=(LevelGraphStorage)((QueryGraph)graph).mainGraph;
//					// now accept a node only if its parent node was CH navigable
//	    			try {
//					int prevBaseNode=graph.getEdgeProps(currEdge.edge, iter.getBaseNode()).getBaseNode();
//	    			add=levelGraphStorage.getChNavigable(currEdge.edge, prevBaseNode,iter.getBaseNode());
//	    			} catch (Exception e) {
//	    				// ignore edge is virtual
//	    			}
//	    			
//	    		}
		if (graph.getEdgeProps(iter.getEdge(),iter.getAdjNode())==null){
    		throw new RuntimeException("yaya!");
    	}
    	
		
	    EdgeEntry ee = shortestWeightMap.get(iterationKey);
	    if (ee == null)
	    {
	        ee = new EdgeEntry(iter.getEdge(), iter.getAdjNode(), tmpWeight);
	        ee.spawnAt=evaluationCnt;
	        ee.parent = currEdge;
	        shortestWeightMap.put(iterationKey, ee);
	        if(add)
	        	prioQueue.add(ee);
	        return ee;
	    } else if (ee.weight > tmpWeight)
	    {
	        prioQueue.remove(ee);
//	        ee.edge = iter.getEdge();
//	        ee.weight = tmpWeight;
//	        ee.parent = currEdge;
	        ee = new EdgeEntry(iter.getEdge(), iter.getAdjNode(), tmpWeight);
	        ee.spawnAt=evaluationCnt;
	        ee.parent = currEdge;
	        shortestWeightMap.put(iterationKey, ee);
	        if (add)
	        	prioQueue.add(ee);
	        return ee;
	    }
	    
	    return null;
    }
	 @Override
    protected void updateBestPath( EdgeIteratorState edgeState, EdgeEntry entryCurrent, long iterationKey){
		 throw new RuntimeException("dont call me!");
	 }
    
    
    
    protected void updateBestPath( EdgeIteratorState edgeState, EdgeEntry entryCurrent, long iterationKey,double tmpWeight )
    {
    	if (graph.getEdgeProps(entryCurrent.edge, entryCurrent.adjNode)==null){
    		throw new RuntimeException("yaya!"+entryCurrent);
    	}
    	
    	EdgeEntry entryCurrentOriginal=entryCurrent;
        EdgeEntry entryOther = bestWeightMapOther.get(iterationKey);
        
        
        
        EdgeEntry entryOtherOriginal=entryOther;
        if (entryOther == null)
            return;
        if (logger.isDebugEnabled())
        	logger.debug("found match on iterationKey:"+iterationKey);
        
        boolean reverse = bestWeightMapFrom == bestWeightMapOther;

        // update μ
        double newWeight = entryCurrent.weight + entryOther.weight;
        if (traversalMode.isEdgeBased())
        {
            if (entryOther.edge != entryCurrent.edge)
               logger.warn("cannot happen for edge based execution of " + getName());

//            if (entryOther.adjNode == entryCurrent.adjNode)
//            {
                // prevents the path to contain the edge at the meeting point twice and subtract the weight (excluding turn weight => no previous edge)
//            	if (entryOther.parent.adjNode==entryCurrent.adjNode)
//            		{entryOther = entryOther.parent;
            	
            	// TODO
            	logger.warn("remove weight!");
                newWeight -= tmpWeight;
//            		} else 
//            		{
//            			entryCurrent=entryCurrent.parent;
//            			newWeight -= weighting.calcWeight(edgeState, reverse, EdgeIterator.NO_EDGE);
//            		}
//                
                
//            } else
//            {
//                // we detected a u-turn at meeting point, skip if not supported
//                if (!traversalMode.hasUTurnSupport())
//                    return;
//            }
        }

//        logger.info("newWeight:"+newWeight);
        if (newWeight < bestPath.getWeight())
        {
//        	 if (entryCurrent.adjNode != entryOther.adjNode){
//        		 entryOther=entryOtherOriginal;
//        		 entryCurrent=entryCurrentOriginal;
//        		 
//                 throw new IllegalStateException("Locations of the 'to'- and 'from'-Edge has to be the same." + toString() + ", fromEntry:" + entryCurrent + ", toEntry:" + entryOther);
//        	 }

            bestPath.setSwitchToFrom(reverse);
            bestPath.setEdgeEntry(entryCurrent);
            bestPath.setWeight(newWeight);
            bestPath.setEdgeEntryTo(entryOther);
        }
    }
    
    

    @Override
    public String getName()
    {
        return "dijkstrabi";
    }

    TLongObjectMap<EdgeEntry> getBestFromMap()
    {
        return bestWeightMapFrom;
    }

    TLongObjectMap<EdgeEntry> getBestToMap()
    {
        return bestWeightMapTo;
    }

    void setBestOtherMap( TLongObjectMap<EdgeEntry> other )
    {
        bestWeightMapOther = other;
    }

    void setFromDataStructures( DijkstraBidirectionRef dijkstra )
    {
        openSetFrom = dijkstra.openSetFrom;
        bestWeightMapFrom = dijkstra.bestWeightMapFrom;
        finishedFrom = dijkstra.finishedFrom;
        currFrom = dijkstra.currFrom;
        visitedCountFrom = dijkstra.visitedCountFrom;
        // outEdgeExplorer
    }

    void setToDataStructures( DijkstraBidirectionRef dijkstra )
    {
        openSetTo = dijkstra.openSetTo;
        bestWeightMapTo = dijkstra.bestWeightMapTo;
        finishedTo = dijkstra.finishedTo;
        currTo = dijkstra.currTo;
        visitedCountTo = dijkstra.visitedCountTo;
        // inEdgeExplorer
    }

    void setUpdateBestPath( boolean b )
    {
        updateBestPath = b;
    }

    void setBestPath( PathBidirRef bestPath )
    {
        this.bestPath = bestPath;
    }
}

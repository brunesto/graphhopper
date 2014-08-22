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

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;

/**
 * A one to many implemented by extending Dijkstra. It is probably slower than DijkstraOneToMany
 * @author Bruno Carle
 *
 */
public class DijkstraOneToManyRef extends Dijkstra {

	TIntObjectMap<EdgeEntry> visited=new TIntObjectHashMap<EdgeEntry>();
	public DijkstraOneToManyRef(Graph g, FlagEncoder encoder, Weighting weighting, TraversalMode tMode) {
	    super(g, encoder, weighting, tMode);
    }
	
	@Override
    public Path calcPath( int from, int to )
    {
	 	int node=findEndNode(from,to);
	 	return path2node(node);
    }
	
	
    protected Path path2node(int node)
    {
    	if (node==NOT_FOUND)
    		return createEmptyPath();
    	EdgeEntry edgeEntry=visited.get(node);
		if (edgeEntry==null)
			return createEmptyPath();
		return extractPath(edgeEntry);
    }
	
	@Override
	protected void visited(EdgeEntry edgeEntry) {
		
    	if (!visited.containsKey(edgeEntry.adjNode)){
//    		System.err.println("visited:"+edgeEntry);
    		visited.put(edgeEntry.adjNode, edgeEntry);
    	} else {
    		//ignore
    	}
    	
    }
	int from=-1;
	
	
	 public int findEndNode(int from, int to) {
		 if (this.from==-1){
				setFrom(from);
		 }
		 else if (this.from!=from)
	    	throw new IllegalArgumentException(" this.from "+this.from+" from:"+from);

		
		EdgeEntry previouslyFound=visited.get(to);
		if (previouslyFound!=null)
			return to;
		else {
			
			return super.findEndNode(to);
		}
	}

	@Override
    public void clear() {
		super.clear();
	    from=-1;
	    visited.clear();
	    
    }
	
	@Override
    public void close(){
		super.close();
		visited=null;
	}

	public double getWeight(int endNode) {
		EdgeEntry edgeEntry=visited.get(endNode);
		if (edgeEntry==null)
			return Double.MAX_VALUE;
		else return edgeEntry.weight;
    }
	

	@Override
    public DijkstraOneToManyRef setLimitWeight(double limitWeight) {
		super.setLimitWeight(limitWeight);
		return this;
	}
		
	@Override
	public DijkstraOneToManyRef setLimitVisitedNodes(int i) {
	    super.setLimitVisitedNodes(i);
		return this;
    }


	

	public String getMemoryUsageAsString() {
	    return "TODO";
    }

}

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
package com.graphhopper.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.PathBidirRef;
import com.graphhopper.routing.ch.PrepareEncoder;
import com.graphhopper.routing.util.AllEdgesSkipIterator;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeSkipExplorer;
import com.graphhopper.util.EdgeSkipIterator;
import com.graphhopper.util.EdgeSkipIterState;

/**
 * A Graph necessary for shortcut algorithms like Contraction Hierarchies. This class enables the
 * storage to hold the level of a node and a shortcut edge per edge.
 * <p/>
 * @see GraphBuilder
 * @author Peter Karich
 */
public class LevelGraphStorage extends GraphHopperStorage implements LevelGraph
{
	private final static Logger logger = LoggerFactory.getLogger(LevelGraphStorage.class);
	
    private static final double WEIGHT_FACTOR = 1000f;
    // 2 bits for access, for now only 32bit => not Long.MAX
    private static final long MAX_WEIGHT_LONG = (Integer.MAX_VALUE >> 2) << 2;
    private static final double MAX_WEIGHT = (Integer.MAX_VALUE >> 2) / WEIGHT_FACTOR;
    private int I_SKIP_EDGE1a;
    private int I_SKIP_EDGE2a;
    private int I_LEVEL;
    private int I_LOWER_NODE_ORIGINAL_EDGE; // will store the original edgeId which is closest to the node with lower id
    private int I_HIGHER_NODE_ORIGINAL_EDGE;  // will store the original edgeId which is closest to the node with higher id
    private int I_CH_NAVIGABLE_FORWARD;
    private int I_CH_NAVIGABLE_BACKWARD;
    
    // after the last edge only shortcuts are stored
    public int lastEdgeIndex = -1;
    private final long scDirMask = PrepareEncoder.getScDirMask();

    public LevelGraphStorage( Directory dir, EncodingManager encodingManager, boolean enabled3D )
    {
        this(dir, encodingManager, enabled3D,new ExtendedStorage.NoExtendedStorage()); 
    }
    public LevelGraphStorage( Directory dir, EncodingManager encodingManager, boolean enabled3D,ExtendedStorage extendedStorage )
    {
        super(dir, encodingManager, enabled3D,extendedStorage);
    }
    

    @Override
    protected void initStorage()
    {
        super.initStorage();
        I_SKIP_EDGE1a = nextEdgeEntryIndex(4);
        I_SKIP_EDGE2a = nextEdgeEntryIndex(4);
        I_LOWER_NODE_ORIGINAL_EDGE = nextEdgeEntryIndex(4);
        I_HIGHER_NODE_ORIGINAL_EDGE = nextEdgeEntryIndex(4);
        I_LEVEL = nextNodeEntryIndex(4);
        I_CH_NAVIGABLE_FORWARD= nextEdgeEntryIndex(4); // TODO use 1 byte for both
        I_CH_NAVIGABLE_BACKWARD= nextEdgeEntryIndex(4); // TODO use byte
        initNodeAndEdgeEntrySize();
    }

    @Override
    public final void setLevel( int index, int level )
    {
        ensureNodeIndex(index);
        nodes.setInt((long) index * nodeEntryBytes + I_LEVEL, level);
    }

    @Override
    public final int getLevel( int index )
    {
        ensureNodeIndex(index);
        return nodes.getInt((long) index * nodeEntryBytes + I_LEVEL);
    }

    
    public void setChNavigable( int index, int fromNode,int toNode,boolean navigable )
    {
        boolean forward=fromNode<toNode;
        
        edges.setInt((long) index * edgeEntryBytes + (forward?I_CH_NAVIGABLE_FORWARD:I_CH_NAVIGABLE_BACKWARD),navigable?1:0);
        if (logger.isDebugEnabled()) logger.debug("setChNavigable("+index+" fromNode:"+fromNode+" toNode:"+toNode+" navigable:"+navigable+")");
    }


    public final boolean getChNavigable( int index, int fromNode,int toNode )
    {
        if (index>=edgeCount)
        	return true;
    	boolean forward=fromNode<toNode;
    	
        boolean retVal=0!=edges.getInt((long) index * edgeEntryBytes + (forward?I_CH_NAVIGABLE_FORWARD:I_CH_NAVIGABLE_BACKWARD));
        if (logger.isDebugEnabled()) logger.debug("getChNavigableFromFlags("+index+" fromNode:"+fromNode+" toNode:"+toNode+") returns "+retVal+"");
        return retVal;
    }
    
    
    
    @Override
    public EdgeSkipIterState shortcut( int a, int b )
    {
        return createEdge(a, b);
    }

    @Override
    public EdgeSkipIterState edge( int a, int b )
    {
        if (lastEdgeIndex + 1 < edgeCount)
            throw new IllegalStateException("Cannot create after shortcut was created");

        lastEdgeIndex = edgeCount;
        return createEdge(a, b);
    }

    private EdgeSkipIterState createEdge( int a, int b )
    {
        ensureNodeIndex(Math.max(a, b));
        int edgeId = internalEdgeAdd(a, b);
        EdgeSkipIteratorImpl iter = new EdgeSkipIteratorImpl(EdgeFilter.ALL_EDGES);
        iter.setBaseNode(a);
        iter.setEdgeId(edgeId);
        iter.next();
        iter.setSkippedEdges(EdgeIterator.NO_EDGE, EdgeIterator.NO_EDGE);
        iter.setOriginalEdges(EdgeIterator.NO_EDGE, EdgeIterator.NO_EDGE);
        setChNavigable(edgeId, a, b, true);
//        setChNavigable(edgeId, b, a, true);
        return iter;
    }

    @Override
    public EdgeSkipExplorer createEdgeExplorer()
    {
        return createEdgeExplorer(EdgeFilter.ALL_EDGES);
    }

    @Override
    public EdgeSkipExplorer createEdgeExplorer( EdgeFilter filter )
    {
        return new EdgeSkipIteratorImpl(filter);
    }

    @Override
    public LevelGraphStorage create( long nodeCount )
    {
        super.create(nodeCount);
        return this;
    }

    @Override
    public final EdgeSkipIterState getEdgeProps( int edgeId, int endNode )
    {
        return (EdgeSkipIterState) super.getEdgeProps(edgeId, endNode);
    }

    public class EdgeSkipIteratorImpl extends EdgeIterable implements EdgeSkipExplorer, EdgeSkipIterator
    {
    	
//    	@Override
//		  public boolean next(){
//				do{
//					if (!super.next())
//						return false;
//				} while(!getChNavigable(getEdge(), getBaseNode(), getAdjNode()));
//				return true;
//		  }
          
        public EdgeSkipIteratorImpl( EdgeFilter filter )
        {
            super(filter);
        }

        @Override
        public final EdgeSkipIterator setBaseNode( int baseNode )
        {
            super.setBaseNode(baseNode);
            return this;
        }

        @Override
        public final void setSkippedEdges( int edge1, int edge2 )
        {
        	
            if (EdgeIterator.Edge.isValid(edge1) != EdgeIterator.Edge.isValid(edge2))
            {
                throw new IllegalStateException("Skipped edges of a shortcut needs "
                        + "to be both valid or invalid but they were not " + edge1 + ", " + edge2);
            }
            edges.setInt(edgePointer + I_SKIP_EDGE1a, edge1);
            edges.setInt(edgePointer + I_SKIP_EDGE2a, edge2);
        }
        
        @Override
        public final void setOriginalEdges( int edge1, int edge2 )
        {
        	if (logger.isDebugEnabled()) logger.debug("edgeId:"+getEdge()+" "+getBaseNode()+" --> "+getAdjNode()+" originalEdges:"+edge1+","+edge2);
            edges.setInt(edgePointer + I_LOWER_NODE_ORIGINAL_EDGE, edge1);
            edges.setInt(edgePointer + I_HIGHER_NODE_ORIGINAL_EDGE, edge2);
        }
        @Override
        public final int getFromOriginalEdge()
        {
            return edges.getInt(edgePointer + I_LOWER_NODE_ORIGINAL_EDGE);
        }

        @Override
        public final int getToOriginalEdge()
        {
            return edges.getInt(edgePointer + I_HIGHER_NODE_ORIGINAL_EDGE);
        }

        @Override
        public final int getSkippedEdge1()
        {
            return edges.getInt(edgePointer + I_SKIP_EDGE1a);
        }

        @Override
        public final int getSkippedEdge2()
        {
            return edges.getInt(edgePointer + I_SKIP_EDGE2a);
        }

        @Override
        public final boolean isShortcut()
        {
            return edgeId > lastEdgeIndex;
        }

        @Override
        public final EdgeSkipIterState setWeight( double weight )
        {
            LevelGraphStorage.this.setWeight(this, weight);
            return this;
        }

        @Override
        public final double getWeight()
        {
            return LevelGraphStorage.this.getWeight(this);
        }

        @Override
        public final EdgeIteratorState detach( boolean reverseArg )
        {
            if (edgeId == nextEdge)
                throw new IllegalStateException("call next before detaching");
            EdgeSkipIteratorImpl iter = new EdgeSkipIteratorImpl(filter);
            iter.setBaseNode(baseNode);
            iter.setEdgeId(edgeId);
            iter.next();
            if (reverseArg)
            {
                iter.reverse = !this.reverse;
                iter.adjNode = baseNode;
                iter.baseNode = adjNode;
            }
            return iter;
        }

        @Override
        public final EdgeIteratorState copyPropertiesTo( EdgeIteratorState edge )
        {
            super.copyPropertiesTo(edge);

//            EdgeSkipIterator eSkip = (EdgeSkipIterator) edge;
//            setSkippedEdges(eSkip.getSkippedEdge1(), eSkip.getSkippedEdge2());
            return edge;
        }
    }

    @Override
    long reverseFlags( long edgePointer, long flags )
    {
        boolean isShortcut = edgePointer > (long) lastEdgeIndex * edgeEntryBytes;
        if (!isShortcut)
            return super.reverseFlags(edgePointer, flags);

        // we need a special swapping for level graph if it is a shortcut as we only store the weight and access flags then
        long dir = flags & scDirMask;
        if (dir == scDirMask || dir == 0)
            return flags;

        // swap the last bits with this mask
        return flags ^ scDirMask;
    }

    /**
     * Disconnects the edges (higher->lower node) via the specified edgeState pointing from lower to
     * higher node.
     * <p>
     * @param edgeState the edge from lower to higher
     */
    public void disconnect( EdgeSkipExplorer explorer, EdgeIteratorState edgeState )
    {
    	if (logger.isDebugEnabled()) logger.debug("disconnect edgeId:"+edgeState.getEdge()+"  "+edgeState.getBaseNode()+" --> "+edgeState.getAdjNode());
    	
    	
    	
        // search edge with opposite direction        
        // EdgeIteratorState tmpIter = getEdgeProps(iter.getEdge(), iter.getBaseNode());
        EdgeSkipIterator tmpIter = explorer.setBaseNode(edgeState.getAdjNode());
        int tmpPrevEdge = EdgeIterator.NO_EDGE;
        boolean found = false;
        while (tmpIter.next())
        {
            // If we disconnect shortcuts only we could run normal algos on the graph too
            // BUT CH queries will be 10-20% slower and preparation will be 10% slower
            if (/*tmpIter.isShortcut() &&*/ tmpIter.getEdge() == edgeState.getEdge())
            {
                found = true;
                break;
            }

            tmpPrevEdge = tmpIter.getEdge();
        }
        if (found){
        	setChNavigable(edgeState.getEdge(), edgeState.getAdjNode(), edgeState.getBaseNode(), false);
//            internalEdgeDisconnect(edgeState.getEdge(), (long) tmpPrevEdge * edgeEntryBytes, edgeState.getAdjNode(), edgeState.getBaseNode());
        }
    }

    @Override
    public AllEdgesSkipIterator getAllEdges()
    {
        return new AllEdgeSkipIterator();
    }

    class AllEdgeSkipIterator extends AllEdgeIterator implements AllEdgesSkipIterator
    {
        @Override
        public final void setSkippedEdges( int edge1, int edge2 )
        {
            edges.setInt(edgePointer + I_SKIP_EDGE1a, edge1);
            edges.setInt(edgePointer + I_SKIP_EDGE2a, edge2);
        }

        @Override
        public final int getSkippedEdge1()
        {
            return edges.getInt(edgePointer + I_SKIP_EDGE1a);
        }

        @Override
        public final int getSkippedEdge2()
        {
            return edges.getInt(edgePointer + I_SKIP_EDGE2a);
        }

        @Override
        public final void setOriginalEdges( int edge1, int edge2 )
        {
            edges.setInt(edgePointer + I_LOWER_NODE_ORIGINAL_EDGE, edge1);
            edges.setInt(edgePointer + I_HIGHER_NODE_ORIGINAL_EDGE, edge2);
        }

        @Override
        public final int getFromOriginalEdge()
        {
            return edges.getInt(edgePointer + I_LOWER_NODE_ORIGINAL_EDGE);
        }

        @Override
        public final int getToOriginalEdge()
        {
            return edges.getInt(edgePointer + I_HIGHER_NODE_ORIGINAL_EDGE);
        }
        
        
        @Override
        public final boolean isShortcut()
        {
            return edgePointer / edgeEntryBytes > lastEdgeIndex;
        }

        @Override
        public final EdgeSkipIterState setWeight( double weight )
        {
            LevelGraphStorage.this.setWeight(this, weight);
            return this;
        }

        @Override
        public final double getWeight()
        {
            return LevelGraphStorage.this.getWeight(this);
        }
    }

    @Override
    protected SingleEdge createSingleEdge( int edge, int nodeId )
    {
        return new SingleLevelEdge(edge, nodeId);
    }

    class SingleLevelEdge extends SingleEdge implements EdgeSkipIterState
    {
        public SingleLevelEdge( int edge, int nodeId )
        {
            super(edge, nodeId);
        }

        @Override
        public final void setSkippedEdges( int edge1, int edge2 )
        {
            edges.setInt(edgePointer + I_SKIP_EDGE1a, edge1);
            edges.setInt(edgePointer + I_SKIP_EDGE2a, edge2);
        }

        @Override
        public final int getSkippedEdge1()
        {
            return edges.getInt(edgePointer + I_SKIP_EDGE1a);
        }

        @Override
        public final int getSkippedEdge2()
        {
            return edges.getInt(edgePointer + I_SKIP_EDGE2a);
        }
        
        @Override
        public final void setOriginalEdges( int edge1, int edge2 )
        {
            edges.setInt(edgePointer + I_LOWER_NODE_ORIGINAL_EDGE, edge1);
            edges.setInt(edgePointer + I_HIGHER_NODE_ORIGINAL_EDGE, edge2);
        }

        @Override
        public final int getFromOriginalEdge()
        {
            return edges.getInt(edgePointer + I_LOWER_NODE_ORIGINAL_EDGE);
        }

        @Override
        public final int getToOriginalEdge()
        {
            return edges.getInt(edgePointer + I_HIGHER_NODE_ORIGINAL_EDGE);
        }
        

        @Override
        public final boolean isShortcut()
        {
            return edgeId > lastEdgeIndex;
        }

        @Override
        public final EdgeSkipIterState setWeight( double weight )
        {
            LevelGraphStorage.this.setWeight(this, weight);
            return this;
        }

        @Override
        public final double getWeight()
        {
            return LevelGraphStorage.this.getWeight(this);
        }
    }

    final void setWeight( EdgeSkipIterState edge, double weight )
    {
        if (!edge.isShortcut())
            throw new IllegalStateException("setWeight is only available for shortcuts");
        if (weight < 0)
            throw new IllegalArgumentException("weight cannot be negative! but was " + weight);

        long weightLong;
        if (weight > MAX_WEIGHT)
            weightLong = MAX_WEIGHT_LONG;
        else
            weightLong = ((long) (weight * WEIGHT_FACTOR)) << 2;

        long accessFlags = edge.getFlags() & PrepareEncoder.getScDirMask();
        edge.setFlags(weightLong | accessFlags);
    }

    final double getWeight( EdgeSkipIterState edge )
    {
        if (!edge.isShortcut())
            throw new IllegalStateException("getWeight is only available for shortcuts");

        double weight = (edge.getFlags() >> 2) / WEIGHT_FACTOR;
        if (weight >= MAX_WEIGHT)
            return Double.POSITIVE_INFINITY;

        return weight;
    }

    @Override
    protected int loadEdgesHeader()
    {
        int next = super.loadEdgesHeader();
        lastEdgeIndex = edges.getHeader(next * 4);
        return next + 1;
    }

    @Override
    protected int setEdgesHeader()
    {
        int next = super.setEdgesHeader();
        edges.setHeader(next * 4, lastEdgeIndex);
        return next + 1;
    }
    
    public EdgeFilter EDGES_CH_NAVIGABLE = new EdgeFilter()
    {
        @Override
        public boolean accept( EdgeIteratorState edgeIterState )
        {
            return getChNavigable(edgeIterState.getEdge(), edgeIterState.getBaseNode(), edgeIterState.getAdjNode());
        }
    };
}

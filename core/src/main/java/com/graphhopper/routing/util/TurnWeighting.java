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
package com.graphhopper.routing.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;

/**
 * Provides methods to retrieve turn costs for a specific turn.
 * <p>
 * @author Karl Hübner
 * @author Peter Karich
 */
public class TurnWeighting implements Weighting
{
	private final static Logger logger = LoggerFactory.getLogger(TurnWeighting.class);
    /**
     * Encoder, which decodes the turn flags
     */
    private final TurnCostEncoder turnCostEncoder;
    private final TurnCostStorage turnCostStorage;
    private final Weighting superWeighting;
    private double defaultUTurnCost = Double.POSITIVE_INFINITY;
    protected GraphStorage graphStorage;

    /**
     * @param turnCostStorage the turn cost storage to be used
     */
    public TurnWeighting( Weighting superWeighting, GraphStorage graphStorage,TurnCostEncoder encoder, TurnCostStorage turnCostStorage )
    {
        this.turnCostEncoder = encoder;
        this.superWeighting = superWeighting;
        this.turnCostStorage = turnCostStorage;
        if (encoder == null)
            throw new IllegalArgumentException("No encoder set to calculate turn weight");
        if (turnCostStorage == null)
            throw new RuntimeException("No storage set to calculate turn weight");
        this.graphStorage=graphStorage;
    }

    /**
     * Set the default cost for an u-turn in seconds. Default is 40s. Should be that high to avoid
     * 'tricking' other turn costs or restrictions.
     */
    public TurnWeighting setDefaultUTurnCost( double costInSeconds )
    {
        this.defaultUTurnCost = costInSeconds;
        return this;
    }

    @Override
    public double getMinWeight( double distance )
    {
        return superWeighting.getMinWeight(distance);
    }

    @Override
    public double calcWeight( EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId )
    {
        double weight = superWeighting.calcWeight(edgeState, reverse, prevOrNextEdgeId);
        if (prevOrNextEdgeId == EdgeIterator.NO_EDGE)
            return weight;
        
        int originalEdgeFrom=PrepareContractionHierarchies.getOriginal(graphStorage,prevOrNextEdgeId,edgeState.getBaseNode(), reverse);
//        			PrepareContractionHierarchies.getOriginalEdgeIdClosestToAdjNode(edgeState);
        int originalEdgeTo;
        if (!reverse)
        	originalEdgeTo=PrepareContractionHierarchies.getOriginalEdgeIdClosestToAdjNode(graphStorage,edgeState);
        else
        	originalEdgeTo=PrepareContractionHierarchies.getOriginalEdgeIdClosestToBaseNode(graphStorage,edgeState);
        int nodeVia=edgeState.getBaseNode();
        
        double turnCosts;
        if (reverse)
        	turnCosts = calcTurnWeight(originalEdgeTo, nodeVia, originalEdgeFrom);
        else
        	turnCosts = calcTurnWeight(originalEdgeFrom, nodeVia, originalEdgeTo);

        if (logger.isDebugEnabled()) logger.debug("edgeFrom:"+originalEdgeFrom+" nodeVia:"+nodeVia+" edgeTo:"+originalEdgeTo+" turnCosts:"+turnCosts);
        
        
        
        if (turnCosts == 0 && originalEdgeFrom == originalEdgeTo){
        	if (logger.isDebugEnabled()) logger.debug("u turn");
            return weight + defaultUTurnCost;
        }

        return weight + turnCosts;
    }

    public double calcTurnWeight( int edgeFrom, int nodeVia, int edgeTo )
    {
        long turnFlags = turnCostStorage.getTurnCostFlags(nodeVia, edgeFrom, edgeTo);
        if (turnCostEncoder.isTurnRestricted(turnFlags))
            return Double.POSITIVE_INFINITY;

        return turnCostEncoder.getTurnCost(turnFlags);
    }

    @Override
    public String toString()
    {
        return "TURN|" + superWeighting.toString();
    }
}

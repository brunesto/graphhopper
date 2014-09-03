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
package com.graphhopper.routing.ch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.PathBidirRef;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeSkipIterState;

/**
 * Recursivly unpack shortcuts.
 * <p/>
 * @see PrepareContractionHierarchies
 * @author Peter Karich
 */
public class Path4CH extends PathBidirRef
{
	private final static Logger logger = LoggerFactory.getLogger(Path4CH.class);
	   
    public Path4CH( Graph g, FlagEncoder encoder )
    {
        super(g, encoder);
    }

    
    
    @Override
    protected void processEdge( int tmpEdge, int endNode )
    {
    	logger.debug(""+graph.getEdgeProps(tmpEdge, Integer.MIN_VALUE));
    	
    	if (tmpEdge==101650){
    		logger.info("101650");
    		}
    	
    	EdgeSkipIterState mainEdgeState=(EdgeSkipIterState) graph.getEdgeProps(tmpEdge, endNode);
    	if (logger.isDebugEnabled()){
    		if (mainEdgeState==null)
    			throw new NullPointerException("you messed it up! tmpEdge:"+tmpEdge+" endNode:"+endNode);
    		 double dist = mainEdgeState.getDistance();
    		 if (mainEdgeState.isShortcut()){
	    		 double weight=mainEdgeState.getWeight();
	    		 logger.debug("expand edgeId:"+mainEdgeState.getEdge()+" "+mainEdgeState.getBaseNode()+","+" --> "+mainEdgeState.getAdjNode()+","+graph.getNodeAccess().getLongitude(mainEdgeState.getAdjNode())+" distance:"+mainEdgeState.getDistance()+" is a shortcut with weight:"+weight);
    		 } else
    			 logger.debug("expand edgeId:"+mainEdgeState.getEdge()+" "+mainEdgeState.getBaseNode()+","+" --> "+mainEdgeState.getAdjNode()+","+graph.getNodeAccess().getLongitude(mainEdgeState.getAdjNode())+" distance:"+mainEdgeState.getDistance()+" is a plain edge");
             
        	
    	}
        
    	
        // Shortcuts do only contain valid weight so first expand before adding
        // to distance and time
        expandEdge(mainEdgeState, !reverseOrder,0);
    }

    private void expandEdge( EdgeSkipIterState mainEdgeState, boolean reverse,int depth4debug )
    {
    	 String debugTab="";
    	 if (logger.isDebugEnabled()){
         	while(debugTab.length()<depth4debug)debugTab+=" ";
    	 }
    	
        if (!mainEdgeState.isShortcut())
        {
            double dist = mainEdgeState.getDistance();
            distance += dist;
            long flags = mainEdgeState.getFlags();
            double timeOnEdge=0;
            try {
            	calcMillis(dist, flags, reverse);
            } catch (IllegalStateException e){
            	logger.warn(e.getMessage());
            }
            millis += timeOnEdge;
            addEdge(mainEdgeState.getEdge());
            
            
            
            
            if (logger.isDebugEnabled()){
            	double speed=encoder.getSpeed(mainEdgeState.getFlags());
            	logger.debug(debugTab+"edgeId:"+mainEdgeState.getEdge()+" "+mainEdgeState.getBaseNode()+" --> "+mainEdgeState.getAdjNode()+" distance:"+mainEdgeState.getDistance()+" speed:"+speed+" time:"+timeOnEdge+" reverse:"+reverse);
            	logger.debug(debugTab+graph.getNodeAccess().getLatitude(mainEdgeState.getBaseNode())+","+graph.getNodeAccess().getLongitude(mainEdgeState.getBaseNode())+" --> "+graph.getNodeAccess().getLatitude(mainEdgeState.getAdjNode())+","+graph.getNodeAccess().getLongitude(mainEdgeState.getAdjNode())+" distance:"+mainEdgeState.getDistance()+" speed:"+speed+" time:"+timeOnEdge);
            }

            
            return;
        }

        if (logger.isDebugEnabled())
        	logger.debug(debugTab+"expand edgeId:"+mainEdgeState.getEdge()+" "+mainEdgeState.getBaseNode()+" --> "+mainEdgeState.getAdjNode());
        
        int skippedEdge1 = mainEdgeState.getSkippedEdge1();
        int skippedEdge2 = mainEdgeState.getSkippedEdge2();
        int from = mainEdgeState.getBaseNode(), to = mainEdgeState.getAdjNode();
        if (reverse)
        {
            int tmp = from;
            from = to;
            to = tmp;
        }

        // getEdgeProps could possibly return an empty edge if the shortcut is available for both directions
        if (reverseOrder)
        {
            EdgeSkipIterState edgeState = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge1, to);
            boolean empty = edgeState == null;
            if (empty)
                edgeState = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge2, to);

            expandEdge(edgeState, false,depth4debug+1);

            if (empty)
                edgeState = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge1, from);
            else
                edgeState = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge2, from);

            expandEdge(edgeState, true,depth4debug+1);
        } else
        {
            EdgeSkipIterState iter = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge1, from);
            boolean empty = iter == null;
            if (empty)
                iter = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge2, from);

            expandEdge(iter, true,depth4debug+1);

            if (empty)
                iter = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge1, to);
            else
                iter = (EdgeSkipIterState) graph.getEdgeProps(skippedEdge2, to);

            expandEdge(iter, false,depth4debug+1);
        }
    }
}

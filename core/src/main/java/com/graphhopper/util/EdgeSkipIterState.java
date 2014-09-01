/*
 *  Licensed to Peter Karich under one or more contributor license
 *  agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  Peter Karich licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.util;

/**
 * @author Peter Karich
 */
public interface EdgeSkipIterState extends EdgeIteratorState
{
    int getSkippedEdge1();

    int getSkippedEdge2();

    void setSkippedEdges( int edge1, int edge2 );

    boolean isShortcut();

    EdgeSkipIterState setWeight( double weight );

    double getWeight();
    
    
    int getFromOriginalEdge();

    int getToOriginalEdge();

    void setOriginalEdges( int edge1, int edge2 );
}

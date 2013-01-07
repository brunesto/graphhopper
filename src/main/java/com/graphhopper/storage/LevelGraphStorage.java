/*
 *  Copyright 2012 Peter Karich 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import com.graphhopper.util.EdgeSkipIterator;

/**
 * A Graph necessary for shortcut algorithms like Contraction Hierarchies (stores level of nodes and
 * shortcut edge per edge).
 *
 * @author Peter Karich
 */
public class LevelGraphStorage extends GraphStorage implements LevelGraph {

    private final int I_SKIP_EDGE;
    private final int I_LEVEL;

    public LevelGraphStorage(Directory dir) {
        super(dir);
        I_SKIP_EDGE = nextEdgeEntryIndex();
        I_LEVEL = nextNodeEntryIndex();
        initNodeAndEdgeEntrySize();
    }

    @Override public final void setLevel(int index, int level) {
        ensureNodeIndex(index);
        nodes.setInt((long) index * nodeEntrySize + I_LEVEL, level);
    }

    @Override public final int getLevel(int index) {
        ensureNodeIndex(index);
        return nodes.getInt((long) index * nodeEntrySize + I_LEVEL);
    }

    @Override protected GraphStorage newThis(Directory dir) {
        return new LevelGraphStorage(dir);
    }

    @Override public EdgeSkipIterator edge(int a, int b, double distance, boolean bothDir) {
        return (EdgeSkipIterator) super.edge(a, b, distance, bothDir);
    }

    @Override public EdgeSkipIterator edge(int a, int b, double distance, int flags) {
        ensureNodeIndex(Math.max(a, b));
        int edgeId = internalEdgeAdd(a, b, distance, flags);
        EdgeSkipIteratorImpl iter = new EdgeSkipIteratorImpl(edgeId, a, false, false);
        iter.next();
        iter.skippedEdge(-1);
        return iter;
    }

    @Override
    public EdgeSkipIterator getEdges(int node) {
        return createEdgeIterable(node, true, true);
    }

    @Override
    public EdgeSkipIterator getIncoming(int node) {
        return createEdgeIterable(node, true, false);
    }

    @Override
    public EdgeSkipIterator getOutgoing(int node) {
        return createEdgeIterable(node, false, true);
    }

    @Override
    protected EdgeSkipIterator createEdgeIterable(int baseNode, boolean in, boolean out) {
        int edge = nodes.getInt((long) baseNode * nodeEntrySize + N_EDGE_REF);
        if (edge < 0)
            return new PillarSkipIteratorImpl(-edge, baseNode, in, out);
        return new EdgeSkipIteratorImpl(edge, baseNode, in, out);
    }

    public class PillarSkipIteratorImpl extends PillarEdgeIterable implements EdgeSkipIterator {

        public PillarSkipIteratorImpl(int edge, int baseNode, boolean in, boolean out) {
            super(edge, baseNode, in, out);
        }

        @Override public int skippedEdge() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override public void skippedEdge(int node) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public class EdgeSkipIteratorImpl extends EdgeIterable implements EdgeSkipIterator {

        public EdgeSkipIteratorImpl(int edge, int node, boolean in, boolean out) {
            super(edge, node, in, out);
        }

        @Override public void skippedEdge(int edgeId) {
            edges.setInt(edgePointer + I_SKIP_EDGE, edgeId);
        }

        @Override public int skippedEdge() {
            return edges.getInt(edgePointer + I_SKIP_EDGE);
        }
    }

    @Override
    public EdgeSkipIterator getEdgeProps(int edgeId, int endNode) {
        return (EdgeSkipIterator) super.getEdgeProps(edgeId, endNode);
    }

    @Override
    protected SingleEdge createSingleEdge(int edge, int nodeId) {
        return new SingleLevelEdge(edge, nodeId);
    }

    protected class SingleLevelEdge extends SingleEdge implements EdgeSkipIterator {

        public SingleLevelEdge(int edge, int nodeId) {
            super(edge, nodeId);
        }

        @Override public void skippedEdge(int node) {
            edges.setInt(edgePointer + I_SKIP_EDGE, node);
        }

        @Override public int skippedEdge() {
            return edges.getInt(edgePointer + I_SKIP_EDGE);
        }
    }
}
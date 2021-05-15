package com.example.fruitymeshappuart.fruity.types

class MeshGraph(private var rootNodeId: Short) {
    val meshes: MutableList<SubMeshGraph> = mutableListOf()
    val tempMeshes: MutableList<SubMeshGraph> = mutableListOf()
    val nodeIdMap: MutableMap<Short, Short> = mutableMapOf()
    val meshSize: Int
        get() = nodeIdMap.size

    init {
        nodeIdMap[rootNodeId] = rootNodeId
    }

    fun initialize(rootNodeId: Short) {
        clear()
        this.rootNodeId = rootNodeId
        nodeIdMap[rootNodeId] = rootNodeId
    }

    private fun clear() {
        meshes.clear()
        nodeIdMap.clear()
        tempMeshes.clear()
    }

    fun addNode(from: Short, to: Short) {
        if (isAlreadyAdded(from, to)) return

        val addRootNodeConnection = fun(_from: Short, _to: Short): Boolean {
            if (_from != rootNodeId) return false
            meshes.find { mesh -> mesh.nodeId == _to }?.let { return false }
            meshes.add(SubMeshGraph(_to))
            nodeIdMap[_to] = _to
            return true
        }

        if (addRootNodeConnection(from, to)) return
        if (addRootNodeConnection(to, from)) return
        meshes.forEach { mesh ->
            parseConnectNode(from, to, mesh)?.let {
                val connectNodeId = if (it.nodeId == from) to else from
                it.subs.add(SubMeshGraph(connectNodeId))
                nodeIdMap[connectNodeId] = connectNodeId
                return
            }
        }
        val nonConnectSubMeshGraph = SubMeshGraph(from)
        nonConnectSubMeshGraph.subs.add(SubMeshGraph(to))
        tempMeshes.add(SubMeshGraph(from))
    }

    /**
     * Find SubMeshGraph whose node ID is "from" or "to", and whose subs contains "from" or "to"
     * When fun returns null, there is no subMeshGraph which does not contain either of node ID
     */
    private fun parseConnectNode(from: Short, to: Short, subMeshGraph: SubMeshGraph):
            SubMeshGraph? {
        val searchConnection =
            fun(_from: Short, _to: Short, _subMeshGraph: SubMeshGraph): SubMeshGraph? {
                if (_subMeshGraph.nodeId != _from) return null
                _subMeshGraph.subs.find { sub -> sub.nodeId == _to }?.let { return null }
                return _subMeshGraph
            }
        searchConnection(from, to, subMeshGraph)?.let { return it }
        searchConnection(to, from, subMeshGraph)?.let { return it }

        for (sub in subMeshGraph.subs) {
            parseConnectNode(from, to, sub)?.let { return it }
        }
        return null
    }

    /**
     * true: already added
     */
    private fun isAlreadyAdded(from: Short, to: Short): Boolean {
        return nodeIdMap.containsKey(from) && nodeIdMap.containsKey(to)
    }

    class SubMeshGraph(val nodeId: Short) {
        val subs: MutableList<SubMeshGraph> = mutableListOf()
    }
}
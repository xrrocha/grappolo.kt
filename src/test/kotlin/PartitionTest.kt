package grappolo


import grappolo.Distances.damerau
import resourceLines
import kotlin.test.Test

class PartitionTest {
    @Test
    fun exploresPartitioning() {

        val distanceThreshold = 0.5
        val countThreshold = 32 // Int.MAX_VALUE

        val words = resourceLines("words.txt")
            .take(countThreshold)
            .map { it.split("\t")[0] }
            .toList()
            .sorted()

        val elems = mutableMapOf<Int, MutableMap<Int, Double>>()
            .also {
                words.indices.forEach { elem -> it[elem] = mutableMapOf() }
            }
        words.indices.forEach { i ->
            (i + 1..<words.size).forEach { j ->
                val distance = damerau(words[i], words[j])
                if (distance < distanceThreshold) {
                    elems[i]!![j] = distance
                    elems[j]!![i] = distance
                }
            }
        }

        val distances = elems.flatMap { (_, neighbors) -> neighbors.values }.distinct().sorted()
        distances
            .fold(listOf<Set<Int>>(words.indices.toSet())) { previousClusters, maxDistance ->
                // TODO Study effect of favoring less connected clusters first
                previousClusters.sortedBy { it.size }.flatMap { previousCluster ->
                    val (nextClusters, _) =
                        // TODO Can cluster elements be sorted so as to visit them in "optimal" order?
                        previousCluster.fold(Pair(emptyList<Set<Int>>(), emptySet<Int>())) { accum, elem ->
                            val (collectedClusters, clusteredSoFar) = accum
                            if (clusteredSoFar.contains(elem)) accum
                            else {
                                fun traverse(start: Int, visited: Set<Int>): Set<Int> {
                                    return if (visited.contains(start)) visited
                                    else {
                                        elems[start]!!
                                            .filter { (_, distance) -> distance <= maxDistance }
                                            .map { (neighbor) -> neighbor }
                                            .filterNot(clusteredSoFar::contains)
                                            .fold(visited + start) { accum, neighbor ->
                                                if (accum.contains(neighbor)) accum
                                                else accum + traverse(neighbor, accum)
                                            }
                                    }
                                }

                                val cluster = traverse(elem, emptySet())
                                Pair(collectedClusters.plusElement(cluster), clusteredSoFar + cluster)
                            }
                        }
                    nextClusters.distinct()
                    .also { clusters ->
                        val showClusters =
                            clusters
                                .sortedBy { -it.size }
                                .joinToString(", ", "[", "]") { cluster ->
                                    cluster.map { words[it] }.sorted().joinToString(",")
                                }
                        println("${"%.08f".format(maxDistance)}\t$showClusters")
                    }
                }
            }
    }
}
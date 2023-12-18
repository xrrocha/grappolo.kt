package grappolo


import grappolo.Distances.damerau
import resourceLines
import kotlin.math.log2
import kotlin.test.Test

class PartitionTest {
    @Test
    fun exploresPartitioning() {

        val distanceThreshold = 0.5
        val countThreshold = 256 // Int.MAX_VALUE

        val words = resourceLines("words.txt")
            .take(countThreshold)
            .map { it.split("\t")[0] }
            .toList()
            .sorted()

        val elems = mutableMapOf<Int, MutableMap<Int, Double>>()
        words.indices.forEach { i ->
            (i..<words.size).forEach { j ->
                val distance = damerau(words[i], words[j])
                if (distance <= distanceThreshold) {
                    elems.computeIfAbsent(i) { mutableMapOf() }[j] = distance
                    elems.computeIfAbsent(j) { mutableMapOf() }[i] = distance
                }
            }
        }

        val distances = elems.flatMap { (_, neighbors) -> neighbors.values }.distinct().sorted()

        data class Partition(val distance: Double, val clusters: Set<Set<Int>>, val previousMetric: Double) {
            val metric = distance * log2(clusters.size.toDouble()) // words.size)
        }

        val seedClusters = words.indices.map { setOf(it) }.toSet()
        val seedPartition = Partition(0.0, seedClusters, 0.0)
        val partitions = generateSequence(
            Pair(distances.drop(1), seedPartition)
        ) { (distances, previousPartition) ->
            if (distances.isEmpty()) null
            else {
                val maxDistance = distances.first()
                val nextClusters = previousPartition.clusters.flatMap { previousCluster ->
                    previousCluster.fold(
                        Pair
                            (
                            emptySet<Set<Int>>(),
                            emptySet<Int>()
                        )
                    ) { accum, elem ->
                        val (clusters, allVisited) = accum
                        if (allVisited.contains(elem)) accum
                        else {
                            fun traverse(start: Int, visited: Set<Int>): Set<Int> {
                                return if (visited.contains(start) || allVisited.contains(start)) visited
                                else {
                                    (visited + start).let { visitedSoFar ->
                                        fun isVisited(i: Int) =
                                            visitedSoFar.contains(i) || allVisited.contains(i)
                                        elems[start]!!
                                            .filter { (_, distance) -> distance <= maxDistance }
                                            .map { (neighbor) -> neighbor }
                                            .fold(visitedSoFar + start) { accum, neighbor ->
                                                if (isVisited(neighbor)) accum
                                                else accum + traverse(neighbor, accum)
                                            }
                                    }
                                }
                            }

                            val cluster = traverse(elem, setOf())
                            Pair(clusters.plusElement(cluster), allVisited + cluster)
                        }
                    }
                        .first
                }
                Pair(distances.drop(1), Partition(maxDistance, nextClusters.toSet(), previousPartition.metric))
                    .also { (_, partition) ->
                        val show = partition.clusters
                            .sortedBy { -it.size }
                            .joinToString(", ", "{", "}") {
                                it.map { words[it] }.sorted().joinToString(
                                    ",", "[", "]"
                                )
                            }
                    }
            }
        }
            .forEach { (_, partition) ->
                partition.clusters
                    .sortedBy { -it.size }
                    .map { it.map { words[it] }.sorted() }
                    .forEach { cluster ->
                        listOf(
                            partition.distance,
                            cluster.joinToString(",")
                        )
                            .joinToString("\t")
                            .also(::println)
                    }
            }
    }
}
package grappolo


import grappolo.Distances.damerau
import resourceLines
import kotlin.test.Test

class PartitionTest {
    @Test
    fun exploresPartitioning() {

        val words = resourceLines("words.txt")
            .take(128)
            .map { it.split("\t")[0] }
            .toList()
            .sorted()

        val elems = mutableMapOf<Int, MutableMap<Int, Double>>()
        words.indices.forEach { i ->
            (i..<words.size).forEach { j ->
                val distance = damerau(words[i], words[j])
                    .let { d ->
                        "%.02f".format(d).toDouble()
                    }
                elems.computeIfAbsent(i) { mutableMapOf() }[j] = distance
                elems.computeIfAbsent(j) { mutableMapOf() }[i] = distance
            }
        }

        val distances = elems.flatMap { (_, neighbors) -> neighbors.values }.distinct().sorted()

        data class Partition(val distance: Double, val clusters: Set<Set<Int>>, val previousMetric: Double) {
            public val metric = distance * clusters.size / words.size.toDouble()
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
                    previousCluster.fold(Pair(emptySet<Set<Int>>(), emptySet<Int>())) { accum, elem ->
                        val (clusters, visited) = accum
                        if (visited.contains(elem)) accum
                        else {
                            fun traverse(start: Int, visitedSoFar: Set<Int>): Set<Int> {
                                return if (visitedSoFar.contains(start) || visited.contains(start)) visitedSoFar
                                else {
                                    (visitedSoFar + start).let { visitedSoFar ->
                                        fun isVisited(i: Int) = visitedSoFar.contains(i) || visited.contains(i)
                                        elems[start]!!
                                            .filter { (neighbor, distance) -> distance <= maxDistance }
                                            .map { (neighbor) -> neighbor }
                                            .fold(visitedSoFar + start) { accum, neighbor ->
                                                if (isVisited(neighbor)) accum
                                                else accum + traverse(neighbor, accum)
                                            }
                                    }
                                }
                            }

                            val cluster = traverse(elem, setOf())
                            Pair(clusters.plusElement(cluster), visited + cluster)
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
                        println("${"%.08f".format(maxDistance)}/${"%.08f".format(partition.metric)}:$show")
                    }
            }
        }
            .dropWhile { (_, partition) ->
                partition.metric >= partition.previousMetric
            }
            .iterator()

        val bestPartition =
            if (partitions.hasNext()) partitions.next().second
            else seedPartition
        println("Best metric: ${bestPartition.previousMetric}")
    }
}
import grappolo.Distances
import grappolo.cluster

fun main() {
    val words = resourceLines("words.txt")
        .map { it.split("\t")[0] }
        .toSet()
    val generatePairs = { set: Set<String> ->
        set.toList().let { list ->
            list.indices.flatMap { i ->
                ((i + 1)..<list.size).map { j ->
                    list[i] to list[j]
                }
            }
        }
    }
    val computeDistance = Distances::damerau
    val maxDistance = 0.5

    val (result, elapsedTime) = time {
        cluster(words, generatePairs, computeDistance, maxDistance)
    }
    val (optimalDistance, clusters) = result
    println("Optimal distance: $optimalDistance")
    clusters
        .sortedBy { it.size }
        .withIndex()
        .forEach { (index, cluster) ->
            listOf(
                index + 1,
                cluster.size,
                cluster
            )
                .joinToString("\t")
                .also(::println)
        }
    println("Time: $elapsedTime")
}

import grappolo.cluster
import info.debatty.java.stringsimilarity.Damerau
import kotlin.math.max

fun main() {
    val words = resourceLines("words.txt")
        .take(5000)
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
    val computeDistance =
        Damerau().let { damerau ->
            { first: String, second: String ->
                damerau.distance(first, second) /
                        max(first.length, second.length).toDouble()
            }
        }
    val maxDistance = 0.5

    val (result, elapsedTime) = time {
        cluster(words, generatePairs, computeDistance, maxDistance)
    }
    val (optimalDistance, clusters) = result
    println("Time: $elapsedTime")
    println("Optimal distance: $optimalDistance")
    clusters
        .sortedBy { it.size }
        .forEach(::println)

}

fun <A> time(block: () -> A) =
    System.currentTimeMillis().let { startTime ->
        block().let { result ->
            result to System.currentTimeMillis() - startTime
        }
    }

fun resourceLines(resourceName: String) =
    Thread.currentThread().contextClassLoader
        .getResourceAsStream(resourceName)!!
        .reader().buffered()
        .lineSequence()

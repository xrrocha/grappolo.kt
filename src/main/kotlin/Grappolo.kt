package grappolo

import info.debatty.java.stringsimilarity.Damerau
import kotlin.math.max

data class Score<T>(val first: T, val second: T, val distance: Double)

fun main(args: Array<String>) {
    val entries = setOf(
        "acmilo", "alejandro", "alejnadro", "andra",
        "camila", "camilla", "camilo", "cmila", "cmilo",
        "deigo", "diego", "digo", "laejandro", "rcardo",
        "ricardo", "ricrdo", "sandar", "sandra", "snadra",
    )
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

    val (distance, clusters) =
        cluster(entries, generatePairs, computeDistance, maxDistance)
    println("Optimal distance: $distance")
    clusters.forEach(::println)
}

fun <T> cluster(
    entries: Set<T>,
    generatePais: (Set<T>) -> Iterable<Pair<T, T>>,
    computeDistance: (T, T) -> Double,
    maxDistance: Double
): Pair<Double, List<Set<T>>> =
    generatePais(entries)
        .map { (first, second) ->
            Score(first, second, computeDistance(first, second))
        }
        .filter { it.distance <= maxDistance }
        .sortedBy(Score<T>::distance)
        .let { scores ->
            val distances =
                scores.map(Score<T>::distance).distinct().sorted()

            data class Step(
                val distance: Double,
                val clusters: List<Set<T>>,
                val value: Double,
                val remainingDistances: List<Double>
            )
            generateSequence(
                Step(
                    0.0,
                    entries.map { setOf(it) },
                    Double.MAX_VALUE,
                    distances
                )
            ) { previous ->
                if (previous.remainingDistances.isEmpty()) {
                    null
                } else {
                    val distance = previous.remainingDistances.first()
                    val clusters = cluster(entries, scores.takeWhile { it.distance <= distance })
                    val value = clusters.size * clusters.size * distance / entries.size
                    if (value > previous.value) {
                        null
                    } else {
                        val nextDistances = previous.remainingDistances.drop(1)
                        Step(distance, clusters, value, nextDistances)
                    }
                }
            }
                .last()
                .let { step ->
                    step.distance to step.clusters
                }
        }

fun <T> cluster(
    entries: Set<T>,
    scores: List<Score<T>>
): List<Set<T>> =
    entries
        .associateWith(::setOf)
        .let { clusters ->
            scores.fold(clusters) { clustersSoFar, score ->
                val mergedSet =
                    (clustersSoFar[score.first]!! + clustersSoFar[score.second]!!)
                (clustersSoFar + mergedSet.associateWith { mergedSet })
            }
                .values
                .distinct()
        }

fun Double.format() = String.format("%.4f", this)

package grappolo

import java.time.Instant

internal data class Score<T>(val first: T, val second: T, val distance: Double)

interface ClusteringListener<T> {
    fun entries(entries: Set<T>)
}

fun <T> cluster(
    entries: Set<T>,
    generatePairs: (Set<T>) -> Iterable<Pair<T, T>>,
    computeDistance: (T, T) -> Double,
    maxDistance: Double
): Pair<Double, Set<Set<T>>> =
    generatePairs(entries)
        .also {
            println("${Instant.now()}: Scoring ${entries.size} entries")
        }
        .map { (first, second) ->
            Score(first, second, computeDistance(first, second))
            // .also(::println)
        }
        .filter { it.distance <= maxDistance }
        .sortedBy(Score<T>::distance)
        .also {
            println("${Instant.now()}: Collected ${it.size} scores")
        }
        .let { allScores ->
            val distinctDistances =
                allScores.map(Score<T>::distance).distinct().sorted()
            val highDistance = distinctDistances.last()
            val allDistances = distinctDistances.map { distance ->
                distance / highDistance
            }
            val seedClusterMap = entries.associateWith(::setOf)

            data class Step(
                val distance: Double,
                val quality: Double,
                val clusterMap: Map<T, Set<T>>,
                val remainingDistances: List<Double>,
                val remainingScores: List<Score<T>>,
            ) {
                val clusters = clusterMap.values.distinct().sortedBy { it.size }
            }

            val initialStep = Step(
                distance = 0.0,
                quality = 0.0,
                clusterMap = seedClusterMap,
                remainingDistances = allDistances,
                remainingScores = allScores
            )
            generateSequence(initialStep) { previousStep ->
                if (previousStep.remainingDistances.isEmpty()) {
                    null
                } else {
                    val distance =
                        previousStep.remainingDistances.first()
                    val scores =
                        previousStep.remainingScores.takeWhile {
                            it.distance <= distance
                        }
                    val clusterMap =
                        scores.fold(previousStep.clusterMap) { soFar, score ->
                            val mergedSet =
                                (soFar[score.first]!! + soFar[score.second]!!)
                            (soFar + mergedSet.associateWith { mergedSet })
                        }
                    val clusters = clusterMap.values.distinct()
                    val clusterCount =
                        (entries.size - clusters.size) / (entries.size - 1.0)
                    val quality = (1.0 - distance) * clusterCount
                    if (quality <= previousStep.quality) {
                        null
                    } else {
                        Step(
                            distance, quality, clusterMap,
                            previousStep.remainingDistances.drop(1),
                            previousStep.remainingScores.drop(scores.size)
                        )
                        /*
                        .also {
                            listOf(
                                it.distance,
                                it.quality,
                                it.clusters.size
                            )
                                .joinToString("\t")
                                .also(::println)
                        }
                    */
                    }
                }
            }
                .last()
                .let { step -> step.distance to step.clusterMap.values.toSet() }
                .also {
                    println("${Instant.now()}: Collected ${it.second.size} clusters")
                }
        }

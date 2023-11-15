package grappolo

internal data class Score<T>(val first: T, val second: T, val distance: Double)

fun <T> cluster(
    entries: Set<T>,
    generatePairs: (Set<T>) -> Iterable<Pair<T, T>>,
    computeDistance: (T, T) -> Double,
    maxDistance: Double
): Pair<Double, Set<Set<T>>> =
    generatePairs(entries)
        .map { (first, second) ->
            Score(first, second, computeDistance(first, second))
        }
        .filter { it.distance <= maxDistance }
        .sortedBy(Score<T>::distance)
        .let { allScores ->
            val allDistances =
                allScores.map(Score<T>::distance).distinct().sorted()
            val seedClusterMap = entries.associateWith(::setOf)

            data class Step(
                val distance: Double,
                val quality: Double,
                val clusterMap: Map<T, Set<T>>,
                val remainingDistances: List<Double>,
                val remainingScores: List<Score<T>>,
            )

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
                    val nextDistances =
                        previousStep.remainingDistances.drop(1)
                    val (scores, nextScores) =
                        previousStep.remainingScores.partition {
                            it.distance <= distance
                        }
                    val clusterMap =
                        scores.fold(previousStep.clusterMap) { soFar, score ->
                            val mergedSet =
                                (soFar[score.first]!! + soFar[score.second]!!)
                            (soFar + mergedSet.associateWith { mergedSet })
                        }
                    val clusters = clusterMap.values.distinct()
                    val clusterCount = (entries.size - clusters.size) / (entries.size - 1.0)
                    val quality = (1.0 - distance) * clusterCount
                    if (quality <= previousStep.quality) {
                        null
                    } else {
                        Step(distance, quality, clusterMap, nextDistances, nextScores)
                    }
                }
            }
                .last()
                .let { step ->
                    step.distance to step.clusterMap.values.toSet()
                }
        }

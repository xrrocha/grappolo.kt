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
            fun <T> List<T>.headAndTail(): Pair<T, List<T>> =
                Pair(first(), drop(1))

            data class Step(
                val distance: Double,
                val quality: Double,
                val clusterMap: Map<T, Set<T>>,
                val remainingDistances: List<Double>,
                val remainingScores: List<Score<T>>,
            )

            val firstStep = Step(
                distance = 0.0,
                quality = 0.0,
                seedClusterMap, allDistances, allScores
            )
            generateSequence(firstStep) { previous ->
                if (previous.remainingDistances.isEmpty()) {
                    null
                } else {
                    val distance =
                        previous.remainingDistances.first()
                    val nextDistances =
                        previous.remainingDistances.drop(1)
                    val (scores, nextScores) =
                        previous.remainingScores.partition {
                            it.distance <= distance
                        }
                    val clusterMap =
                        scores.fold(previous.clusterMap) { soFar, score ->
                            val mergedSet =
                                (soFar[score.first]!! + soFar[score.second]!!)
                            (soFar + mergedSet.associateWith { mergedSet })
                        }
                    val clusters = clusterMap.values.distinct()
                    val clusterCount = (entries.size - clusters.size) / (entries.size - 1.0)
                    val quality = (1.0 - distance) * clusterCount
                    if (quality <= previous.quality) {
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

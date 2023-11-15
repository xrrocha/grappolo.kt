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
        .let { scores ->
            val distances =
                scores.map(Score<T>::distance).distinct().sorted()

            data class Step(
                val distance: Double,
                val clusters: List<Set<T>>,
                val quality: Double,
                val remainingDistances: List<Double>
            )
            generateSequence(
                Step(
                    0.0,
                    entries.map { setOf(it) },
                    0.0,
                    distances
                )
            ) { previous ->
                if (previous.remainingDistances.isEmpty()) {
                    null
                } else {
                    val distance = previous.remainingDistances.first()
                    val clusters = cluster(entries, scores.takeWhile { it.distance <= distance })
                    val clusterFactor = (entries.size - clusters.size) / (entries.size - 1.0)
                    val quality = (1.0 - distance) * clusterFactor
                    println("clusters: ${clusters.size}, clusterFactor: $clusterFactor, quality: $quality")
                    if (quality <= previous.quality) {
                        null
                    } else {
                        val nextDistances = previous.remainingDistances.drop(1)
                        Step(distance, clusters, quality, nextDistances)
                    }
                }
            }
                .last()
                .let { step ->
                    step.distance to step.clusters.toSet()
                }
        }

internal fun <T> cluster(
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

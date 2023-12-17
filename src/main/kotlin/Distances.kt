package grappolo

import info.debatty.java.stringsimilarity.Damerau
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.sqrt

object Distances {
    private val damerau = Damerau()

    fun damerau(first: String, second: String): Double =
        if (first == second) 0.0
        else damerau.distance(first, second) /
                max(first.length, second.length).toDouble()

    fun <T> tfidfMetric(groups: Set<List<T>>): (List<T>, List<T>) -> Double {
        val groupCountLog = log2(groups.size.toDouble())
        val elementTfidf: Map<T, Map<List<T>, Double>> =
            groups.flatten()
                .groupBy { it }
                .map { (element, values) ->
                    val idf =
                        groupCountLog - log2(values.size.toDouble())
                    val tfidf =
                        groups
                            .map { group ->
                                val count = group.count {
                                    it == element
                                }
                                group to count
                            }
                            .filter { (_, count) -> count > 0 }
                            .associate { (group, count) ->
                                val tf =
                                    count.toDouble() / group.size
                                group to tf * idf
                            }
                            .withDefault { 0.0 }
                    element to tfidf
                }
                .toMap()
        val vectors: Map<List<T>, Map<T, Double>> =
            groups.associateWith { group ->
                group.associateWith { element ->
                    elementTfidf[element]!![group]!!
                }
            }
        return { group1, group2 ->
            1.0 - cosineSimilarity(vectors[group1]!!, vectors[group2]!!)
        }
    }

    private fun <T> cosineSimilarity(
        vector1: Map<T, Double>,
        vector2: Map<T, Double>,
    ): Double {
        val (dotProduct, sqSum1, sqSum2) =
            (vector1.keys + vector2.keys)
                .fold(Triple(0.0, 0.0, 0.0)) { accum, a ->
                    val s1 = vector1[a] ?: 0.0
                    val s2 = vector2[a] ?: 0.0
                    val (dotProduct, sqSum1, sqSum2) = accum
                    Triple(
                        dotProduct + s1 * s2,
                        sqSum1 + s1 * s1,
                        sqSum2 + s2 * s2
                    )
                }
        val magnitude1 = sqrt(sqSum1)
        val magnitude2 = sqrt(sqSum2)
        return if (magnitude1 == 0.0 && magnitude2 == 0.0) 0.0
        else dotProduct / (magnitude1 * magnitude2)
    }
}
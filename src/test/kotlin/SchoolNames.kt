import grappolo.Distances
import grappolo.cluster

fun main() {

    val code = "090750"

    val nameCounts: Map<String, Int> =
        resourceLines("school-names.txt")
            .map { it.split("\t") }
            .filter { it[0] == code }
            .map { it[1] }
            .groupBy { it }
            .mapValues { (_, values) -> values.size }

    val nonAlnum = "[^\\p{Alnum}]+".toRegex()
    val wordBag2Name: Map<List<String>, List<String>> =
        nameCounts
            .keys
            .map { name ->
                val wordBag =
                    nonAlnum
                        .split(name)
                        .map { it.trim().lowercase() }
                        .filterNot { it.isEmpty() }
                wordBag to name
            }
            .filterNot { it.first.isEmpty() }
            .groupBy { it.first }
            .mapValues { (_, values) -> values.map { it.second } }

    val wordCounts = wordBag2Name
        .keys
        .flatMap { wordBag ->
            val wordBagNameCount =
                wordBag2Name[wordBag]!!.sumOf { nameCounts[it]!! }
            wordBag
                .groupBy { it }
                .map { (word, values) ->
                    word to values.size * wordBagNameCount
                }
        }
        .groupBy { it.first }
        .mapValues { (_, values) ->
            values.sumOf { it.second }
        }
    val wordClusters = clusterWords(wordCounts.keys)
    wordClusters
        .sortedBy { it.size }
        .map { it.sortedBy { -wordCounts[it]!! } }
        .withIndex()
        .forEach(::println)
}

fun clusterWords(words: Set<String>): Set<Set<String>> {
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

    return clusters
}

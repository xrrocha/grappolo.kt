import grappolo.Distances
import grappolo.cluster

fun main() {
    /*
     */
    val nonAlnum = "[^\\p{Alnum}]+".toRegex()
    val sqlStmts = resourceLines("sqlstmts.txt").toSet()
    /*
    val nonAlnum = "\\s+".toRegex()
    val sqlStmts = setOf(
        "The cat",
        "The cat is on the mat",
        "My dog and cat are the best",
        "The locals are playing",
    )
    */
    val wordBags2Sqlstmts = sqlStmts
        .map {
            val words = nonAlnum.split(it)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(String::lowercase)
            words to it
        }
        .groupBy { it.first }
        .mapValues { (_, values) ->
            values.map { it.second }
        }
    val wordBags = wordBags2Sqlstmts.keys
    // println(wordBags)
    val generatePairs = { set: Set<List<String>> ->
        set.toList().let { list ->
            list.indices.flatMap { i ->
                ((i + 1)..<list.size).map { j ->
                    list[i] to list[j]
                }
            }
        }
    }
    val computeDistance = Distances.tfidfMetric(wordBags)
    val maxDistance = 1.0 // 0.5

    val (result, elapsedTime) = time {
        cluster(wordBags, generatePairs, computeDistance, maxDistance)
    }
    val (optimalDistance, clusters) = result
    clusters
        .map { cluster ->
            cluster.flatMap { wordBag ->
                wordBags2Sqlstmts[wordBag]!!
            }
        }
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
    println("Optimal distance: $optimalDistance")
    println("WordBags: ${wordBags.size}")
    println("Clusters: ${clusters.size}")
    // wordBags.withIndex().forEach(::println)
}

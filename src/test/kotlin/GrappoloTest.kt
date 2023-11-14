import grappolo.cluster
import info.debatty.java.stringsimilarity.Damerau
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals

class GrappoloTest {
    @Test
    fun `clusters names properly`() {
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

        val (optimalDistance, clusters) =
            cluster(entries, generatePairs, computeDistance, maxDistance)
        assertEquals(0.2, optimalDistance)
        assertEquals(
            setOf(
                setOf("acmilo", "camila", "camilla", "camilo", "cmila", "cmilo"),
                setOf("alejandro", "alejnadro", "laejandro"),
                setOf("andra", "sandra", "sandar", "snadra"),
                setOf("deigo", "diego", "digo"),
                setOf("rcardo", "ricardo", "ricrdo"),
            ),
            clusters
        )
    }
}
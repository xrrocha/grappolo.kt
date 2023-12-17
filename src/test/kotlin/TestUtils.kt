
fun <A> time(block: () -> A) =
    System.currentTimeMillis().let { startTime ->
        block() to System.currentTimeMillis() - startTime
    }

fun resourceLines(resourceName: String) =
    Thread.currentThread().contextClassLoader
        .getResourceAsStream(resourceName)!!
        .reader().buffered()
        .lineSequence()

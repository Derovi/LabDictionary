package by.derovi.botp2p

fun <T> Iterable<T>.pairs(): Iterable<Pair<T, T>> = PairsIterable { iterator() }

class PairsIterable<T>(private val iteratorFactory: () -> Iterator<T>): Iterable<Pair<T, T>> {
    override fun iterator(): Iterator<Pair<T, T>> = PairsIterator(iteratorFactory)
}

class PairsIterator<T>(private val iteratorFactory: () -> Iterator<T>): Iterator<Pair<T, T>> {
    private val firstIterator = iteratorFactory()
    private var secondIterator = iteratorFactory()

    private var firstValue = firstIterator.next()

    override fun hasNext(): Boolean = firstIterator.hasNext() || secondIterator.hasNext()

    override fun next(): Pair<T, T> {
        return if (secondIterator.hasNext()) {
            firstValue to secondIterator.next()
        } else {
            secondIterator = iteratorFactory()
            firstValue = firstIterator.next()
            firstValue to secondIterator.next()
        }
    }
}

fun <T> List<T>.distinctPairs(): Iterable<Pair<T, T>> = DistinctPairsIterable { idx -> this.listIterator(idx) }

class DistinctPairsIterable<T>(private val iteratorFactory: (idx: Int) -> Iterator<T>): Iterable<Pair<T, T>> {
    override fun iterator(): Iterator<Pair<T, T>> = DistinctPairsIterator(iteratorFactory)
}

class DistinctPairsIterator<T>(private val iteratorFactory: (idx: Int) -> Iterator<T>): Iterator<Pair<T, T>> {
    var idx = 1
    var cnt = 0
    private val firstIterator = iteratorFactory(0)
    private var secondIterator = if (firstIterator.hasNext()) iteratorFactory(1) else null

    private var firstValue = if (firstIterator.hasNext()) firstIterator.next() else null

    override fun hasNext(): Boolean = firstIterator.hasNext() && cnt > 1 || secondIterator?.hasNext() ?: false

    override fun next(): Pair<T, T> {
        if (firstValue == null || secondIterator == null) {
            throw java.lang.IllegalStateException()
        }

        return if (secondIterator!!.hasNext()) {
            ++cnt
            firstValue!! to secondIterator!!.next()
        } else {
            cnt = 1
            secondIterator = iteratorFactory(++idx)
            firstValue = firstIterator.next()
            firstValue!! to secondIterator!!.next()
        }
    }
}

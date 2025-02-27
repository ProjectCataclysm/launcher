package me.pavanvo.events

class Event {
    private val observers = mutableSetOf<() -> Unit>()

    operator fun plusAssign(observer: () -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: () -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke() {
        for (observer in observers)
            observer()
    }
}

class Event1<T> {
    private val observers = mutableSetOf<(T) -> Unit>()

    operator fun plusAssign(observer: (T) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value: T) {
        for (observer in observers)
            observer(value)
    }
}

class Event2<T, U> {
    private val observers = mutableSetOf<(T, U) -> Unit>()

    operator fun plusAssign(observer: (T, U) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T, U) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value1: T, value2: U) {
        for (observer in observers)
            observer(value1, value2)
    }
}

class Event3<T1, T2, T3> {
    private val observers = mutableSetOf<(T1, T2, T3) -> Unit>()

    operator fun plusAssign(observer: (T1, T2, T3) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T1, T2, T3) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value1: T1, value2: T2, value3: T3) {
        for (observer in observers)
            observer(value1, value2, value3)
    }
}

class Event4<T1, T2, T3, T4> {
    private val observers = mutableSetOf<(T1, T2, T3, T4) -> Unit>()

    operator fun plusAssign(observer: (T1, T2, T3, T4) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T1, T2, T3, T4) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value1: T1, value2: T2, value3: T3, value4: T4) {
        for (observer in observers)
            observer(value1, value2, value3, value4)
    }
}
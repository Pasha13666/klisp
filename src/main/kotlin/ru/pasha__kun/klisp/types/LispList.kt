package ru.pasha__kun.klisp.types

import ru.pasha__kun.klisp.Environment
import ru.pasha__kun.klisp.LispInterpreter

class LispList : LispValue, MutableList<LispValue> {
    private var head: LispValue? = null
    private var tail: LispList? = null

    override val size: Int
        get(){
            var n = 0
            foreach {
                n += 1
            }
            return n
        }

    inline private fun foreach(fn: (LispValue)->Unit){
        var i: LispList? = this
        while (i != null){
            if (i.head != null) fn(i.head!!)
            i = i.tail
        }
    }

    override fun contains(element: LispValue): Boolean {
        foreach {
            if (it == element) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<LispValue>): Boolean {
        elements.forEach {
            if (!contains(it)) return false
        }
        return true
    }

    override fun get(index: Int): LispValue {
        var n = 0
        foreach {
            if (n == index) return it
            n += 1
        }
        throw IndexOutOfBoundsException("get($index), size = $size")
    }

    override fun indexOf(element: LispValue): Int {
        var n = 0
        var i: LispList? = this
        while (i != null){
            if (i.head == element)
                return n
            n += 1
            i = i.tail
        }
        return -1
    }

    override fun isEmpty() = size == 0

    override fun iterator(): MutableIterator<LispValue> = listIterator()

    override fun lastIndexOf(element: LispValue): Int {
        var k = -1
        var n = 0
        var i: LispList? = this
        while (i != null){
            if (i.head == element){
                k = n
            }
            n += 1
            i = i.tail
        }
        return k
    }

    override fun add(element: LispValue): Boolean {
        var i: LispList = this
        while (i.head != null && i.tail != null) i = i.tail!!

        if (i.head != null) {
            val v = LispList()
            i.tail = v
            i = v
        }

        i.head = element
        return true
    }

    override fun add(index: Int, element: LispValue) {
        assert(index >= 0)
        if (head == null){
            assert(index == 0)
            head = element
        } else if (index == size)
            add(element)
        else {

            TODO("not implemented")
        }
    }

    override fun addAll(index: Int, elements: Collection<LispValue>): Boolean {
        elements.forEachIndexed { i, v ->
            add(i + index, v)
        }
        return true
    }

    override fun addAll(elements: Collection<LispValue>): Boolean {
        elements.forEach {
            add(it)
        }
        return true
    }

    override fun clear() {
        head = null
        tail = null
    }

    override fun listIterator(index: Int): MutableListIterator<LispValue> = subList(index, lastIndex - index).listIterator()

    override fun removeAll(elements: Collection<LispValue>): Boolean {
        elements.forEach {
            remove(it)
        }
        return true
    }

    override fun set(index: Int, element: LispValue): LispValue {
        var n = 0
        var i: LispList? = this
        while (i != null){
            if (n == index){
                i.head = element
                return element
            }
            n += 1
            i = i.tail
        }
        throw IndexOutOfBoundsException()
    }

    override fun listIterator() = object: MutableListIterator<LispValue> {
        private var index = 0
        private var max = lastIndex

        override fun hasPrevious() = index > 0

        override fun nextIndex() = index + 1

        override fun previous() = get(index--)

        override fun previousIndex() = index - 1

        override fun add(element: LispValue) {
            add(index, element)
        }

        override fun hasNext() = index <= max

        override fun next() = get(index++)

        override fun remove(){
            removeAt(index)
        }

        override fun set(element: LispValue) {
            set(index, element)
        }

    }

    override fun remove(element: LispValue): Boolean {
        var n = 0
        var i: LispList? = this
        while (i != null){
            if (i.head == element){
                removeAt(n)
            }
            n += 1
            i = i.tail
        }
        return true
    }

    override fun removeAt(index: Int): LispValue {
        if (index == 0) throw IndexOutOfBoundsException("Cannot remove first element")
        var v = this
        var i = 0
        while (v.tail != null && ++i < index) v = v.tail!!
        val r = v.tail?.head
        v.tail = v.tail?.tail
        if (r == null) throw IndexOutOfBoundsException("index: $index, size: $size")
        return r
    }

    override fun retainAll(elements: Collection<LispValue>): Boolean {
        TODO("not implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<LispValue> {
        val s = size
        if (fromIndex !in 0..s - 1 || toIndex !in 0..s - 1 || fromIndex > toIndex)
            throw IndexOutOfBoundsException("index: $fromIndex, $toIndex, size: $size")

        if (fromIndex == toIndex) return LispList()

        if (toIndex == s - 1){
            var v = this
            var i = 0
            while (i++ < fromIndex) v = v.tail!!
            return v
        }

        var v = this
        var i = 0
        while (i++ < fromIndex) v = v.tail!!
        val r = LispList()
        var e = r
        while (i++ < toIndex) {
            e.head = v.head
            e = LispList().also {
                e.tail = it
            }
        }
        return r
    }

    override fun toString() = buildString {
        var r = this@LispList
        append('(')
        while (true){
            if (r.head != null) append(r.head)
            append(' ')
            r = r.tail ?: break
        }
        append(')')
    }

    override fun eval(strict: Boolean, env: Environment, inter: LispInterpreter): LispValue {
        if (isEmpty()) return this

        val op = get(0).eval(size != 1 || strict, env, inter)
        return op.call(this, strict, env, inter)
    }

    override fun boolean() = isNotEmpty()
}

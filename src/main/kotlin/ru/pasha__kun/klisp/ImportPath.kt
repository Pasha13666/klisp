package ru.pasha__kun.klisp

import java.io.File

class ImportPath  : MutableCollection<String> {
    private data class CacheNode(val module: String, val file: String, val path: String)

    private val values = ArrayList<String>()
    private val cache = HashMap<String, CacheNode>()

    override val size = values.size
    override fun contains(element: String) = values.contains(element)
    override fun containsAll(elements: Collection<String>) = values.containsAll(elements)
    override fun isEmpty() = values.isEmpty()
    override fun iterator() = values.iterator()
    override fun addAll(elements: Collection<String>) = elements.any(this::add)
    override fun removeAll(elements: Collection<String>) = elements.any(this::remove)

    @Synchronized override fun add(element: String): Boolean {
        val v = File(element).list() ?: return false
        values.add(element)
        v.forEach {
            val m = it.substring(it.lastIndexOf(File.separatorChar) + 1, it.indexOf('.'))
            cache[m] = CacheNode(m, it, element)
        }
        return true
    }

    @Synchronized override fun clear() {
        values.clear()
        cache.clear()
    }

    @Synchronized override fun remove(element: String): Boolean {
        if (values.remove(element)){
            val v = cache.iterator()
            while (v.hasNext()) {
                val i = v.next()
                if (i.value.path == element)
                    v.remove()
            }
            return true
        }
        return false
    }

    override fun retainAll(elements: Collection<String>): Boolean {
        TODO("not implemented")
    }

    fun lookup(mod: String) = cache[mod]?.file
}
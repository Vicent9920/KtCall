package com.contact.ktcall.utils

open class SelectionBuilder {
    private val selections = arrayListOf<String>()
    private val selectionArgsList = arrayListOf<String>()

    fun addSelection(key: String, value: Any?) = this.also {
        value?.let { selections.add("$key = ?"); selectionArgsList.add(value.toString()) }
    }

    fun addLike(key: String, value: String?) = this.also {
        value?.let { selections.add("$key LIKE ?"); selectionArgsList.add("%$value%") }
    }

    fun addOrLike(keys: Array<String>, value: String?) = this.also {
        value?.let {
            val conditions = keys.map { "$it LIKE ?" }
            selections.add("(${conditions.joinToString(" OR ")})")
            keys.forEach { _ -> selectionArgsList.add("%$value%") }
        }
    }

    fun addNotNull(key: String) = this.also {
        selections.add("$key IS NOT NULL")
    }

    fun addString(string: String) = this.also {
        selections.add(string)
    }

    fun build() = selections.joinToString(" AND ")
    fun getSelectionArgs() = selectionArgsList.toTypedArray()
}
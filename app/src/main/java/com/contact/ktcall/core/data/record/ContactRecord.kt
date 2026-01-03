package com.contact.ktcall.core.data.record

import androidx.compose.ui.graphics.Color

data class ContactRecord(
    val id: Long = 0,
    val name: String? = null,
    val number: String? = null,
    val photoUri: String? = null,
    val starred: Boolean = false,
    val lookupKey: String? = null,
) {
    override fun toString() = "Contact with id:$id name:$name"

    override fun equals(other: Any?) = other is ContactRecord && id == other.id
    override fun hashCode(): Int {
        return id.hashCode()
    }

    var avatarColor = Color(0xFF9FA8DA)
}
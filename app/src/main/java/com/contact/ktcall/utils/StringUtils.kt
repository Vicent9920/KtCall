package com.contact.ktcall.utils
 fun String.getSortKey(): String {
    return if (this == "#") "zzzzz$this" else this
}
package com.contact.ktcall.core.repository

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers

interface BlockedRepository : BaseRepository {
    suspend fun blockNumber(number: String)
    suspend fun unblockNumber(number: String)
    suspend fun isNumberBlocked(number: String): Boolean
}

class BlockedRepositoryImpl  constructor(
     private val context: Context,
) : BaseRepositoryImpl(), BlockedRepository {

    override suspend fun isNumberBlocked(number: String) =
        BlockedNumberContract.isBlocked(context, number)


    override suspend fun blockNumber(number: String) {
        if (isNumberBlocked(number)) return
        val contentValues = ContentValues()
        contentValues.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        context.contentResolver.insert(BlockedNumbers.CONTENT_URI, contentValues)
    }


    override suspend fun unblockNumber(number: String) {
        if (!isNumberBlocked(number)) return
        val contentValues = ContentValues()
        contentValues.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        context.contentResolver.insert(BlockedNumbers.CONTENT_URI, contentValues)?.also {
            context.contentResolver.delete(it, null, null)
        }
    }
}
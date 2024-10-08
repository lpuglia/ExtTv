package com.android.exttv.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.tvprovider.media.tv.ChannelLogoUtils.storeChannelLogo
import androidx.tvprovider.media.tv.TvContractCompat
import com.android.exttv.MainActivity
import com.android.exttv.R
import com.android.exttv.model.Favourite
import com.android.exttv.model.SectionManager.CardItem
import java.io.File
import androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms as PreviewPrograms
import androidx.tvprovider.media.tv.TvContractCompat.Channels as Channels

object TvContractUtil {
    lateinit var context: MainActivity
    private val channelUri: Uri = Channels.CONTENT_URI
    private val programUri: Uri = PreviewPrograms.CONTENT_URI

    fun init(context: MainActivity) {
        this.context = context
    }

    private fun getCursor(contentUri: Uri, projection: Array<String>? = null, selection: String? = null,
                          selectionArgs: Array<String>? = null, sortOrder: String? = null): Cursor? {
        val cursor = context.contentResolver.query(
            contentUri, projection, selection, selectionArgs, sortOrder)
        return cursor
    }

    @SuppressLint("Range")
    fun printAll(contentUri: Uri) {
        val cursor = getCursor(contentUri)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Get the column count
                val columnCount = cursor.columnCount
                val row = StringBuilder()

                // Iterate over each column in the row
                for (i in 0 until columnCount) {
                    val columnName = cursor.getColumnName(i)

                    try{
                        val columnValue = cursor.getString(i) ?: "null"
                        if(columnValue != "null") {
                            row.append("$columnName: $columnValue, ")
                        }
                    } catch (_: Exception) {}
                }
                println(row.toString())

            } while (cursor.moveToNext())
        } else {
            println("Cursor is empty or null.")
        }
    }

    @SuppressLint("Range")
    private fun getChannels(): MutableMap<String, Uri> {
        val channels = mutableMapOf<String, Uri>()
        getCursor(channelUri)?.use {
            while (it.moveToNext()) {
                val channelId = it.getLong(it.getColumnIndex(PreviewPrograms._ID))
                val channelName = it.getString(it.getColumnIndex(Channels.COLUMN_DISPLAY_NAME))
                channels[channelName] = ContentUris.withAppendedId(channelUri, channelId)
            }
        }
        return channels
    }

    @SuppressLint("RestrictedApi", "Range")
    private fun getPrograms(channelId: Long): Map<String, Uri> {
        val programs = mutableMapOf<String, Uri>()
        getCursor(programUri)?.use {
            while (it.moveToNext()) {
                val channelIdColumn = it.getLong(it.getColumnIndex(PreviewPrograms.COLUMN_CHANNEL_ID))
                if (channelId == channelIdColumn){
                    val programId = it.getLong(it.getColumnIndex(PreviewPrograms._ID))
                    val programTitle = it.getString(it.getColumnIndex(PreviewPrograms.COLUMN_TITLE))
                    programs[programTitle] = ContentUris.withAppendedId(programUri, programId)
                }
            }
        }
        return programs
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun createOrUpdateChannel(favourites: Map<String, Favourite>) {
        // Step 1: Get the list of existing channels
        val existingChannels = getChannels()

        // Step 2: Delete channels that are no longer in favourites
        for (channelName in existingChannels.keys) {
            if (channelName !in favourites) {
                existingChannels[channelName]?.let { context.contentResolver.delete(it, null, null) }
                Log.d("Channels", "Deleted channel for $channelName")
            }
        }

        // Step 3: Update or create channels based on favourites
        for ((channelName, cards) in favourites) {
            val channelId = updateOrAddChannel(channelName, existingChannels)
            val existingPrograms = getPrograms(channelId)

            // Step 4: Delete programs that are no longer in the card list
            for (programName in existingPrograms.keys) {
                if(programName !in cards.map { it.label }) {
                    existingPrograms[programName]?.let { context.contentResolver.delete(it, null, null) }
                    Log.d("Programs", "Deleted program $programName from channel $channelName")
                }
            }

            // Step 5: Update or create programs based on cards
            for (card in cards) {
                updateOrAddProgram(channelId, card, existingPrograms)
            }

        }
    }

    @SuppressLint("RestrictedApi", "UseCompatLoadingForDrawables")
    private fun updateOrAddChannel(channelName: String, existingChannels: MutableMap<String, Uri>): Long {
        if (channelName !in existingChannels) {
            Log.d("Channels", "Adding new channel ${channelName}.")
            val contentValues = ContentValues().apply {
                put(Channels.COLUMN_DISPLAY_NAME, channelName)
                put(Channels.COLUMN_INPUT_ID, "com.android.exttv/.TvInputService")
                put(Channels.COLUMN_TYPE, Channels.TYPE_PREVIEW)
            }

            val channelId = ContentUris.parseId(context.contentResolver.insert(Channels.CONTENT_URI, contentValues)!!)
            val drawable = context.resources.getDrawable(R.drawable.icon_ch, null)
            val bitmap = drawableToBitmap(drawable)
            storeChannelLogo(context, channelId, bitmap)
            existingChannels[channelName] = ContentUris.withAppendedId(channelUri, channelId)
        } else {
            Log.d("Channels", "Channel ${channelName} exists.")
            // Nothing to do, channel already exists and can't be updated for now
        }
        return ContentUris.parseId(existingChannels[channelName]!!)
    }

    @SuppressLint("RestrictedApi")
    private fun updateOrAddProgram(channelId: Long, card: CardItem, existingPrograms: Map<String, Uri>) {
        if (card.label !in existingPrograms) {
            Log.d("Programs", "Adding new program ${card.label}.")
            context.contentResolver.insert(
                PreviewPrograms.CONTENT_URI,
                programFromCard(channelId, card)
            )
        } else {
            Log.d("Programs", "Program for ${card.label} exists, updating.")
            context.contentResolver.update(
                existingPrograms[card.label]!!,
                programFromCard(channelId, card),
                null,null
            )
        }
    }

    @SuppressLint("RestrictedApi")
    private fun programFromCard(channelId: Long, card: CardItem): ContentValues {
        val posterUrl = if (card.posterUrl.isEmpty()) card.thumbnailUrl else card.posterUrl
        val thumbnailUrl = if (card.thumbnailUrl.isEmpty()) card.posterUrl else card.thumbnailUrl

        val posterFile = File(context.filesDir, posterUrl.substringAfter("/files/"))
        val thumbnailFile = File(context.filesDir, thumbnailUrl.substringAfter("/files/"))

        val thumbnailUri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", thumbnailFile
        )
        val posterUri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", posterFile)

        context.grantUriPermission("com.google.android.tvlauncher", posterUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.grantUriPermission("com.google.android.tvlauncher", thumbnailUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val contentValues = ContentValues().apply {
            put(PreviewPrograms.COLUMN_CHANNEL_ID, channelId)
            put(PreviewPrograms.COLUMN_TYPE, TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
            put(PreviewPrograms.COLUMN_TITLE, card.label)
            put(PreviewPrograms.COLUMN_SHORT_DESCRIPTION, card.label2)
            put(PreviewPrograms.COLUMN_LONG_DESCRIPTION, card.plot)
            put(PreviewPrograms.COLUMN_POSTER_ART_URI, posterUri.toString())
            put(PreviewPrograms.COLUMN_THUMBNAIL_URI, thumbnailUri.toString())
            put(PreviewPrograms.COLUMN_INTENT_URI, "exttv://" + card.uri)
        }
        return contentValues
    }
}
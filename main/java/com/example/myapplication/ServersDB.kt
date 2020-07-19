package com.example.flightmobileapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database (entities = [(Server::class)], version = 1)
abstract class ServersDB : RoomDatabase() {
    //abstract fun serverDao() : ServerDao

    //abstract val serverDao : ServerDao
    abstract fun serverDao() : ServerDao

    companion object {
        @Volatile
        private var INSTANCE : ServersDB? = null
        fun getInstance(context : Context) : ServersDB {
            synchronized(this) {
                var instance = INSTANCE
                if(instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ServersDB::class.java,
                        "serversDB"
                    ).build()
                }
                return instance
            }
        }
    }
}
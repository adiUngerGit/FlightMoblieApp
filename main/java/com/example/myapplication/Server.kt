package com.example.flightmobileapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "serversDB")
class Server {
    @PrimaryKey
    @ColumnInfo (name = "server_url")
    var serverUrl : String = ""

    @ColumnInfo (name = "last_connection")
    var lastConnection : Long = System.currentTimeMillis()
}
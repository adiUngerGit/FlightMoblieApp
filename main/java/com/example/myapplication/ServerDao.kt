package com.example.flightmobileapp

import androidx.room.*


@Dao
interface ServerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateServer(server : Server)


    @Query("select * from serversDB ORDER BY last_connection DESC")
    fun readServer() : List<Server>

    @Update
    fun updateServer(server : Server)

    @Query("DELETE FROM serversDB WHERE server_url = :url")
    fun deleteServerByUrl(url : String)
}
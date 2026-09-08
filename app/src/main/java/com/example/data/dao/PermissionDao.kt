package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RolePermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionDao {
    @Query("SELECT * FROM role_permissions WHERE role = :role")
    fun getPermissionsByRole(role: String): Flow<List<RolePermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<RolePermissionEntity>)

    @Update
    suspend fun updatePermission(permission: RolePermissionEntity)

    @Query("UPDATE role_permissions SET isGranted = :isGranted WHERE id = :id")
    suspend fun updatePermissionStatus(id: String, isGranted: Boolean)
}

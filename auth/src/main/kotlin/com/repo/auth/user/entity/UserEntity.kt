package com.repo.auth.user.entity

import com.repo.auth.user.table.Users
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserEntity(pk: EntityID<Long>): LongEntity(pk) {
    companion object : LongEntityClass<UserEntity>(Users)
    var username by Users.username
    var email by Users.email
    var password by Users.password
    var userRole by Users.userRole
    var status by Users.status
}
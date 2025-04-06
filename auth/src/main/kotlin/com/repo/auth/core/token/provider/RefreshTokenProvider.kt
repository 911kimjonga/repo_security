package com.repo.auth.core.token.provider

import com.repo.auth.core.redis.enums.AuthRedisKeyType.REFRESH
import com.repo.auth.core.redis.service.AuthRedisService
import com.repo.auth.core.token.constants.REFRESH_TOKEN_TTL
import com.repo.auth.user.enums.UserRole
import com.repo.auth.user.service.UserService
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class RefreshTokenProvider(
    private val redisService: AuthRedisService,
    private val accessTokenProvider: AccessTokenProvider,
    private val userService: UserService,
) {

    fun generateRefreshToken(
        userId: String
    ): String {
        val refreshToken = UUID.randomUUID().toString()
        val ttl = REFRESH_TOKEN_TTL

        redisService.save(REFRESH, refreshToken, userId, ttl)

        return refreshToken
    }

    fun reissueAccessToken(
        token: String
    ): String {
        val userId = redisService.get(REFRESH, token)

        val user = userService.findUser(userId.toLong())

        return accessTokenProvider.generateAccessToken(
            user.id,
            UserRole.fromRole(user.userRole)
        )
    }

    fun rotateRefreshToken(
        oldToken: String
    ): String {
        val userId = redisService.get(REFRESH, oldToken)

        val newToken = this.generateRefreshToken(userId)

        redisService.delete(REFRESH, oldToken)

        return newToken
    }

    fun deleteRefreshToken(
        token: String
    ): String {
        val userId = redisService.get(REFRESH, token)

        redisService.delete(REFRESH, token)

        return userId
    }

}
package com.example.buildlearn.data.repo

import com.example.buildlearn.data.db.FavoriteDao
import com.example.buildlearn.data.db.FavoriteEntity
import com.example.buildlearn.data.db.PurchaseDao
import com.example.buildlearn.data.db.PurchaseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商城相关的本地数据：收藏与购买。
 *
 * 购买是占位功能，不接入任何支付渠道，不会产生真实扣款。
 * 购买记录只表示"该账号拥有这门课程的学习权限"。
 */
@Singleton
class StoreRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val purchaseDao: PurchaseDao
) {
    fun observeFavorites(userId: Long): Flow<Set<String>> =
        favoriteDao.observeAll(userId).map { list -> list.map { it.courseId }.toSet() }

    fun observePurchases(userId: Long): Flow<Set<String>> =
        purchaseDao.observeAll(userId).map { list -> list.map { it.courseId }.toSet() }

    /** 切换收藏状态，返回切换后是否为已收藏。 */
    suspend fun toggleFavorite(userId: Long, courseId: String): Boolean {
        return if (favoriteDao.get(userId, courseId) != null) {
            favoriteDao.delete(userId, courseId)
            false
        } else {
            favoriteDao.insert(FavoriteEntity(userId, courseId, System.currentTimeMillis()))
            true
        }
    }

    /** 购买或免费领取。重复调用不会产生重复记录。返回是否为本次新增。 */
    suspend fun purchase(userId: Long, courseId: String): Boolean {
        val inserted = purchaseDao.insertIfAbsent(
            PurchaseEntity(userId, courseId, System.currentTimeMillis())
        )
        return inserted != -1L
    }
}

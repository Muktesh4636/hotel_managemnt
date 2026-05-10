package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.restaurant.management.data.local.OrderWithLines
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertLine(line: OrderLineEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Update
    suspend fun updateLine(line: OrderLineEntity)

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrder(id: Long): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderWithLines(orderId: Long): OrderWithLines?

    @Query("SELECT * FROM order_lines WHERE id = :id")
    suspend fun getLine(id: Long): OrderLineEntity?

    @Query("SELECT * FROM order_lines WHERE orderId = :orderId")
    suspend fun getLinesForOrder(orderId: Long): List<OrderLineEntity>

    @Transaction
    @Query(
        "SELECT * FROM orders WHERE status NOT IN ('PAID', 'CANCELLED') ORDER BY createdAtEpochMillis DESC",
    )
    fun observeOpenOrders(): Flow<List<OrderWithLines>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    suspend fun recentOrders(limit: Int): List<OrderWithLines>

    @Transaction
    @Query(
        "SELECT * FROM orders WHERE createdAtEpochMillis >= :fromMillis AND createdAtEpochMillis < :toMillisExclusive ORDER BY createdAtEpochMillis DESC",
    )
    suspend fun ordersBetween(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): List<OrderWithLines>

    @Query(
        "SELECT IFNULL(SUM(totalCents), 0) FROM orders WHERE createdAtEpochMillis >= :fromMillis AND createdAtEpochMillis < :toMillisExclusive AND status = 'PAID'",
    )
    suspend fun sumPaidRevenueBetween(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): Int

    @Query(
        "SELECT COUNT(*) FROM orders WHERE createdAtEpochMillis >= :fromMillis AND createdAtEpochMillis < :toMillisExclusive AND status = 'PAID'",
    )
    suspend fun countPaidOrdersBetween(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): Int

    @Query(
        "SELECT COUNT(*) FROM orders WHERE createdAtEpochMillis >= :fromMillis AND createdAtEpochMillis < :toMillisExclusive",
    )
    suspend fun countOrdersBetween(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): Int

    @Query(
        "SELECT IFNULL(SUM(totalCents), 0) FROM orders WHERE createdAtEpochMillis >= :from AND status = 'PAID'",
    )
    suspend fun sumPaidRevenueSince(from: Long): Int

    @Query(
        "SELECT COUNT(*) FROM orders WHERE createdAtEpochMillis >= :from AND status = 'PAID'",
    )
    suspend fun countPaidOrdersSince(from: Long): Int

    @Query(
        "SELECT IFNULL(SUM(totalCents), 0) FROM orders WHERE status = 'PAID' AND createdAtEpochMillis >= :from",
    )
    fun observePaidRevenueSince(from: Long): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM orders WHERE status NOT IN ('PAID', 'CANCELLED')",
    )
    fun observeActiveOrderCount(): Flow<Int>
}

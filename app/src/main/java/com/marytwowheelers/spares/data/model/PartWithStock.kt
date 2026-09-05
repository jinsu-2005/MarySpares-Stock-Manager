package com.marytwowheelers.spares.data.model

import androidx.room.Embedded
import com.marytwowheelers.spares.data.local.PartEntity

data class PartWithStock(
    @Embedded val part: PartEntity,
    val currentStock: Int
)

val PartWithStock.stockState: StockState
    get() = when {
        currentStock <= 0 -> StockState.OUT
        currentStock <= 3 -> StockState.LOW
        else -> StockState.HEALTHY
    }

enum class StockState {
    HEALTHY, LOW, OUT
}

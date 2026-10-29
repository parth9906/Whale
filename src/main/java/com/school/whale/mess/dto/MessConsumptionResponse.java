package com.school.whale.mess.dto;

import com.school.whale.inventory.entity.InventoryUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record MessConsumptionResponse(
        UUID itemId,
        String itemCode,
        String itemName,
        InventoryUnit unit,
        BigDecimal quantity
) {}

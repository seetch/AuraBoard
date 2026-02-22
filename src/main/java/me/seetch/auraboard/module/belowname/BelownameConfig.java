package me.seetch.auraboard.module.belowname;

import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;

import java.util.List;

/**
 * @param score             placeholder or number
 * @param displayName       text next to number
 * @param customScoreFormat 1.20.4+ custom format (nullable)
 */
public record BelownameConfig(String id, int priority, List<Condition> conditions, ConditionMode conditionMode,
                              String score, String displayName, String customScoreFormat) {

}
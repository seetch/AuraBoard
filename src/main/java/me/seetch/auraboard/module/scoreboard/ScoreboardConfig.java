package me.seetch.auraboard.module.scoreboard;

import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;

import java.util.List;
import java.util.Map;

public record ScoreboardConfig(String id, int priority, List<String> worlds, List<Condition> conditions,
                               ConditionMode conditionMode, boolean titleAnimation, String titleStatic,
                               List<String> titleFrames, int titleFrameInterval, Map<Integer, List<String>> pages,
                               boolean pagesEnabled, int pagesInterval) {

}
package me.seetch.auraboard.module.nametag;

import me.seetch.auraboard.condition.Condition;
import me.seetch.auraboard.condition.ConditionMode;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * @param visibility ALWAYS | HIDE_FOR_OTHER_TEAMS | NEVER
 */
public record NametagConfig(String id, int priority, List<Condition> conditions, ConditionMode conditionMode,
                            String prefix, String suffix, NamedTextColor playerColor, String visibility) {

}
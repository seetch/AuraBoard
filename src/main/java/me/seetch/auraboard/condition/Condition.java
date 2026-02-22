package me.seetch.auraboard.condition;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record Condition(String placeholder, ConditionOperator operator, String value) {

    public boolean evaluate(String resolved) {
        return operator.evaluate(resolved, value);
    }

    public static List<Condition> loadList(ConfigurationSection section, String key) {
        List<Condition> conditions = new ArrayList<>();
        if (section == null) return conditions;
        List<Map<?, ?>> list = (List<Map<?, ?>>) section.getList(key, new ArrayList<>());
        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> map)) continue;
            String ph = (String) map.get("placeholder");
            String op = (String) map.get("operator");
            String val = (String) map.get("value");
            if (ph == null || op == null || val == null) continue;
            try {
                conditions.add(new Condition(ph, ConditionOperator.valueOf(op.toUpperCase()), val));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return conditions;
    }
}
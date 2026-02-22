package me.seetch.auraboard.module.tab;

import me.seetch.auraboard.condition.Condition;

import java.util.List;
import java.util.Map;

public class TabConfig {

    // Header
    public boolean headerAnimation;
    public int headerFrameInterval;
    public List<String> headerFrames;
    public String headerStatic;

    // Footer
    public boolean footerAnimation;
    public int footerFrameInterval;
    public List<String> footerFrames;
    public String footerStatic;

    // Player format
    public String prefix;
    public String playerName;
    public String suffix;

    // Sorting
    public boolean sortingEnabled;
    public String sortingMode; // LUCKPERMS_WEIGHT | ALPHABETICAL | NONE
    public String secondarySort;
    public Map<String, Integer> groupWeights;
    public int fallbackWeight;

    // Hide conditions
    public List<Condition> hideConditions;
}
# AuraBoard

> A lightweight, high-performance plugin for Minecraft Paper servers (1.20 – 1.21.x)
> that combines a TAB list, nametags, belowname display, and scoreboard into a single solution.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration Structure](#configuration-structure)
- [Scoreboard (Sidebar)](#scoreboard-sidebar)
- [TAB List](#tab-list)
- [Nametag](#nametag)
- [Belowname](#belowname)
- [Condition System](#condition-system)
- [Page System](#page-system)
- [Data Storage](#data-storage)
- [Commands](#commands)
- [Permissions](#permissions)
- [Developer API](#developer-api)
- [Integrations](#integrations)
- [Building from Source](#building-from-source)

---

## Features

| Module | Description |
|---|---|
| **Scoreboard** | Unlimited scoreboards with conditions, animations, and pagination |
| **TAB List** | Animated header/footer, LuckPerms group-based sorting |
| **Nametag** | Prefix/suffix above the player's head, visibility control, per-viewer support |
| **Belowname** | Text below the player's name, custom score format (1.20.4+) |
| **Conditions** | EQUALS, CONTAINS, REGEX, GT, LTE, and other operators |
| **PlaceholderAPI** | Supported in all fields with result caching |
| **Storage** | YAML, SQLite, or MySQL for saving player states |
| **Hot Reload** | `/ab reload` without a server restart |
| **Packet-level** | Fully asynchronous, does not conflict with other plugins |
| **Floodgate** | Bedrock player detection via the built-in `IS_BEDROCK` condition |

---

## Requirements

| Dependency | Type | Version |
|---|---|---|
| Paper / Spigot | Required | 1.20 – 1.21.x |
| PlaceholderAPI | Soft (recommended) | 2.11+ |
| LuckPerms | Soft | 5.4+ |
| Floodgate | Soft | 2.2+ |

> ⚠️ Without PlaceholderAPI, conditions and dynamic strings will not work.

---

## Installation

1. Download `AuraBoard.jar` and place it in the `plugins/` folder
2. Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
3. Install the LuckPerms expansion for PAPI:
   ```
   /papi ecloud download LuckPerms
   /papi reload
   ```
4. Start the server — config files will be generated automatically
5. Configure the files in `plugins/AuraBoard/` to suit your server
6. Apply your changes with `/ab reload`

---

## Configuration Structure

```
plugins/AuraBoard/
├── config.yml        # Global settings, storage, modules
├── scoreboard.yml    # Scoreboards
├── tab.yml           # TAB list
├── nametag.yml       # Nametags
├── belowname.yml     # Belowname display
└── playerdata.yml    # Player data (when storage: YAML)
```

### config.yml — global settings

```yaml
debug: false
update-interval: 20        # ticks (20 = 1 sec)
placeholder-cache-ms: 500  # PlaceholderAPI cache in ms

storage:
  type: YAML               # YAML | SQLITE | MYSQL
  mysql:
    host: localhost
    port: 3306
    database: auraboard
    user: root
    password: ""
    pool-size: 5

modules:
  scoreboard: true
  tab: true
  nametag: true
  belowname: true
```

---

## Scoreboard (Sidebar)

Scoreboards are defined in `scoreboard.yml`. You can create an unlimited number of scoreboards — each with its own priority and activation conditions. The first scoreboard whose conditions are met will be shown to the player.

### Full Example

```yaml
scoreboards:

  vip_scoreboard:
    priority: 100                        # Higher value = higher priority
    worlds: [world, world_nether]        # [] = all worlds
    conditions:
      - placeholder: "%luckperms_primary_group_name%"
        operator: EQUALS
        value: "vip"
    condition-mode: ALL                  # ALL (AND) | ANY (OR)

    title:
      animation: true                    # Animated title
      frames:
        - "<gradient:gold:yellow>⭐ VIP ⭐</gradient>"
        - "<gradient:yellow:gold>⭐ VIP ⭐</gradient>"
      frame-interval: 10                 # Ticks between frames
      # For a static title use:
      # static: "<bold><blue>EpicServer"

    lines:
      - "<gray>Online: <white>%server_online%"
      - ""                               # Empty line
      - "<gold>Group: <white>%luckperms_prefix%"
      - "<aqua>Balance: <white>%vault_balance%"
      - ""
      - "<yellow>epicserver.net"

    pages:
      enabled: true
      interval: 100                      # Ticks between page changes
      # Page 1 = lines (always)
      # Additional pages:
      2:
        - "<gray>Page 2"
        - "<aqua>Statistics:"
        - "<white>Kills: %statistic_player_kills%"
      3:
        - "<gray>Page 3"
        - "<yellow>More information"

  default:
    priority: 0                          # Fallback — shown to everyone
    conditions: []                       # No conditions = always active
    worlds: []
    title:
      animation: false
      static: "<bold><blue>EpicServer"
    lines:
      - "<gray>Online: <white>%server_online%"
      - ""
      - "<aqua>Name: <white>%player_name%"
      - "<aqua>Health: <white>%player_health%"
      - ""
      - "<yellow>epicserver.net"
    pages:
      enabled: false
      interval: 0
```

### How Priority Works

On each update cycle, the plugin evaluates all scoreboards in descending priority order. The first scoreboard whose conditions are satisfied becomes active. A scoreboard with `conditions: []` acts as a fallback — set its `priority: 0`.

---

## TAB List

Configured in `tab.yml`. Supports animated header/footer, a custom player name format, and LuckPerms group-based sorting.

```yaml
header:
  animation: true
  frame-interval: 15
  frames:
    - "\n<bold><gradient:aqua:blue>✦ EpicServer ✦</gradient></bold>\n"
    - "\n<bold><gradient:blue:aqua>✦ EpicServer ✦</gradient></bold>\n"
  # For a static header use:
  # static: "\n<bold>EpicServer\n"

footer:
  animation: false
  static: "\n<gray>Online: <white>%server_online% / %server_max_players%\n<yellow>epicserver.net"

player-format:
  prefix: "&8[%luckperms_prefix%&8] "
  name: "&f%player_name%"
  suffix: ""

sorting:
  enabled: true
  mode: LUCKPERMS_WEIGHT      # LUCKPERMS_WEIGHT | ALPHABETICAL | NONE
  secondary: ALPHABETICAL     # Secondary sort within the same group
  group-weights:              # Override weights from LuckPerms
    owner: 1000
    admin: 900
    moderator: 800
    vip: 500
    default: 0
  fallback-weight: 0          # For players without a group

hide-conditions:              # Hide a player from TAB based on a condition
  - placeholder: "%essentials_vanished%"
    operator: EQUALS
    value: "yes"
```

### TAB Sorting

Sorting is implemented via ScoreboardTeam — Minecraft sorts the TAB list lexicographically by team names. The plugin constructs team names in the format `ab_sort_0000_admin`, where the number is inversely proportional to the group weight. This is native sorting with no hacks required.

---

## Nametag

Configured in `nametag.yml`. Controls the prefix/suffix displayed above a player's head, their name color, and nametag visibility.

```yaml
nametags:

  vanished_tag:
    priority: 500              # Highest priority — checked first
    conditions:
      - placeholder: "%essentials_vanished%"
        operator: EQUALS
        value: "yes"
    prefix: ""
    suffix: ""
    visibility: NEVER          # Completely hide the nametag

  admin_tag:
    priority: 100
    conditions:
      - placeholder: "%luckperms_primary_group_name%"
        operator: EQUALS
        value: "admin"
    prefix: "<red>[ADMIN] "
    suffix: ""
    player-color: RED          # Player name color above their head
    visibility: ALWAYS         # ALWAYS | HIDE_FOR_OTHER_TEAMS | NEVER

  default:
    priority: 0
    conditions: []
    prefix: "%luckperms_prefix%"
    suffix: "%luckperms_suffix%"
    player-color: WHITE
    visibility: ALWAYS
```

---

## Belowname

Configured in `belowname.yml`. Displays text and a number below the player's name — a native Minecraft feature via the `BELOW_NAME` slot. Supports custom score format (1.20.4+).

```yaml
enabled: true
worlds: []                   # [] = all worlds

modes:

  low_health:
    priority: 100
    conditions:
      - placeholder: "%player_health%"
        operator: LTE
        value: "5"
    score: "%player_health_rounded%"          # Numeric value
    display-name: "<red>❤ HP"                 # Text shown beside the number
    custom-score-format: "<red><bold>%player_health_rounded% ❤"  # 1.20.4+

  default:
    priority: 0
    conditions: []
    score: "%player_health_rounded%"
    display-name: "<white>❤ HP"
    custom-score-format: "<green>%player_health_rounded% ❤"

  # Example: show balance instead of health
  # score: "%vault_balance_fixed%"
  # display-name: "<gold>$ Balance"
  # custom-score-format: "<gold>%vault_balance_fixed% $"
```

---

## Condition System

Conditions allow you to apply different configurations based on PlaceholderAPI values.

### Operators

| Operator | Description | Example |
|---|---|---|
| `EQUALS` | Strict equality | `%player_name% == "Steve"` |
| `NOT_EQUALS` | Inequality | `%luckperms_primary_group_name% != "default"` |
| `CONTAINS` | Contains a substring | `%player_world% CONTAINS "nether"` |
| `STARTS_WITH` | Starts with | `%player_name% STARTS_WITH "_"` |
| `ENDS_WITH` | Ends with | `%player_name% ENDS_WITH "_YT"` |
| `REGEX` | Regular expression | `%player_name% REGEX "^Admin.*"` |
| `GT` / `LT` | Greater / less than (numeric) | `%player_health% GT 10` |
| `GTE` / `LTE` | Greater or equal / less or equal | `%vault_balance% GTE 1000` |

### Condition Modes

```yaml
condition-mode: ALL   # All conditions must be met (AND)
condition-mode: ANY   # At least one condition must be met (OR)
```

### Multiple Conditions Example

```yaml
conditions:
  - placeholder: "%luckperms_primary_group_name%"
    operator: EQUALS
    value: "vip"
  - placeholder: "%player_world%"
    operator: CONTAINS
    value: "world"
condition-mode: ALL   # Player is VIP AND is in the "world" world
```

### Built-in IS_BEDROCK Condition

To detect Bedrock players (via Floodgate), a built-in custom condition is available:

```yaml
conditions:
  - placeholder: "IS_BEDROCK"
    operator: EQUALS
    value: "true"
```

---

## Page System

The scoreboard supports pagination. The `lines` field always represents the first page. Additional pages are defined with numeric keys inside `pages`.

```yaml
lines:
  - "&fName: &b%player_name%"
  - "&fGroup: &b%luckperms_prefix%"
  - ""
  - "&fCoins: &b%vault_balance%"

pages:
  enabled: true
  interval: 100          # Ticks between page changes (100 = 5 sec)
  # Page 1 = lines (defined above, automatic)
  2:
    - "&fStatistics"
    - ""
    - "&fKills: &b%statistic_player_kills%"
    - "&fDeaths: &b%statistic_deaths%"
  3:
    - "&fWorld: &b%player_world%"
    - "&fPing: &b%player_ping% ms"
```

---

## Data Storage

The plugin saves player states (scoreboard visibility, TAB, belowname, forced scoreboard).

```yaml
storage:
  type: YAML    # Simple playerdata.yml file — suitable for small servers
  type: SQLITE  # Local auraboard.db database — faster than YAML with many players
  type: MYSQL   # External database — for networks and bungeecord setups
```

### MySQL Configuration

```yaml
storage:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: auraboard
    user: root
    password: "yourpassword"
    pool-size: 5
```

---

## Commands

| Command | Description | Alias |
|---|---|---|
| `/auraboard reload` | Reload all configs without a restart | `/ab reload` |
| `/auraboard debug <player>` | Show active modules for a player | `/ab debug` |
| `/auraboard set scoreboard <player> <id>` | Force a specific scoreboard | `/ab set` |
| `/auraboard clear scoreboard <player>` | Reset to auto mode | `/ab clear` |
| `/auraboard version` | Show version and module status | `/ab version` |
| `/scoreboard toggle` | Hide / show the scoreboard (saved) | — |
| `/tab toggle` | Hide / show header and footer | — |
| `/belowname toggle` | Hide / show the belowname display | — |

### `/ab debug` Output

```
=== Debug: PlayerName ===
Active Scoreboard: default
Forced Scoreboard: null
Active Nametag: admin_tag
Active Belowname: default
Scoreboard Visible: true
TAB Visible: true
Belowname Visible: true
World: world
```

---

## Permissions

| Permission | Description | Default |
|---|---|---|
| `auraboard.reload` | `/ab reload` command | op |
| `auraboard.debug` | `/ab debug` command | op |
| `auraboard.set` | Force-set scoreboard | op |
| `auraboard.bypass.conditions` | Ignore conditions (for testing) | op |
| `auraboard.scoreboard.toggle` | `/scoreboard toggle` command | all players |
| `auraboard.tab.toggle` | `/tab toggle` command | all players |
| `auraboard.belowname.toggle` | `/belowname toggle` command | all players |

---

## Developer API

AuraBoard provides a Java API for other plugins.

```java
AuraBoardAPI api = AuraBoardAPI.get();

// Force a scoreboard (ignores conditions)
api.setScoreboard(player, "vip_scoreboard");

// Reset to auto mode
api.clearForcedScoreboard(player);

// Check scoreboard visibility
boolean visible = api.isScoreboardVisible(player);

// Register a custom condition
api.registerCondition("HAS_PERMISSION", (player, value) ->
    player.hasPermission(value));
```

### Using a Custom Condition in Config

```yaml
conditions:
  - placeholder: "HAS_PERMISSION"
    operator: EQUALS
    value: "myplugin.vip"
```

---

## Integrations

| Plugin | Type | What it provides |
|---|---|---|
| **PlaceholderAPI** | Soft-depend | All placeholders in lines and conditions |
| **LuckPerms** | Soft-depend | TAB sorting, group weights, `%luckperms_*%` |
| **Vault** | Soft-depend | Balance placeholders via PAPI |
| **EssentialsX** | Soft-depend | Vanish conditions, `%essentials_*%` |
| **CMI** | Soft-depend | Alternative to EssentialsX |
| **Floodgate** | Soft-depend | Bedrock player detection (`IS_BEDROCK`) |
| **Folia** | Support | Compatibility via scoreboard-library |

---

## Text Formatting

The plugin supports two formats simultaneously:

**MiniMessage** (recommended):
```
<red>Text</red>
<bold><blue>Bold blue</blue></bold>
<gradient:gold:yellow>Gradient</gradient>
<#FF5500>HEX color</#FF5500>
```

**Legacy** (for compatibility with PAPI placeholders):
```
&cRed text
&l&bBold aqua
&#FF5500HEX via legacy
```

> PAPI placeholders return legacy color codes (`§`). The plugin processes these automatically.

---

## Building from Source

```bash
git clone https://github.com/seetch/AuraBoard.git
cd AuraBoard
mvn package
```

The built JAR will appear at `target/AuraBoard-1.0.0.jar`.

### Dependencies (Maven)

```xml
<dependency>
    <groupId>net.megavex</groupId>
    <artifactId>scoreboard-library-api</artifactId>
    <version>2.6.0</version>
</dependency>
```

---

## License

MIT License — free to use, attribution appreciated.

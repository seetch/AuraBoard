# AuraBoard

> Лёгкий, высокопроизводительный плагин для Minecraft Paper/Spigot серверов (1.20 – 1.21.x),
> объединяющий TAB-лист, нейминтеги, подпись под ником и скорборд в одном решении.

---

## Содержание

- [Возможности](#возможности)
- [Требования](#требования)
- [Установка](#установка)
- [Структура конфигурации](#структура-конфигурации)
- [Scoreboard (Sidebar)](#scoreboard-sidebar)
- [TAB-лист](#tab-лист)
- [Nametag](#nametag)
- [Belowname](#belowname)
- [Система условий](#система-условий)
- [Система страниц](#система-страниц)
- [Хранилище данных](#хранилище-данных)
- [Команды](#команды)
- [Права доступа](#права-доступа)
- [API для разработчиков](#api-для-разработчиков)
- [Интеграции](#интеграции)
- [Сборка из исходников](#сборка-из-исходников)

---

## Возможности

| Модуль | Описание |
|---|---|
| **Scoreboard** | Неограниченное количество скорбордов с условиями, анимацией, пагинацией |
| **TAB-лист** | Header/Footer с анимацией, сортировка по группам LuckPerms |
| **Nametag** | Prefix/suffix над головой, управление видимостью, per-viewer |
| **Belowname** | Подпись под ником, custom score format (1.20.4+) |
| **Условия** | EQUALS, CONTAINS, REGEX, GT, LTE и другие операторы |
| **PlaceholderAPI** | Поддержка во всех полях с кешированием результатов |
| **Хранилище** | YAML, SQLite или MySQL для сохранения состояний игроков |
| **Горячая перезагрузка** | `/ab reload` без рестарта сервера |
| **Packet-level** | Полностью асинхронно, не конфликтует с другими плагинами |
| **Floodgate** | Определение Bedrock-игроков через кастомное условие `IS_BEDROCK` |

---

## Требования

| Зависимость | Тип | Версия |
|---|---|---|
| Paper / Spigot | Обязательно | 1.20 – 1.21.x |
| PlaceholderAPI | Мягкая (рекомендуется) | 2.11+ |
| LuckPerms | Мягкая | 5.4+ |
| Floodgate | Мягкая | 2.2+ |

> ⚠️ Без PlaceholderAPI условия и динамические строки работать не будут.

---

## Установка

1. Скачай `AuraBoard.jar` и помести в папку `plugins/`
2. Установи [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
3. Установи расширение LuckPerms для PAPI:
   ```
   /papi ecloud download LuckPerms
   /papi reload
   ```
4. Запусти сервер — конфиги создадутся автоматически
5. Настрой файлы в `plugins/AuraBoard/` под свой сервер
6. Примени изменения командой `/ab reload`

---

## Структура конфигурации

```
plugins/AuraBoard/
├── config.yml        # Глобальные настройки, хранилище, модули
├── scoreboard.yml    # Скорборды
├── tab.yml           # TAB-лист
├── nametag.yml       # Нейминтеги
├── belowname.yml     # Подпись под ником
└── playerdata.yml    # Данные игроков (при storage: YAML)
```

### config.yml — глобальные настройки

```yaml
debug: false
update-interval: 20        # тиков (20 = 1 сек)
placeholder-cache-ms: 500  # кеш PlaceholderAPI в мс

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

Скорборды определяются в `scoreboard.yml`. Можно создать неограниченное количество скорбордов — каждый имеет приоритет и условия активации. Игроку показывается первый скорборд с выполненными условиями.

### Полный пример

```yaml
scoreboards:

  vip_scoreboard:
    priority: 100                        # Чем выше — тем приоритетнее
    worlds: [world, world_nether]        # [] = все миры
    conditions:
      - placeholder: "%luckperms_primary_group_name%"
        operator: EQUALS
        value: "vip"
    condition-mode: ALL                  # ALL (И) | ANY (ИЛИ)

    title:
      animation: true                    # Анимированный заголовок
      frames:
        - "<gradient:gold:yellow>⭐ VIP ⭐</gradient>"
        - "<gradient:yellow:gold>⭐ VIP ⭐</gradient>"
      frame-interval: 10                 # Тиков между фреймами
      # Для статичного заголовка:
      # static: "<bold><blue>EpicServer"

    lines:
      - "<gray>Онлайн: <white>%server_online%"
      - ""                               # Пустая строка
      - "<gold>Группа: <white>%luckperms_prefix%"
      - "<aqua>Деньги: <white>%vault_balance%"
      - ""
      - "<yellow>epicserver.net"

    pages:
      enabled: true
      interval: 100                      # Тиков между сменой страниц
      # Страница 1 = lines (всегда)
      # Дополнительные страницы:
      2:
        - "<gray>Страница 2"
        - "<aqua>Статистика:"
        - "<white>Убийств: %statistic_player_kills%"
      3:
        - "<gray>Страница 3"
        - "<yellow>Ещё информация"

  default:
    priority: 0                          # Fallback — показывается всем
    conditions: []                       # Нет условий = всегда активен
    worlds: []
    title:
      animation: false
      static: "<bold><blue>EpicServer"
    lines:
      - "<gray>Онлайн: <white>%server_online%"
      - ""
      - "<aqua>Ник: <white>%player_name%"
      - "<aqua>Здоровье: <white>%player_health%"
      - ""
      - "<yellow>epicserver.net"
    pages:
      enabled: false
      interval: 0
```

### Как работает приоритет

При каждом обновлении плагин проверяет все скорборды в порядке убывания приоритета. Первый скорборд с выполненными условиями становится активным. Скорборд с `conditions: []` работает как fallback — ставь ему `priority: 0`.

---

## TAB-лист

Настраивается в `tab.yml`. Поддерживает анимированные Header/Footer, кастомный формат имени игрока и сортировку по группам LuckPerms.

```yaml
header:
  animation: true
  frame-interval: 15
  frames:
    - "\n<bold><gradient:aqua:blue>✦ EpicServer ✦</gradient></bold>\n"
    - "\n<bold><gradient:blue:aqua>✦ EpicServer ✦</gradient></bold>\n"
  # Для статичного header:
  # static: "\n<bold>EpicServer\n"

footer:
  animation: false
  static: "\n<gray>Онлайн: <white>%server_online% / %server_max_players%\n<yellow>epicserver.net"

player-format:
  prefix: "&8[%luckperms_prefix%&8] "
  name: "&f%player_name%"
  suffix: ""

sorting:
  enabled: true
  mode: LUCKPERMS_WEIGHT      # LUCKPERMS_WEIGHT | ALPHABETICAL | NONE
  secondary: ALPHABETICAL     # Вторичная сортировка внутри группы
  group-weights:              # Переопределить вес из LuckPerms
    owner: 1000
    admin: 900
    moderator: 800
    vip: 500
    default: 0
  fallback-weight: 0          # Для игроков без группы

hide-conditions:              # Скрыть игрока из TAB по условию
  - placeholder: "%essentials_vanished%"
    operator: EQUALS
    value: "yes"
```

### Сортировка TAB

Сортировка реализована через ScoreboardTeam — Minecraft сортирует TAB лексикографически по именам команд. Плагин формирует имя команды вида `ab_sort_0000_admin`, где число обратно пропорционально весу группы. Это нативная сортировка без хаков.

---

## Nametag

Настраивается в `nametag.yml`. Управляет prefix/suffix над головой игрока, цветом имени и видимостью нейминтега.

```yaml
nametags:

  vanished_tag:
    priority: 500              # Самый высокий приоритет — проверяется первым
    conditions:
      - placeholder: "%essentials_vanished%"
        operator: EQUALS
        value: "yes"
    prefix: ""
    suffix: ""
    visibility: NEVER          # Скрыть нейминтег полностью

  admin_tag:
    priority: 100
    conditions:
      - placeholder: "%luckperms_primary_group_name%"
        operator: EQUALS
        value: "admin"
    prefix: "<red>[ADMIN] "
    suffix: ""
    player-color: RED          # Цвет имени игрока над головой
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

Настраивается в `belowname.yml`. Отображает текст и число под именем игрока — нативная возможность Minecraft через слот `BELOW_NAME`. Поддерживает custom score format (1.20.4+).

```yaml
enabled: true
worlds: []                   # [] = все миры

modes:

  low_health:
    priority: 100
    conditions:
      - placeholder: "%player_health%"
        operator: LTE
        value: "5"
    score: "%player_health_rounded%"          # Числовое значение
    display-name: "<red>❤ HP"                 # Текст рядом с числом
    custom-score-format: "<red><bold>%player_health_rounded% ❤"  # 1.20.4+

  default:
    priority: 0
    conditions: []
    score: "%player_health_rounded%"
    display-name: "<white>❤ HP"
    custom-score-format: "<green>%player_health_rounded% ❤"

  # Пример: показывать баланс вместо здоровья
  # score: "%vault_balance_fixed%"
  # display-name: "<gold>$ Баланс"
  # custom-score-format: "<gold>%vault_balance_fixed% $"
```

---

## Система условий

Условия позволяют применять разные конфигурации в зависимости от значений PlaceholderAPI.

### Операторы

| Оператор | Описание | Пример |
|---|---|---|
| `EQUALS` | Строгое равенство | `%player_name% == "Steve"` |
| `NOT_EQUALS` | Неравенство | `%luckperms_primary_group_name% != "default"` |
| `CONTAINS` | Содержит подстроку | `%player_world% CONTAINS "nether"` |
| `STARTS_WITH` | Начинается с | `%player_name% STARTS_WITH "_"` |
| `ENDS_WITH` | Заканчивается на | `%player_name% ENDS_WITH "_YT"` |
| `REGEX` | Регулярное выражение | `%player_name% REGEX "^Admin.*"` |
| `GT` / `LT` | Больше / меньше (числа) | `%player_health% GT 10` |
| `GTE` / `LTE` | Больше или равно / меньше или равно | `%vault_balance% GTE 1000` |

### Режимы условий

```yaml
condition-mode: ALL   # Все условия должны выполняться (И)
condition-mode: ANY   # Достаточно одного условия (ИЛИ)
```

### Пример нескольких условий

```yaml
conditions:
  - placeholder: "%luckperms_primary_group_name%"
    operator: EQUALS
    value: "vip"
  - placeholder: "%player_world%"
    operator: CONTAINS
    value: "world"
condition-mode: ALL   # Игрок VIP И находится в мире "world"
```

### Кастомное условие IS_BEDROCK

Для определения Bedrock-игроков (через Floodgate) доступно встроенное кастомное условие:

```yaml
conditions:
  - placeholder: "IS_BEDROCK"
    operator: EQUALS
    value: "true"
```

---

## Система страниц

Скорборд поддерживает пагинацию. Поле `lines` всегда является первой страницей. Дополнительные страницы задаются числовыми ключами внутри `pages`.

```yaml
lines:
  - "&fНик: &b%player_name%"
  - "&fГруппа: &b%luckperms_prefix%"
  - ""
  - "&fМонет: &b%vault_balance%"

pages:
  enabled: true
  interval: 100          # Тиков между сменой страниц (100 = 5 сек)
  # Страница 1 = lines (задаётся выше, автоматически)
  2:
    - "&fСтатистика"
    - ""
    - "&fУбийств: &b%statistic_player_kills%"
    - "&fСмертей: &b%statistic_deaths%"
  3:
    - "&fМир: &b%player_world%"
    - "&fПинг: &b%player_ping% мс"
```

---

## Хранилище данных

Плагин сохраняет состояния игроков (видимость скорборда, TAB, belowname, принудительный скорборд).

```yaml
storage:
  type: YAML    # Простой файл playerdata.yml — подходит для небольших серверов
  type: SQLITE  # Локальная БД auraboard.db — быстрее YAML при большом числе игроков
  type: MYSQL   # Внешняя БД — для сетей и бункерных серверов
```

### MySQL настройка

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

## Команды

| Команда | Описание | Алиас |
|---|---|---|
| `/auraboard reload` | Перезагрузить все конфиги без рестарта | `/ab reload` |
| `/auraboard debug <игрок>` | Показать активные модули для игрока | `/ab debug` |
| `/auraboard set scoreboard <игрок> <id>` | Принудительно установить скорборд | `/ab set` |
| `/auraboard clear scoreboard <игрок>` | Сбросить к авто-режиму | `/ab clear` |
| `/auraboard version` | Версия и статус модулей | `/ab version` |
| `/scoreboard toggle` | Скрыть / показать скорборд (сохраняется) | — |
| `/tab toggle` | Скрыть / показать Header и Footer | — |
| `/belowname toggle` | Скрыть / показать подпись под ником | — |

### Вывод `/ab debug`

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

## Права доступа

| Permission | Описание | По умолчанию |
|---|---|---|
| `auraboard.reload` | Команда `/ab reload` | op |
| `auraboard.debug` | Команда `/ab debug` | op |
| `auraboard.set` | Принудительная установка скорборда | op |
| `auraboard.bypass.conditions` | Игнорировать условия (для тестирования) | op |
| `auraboard.scoreboard.toggle` | Команда `/scoreboard toggle` | все игроки |
| `auraboard.tab.toggle` | Команда `/tab toggle` | все игроки |
| `auraboard.belowname.toggle` | Команда `/belowname toggle` | все игроки |

---

## API для разработчиков

AuraBoard предоставляет Java API для других плагинов.

```java
AuraBoardAPI api = AuraBoardAPI.get();

// Принудительно установить скорборд (игнорирует условия)
api.setScoreboard(player, "vip_scoreboard");

// Сбросить к авто-режиму
api.clearForcedScoreboard(player);

// Проверить видимость скорборда
boolean visible = api.isScoreboardVisible(player);

// Зарегистрировать кастомное условие
api.registerCondition("HAS_PERMISSION", (player, value) ->
    player.hasPermission(value));
```

### Использование кастомного условия в конфиге

```yaml
conditions:
  - placeholder: "HAS_PERMISSION"
    operator: EQUALS
    value: "myplugin.vip"
```

---

## Интеграции

| Плагин | Тип | Что даёт |
|---|---|---|
| **PlaceholderAPI** | Soft-depend | Все плейсхолдеры в строках и условиях |
| **LuckPerms** | Soft-depend | Сортировка TAB, вес групп, `%luckperms_*%` |
| **Vault** | Soft-depend | Плейсхолдеры баланса через PAPI |
| **EssentialsX** | Soft-depend | Vanish-условия, `%essentials_*%` |
| **CMI** | Soft-depend | Альтернатива EssentialsX |
| **Floodgate** | Soft-depend | Определение Bedrock-игроков (`IS_BEDROCK`) |
| **Folia** | Поддержка | Совместимость через scoreboard-library |

---

## Форматирование текста

Плагин поддерживает два формата одновременно:

**MiniMessage** (рекомендуется):
```
<red>Текст</red>
<bold><blue>Жирный синий</blue></bold>
<gradient:gold:yellow>Градиент</gradient>
<#FF5500>HEX цвет</#FF5500>
```

**Legacy** (для совместимости с PAPI-плейсхолдерами):
```
&cКрасный текст
&l&bЖирный голубой
&#FF5500HEX через legacy
```

> Плейсхолдеры PAPI возвращают legacy-коды (`§`). Плагин автоматически их обрабатывает.

---

## Сборка из исходников

```bash
git clone https://github.com/seetch/AuraBoard.git
cd AuraBoard
mvn package
```

Готовый JAR появится в `target/AuraBoard-1.0.0.jar`.

### Зависимости (Maven)

```xml
<dependency>
    <groupId>net.megavex</groupId>
    <artifactId>scoreboard-library-api</artifactId>
    <version>2.6.0</version>
</dependency>
```

---

## Лицензия

MIT License — используй свободно, упоминание автора приветствуется.

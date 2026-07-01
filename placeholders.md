# SiDaStuff SMP - PlaceholderAPI Placeholders

## `%sidastuffsmp_<placeholder>%`

These placeholders are registered under the `sidastuffsmp` identifier.

### Player Stats
| Placeholder | Description |
|---|---|
| `%sidastuffsmp_kills%` | Player's kill count |
| `%sidastuffsmp_deaths%` | Player's death count |
| `%sidastuffsmp_kdr%` | Player's kill/death ratio (e.g. `2.5`) |
| `%sidastuffsmp_playtime%` | Player's total playtime in seconds |
| `%sidastuffsmp_playtime_formatted%` | Player's playtime formatted (e.g. `5d 3h 20m 10s`) |
| `%sidastuffsmp_rtp_count%` | Number of times player has used RTP |
| `%sidastuffsmp_punish_count%` | Player's punishment escalation count |
| `%sidastuffsmp_warn_count%` | Number of active warnings |

### Auction House (Per-Player)
| Placeholder | Description |
|---|---|
| `%sidastuffsmp_auction_mine%` | Player's active auction listing count |
| `%sidastuffsmp_auction_mine_sold%` | Player's uncollected sold auction count |
| `%sidastuffsmp_auction_mine_expired%` | Player's uncollected expired auction count |

### Auction House (Global)
| Placeholder | Description |
|---|---|
| `%sidastuffsmp_auction_active%` | Total active auction listings |
| `%sidastuffsmp_auction_total%` | Total auctions ever created |
| `%sidastuffsmp_auction_sold%` | Total auctions sold |
| `%sidastuffsmp_auction_expired%` | Total auctions expired |

### Orders (Per-Player)
| Placeholder | Description |
|---|---|
| `%sidastuffsmp_orders_active%` | Player's active buy order count |
| `%sidastuffsmp_orders_stash%` | Player's uncollected order stash item count |

### Orders (Global)
| Placeholder | Description |
|---|---|
| `%sidastuffsmp_orders_market_active%` | Total active buy orders on the market |
| `%sidastuffsmp_orders_market_total%` | Total orders ever created |

---

## `%economy_<placeholder>%`

These placeholders are registered under the `economy` identifier.

### Player Balance
| Placeholder | Description |
|---|---|
| `%economy_balance%` | Player's raw balance (e.g. `1234.56`) |
| `%economy_balance_formatted%` | Player's balance with commas (e.g. `1,234.56`) |
| `%economy_balance_formatted_symbol%` | Player's balance with currency symbol (e.g. `$1,234.56`) |
| `%economy_rank%` | Player's balance rank position (1 = richest) |
| `%economy_has_account%` | Whether player has an economy account (`true`/`false`) |

### Currency Info
| Placeholder | Description |
|---|---|
| `%economy_currency_name%` | Currency singular name (e.g. `Dollar`) |
| `%economy_currency_name_plural%` | Currency plural name (e.g. `Dollars`) |
| `%economy_currency_symbol%` | Currency symbol (e.g. `$`) |

### Top Balance Leaderboard
| Placeholder | Description |
|---|---|
| `%economy_top_<n>%` | Name of the player at rank `n` (e.g. `%economy_top_1%`) |
| `%economy_top_<n>_balance%` | Raw balance of the player at rank `n` |
| `%economy_top_<n>_balance_formatted%` | Formatted balance with symbol of the player at rank `n` |

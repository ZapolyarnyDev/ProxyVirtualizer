# ProxyVirtualizer

🧩 ProxyVirtualizer is a Velocity plugin that allows you to create and manage virtual Minecraft servers directly inside the proxy without running backend instances.

## ✨ Features

- 🚀 Start / stop virtual servers
- 👤 Connect a player to a virtual server (automatically disconnecting them from their backend server)
- 📦 Send basic packets to players via commands (`chat`, `title`, `actionbar`, `keepalive`, `disconnect`)
- 🌌 Limbo bootstrap is currently implemented for 1.21.4

## ❓ Why ProxyVirtualizer?

- Reduce backend load
- Handle authentication or filtering in-proxy
- Create limbo/void servers without extra processes
- Full control over client state

## ▶️ Quick Start

1. Build the plugin:
```bash
./gradlew build
```
2. Put the JAR from `plugin/build/libs/` into your Velocity `plugins/` folder
3. Restart the proxy

## 🕹️ Main Commands

> Required permission: `proxyvirtualizer.command`

- `/vserver help` - show command list
- `/vserver launch <name>` - create server
- `/vserver list` - virtual servers list
- `/vserver connect <name> [player]` - connect a player to a virtual server
- `/vserver disconnect [player]` - return a player from a virtual server
- `/vserver stop <name>` - stop a virtual server

## 🌌 Limbo Example (1.21.4)

```text
/vserver launch limbo
/vserver connect limbo <player>
```

## 📬 Packets (Manual Send)

- `/vserver packet limbo <server>`
- `/vserver packet keepalive <server>`
- `/vserver packet chat <server> <message>`
- `/vserver packet actionbar <server> <message>`
- `/vserver packet title <server> <title[||subtitle]>`
- `/vserver packet disconnect <server> [reason]`

Supported message formats:
- `mm:<...>` (MiniMessage)
- `legacy:&a...`
- `json:{...}`

## License

&copy; 2025 ZapolyarnyDev

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details

## Sudden Cat in README!
<img src="docs/sudden-cat.jpg" width=320>
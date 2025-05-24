[![License: GPL](https://img.shields.io/badge/license-GPL-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/628396916639793152.svg?color=%237289da&label=discord)](https://shantek.co/discord)
[![CodeFactor](https://www.codefactor.io/repository/github/shantek/postoffice/badge)](https://www.codefactor.io/repository/github/shantek/postoffice)
[![Modrinth](https://img.shields.io/badge/Modrinth-Post%20Office-green?logo=modrinth)](https://modrinth.com/plugin/postoffice)


<img src="https://www.shantek.io/wp-content/uploads/2024/09/Banner.png" alt="Post Office Plugin Banner" />

# 📮 Post Office Plugin

**A fully functional post box system for Bukkit-based Minecraft servers!**  
Turn barrels into secure player mailboxes with claims, protection, and notifications.

> 🔄 **Version 2 is a full rewrite.** You’ll need to re-register existing post boxes.  
> Just remove the player’s name from the sign and follow the steps below.

### 🔧 [Get the latest development builds here](https://github.com/shantek/PostOffice/releases/)

---

## ✉️ How It Works

1. Upload the JAR to your server and restart.
2. Edit the `config.yml` if needed (optional).
3. Place barrels in your post office build.
4. Attach a blank sign to the front of each barrel.
5. While looking at the sign, type `/postoffice register`.
6. Players can use `/postoffice claim` to claim a registered box.
7. Admins can claim on behalf of others with `/postoffice claim <playername>`.

> 📦 Anyone can deliver items into a post box, but only the **owner** can retrieve them.  
> 🔐 Post boxes are protected from tampering unless permissions override this.  
> You can disable built-in protection and use WorldGuard or other systems instead.

---

## 🎥 YouTube Tutorial

[![YouTube](http://i.ytimg.com/vi/skb3oYxVzfg/hqdefault.jpg)](https://www.youtube.com/watch?v=skb3oYxVzfg)

---

## 🛠️ Commands

| Command | Description |
|--------|-------------|
| `/postoffice register` | Register a barrel + sign as a post box. |
| `/postoffice claim` | Claim a registered post box. |
| `/postoffice claim <player>` | Admin command to claim on behalf of a player. |
| `/postoffice remove` | Remove a post box (while looking at it). |
| `/postoffice info` | Show info about the post box you’re looking at. |

---

## 🔐 Permissions

| Node | Description |
|------|-------------|
| `shantek.postoffice.use` | Default true. Deny this to ban a player from using post boxes. |
| `shantek.postoffice.removeitems` | Allow access to remove items from any post box. |
| `shantek.postoffice.register` | Register or remove post boxes. |
| `shantek.postoffice.claim` | Claim a post box. |
| `shantek.postoffice.claim.others` | Claim for another player (admin/mod use). |
| `shantek.postoffice.updatenotification` | Receive plugin update notifications. |

---

## 📦 Downloads

Available on:
- [Spigot](https://www.spigotmc.org/resources/post-office.108343/)
- [Modrinth](https://modrinth.com/plugin/postoffice)
- [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/post-office)

---

## 💬 Community & Support

- 🛠 [Report Bugs or Make Suggestions](https://github.com/shantek/PostOffice/issues)
- 💬 [Join the Discord](https://shantek.co/discord)
- ❤️ [Support via Patreon](https://shantek.co/patreon)
- ☕ [Or via PayPal](https://www.paypal.com/donate/?hosted_button_id=9N3RCSJF6PYPU)

---

## 📄 License

Distributed under the **GNU General Public License v3.0**.  
See the [`LICENSE`](LICENSE) file for details.

---

![Plugin Usage Stats](https://bstats.org/signatures/bukkit/Post%20Office.svg)

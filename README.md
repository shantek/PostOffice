[![License: GPL](https://img.shields.io/badge/license-GPL-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/628396916639793152.svg?color=%237289da&label=discord)](https://shantek.co/discord)

<img src="https://www.shantek.io/wp-content/uploads/2024/09/Banner.png" alt="Post Office Plugin" />

#### Looking for the latest dev builds? You can find them [here!](https://shantek.dev/job/PostOffice/)

### Post Office is a fully functional Minecraft Post Office plugin for your Bukkit based Minecraft servers.

Show your support for the Plugin via [PayPal](https://www.paypal.com/donate/?hosted_button_id=9N3RCSJF6PYPU) or [Patreon](https://shantek.co/patreon).

With built-in post box protection, you can rest easy knowing your post boxes are safe on your Minecraft SMP.

Players are unable to break a post box or sign unless they have the correct permission node allowing them to do so.

Available now on [Modrinth](https://modrinth.com/plugin/postoffice), [CurgeForge](https://www.curseforge.com/minecraft/bukkit-plugins/post-office/) and [Spigot](https://www.spigotmc.org/resources/post-office.108343/).

## How does the plugin work?

- Upload the JAR to your server and reboot
- Make any desired changes to the config file (not essential), and then type /postoffice reload' as an op.
- Place your barrels in your amazing Post Office build.
- Place a blank sign on the front of the barrel.
- While looking at the sign, type '/postoffice register' to register the barrel in the config file.
- Again while looking at the sign, if permitted your players can type '/postoffice claim' on a registered post box to claim it.
- Admin/mods can look at a sign and use '/postoffice claim playername' to claim a post box on behalf of another player.
- Enjoy your awesome post office on your SMP!

![Plugin Usage Stats](https://bstats.org/signatures/bukkit/Post%20Office.svg)

## Commands
Below are the commands used to manage the post office barrels and plugin:

### /postoffice register
Place a barrel and a sign on the front. Run this command while looking at the sign to register the post box in the config.

### /postoffice claim
If players have the claim permission, this command will allow them to claim an already registered post box.

### /postoffice claim playername
Used for an admin/mod to claim a post box on behalf of another player.

### /postoffice remove
Run while looking at a registered or claimed post box to remove the owner and the post box from the config.

### /postoffice info
Run while looking at a post box barrel to get information about the registration state/owner.


## Permissions
The below permissions are intended for giving your mods extra access and abilities within your post office. By default, players will be able to access their own post box without any additional permission nodes being granted.

### shantek.postoffice.use
This permission prevents a player from using/interacting with the post office. All players have this by default, so use this to deny access to any players you wish to ban from the post box system.

### shantek.postoffice.removeitems
Allow these players to remove items from any post box.

### shantek.postoffice.register
Allow a player to register/remove a post box in the config.

### shantek.postoffice.claim
Allow a player to claim their own post box.

### shantek.postoffice.claim.others
Allow a player to claim a post box for other players (generally used by admin/mods).

### shantek.postoffice.updatenotification
Any player with this permission will be notified if there is an update to the plugin.

## External Links

[Support via Patreon](https://shantek.co/patreon)

[Discord](https://shantek.co/discord)

[Report bugs/make suggestions](https://github.com/shantek/PostOffice/issues)

### License
Distributed under the GNU General Public License v3.0.

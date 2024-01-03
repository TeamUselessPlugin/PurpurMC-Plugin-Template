package io.github.teamuselessplugin.punishment.invfx

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.invfx.InvFX.frame
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import io.github.teamuselessplugin.punishment.Main
import io.github.teamuselessplugin.punishment.events.BlockEvents
import io.github.teamuselessplugin.punishment.protocol.GlowPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import litebans.api.Database
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture

class PunishmentGUI {
    private val errorByServer = Component.text("§c데이터를 가져올 수 없습니다. 관리자에게 문의해주세요.")
    private val errorByPlayer = Component.text("§c플레이어 데이터를 가져올 수 없습니다.")
    private val errorIsOfflinePlayer = Component.text("§c오프라인 플레이어입니다.")
    enum class PunishmentType {
        BAN, KICK, MUTE, WARN
    }
    enum class PluginType {
        LITEBANS, VANILLA
    }
    fun main(sender: Player, targetUUID: UUID?): InvFrame? {
        val playerOffline: OfflinePlayer? = targetUUID?.let { sender.server.getOfflinePlayer(it) }
        val isValid = playerOffline?.hasPlayedBefore()
        val isOnline = playerOffline?.isOnline
        var playerOnline: Player? = null
        if (isOnline!!) {
            playerOnline = playerOffline.player
        }

        if (isValid!!) {
            try {
                var isBanned = false
                var bannedCount = 0L
                var kickedCount = 0L
                var mutedCount = 0L
                var warnedCount = 0L
                var latestPunishmentReason = "없음"
                var latestPunishmentDate = 0L
                var latestPunishmentType = "없음"
                var latestPunishmentEndTime = 0L
                var latestPunishmentActive = false

                val a: InvFrame?
                // LiteBan이 활성화 되어있을 경우
                if (Main.liteBans_enable) {
                    a = frame(6, Component.text("Punishment GUI")) {
                        CompletableFuture.runAsync {
                            // LiteBans이 활성화 되어있을 경우에만 데이터를 가져옴
                            isBanned = Database.get().isPlayerBanned(playerOffline.uniqueId, null)
                            try {
                                Database.get().prepareStatement("SELECT COUNT(*) FROM {bans} WHERE uuid=?").also {
                                    it.setString(1, playerOffline.uniqueId.toString())
                                    it.executeQuery().also { rs ->
                                        if (rs.next()) {
                                            bannedCount = rs.getLong(1)
                                        }
                                        rs.close()
                                    }
                                    it.close()
                                }

                                Database.get().prepareStatement("SELECT COUNT(*) FROM {kicks} WHERE uuid=?").also {
                                    it.setString(1, playerOffline.uniqueId.toString())
                                    it.executeQuery().also { rs ->
                                        if (rs.next()) {
                                            kickedCount = rs.getLong(1)
                                        }
                                        rs.close()
                                    }
                                    it.close()
                                }

                                Database.get().prepareStatement("SELECT COUNT(*) FROM {mutes} WHERE uuid=?").also {
                                    it.setString(1, playerOffline.uniqueId.toString())
                                    it.executeQuery().also { rs ->
                                        if (rs.next()) {
                                            mutedCount = rs.getLong(1)
                                        }
                                        rs.close()
                                    }
                                    it.close()
                                }

                                Database.get().prepareStatement("SELECT COUNT(*) FROM {warnings} WHERE uuid=?").also {
                                    it.setString(1, playerOffline.uniqueId.toString())
                                    it.executeQuery().also { rs ->
                                        if (rs.next()) {
                                            warnedCount = rs.getLong(1)
                                        }
                                        rs.close()
                                    }
                                    it.close()
                                }

                                // Get All Punishment Data (Bans, Kicks, Mutes, Warnings) and Get Latest Punishment Data
                                if (bannedCount > 0) {
                                    Database.get().prepareStatement("SELECT * FROM {bans} WHERE uuid=? ORDER BY id DESC LIMIT 1").also {
                                        it.setString(1, playerOffline.uniqueId.toString())
                                        it.executeQuery().also { rs ->
                                            if (rs.next()) {
                                                if (latestPunishmentDate < rs.getLong("time")) {
                                                    latestPunishmentReason = rs.getString("reason")
                                                    latestPunishmentDate = rs.getLong("time")
                                                    latestPunishmentType = "차단"
                                                    latestPunishmentEndTime = rs.getLong("until")
                                                    latestPunishmentActive = rs.getBoolean("active")
                                                }
                                            }
                                            rs.close()
                                        }
                                        it.close()
                                    }
                                }

                                if (kickedCount > 0) {
                                    Database.get().prepareStatement("SELECT * FROM {kicks} WHERE uuid=? ORDER BY id DESC LIMIT 1").also {
                                        it.setString(1, playerOffline.uniqueId.toString())
                                        it.executeQuery().also { rs ->
                                            if (rs.next()) {
                                                if (latestPunishmentDate < rs.getLong("time")) {
                                                    latestPunishmentReason = rs.getString("reason")
                                                    latestPunishmentDate = rs.getLong("time")
                                                    latestPunishmentType = "킥"
                                                    latestPunishmentEndTime = 0L
                                                    latestPunishmentActive = false
                                                }
                                            }
                                            rs.close()
                                        }
                                        it.close()
                                    }
                                }

                                if (mutedCount > 0) {
                                    Database.get().prepareStatement("SELECT * FROM {mutes} WHERE uuid=? ORDER BY id DESC LIMIT 1").also {
                                        it.setString(1, playerOffline.uniqueId.toString())
                                        it.executeQuery().also { rs ->
                                            if (rs.next()) {
                                                if (latestPunishmentDate < rs.getLong("time")) {
                                                    latestPunishmentReason = rs.getString("reason")
                                                    latestPunishmentDate = rs.getLong("time")
                                                    latestPunishmentType = "뮤트"
                                                    latestPunishmentEndTime = rs.getLong("until")
                                                    latestPunishmentActive = rs.getBoolean("active")
                                                }
                                            }
                                            rs.close()
                                        }
                                        it.close()
                                    }
                                }

                                if (warnedCount > 0) {
                                    Database.get().prepareStatement("SELECT * FROM {warnings} WHERE uuid=? ORDER BY id DESC LIMIT 1").also {
                                        it.setString(1, playerOffline.uniqueId.toString())
                                        it.executeQuery().also { rs ->
                                            if (rs.next()) {
                                                if (latestPunishmentDate < rs.getLong("time")) {
                                                    latestPunishmentReason = rs.getString("reason")
                                                    latestPunishmentDate = rs.getLong("time")
                                                    latestPunishmentType = "경고"
                                                    latestPunishmentEndTime = 0L
                                                    latestPunishmentActive = rs.getBoolean("active")
                                                }
                                            }
                                            rs.close()
                                        }
                                        it.close()
                                    }
                                }
                            } catch (e: SQLException) {
                                sender.sendMessage(errorByServer)
                                e.printStackTrace()
                            }
                        }.thenRun {
                            // Player Head
                            slot(3, 0) {
                                item = ItemStack(Material.PLAYER_HEAD).apply {

                                    itemMeta = itemMeta.apply {
                                        (this as SkullMeta).owningPlayer = playerOffline

                                        if (!isBanned && isOnline) {
                                            displayName(
                                                Component.text("${playerOffline.name} [${playerOffline.uniqueId}]")
                                                    .color(TextColor.color(Color.WHITE.asRGB()))
                                            )

                                        } else if (isBanned) {
                                            displayName(
                                                Component.text("${playerOffline.name} [${playerOffline.uniqueId}]")
                                                    .decorate(TextDecoration.STRIKETHROUGH)
                                                    .color(TextColor.color(Color.GRAY.asRGB()))
                                            )

                                            lore(
                                                listOf(
                                                    Component.text("§c이미 서버에서 차단된 플레이어입니다.")
                                                )
                                            )
                                        } else {
                                            displayName(
                                                Component.text("${playerOffline.name} [${playerOffline.uniqueId}]")
                                                    .color(TextColor.color(Color.GRAY.asRGB()))
                                            )

                                            lore(
                                                listOf(
                                                    Component.text("§c오프라인 플레이어입니다.")
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Player Punishment Data
                            slot(5, 0) {
                                item = ItemStack(Material.PAPER).apply {
                                    itemMeta = itemMeta.apply {
                                        val timeFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초")

                                        val time = Timestamp(latestPunishmentDate)
                                        val until = Timestamp(latestPunishmentEndTime)

                                        val loreData = mutableListOf<Component>(
                                            Component.text(""),
                                            Component.text("§7차단 횟수: §c${bannedCount}회"),
                                            Component.text("§7추방 횟수: §c${kickedCount}회"),
                                            Component.text("§7뮤트 횟수: §c${mutedCount}회"),
                                            Component.text("§7경고 횟수: §c${warnedCount}회"),
                                            Component.text(""),
                                            Component.text("§7최근 처벌: §c${latestPunishmentType}"),
                                            Component.text("§7처벌 이유: §c${latestPunishmentReason}"),
                                            Component.text("§7처벌 날짜: §c${timeFormat.format(time)}"),
                                            Component.text("§7처벌 종료: §c${timeFormat.format(until)}"),
                                            Component.text("§7처벌 상태: §c${if (latestPunishmentActive) "활성화" else "비활성화"}")
                                        )

                                        if (latestPunishmentEndTime == 0L) {
                                            loreData[9] = Component.text("§7처벌 종료: §c영구")
                                        }

                                        if (latestPunishmentType == "없음") {
                                            loreData.removeAt(10)
                                            loreData.removeAt(9)
                                            loreData.removeAt(8)
                                            loreData.removeAt(7)
                                            loreData.removeAt(6)
                                            loreData.removeAt(5)
                                        }

                                        displayName(Component.text("${playerOffline.name}님의 처벌 기록"))

                                        lore(loreData)
                                    }
                                }
                            }

                            /* Admin Buttons */
                            // Ban Button
                            slot(1, 2) {
                                item = ItemStack(Material.BARRIER).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c차단"))
                                        lore(listOf(Component.text("§7플레이어를 서버에서 차단합니다.")))
                                    }
                                }

                                onClick {
                                    sender.openFrame(template(sender, targetUUID, PunishmentType.BAN, PluginType.LITEBANS)!!)
                                }
                            }

                            // Kick Button
                            slot(3, 2) {
                                item = ItemStack(Material.IRON_DOOR).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c추방"))
                                        lore(listOf(Component.text("§7플레이어를 서버에서 추방합니다.")))
                                    }
                                }

                                onClick {
                                    sender.openFrame(template(sender, targetUUID, PunishmentType.KICK, PluginType.LITEBANS)!!)
                                }
                            }

                            // Mute Button
                            slot(5, 2) {
                                item = ItemStack(Material.BOOK).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c뮤트"))
                                        lore(listOf(Component.text("§7플레이어를 서버에서 뮤트합니다.")))
                                    }
                                }

                                onClick {
                                    sender.openFrame(template(sender, targetUUID, PunishmentType.MUTE, PluginType.LITEBANS)!!)
                                }
                            }

                            // Warn Button
                            slot(7, 2) {
                                item = ItemStack(Material.PAPER).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c경고"))
                                        lore(listOf(Component.text("§7플레이어에게 경고를 부여합니다.")))
                                    }
                                }

                                onClick {
                                    sender.openFrame(template(sender, targetUUID, PunishmentType.WARN, PluginType.LITEBANS)!!)
                                }
                            }

                            // Tracking Player Button
                            slot(1, 4) {
                                if (BlockEvents.tracking[playerOffline.uniqueId] == true) {
                                    item = ItemStack(Material.COMPASS).apply {
                                        itemMeta = itemMeta.apply {
                                            displayName(Component.text("§c플레이어 추적 §a(설정됨)"))
                                            lore(listOf(Component.text("§7플레이어를 추적합니다.")))
                                            addEnchant(Enchantment.DURABILITY, 1, true)
                                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                                        }
                                    }
                                } else {
                                    item = ItemStack(Material.COMPASS).apply {
                                        itemMeta = itemMeta.apply {
                                            displayName(Component.text("§c플레이어 추적"))
                                            lore(listOf(Component.text("§7플레이어를 추적합니다.")))
                                        }
                                    }
                                }

                                onClick {
                                    if (isOnline) {
                                        BlockEvents.tracking[sender.uniqueId] = true
                                        sender.sendMessage(Component.text("§a${playerOffline.name}님에 대한 추적이 설정되었습니다."))
                                        sender.sendMessage(Component.text("§a추적을 종료하려면 ")
                                            .append(Component.text("/추적종료")
                                                .clickEvent(ClickEvent.runCommand("/추적종료"))
                                                .hoverEvent(HoverEvent.showText(Component.text("§7클릭하여 추적을 종료합니다."))))
                                            .append(Component.text("§a를 입력하세요.")))

                                        BlockEvents.oldLoc[sender.uniqueId] = sender.location
                                        BlockEvents.oldGameMode[sender.uniqueId] = sender.gameMode
                                        BlockEvents.trackingPlayer[sender.uniqueId] = playerOffline.uniqueId

                                        sender.gameMode = GameMode.SPECTATOR
                                        sender.spectatorTarget = playerOnline
                                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                        sender.closeInventory()

                                        HeartbeatScope().launch {
                                            val glow = GlowPlayer(playerOnline).apply { addWatcher(sender) }
                                            while (BlockEvents.tracking[sender.uniqueId] == true) {
                                                if (sender.location.distance(playerOnline?.location!!) > Main.conf?.getInt("trackingDistance")!!) {
                                                    sender.teleport(playerOnline.location)
                                                }
                                                glow.show()
                                                sender.sendActionBar(Component.text("§f${playerOffline.name}님의 위치: X : §c${playerOnline.location.blockX}§f, Y : §c${playerOnline.location.blockY}§f, Z : §c${playerOnline.location.blockZ} §f[거리 : §c${Math.round(sender.location.distance(playerOnline.location))}m§f]"))
                                                delay(50L)
                                            }
                                            glow.hide()
                                        }
                                    } else {
                                        sender.sendMessage(errorIsOfflinePlayer)
                                    }
                                }
                            }

                            // Block Movement Button
                            slot(3, 4) {
                                if (BlockEvents.blocker[playerOffline.uniqueId] == true) {
                                    item = ItemStack(Material.STRUCTURE_VOID).apply {
                                        itemMeta = itemMeta.apply {
                                            displayName(Component.text("§c이동 제한 & 상호작용 제한 §a(설정됨)"))
                                            lore(listOf(Component.text("§7플레이어의 이동과 상호작용을 제한합니다.")))
                                            addEnchant(Enchantment.DURABILITY, 1, true)
                                            addItemFlags(ItemFlag.HIDE_ENCHANTS)
                                        }
                                    }
                                } else {
                                    item = ItemStack(Material.STRUCTURE_VOID).apply {
                                        itemMeta = itemMeta.apply {
                                            displayName(Component.text("§c이동 제한 & 상호작용 제한"))
                                            lore(listOf(Component.text("§7플레이어의 이동과 상호작용을 제한합니다.")))
                                        }
                                    }
                                }

                                onClick {
                                    if (isOnline) {
                                        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                        if (BlockEvents.blocker[playerOffline.uniqueId] == true) {
                                            BlockEvents.blocker[playerOffline.uniqueId] = false
                                            sender.sendMessage(Component.text("§a${playerOffline.name}님의 이동 제한 상태가 해제되었습니다."))
                                            sender.openFrame(main(sender, targetUUID)!!)
                                        } else {
                                            BlockEvents.blocker[playerOffline.uniqueId] = true
                                            sender.sendMessage(Component.text("§a${playerOffline.name}님의 이동 제한 상태가 설정되었습니다."))
                                            sender.openFrame(main(sender, targetUUID)!!)
                                        }
                                    } else {
                                        sender.sendMessage(errorIsOfflinePlayer)
                                    }
                                }
                            }

                            // Inventory See Button
                            slot(5, 4) {
                                item = ItemStack(Material.CHEST).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c인벤토리 열람 §7(미구현)"))
                                        lore(listOf(Component.text("§7플레이어의 인벤토리를 열람합니다.")))
                                    }
                                }

                                onClick {
                                    // TODO: Inventory
                                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.1f)
                                    sender.sendMessage(Component.text("§c아직 구현되지 않았습니다."))
                                }
                            }

                            // EnderChest See Button
                            slot(7, 4) {
                                item = ItemStack(Material.ENDER_CHEST).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c엔더 상자 열람 §7(미구현)"))
                                        lore(listOf(Component.text("§7플레이어의 엔더 상자를 열람합니다.")))
                                    }
                                }

                                onClick {
                                    // TODO: EnderChest
                                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.1f)
                                    sender.sendMessage(Component.text("§c아직 구현되지 않았습니다."))
                                }
                            }
                            /* Admin Buttons */
                        }
                    }
                } else {
                    // LiteBan이 비활성화 되어있을 경우
                    a = frame(6, Component.text("Punishment GUI")) {
                        isBanned = playerOffline.isBanned

                        // Player Head
                        slot(4, 0) {
                            item = ItemStack(Material.PLAYER_HEAD).apply {

                                itemMeta = itemMeta.apply {
                                    (this as SkullMeta).owningPlayer = playerOffline

                                    if (!isBanned && isOnline) {
                                        displayName(
                                            Component.text("${playerOffline.name} [${playerOffline.uniqueId}]")
                                                .color(TextColor.color(Color.WHITE.asRGB()))
                                        )

                                    } else if (isBanned) {
                                        displayName(
                                            Component.text("${playerOffline.name} [${playerOffline.uniqueId}]")
                                                .decorate(TextDecoration.STRIKETHROUGH)
                                                .color(TextColor.color(Color.GRAY.asRGB()))
                                        )

                                        lore(
                                            listOf(
                                                Component.text("§c이미 서버에서 차단된 플레이어입니다.")
                                            )
                                        )
                                    } else {
                                        displayName(
                                            Component.text("${playerOffline.name} [${playerOffline.uniqueId}]")
                                                .color(TextColor.color(Color.GRAY.asRGB()))
                                        )

                                        lore(
                                            listOf(
                                                Component.text("§c오프라인 플레이어입니다.")
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        /* Admin Buttons */
                        // Ban / Unban Button
                        slot(2, 2) {
                            if (!isBanned) {
                                item = ItemStack(Material.BARRIER).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c차단"))
                                        lore(listOf(Component.text("§7플레이어를 서버에서 차단합니다.")))
                                    }
                                }

                                onClick {
                                    sender.openFrame(template(sender, targetUUID, PunishmentType.BAN, PluginType.VANILLA)!!)
                                }
                            } else {
                                item = ItemStack(Material.BARRIER).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c차단 해제"))
                                        lore(listOf(Component.text("§7플레이어의 차단을 해제합니다.")))
                                        addEnchant(Enchantment.DURABILITY, 1, true)
                                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                                    }
                                }

                                onClick {
                                    sender.performCommand("minecraft:pardon ${playerOffline.name}")
                                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                    sender.sendMessage(Component.text("§a${playerOffline.name}님의 차단이 해제되었습니다."))
                                    sender.openFrame(main(sender, targetUUID)!!)
                                }
                            }
                        }

                        // Kick Button
                        slot(6, 2) {
                            item = ItemStack(Material.IRON_DOOR).apply {
                                itemMeta = itemMeta.apply {
                                    displayName(Component.text("§c추방"))
                                    lore(listOf(Component.text("§7플레이어를 서버에서 추방합니다.")))
                                }
                            }

                            onClick {
                                sender.openFrame(template(sender, targetUUID, PunishmentType.KICK, PluginType.VANILLA)!!)
                            }
                        }

                        // Tracking Player Button
                        slot(1, 4) {
                            if (BlockEvents.tracking[playerOffline.uniqueId] == true) {
                                item = ItemStack(Material.COMPASS).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c플레이어 추적 §a(설정됨)"))
                                        lore(listOf(Component.text("§7플레이어를 추적합니다.")))
                                        addEnchant(Enchantment.DURABILITY, 1, true)
                                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                                    }
                                }
                            } else {
                                item = ItemStack(Material.COMPASS).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c플레이어 추적"))
                                        lore(listOf(Component.text("§7플레이어를 추적합니다.")))
                                    }
                                }
                            }

                            onClick {
                                if (isOnline) {
                                    BlockEvents.tracking[sender.uniqueId] = true
                                    sender.sendMessage(Component.text("§a${playerOffline.name}님에 대한 추적이 설정되었습니다."))
                                    sender.sendMessage(Component.text("§a추적을 종료하려면 ")
                                        .append(Component.text("/추적종료")
                                            .clickEvent(ClickEvent.runCommand("/추적종료"))
                                            .hoverEvent(HoverEvent.showText(Component.text("§7클릭하여 추적을 종료합니다."))))
                                        .append(Component.text("§a를 입력하세요.")))

                                    BlockEvents.oldLoc[sender.uniqueId] = sender.location
                                    BlockEvents.oldGameMode[sender.uniqueId] = sender.gameMode
                                    BlockEvents.trackingPlayer[sender.uniqueId] = playerOffline.uniqueId

                                    sender.gameMode = GameMode.SPECTATOR
                                    sender.spectatorTarget = playerOnline
                                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                    sender.closeInventory()

                                    HeartbeatScope().launch {
                                        val glow = GlowPlayer(playerOnline).apply { addWatcher(sender) }
                                        while (BlockEvents.tracking[sender.uniqueId] == true) {
                                            if (sender.location.distance(playerOnline?.location!!) > Main.conf?.getInt("trackingDistance")!!) {
                                                sender.teleport(playerOnline.location)
                                            }
                                            glow.show()
                                            sender.sendActionBar(Component.text("§f${playerOffline.name}님의 위치: X : §c${playerOnline.location.blockX}§f, Y : §c${playerOnline.location.blockY}§f, Z : §c${playerOnline.location.blockZ} §f[거리 : §c${Math.round(sender.location.distance(playerOnline.location))}m§f]"))
                                            delay(50L)
                                        }
                                        glow.hide()
                                    }
                                } else {
                                    sender.sendMessage(errorIsOfflinePlayer)
                                }
                            }
                        }

                        // Block Movement Button
                        slot(3, 4) {
                            if (BlockEvents.blocker[playerOffline.uniqueId] == true) {
                                item = ItemStack(Material.STRUCTURE_VOID).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c이동 제한 & 상호작용 제한 §a(설정됨)"))
                                        lore(listOf(Component.text("§7플레이어의 이동과 상호작용을 제한합니다.")))
                                        addEnchant(Enchantment.DURABILITY, 1, true)
                                        addItemFlags(ItemFlag.HIDE_ENCHANTS)
                                    }
                                }
                            } else {
                                item = ItemStack(Material.STRUCTURE_VOID).apply {
                                    itemMeta = itemMeta.apply {
                                        displayName(Component.text("§c이동 제한 & 상호작용 제한"))
                                        lore(listOf(Component.text("§7플레이어의 이동과 상호작용을 제한합니다.")))
                                    }
                                }
                            }

                            onClick {
                                if (isOnline) {
                                    sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
                                    if (BlockEvents.blocker[playerOffline.uniqueId] == true) {
                                        BlockEvents.blocker[playerOffline.uniqueId] = false
                                        sender.sendMessage(Component.text("§a${playerOffline.name}님의 이동 제한 상태가 해제되었습니다."))
                                        sender.openFrame(main(sender, targetUUID)!!)
                                    } else {
                                        BlockEvents.blocker[playerOffline.uniqueId] = true
                                        sender.sendMessage(Component.text("§a${playerOffline.name}님의 이동 제한 상태가 설정되었습니다."))
                                        sender.openFrame(main(sender, targetUUID)!!)
                                    }
                                } else {
                                    sender.sendMessage(errorIsOfflinePlayer)
                                }
                            }
                        }

                        // Inventory See Button
                        slot(5, 4) {
                            item = ItemStack(Material.CHEST).apply {
                                itemMeta = itemMeta.apply {
                                    displayName(Component.text("§c인벤토리 열람 §7(미구현)"))
                                    lore(listOf(Component.text("§7플레이어의 인벤토리를 열람합니다.")))
                                }
                            }

                            onClick {
                                // TODO: Inventory
                                sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.1f)
                                sender.sendMessage(Component.text("§c아직 구현되지 않았습니다."))
                            }
                        }

                        // EnderChest See Button
                        slot(7, 4) {
                            item = ItemStack(Material.ENDER_CHEST).apply {
                                itemMeta = itemMeta.apply {
                                    displayName(Component.text("§c엔더 상자 열람 §7(미구현)"))
                                    lore(listOf(Component.text("§7플레이어의 엔더 상자를 열람합니다.")))
                                }
                            }

                            onClick {
                                // TODO: EnderChest
                                sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.1f)
                                sender.sendMessage(Component.text("§c아직 구현되지 않았습니다."))
                            }
                        }
                        /* Admin Buttons */
                    }
                }
                return a
            } catch (e: IllegalStateException) {
                sender.sendMessage(errorByServer)
                e.printStackTrace()
            }
            return null
        } else {
            sender.sendMessage(errorByPlayer)
            return null
        }
    }

    private fun template(sender: Player, targetUUID: UUID, punishmentType: PunishmentType, pluginType: PluginType): InvFrame? {
        val playerOffline: OfflinePlayer = targetUUID.let { sender.server.getOfflinePlayer(it) }
        val isOnline = playerOffline.isOnline
        var playerOnline: Player? = null
        if (isOnline) {
            playerOnline = playerOffline.player
        }

        when(pluginType) {
            PluginType.LITEBANS -> {
                when(punishmentType) {
                    PunishmentType.BAN -> TODO()
                    PunishmentType.KICK -> TODO()
                    PunishmentType.MUTE -> TODO()
                    PunishmentType.WARN -> TODO()
                }
            }
            PluginType.VANILLA -> {
                when(punishmentType) {
                    PunishmentType.BAN -> TODO()
                    PunishmentType.KICK -> TODO()
                    else -> {
                        sender.sendMessage(Component.text("§c해당 기능은 LiteBans플러그인이 있을 때만 사용할 수 있습니다."))
                        return null
                    }
                }
            }
        }
        return null
    }
}
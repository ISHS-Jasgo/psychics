package com.github.jasgo.psychics.hook

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.AbilityType
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.damage.psychicDamage
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.math.normalizeAndLength
import com.github.noonmaru.tap.trail.trail
import org.bukkit.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

@Name("hook")
class HookConcept : AbilityConcept() {

    @Config
    var raySize = 0.2

    init {
        type = AbilityType.ACTIVE
        displayName = "후크"
        range = 100.0
        description = listOf(
            "화살을 좌클릭하여 적에게 갈고리를 발사해 끌어옵니다",
            "적에게 적중시 3초동안 공격력이 증가합니다",
            "화살을 우클릭 하여 블럭에 갈고리를 발사합니다",
            "블럭에 적중시 해당 블럭을 향해 도약하고 속도가 증가합니다"
        )
        val item = ItemStack(Material.ARROW)
        val meta: ItemMeta = item.itemMeta
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b$displayName"))
        item.itemMeta = meta
        wand = item
        supplyItems = listOf(item)
        cooldownTicks = 50
    }
}

class Hook : Ability<HookConcept>(), Listener {

    override fun onEnable() {
        psychic.registerEvents(this@Hook)

    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action == Action.PHYSICAL) return
        if (event.item == concept.wand && event.item?.let { event.player.getCooldown(it.type) } == 0) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                val loc = esper.player.eyeLocation
                val direction = loc.direction
                loc.subtract(direction)
                val to = processRayTrace(loc, direction)
                if (to != null) {
                    spawnParticles(loc, to)
                };
            } else {
                val loc = esper.player.eyeLocation
                val direction = loc.direction
                loc.subtract(direction)
                val to = processRayTraceBlock(loc, direction)
                if (to != null) {
                    spawnParticles(loc, to)
                };
            }
            esper.player.setCooldown(event.material, concept.cooldownTicks.toInt());
        }
    }

    private fun processRayTrace(from: Location, direction: Vector): Location? {
        esper.player.world.rayTrace(
            from,
            direction,
            concept.range,
            FluidCollisionMode.NEVER,
            true,
            concept.raySize,
            TargetFilter(esper.player)
        )?.let { rayTraceResult ->
            val to = rayTraceResult.hitPosition.toLocation(from.world)
            rayTraceResult.hitEntity?.let { target ->
                if (target is LivingEntity) {
                    target.teleport(esper.player.location.add(esper.player.eyeLocation.direction.normalize()));
                    esper.player.addPotionEffect(PotionEffect(PotionEffectType.INCREASE_DAMAGE, 60, 1, true, false));
                }
            }
            return to
        }
        return null
    }

    private fun processRayTraceBlock(from: Location, direction: Vector): Location? {
        esper.player.world.rayTrace(
            from,
            direction,
            concept.range,
            FluidCollisionMode.NEVER,
            true,
            concept.raySize,
            TargetFilter(esper.player)
        )?.let { rayTraceResult ->
            var to = rayTraceResult.hitPosition.toLocation(from.world)
            rayTraceResult.hitBlock?.let { block ->
                if (!block.isEmpty && !block.isLiquid) {
                    to = rayTraceResult.hitBlock?.location!!
                    esper.player.velocity = esper.player.eyeLocation.direction.multiply(3);
                    esper.player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 60, 2, true, false));
                }
            }
            return to
        }
        return null
    }
    private fun spawnParticles(from: Location, to: Location) {
        trail(from, to, 0.25) { w, x, y, z ->
            w.spawnParticle(
                Particle.CRIT,
                x, y, z,
                1,
                0.05, 0.05, 0.05,
                0.0,
                null,
                true
            )
        }
    }
}
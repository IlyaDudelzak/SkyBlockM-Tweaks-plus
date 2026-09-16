package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SmoothTargetBot {
    private static Entity targetEntity = null;
    private static boolean shouldClickWhenFacing = false;
    private static float speed = 0.18f; // Скорость доводки (0.0 - моментально неподвижно, 1.0 - мгновенный флик)
    private static final float AIM_THRESHOLD = 3.0f; // Угол в градусах, при котором цель считается "наведенной"

    private static final java.util.Queue<Entity> targetQueue = new java.util.LinkedList<>();
    private static int delayTicksRemaining = 0;
    private static int delayBetweenTargets = 10; // Задержка между целями в тиках (~0.5 сек)
    private static Runnable onFinishedCallback = null;

    // Регистрируйте этот метод один раз при инициализации мода (в ClientModInitializer)
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Обработка задержки между целями
            if (delayTicksRemaining > 0) {
                delayTicksRemaining--;
                return;
            }

            // Если текущей цели нет, берем следующую из очереди
            if (targetEntity == null) {
                if (!targetQueue.isEmpty()) {
                    targetEntity = targetQueue.poll();
                    shouldClickWhenFacing = true;
                } else if (onFinishedCallback != null) {
                    Runnable cb = onFinishedCallback;
                    onFinishedCallback = null;
                    cb.run();
                    return;
                } else {
                    return;
                }
            }

            // Проверяем, жива ли еще цель
            if (!targetEntity.isAlive() || targetEntity.isRemoved()) {
                targetEntity = null;
                return;
            }

            // 1. Вычисляем целевые углы
            ClientPlayerEntity player = client.player;
            Vec3d eyePos = player.getEyePos();
            // Нацеливаемся в центр хитбокса сущности (высота / 2)
            Vec3d targetPos = new Vec3d(targetEntity.getX(), targetEntity.getY() + targetEntity.getHeight() / 2.0, targetEntity.getZ());

            double diffX = targetPos.x - eyePos.x;
            double diffY = targetPos.y - eyePos.y;
            double diffZ = targetPos.z - eyePos.z;
            double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

            float targetYaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
            float targetPitch = MathHelper.wrapDegrees((float) (-Math.toDegrees(Math.atan2(diffY, diffXZ))));

            // 2. Плавно интерполируем углы (LERP)
            float currentYaw = player.getYaw();
            float currentPitch = player.getPitch();

            // Интерполяция Yaw с учетом перехода через границу -180/180 градусов
            float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
            float nextYaw = currentYaw + deltaYaw * speed;

            // Интерполяция Pitch
            float deltaPitch = targetPitch - currentPitch;
            float nextPitch = currentPitch + deltaPitch * speed;

            // Устанавливаем новые углы игроку
            player.setYaw(nextYaw);
            player.setPitch(nextPitch);

            // 3. Логика клика при достижении цели
            if (shouldClickWhenFacing) {
                // Если разница между взглядом и целью минимальна — стреляем/бьем
                if (Math.abs(deltaYaw) < AIM_THRESHOLD && Math.abs(deltaPitch) < AIM_THRESHOLD) {
                    executePlayerClick(client);
                    shouldClickWhenFacing = false; // Сбрасываем триггер клика
                    targetEntity = null; // Завершили текущую цель
                    delayTicksRemaining = delayBetweenTargets; // Пауза перед следующей целью
                }
            }
        });
    }

    /**
     * Запускает очередь плавной наводки и кликов по списку сущностей.
     */
    public static void queueTargets(java.util.List<? extends Entity> entities, float interpolationSpeed, int delayTicks, Runnable onComplete) {
        reset();
        if (entities != null) {
            targetQueue.addAll(entities);
        }
        speed = MathHelper.clamp(interpolationSpeed, 0.01f, 1.0f);
        delayBetweenTargets = Math.max(0, delayTicks);
        onFinishedCallback = onComplete;
    }

    /**
     * Запускает процесс плавной наводки на одну сущность и совершает клик по достижении цели.
     *
     * @param entity Целевая сущность
     * @param interpolationSpeed Скорость наведения (оптимально 0.1 - 0.3)
     */
    public static void startAimAndClick(Entity entity, float interpolationSpeed) {
        reset();
        targetEntity = entity;
        shouldClickWhenFacing = true;
        speed = MathHelper.clamp(interpolationSpeed, 0.01f, 1.0f);
    }

    /**
     * Полная имитация нажатия игроком левой кнопки мыши (ЛКМ).
     * Соблюдает ванильные правила: бьет объект перед собой (даже если это стена или другая сущность).
     */
    private static void executePlayerClick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        // Заставляем руку игрока качнуться визуально и для сервера
        client.player.swingHand(Hand.MAIN_HAND);

        // crosshairTarget — это то, на что РЕАЛЬНО сейчас смотрит игрок (с учетом всех блоков и хитбоксов перед ним)
        HitResult hitResult = client.crosshairTarget;

        if (hitResult == null) return;

        switch (hitResult.getType()) {
            case ENTITY:
                // Если перед игроком оказалась сущность (целевая или любая, загородившая ее)
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                break;

            case BLOCK:
                // If a wall is in front of the target, the player will hit the block instead
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                break;

            case MISS:
                // Клик по воздуху
                break;
        }
    }

    public static void reset() {
        targetEntity = null;
        targetQueue.clear();
        shouldClickWhenFacing = false;
        delayTicksRemaining = 0;
        onFinishedCallback = null;
    }

    public static boolean isBusy() {
        return targetEntity != null || !targetQueue.isEmpty();
    }
}
package despairscent.skyblockm.tweaks.features.updater;

import despairscent.skyblockm.tweaks.ModUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class UpdatePromptScreen extends Screen {

    private enum State {
        IDLE,
        DOWNLOADING,
        COMPLETED,
        ERROR
    }

    private final Screen parent;
    private final UpdateInfo update;

    private State state = State.IDLE;
    private float downloadProgress = 0.0f;
    private long downloadedBytes = 0;
    private long totalBytes = 0;
    private String errorMessage = "";

    private ButtonWidget btnDownload;
    private ButtonWidget btnOpenGitHub;
    private ButtonWidget btnLater;
    private ButtonWidget btnSkip;

    private ButtonWidget btnRestart;
    private ButtonWidget btnDone;

    private final List<String> changelogLines = new ArrayList<>();

    public UpdatePromptScreen(Screen parent, UpdateInfo update) {
        super(Text.literal("SkyBlockM Tweaks+ Обновление"));
        this.parent = parent;
        this.update = update;
        this.totalBytes = update.assetSize;

        // Разделяем описание релиза на строки для отображения
        if (update.body != null && !update.body.isEmpty()) {
            String[] lines = update.body.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    changelogLines.add(trimmed);
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int boxWidth = 340;
        int boxHeight = 220;
        int boxLeft = (this.width - boxWidth) / 2;
        int boxTop = (this.height - boxHeight) / 2;

        int btnWidth = 145;
        int btnHeight = 20;

        // Кнопки для состояния IDLE / DOWNLOADING
        btnDownload = this.addDrawableChild(ButtonWidget.builder(Text.literal("Скачать и установить"), btn -> startDownload())
                .dimensions(boxLeft + 20, boxTop + boxHeight - 56, btnWidth, btnHeight)
                .build());

        btnOpenGitHub = this.addDrawableChild(ButtonWidget.builder(Text.literal("Открыть GitHub"), btn -> openGitHub())
                .dimensions(boxLeft + boxWidth - 20 - btnWidth, boxTop + boxHeight - 56, btnWidth, btnHeight)
                .build());

        btnLater = this.addDrawableChild(ButtonWidget.builder(Text.literal("Позже"), btn -> this.close())
                .dimensions(boxLeft + 20, boxTop + boxHeight - 30, btnWidth, btnHeight)
                .build());

        btnSkip = this.addDrawableChild(ButtonWidget.builder(Text.literal("Пропустить версию"), btn -> skipVersion())
                .dimensions(boxLeft + boxWidth - 20 - btnWidth, boxTop + boxHeight - 30, btnWidth, btnHeight)
                .build());

        // Кнопки для состояния COMPLETED
        btnRestart = this.addDrawableChild(ButtonWidget.builder(Text.literal("Перезапустить игру"), btn -> restartGame())
                .dimensions(boxLeft + 20, boxTop + boxHeight - 38, btnWidth, btnHeight)
                .build());

        btnDone = this.addDrawableChild(ButtonWidget.builder(Text.literal("Готово (закрыть)"), btn -> this.close())
                .dimensions(boxLeft + boxWidth - 20 - btnWidth, boxTop + boxHeight - 38, btnWidth, btnHeight)
                .build());

        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        boolean idleOrDownloading = (state == State.IDLE || state == State.DOWNLOADING);
        btnDownload.visible = idleOrDownloading;
        btnOpenGitHub.visible = idleOrDownloading;
        btnLater.visible = idleOrDownloading;
        btnSkip.visible = idleOrDownloading;

        boolean isCompleted = (state == State.COMPLETED);
        btnRestart.visible = isCompleted;
        btnDone.visible = isCompleted;

        if (state == State.DOWNLOADING) {
            btnDownload.active = false;
            btnLater.active = false;
            btnSkip.active = false;
        } else if (state == State.ERROR) {
            btnDownload.visible = true;
            btnDownload.active = true;
            btnLater.visible = true;
            btnLater.active = true;
        }
    }

    private void startDownload() {
        state = State.DOWNLOADING;
        updateButtonVisibility();

        ModDownloader.downloadAndInstall(update,
                (percent, downloaded, total) -> {
                    this.downloadProgress = percent;
                    this.downloadedBytes = downloaded;
                    this.totalBytes = total;
                },
                () -> {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            this.state = State.COMPLETED;
                            updateButtonVisibility();
                        });
                    }
                },
                (error) -> {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            this.state = State.ERROR;
                            this.errorMessage = error;
                            updateButtonVisibility();
                        });
                    }
                }
        );
    }

    private void openGitHub() {
        try {
            net.minecraft.util.Util.getOperatingSystem().open(new URI(update.htmlUrl));
        } catch (Throwable t) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(new URI(update.htmlUrl));
                }
            } catch (Throwable ignored) {}
        }
    }

    private void skipVersion() {
        if (ModUtils.CONFIG != null && ModUtils.CONFIG.updater != null) {
            ModUtils.CONFIG.updater.ignoredVersion = update.version;
            ModUtils.CONFIG.save();
        }
        this.close();
    }

    private void restartGame() {
        if (this.client != null) {
            this.client.scheduleStop();
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнение всего экрана
        context.fill(0, 0, this.width, this.height, 0xC0101010);

        int boxWidth = 340;
        int boxHeight = 220;
        int boxLeft = (this.width - boxWidth) / 2;
        int boxTop = (this.height - boxHeight) / 2;

        // Фон карточки диалога
        context.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, 0xEE1E1E1E);

        // Рамка карточки
        int borderColor = update.isNightly ? 0xFFFFAA00 : 0xFF55FF55;
        context.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + 1, borderColor);
        context.fill(boxLeft, boxTop + boxHeight - 1, boxLeft + boxWidth, boxTop + boxHeight, borderColor);
        context.fill(boxLeft, boxTop, boxLeft + 1, boxTop + boxHeight, borderColor);
        context.fill(boxLeft + boxWidth - 1, boxTop, boxLeft + boxWidth, boxTop + boxHeight, borderColor);

        // Заголовок
        String badge = update.isNightly ? "§e[Dev / Nightly]" : "§a[Релиз]";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6§lSkyBlockM Tweaks+ " + badge), boxLeft + boxWidth / 2, boxTop + 12, 0xFFFFFFFF);

        // Версии
        String currentVer = UpdateChecker.getCurrentModVersion();
        String verInfo = String.format("§7Текущая: §f%s §7➔ Новая: §a%s", currentVer, update.version);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(verInfo), boxLeft + boxWidth / 2, boxTop + 27, 0xFFFFFFFF);

        // Разделительная линия
        context.fill(boxLeft + 15, boxTop + 40, boxLeft + boxWidth - 15, boxTop + 41, 0xFF444444);

        if (state == State.IDLE) {
            // Список изменений (до 5 строк)
            context.drawText(this.textRenderer, Text.literal("§eЧто нового:"), boxLeft + 20, boxTop + 47, 0xFFFFFFFF, false);
            int lineY = boxTop + 60;
            int maxLines = 4;
            int count = 0;
            for (String line : changelogLines) {
                if (count >= maxLines) {
                    context.drawText(this.textRenderer, Text.literal("§8...и другие изменения"), boxLeft + 20, lineY, 0xFF888888, false);
                    break;
                }
                String truncated = this.textRenderer.trimToWidth(line, boxWidth - 40);
                context.drawText(this.textRenderer, Text.literal("§7" + truncated), boxLeft + 20, lineY, 0xFFCCCCCC, false);
                lineY += 12;
                count++;
            }

            // Размер файла
            if (update.assetSize > 0) {
                float sizeMb = (float) update.assetSize / (1024 * 1024);
                context.drawText(this.textRenderer, Text.literal(String.format("§8Размер файла: %.1f МБ", sizeMb)), boxLeft + 20, boxTop + boxHeight - 74, 0xFF888888, false);
            }

        } else if (state == State.DOWNLOADING) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eЗагрузка обновления..."), boxLeft + boxWidth / 2, boxTop + 65, 0xFFFFFFFF);

            // Прогресс бар
            int barX = boxLeft + 20;
            int barY = boxTop + 85;
            int barW = boxWidth - 40;
            int barH = 14;

            context.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
            int filled = (int) (barW * Math.min(1.0f, Math.max(0.0f, downloadProgress)));
            context.fill(barX, barY, barX + filled, barY + barH, 0xFF55FF55);

            String progressText = String.format("%.0f%% (%.1f / %.1f МБ)",
                    downloadProgress * 100,
                    (float) downloadedBytes / (1024 * 1024),
                    (float) totalBytes / (1024 * 1024));
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(progressText), boxLeft + boxWidth / 2, barY + 3, 0xFFFFFFFF);

            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Пожалуйста, подождите..."), boxLeft + boxWidth / 2, boxTop + 110, 0xFF888888);

        } else if (state == State.COMPLETED) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§a✔ Обновление успешно установлено!"), boxLeft + boxWidth / 2, boxTop + 65, 0xFF55FF55);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§fНовый jar сохранён в папку mods."), boxLeft + boxWidth / 2, boxTop + 85, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Для применения изменений перезапустите игру."), boxLeft + boxWidth / 2, boxTop + 105, 0xFFAAAAAA);

        } else if (state == State.ERROR) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§c✖ Ошибка загрузки обновления"), boxLeft + boxWidth / 2, boxTop + 65, 0xFFFF5555);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7" + errorMessage), boxLeft + boxWidth / 2, boxTop + 85, 0xFFCCCCCC);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8Вы можете скачать файл вручную через кнопку «Открыть GitHub»"), boxLeft + boxWidth / 2, boxTop + 105, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}

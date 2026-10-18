package despairscent.skyblockm.tweaks.features.updater;

public class UpdateInfo {
    public final String version;
    public final String releaseName;
    public final String tagName;
    public final String body;
    public final String assetName;
    public final String downloadUrl;
    public final long assetSize;
    public final boolean isNightly;
    public final String publishedAt;
    public final String htmlUrl;

    public UpdateInfo(String version, String releaseName, String tagName, String body,
                      String assetName, String downloadUrl, long assetSize,
                      boolean isNightly, String publishedAt, String htmlUrl) {
        this.version = version;
        this.releaseName = releaseName;
        this.tagName = tagName;
        this.body = body != null ? body : "";
        this.assetName = assetName;
        this.downloadUrl = downloadUrl;
        this.assetSize = assetSize;
        this.isNightly = isNightly;
        this.publishedAt = publishedAt != null ? publishedAt : "";
        this.htmlUrl = htmlUrl != null ? htmlUrl : "https://github.com/IlyaDudelzak/SkyBlockM-Tweaks-plus/releases";
    }
}

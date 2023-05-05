package info.deconinck.inclinometer.util;

public class AppPermission {
    private int minBuildVersion;
    private String permission;
    private String text;
    private boolean isMandatory;

    public AppPermission(int minBuildVersion, String permission, String text, boolean isMandatory) {
        this.minBuildVersion = minBuildVersion;
        this.permission = permission;
        this.text = text;
        this.isMandatory = isMandatory;
    }

    public int getMinBuildVersion() {
        return minBuildVersion;
    }

    public String getPermission() {
        return permission;
    }

    public String getText() {
        return text;
    }

    public boolean isMandatory() {
        return isMandatory;
    }
}

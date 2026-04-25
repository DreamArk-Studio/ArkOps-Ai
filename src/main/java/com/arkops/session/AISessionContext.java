package com.arkops.session;

import java.util.Objects;

/**
 * AI 会话权限上下文 - 不可变对象，线程安全
 * 用于在 AI 调用链中传递用户身份和权限信息
 */
public final class AISessionContext {

    private final String qqUserId;
    private final String permissionLevel;
    private final String displayName;

    public AISessionContext(String qqUserId, String permissionLevel, String displayName) {
        this.qqUserId = qqUserId;
        this.permissionLevel = permissionLevel;
        this.displayName = displayName;
    }

    public String getQqUserId() {
        return qqUserId;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isQQUser() {
        return qqUserId != null && !qqUserId.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AISessionContext)) return false;
        AISessionContext that = (AISessionContext) o;
        return Objects.equals(qqUserId, that.qqUserId) &&
               Objects.equals(permissionLevel, that.permissionLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qqUserId, permissionLevel);
    }

    @Override
    public String toString() {
        return "AISessionContext{qqUserId='" + qqUserId + "', displayName='" + displayName + "', permissionLevel='" + permissionLevel + "'}";
    }

    public static AISessionContext console() {
        return new AISessionContext(null, "CONSOLE", "CONSOLE");
    }

    public static AISessionContext player(String displayName, String permissionLevel) {
        return new AISessionContext(null, permissionLevel, displayName);
    }

    public static AISessionContext qqUser(String qqUserId, String displayName, String permissionLevel) {
        return new AISessionContext(qqUserId, permissionLevel, displayName);
    }
}

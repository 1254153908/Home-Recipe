package org.huhu.recipe.auth.config;

/**
 * 请求级用户上下文，ThreadLocal 保证线程隔离。
 * 请求结束后必须 clear()，防止线程池复用导致内存泄漏和脏数据。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}

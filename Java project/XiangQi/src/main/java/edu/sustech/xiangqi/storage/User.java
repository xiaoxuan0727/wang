package edu.sustech.xiangqi.storage;

/**
 * 登录用户。游客不是注册用户，不能保存或读取存档。
 *
 * <p>密码不保存明文，只保存随机盐和加盐哈希。</p>
 */
public final class User {
    private final String username;
    private final String saltBase64;
    private final String passwordHashBase64;
    private final boolean guest;

    public User(String username, String saltBase64, String passwordHashBase64) {
        this(username, saltBase64, passwordHashBase64, false);
    }

    private User(String username, String saltBase64, String passwordHashBase64, boolean guest) {
        this.username = username;
        this.saltBase64 = saltBase64;
        this.passwordHashBase64 = passwordHashBase64;
        this.guest = guest;
    }

    public static User guest() {
        return new User("游客", "", "", true);
    }

    public String getUsername() {
        return username;
    }

    public String getSaltBase64() {
        return saltBase64;
    }

    public String getPasswordHashBase64() {
        return passwordHashBase64;
    }

    public boolean isGuest() {
        return guest;
    }

    @Override
    public String toString() {
        return guest ? "游客" : username;
    }
}

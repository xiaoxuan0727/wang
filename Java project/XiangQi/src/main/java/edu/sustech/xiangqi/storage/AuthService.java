package edu.sustech.xiangqi.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 注册与登录的业务逻辑。
 *
 * <p>安全做法（演示级别）：为每个用户生成随机盐，密码经过多轮 SHA-256
 * 后再保存。即使两个用户密码相同，保存的哈希也不同。真实产品应使用
 * PBKDF2、bcrypt 或 Argon2 等专门的密码哈希算法。</p>
 */
public final class AuthService {
    /** 用户名限制为 2~20 位字母、数字或下划线，同时保证可以安全地用作文件夹名。 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{2,20}");
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 64;
    private static final int HASH_ITERATIONS = 12_000;

    private final UserStore userStore;
    private final SecureRandom random = new SecureRandom();

    public AuthService() throws IOException {
        this(StoragePaths.baseDir());
    }

    public AuthService(java.nio.file.Path baseDir) throws IOException {
        userStore = new UserStore(baseDir);
    }

    /**
     * 注册新用户。
     *
     * @throws IllegalArgumentException 用户名或密码不符合要求、用户名已存在
     * @throws IOException              写入用户文件失败
     */
    public User register(String username, char[] password) throws IOException {
        String cleanedUsername = username == null ? "" : username.trim();
        validateUsername(cleanedUsername);
        validatePassword(password);

        if (userStore.exists(cleanedUsername)) {
            throw new IllegalArgumentException("用户名已存在，请换一个");
        }

        byte[] salt = new byte[16];
        random.nextBytes(salt);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = hashPassword(salt, password);

        User user = new User(cleanedUsername, saltBase64, hashBase64);
        userStore.add(user);
        return user;
    }

    /**
     * 登录。
     *
     * @return 登录成功返回用户；用户名不存在或密码错误返回 null
     */
    public User login(String username, char[] password) throws IOException {
        String cleanedUsername = username == null ? "" : username.trim();
        User user = userStore.find(cleanedUsername);
        if (user == null || password == null) {
            return null;
        }

        byte[] salt;
        try {
            salt = Base64.getDecoder().decode(user.getSaltBase64());
        } catch (IllegalArgumentException exception) {
            // 账号文件中该条记录损坏时，不崩溃，按登录失败处理
            return null;
        }

        String candidate = hashPassword(salt, password);
        boolean matches = MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.US_ASCII),
                user.getPasswordHashBase64().getBytes(StandardCharsets.US_ASCII));
        return matches ? user : null;
    }

    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("用户名需为 2~20 位字母、数字或下划线");
        }
    }

    private void validatePassword(char[] password) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码至少需要 " + MIN_PASSWORD_LENGTH + " 个字符");
        }
        if (password.length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码不能超过 " + MAX_PASSWORD_LENGTH + " 个字符");
        }
    }

    /** 盐 + 密码 → 多轮 SHA-256 → Base64。 */
    private static String hashPassword(byte[] salt, char[] password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);

            byte[] passwordBytes = toUtf8Bytes(password);
            byte[] hash;
            try {
                hash = digest.digest(passwordBytes);
            } finally {
                Arrays.fill(passwordBytes, (byte) 0);
            }

            for (int i = 0; i < HASH_ITERATIONS; i++) {
                hash = digest.digest(hash);
            }
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            // JDK 必须支持 SHA-256；这里说明运行环境异常
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    /** 不通过 String 保存密码，避免密码明文长期留在不可变字符串中。 */
    private static byte[] toUtf8Bytes(char[] password) {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}

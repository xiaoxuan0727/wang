package edu.sustech.xiangqi.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 用户账号文件的读写。
 *
 * <p>文件格式（UTF-8，每行一个用户，用制表符分隔）：
 * {@code 用户名 \t 盐(Base64) \t 密码哈希(Base64)}。</p>
 *
 * <p>用户名查找不区分大小写，但显示时保留注册时的写法。</p>
 */
public final class UserStore {
    private final Path file;
    /** key 是转小写后的用户名。 */
    private final Map<String, User> users = new LinkedHashMap<>();

    public UserStore(Path baseDir) throws IOException {
        this.file = StoragePaths.usersFile(baseDir);
        Files.createDirectories(baseDir);
        load();
    }

    private void load() throws IOException {
        users.clear();
        if (!Files.exists(file)) {
            return;
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length != 3 || parts[0].isBlank()
                    || parts[1].isBlank() || parts[2].isBlank()) {
                // 跳过损坏的行，而不是让整个程序无法启动
                continue;
            }
            User user = new User(parts[0], parts[1], parts[2]);
            users.put(keyOf(parts[0]), user);
        }
    }

    private static String keyOf(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    public User find(String username) {
        if (username == null) {
            return null;
        }
        return users.get(keyOf(username.trim()));
    }

    public boolean exists(String username) {
        return find(username) != null;
    }

    /** 新增用户并立即写入磁盘。 */
    public void add(User user) throws IOException {
        users.put(keyOf(user.getUsername()), user);
        persist();
    }

    private void persist() throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder content = new StringBuilder();
        for (User user : users.values()) {
            content.append(user.getUsername()).append('\t')
                    .append(user.getSaltBase64()).append('\t')
                    .append(user.getPasswordHashBase64()).append('\n');
        }

        // 先写临时文件，再原子替换，避免写到一半被中断造成账号文件损坏
        Path temp = Files.createTempFile(parent, "users-", ".tmp");
        try {
            Files.writeString(temp, content.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temp, file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}

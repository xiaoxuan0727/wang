package edu.sustech.xiangqi.storage;

import java.nio.file.Path;

/**
 * 用户数据与存档的路径统一管理。
 *
 * <p>默认保存到用户主目录下的 {@code .xiangqi-demo}，这样无论 IntelliJ
 * 的工作目录是什么，重新打开程序后都能找到之前的数据。</p>
 *
 * <p>调试或测试时可以加 JVM 参数覆盖，例如：
 * {@code -Dxiangqi.home=D:/tmp/xiangqi-test}。</p>
 */
public final class StoragePaths {
    private static final String OVERRIDE_PROPERTY = "xiangqi.home";

    private StoragePaths() {
    }

    public static Path baseDir() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath();
        }
        return Path.of(System.getProperty("user.home"), ".xiangqi-demo");
    }

    public static Path usersFile(Path baseDir) {
        return baseDir.resolve("users.tsv");
    }

    public static Path saveFile(Path baseDir, String username, int slot) {
        return baseDir.resolve("saves").resolve(username).resolve("slot" + slot + ".txt");
    }
}

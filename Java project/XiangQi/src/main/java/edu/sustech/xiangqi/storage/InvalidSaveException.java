package edu.sustech.xiangqi.storage;

/**
 * 存档格式错误、内容被篡改或局面不合法时抛出。
 *
 * <p>这是受检异常（checked exception），强制调用者显式处理，
 * 界面层可以把它转换成用户能看懂的提示，而不是让程序崩溃。</p>
 */
public class InvalidSaveException extends Exception {
    public InvalidSaveException(String message) {
        super(message);
    }

    public InvalidSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}

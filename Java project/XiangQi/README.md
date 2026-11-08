# 中国象棋 Xiangqi — CS109 项目基础 Demo

一个用 **Java 25+ / Swing** 实现的中国象棋基础 Demo，覆盖项目要求中的 **Task 1–5（80 分基础部分）**，并额外实现了合法落点高亮、上一步高亮、多存档位、切换账号等易用功能。

完整的知识点清单、学习路线和代码讲解见 **[学习指南.md](学习指南.md)**。

---

## 1. 已实现功能

| 项目要求 | 实现情况 |
|---|---|
| Task 1 游戏初始化 | 10×9 棋盘、楚河汉界、九宫；红黑各 16 子；可随时新开一局并重置 |
| Task 2 多用户登录 | 注册、登录、游客模式；账号持久化，关闭程序后仍可登录；可切换账号 |
| Task 3 保存/读取 | 每个注册用户 3 个存档位；保存棋盘、行棋方、着法历史；校验损坏/篡改存档；用户只能读自己的存档 |
| Task 4 游戏逻辑 | 七种棋子完整走法、回合、吃子、将军、将死、困毙、认输、和棋、飞将、不能送将、上一步记录 |
| Task 5 图形界面 | 纯 Swing 图形界面，点击走子，显示行棋方、将军/胜负/和棋状态，无需命令行操作 |

附带的加分/易用功能：

- 选中棋子后显示全部合法落点（绿点/绿环）。
- 橙色标记上一步的起点和终点，侧栏显示文字记录。
- 将军状态用醒目颜色提示。
- 3 个本地存档位、切换账号、Ctrl+N / Ctrl+S / Ctrl+O 快捷键。
- 自带 87 项无依赖规则自测（见第 5 节）。

---

## 2. 运行方法

### 2.1 IntelliJ IDEA（推荐）

1. 用 IntelliJ IDEA 打开 `XiangQi` 文件夹（其中有 `pom.xml`）。
2. 选择 JDK 25 或更高版本（本机 JDK 26 也可，项目按 Java 25 编译）。
3. 打开 `src/main/java/edu/sustech/xiangqi/Main.java`，运行 `main` 方法。
4. 在登录窗口中选择“注册”创建账号，或点击“游客进入”。

> 如果没有 Maven 环境，不影响运行：IntelliJ 可以直接识别 `src/main/java` 源码目录并编译运行。

### 2.2 命令行（Git Bash / PowerShell）

在 `XiangQi` 目录下执行：

```bash
# 编译主程序
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 --release 25 -d out/production/XiangQi @sources.txt

# 启动
java -cp out/production/XiangQi edu.sustech.xiangqi.Main
```

如果本机 JDK 正好是 25，可省略 `--release 25`。Windows PowerShell 中可用：

```powershell
Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName } | Set-Content sources.txt
javac -encoding UTF-8 --release 25 -d out/production/XiangQi "@sources.txt"
java -cp out/production/XiangQi edu.sustech.xiangqi.Main
```

### 2.3 Maven

```bash
mvn compile
mvn exec:java -Dexec.mainClass=edu.sustech.xiangqi.Main
```

（`exec` 插件未写入 `pom.xml`，如使用该命令需在 IDE 中直接运行主类，或自行添加插件。）

---

## 3. 操作说明

| 操作 | 方法 |
|---|---|
| 选中 / 取消选中 | 左键点击己方棋子；再次点击同一棋子取消 |
| 走子 | 选中后点击绿色圆点（空位）或绿色圆环（可吃子） |
| 查看合法落点 | 选中棋子后自动显示 |
| 查看上一步 | 橙色框 + 侧栏“上一步”文字 |
| 新开一局 | 侧栏“新开一局”按钮或菜单“游戏 → 新开一局”（Ctrl+N） |
| 认输 / 和棋 | 侧栏按钮或“游戏”菜单；认输方为当前行棋方 |
| 保存 / 载入 | 侧栏按钮或“游戏”菜单（Ctrl+S / Ctrl+O），选择 1~3 号存档 |
| 切换账号 | 侧栏“切换账号”或“账号”菜单 |
| 查看规则 | “帮助 → 象棋规则速查” |

**登录规则：**

- 用户名：2~20 位字母、数字或下划线。
- 密码：至少 4 位（演示项目的最低要求）。
- 游客可以完整下棋，但“保存”和“载入”按钮会被禁用，菜单操作也会提示不能存档。

> 坐标约定：`(行,列)`，棋盘左上角为 `(0,0)`，红方底线在第 9 行，黑方底线在第 0 行。

---

## 4. 数据保存位置

默认保存在当前用户主目录下：

```
%USERPROFILE%\.xiangqi-demo\
├── users.tsv                     # 用户账号（用户名、盐、密码哈希）
└── saves\
    └── <用户名>\
        ├── slot1.txt             # 1 号存档
        ├── slot2.txt
        └── slot3.txt
```

- 账号密码不会以明文保存，使用随机盐 + 多轮 SHA-256 哈希。
- 要清空所有账号和存档，关闭程序后删除 `.xiangqi-demo` 文件夹即可。
- 调试时可用 JVM 参数改变数据目录：
  `-Dxiangqi.home=D:/tmp/xiangqi-data`。

---

## 5. 运行自测

项目自带不依赖 JUnit 的规则自测（87 项），覆盖：

- 初始局面、回合、吃子与着法记录
- 帅/将、仕/士、相/象、马、车、炮、兵/卒的全部规则
- 象眼、蹩马腿、炮架、过河、九宫、将帅照面
- 将军、应将、将死、困毙、认输、和棋
- 注册登录持久化、错误密码、重复用户名
- 存读档往返、只能读自己的存档、损坏存档与缺少将帅的存档被拒绝

```bash
find src/main/java src/test/java -name "*.java" > sources-test.txt
javac -encoding UTF-8 --release 25 -d out/test @sources-test.txt
java -Dstdout.encoding=UTF-8 -cp out/test edu.sustech.xiangqi.ModelSelfTest
```

全部通过时最后会输出：

```
通过：87，失败：0
```

---

## 6. 代码结构

```
src/main/java/edu/sustech/xiangqi/
├── Main.java                       # 程序入口、登录流程、切换账号
├── model/                          # 规则与数据（不依赖 Swing）
│   ├── Board.java                  # 棋盘、回合、吃子、将军、将死、困毙
│   ├── Piece.java                  # 棋子抽象父类
│   ├── PieceType.java              # 七种棋子类型与存档字符
│   ├── PieceFactory.java           # 根据类型创建棋子
│   ├── GeneralPiece.java           # 帅/将（含飞将）
│   ├── AdvisorPiece.java           # 仕/士
│   ├── ElephantPiece.java          # 相/象（象眼、不过河）
│   ├── HorsePiece.java             # 马（蹩马腿）
│   ├── RookPiece.java              # 车
│   ├── CannonPiece.java            # 炮（炮架）
│   ├── SoldierPiece.java           # 兵/卒（过河）
│   ├── Position.java               # 不可变坐标
│   ├── Move.java                   # 着法记录
│   ├── GameStatus.java             # 进行中/红胜/黑胜/和棋
│   └── GameSnapshot.java           # 读档用的完整局面快照
├── controller/
│   └── ChessController.java        # 鼠标点击 → 选中/移动
├── view/
│   ├── LoginDialog.java            # 登录/注册/游客对话框
│   ├── BoardPanel.java             # 棋盘绘制、高亮、坐标转换
│   └── ChessFrame.java             # 主窗口、侧栏、菜单、对局操作
└── storage/                        # 账号与存档
    ├── User.java
    ├── UserStore.java
    ├── AuthService.java            # 注册/登录、加盐哈希
    ├── SaveManager.java            # 存档读写与完整性校验
    ├── StoragePaths.java
    └── InvalidSaveException.java

src/test/java/edu/sustech/xiangqi/
└── ModelSelfTest.java              # 87 项自测
```

**架构要点：**

- `model` 完全不引用 Swing，可以单独测试。
- 所有走法判断集中在 `Board`，控制器和界面不重复写规则。
- “走完后是否安全”通过 `Board` 内“临时移动 → 检查 → 回滚”实现，因此能正确处理送将、应将、将帅照面。
- 存档使用版本化文本格式，读取时先在临时对象上完整校验，再覆盖真实棋盘，读档失败不会破坏当前对局。

---

## 7. 已知限制（可继续扩展）

- 目前是本地双人对弈，没有 AI 对手和联网对战。
- 没有悔棋、逐着回放、计时器、自动存档、音效。
- 着法记录使用“行,列”坐标，不是传统中文记谱（如“炮二平五”）。
- 只允许保存/载入进行中的对局（将死、认输后的终局不提供保存）。

这些都可以作为 Task 6 的进阶功能，具体实现思路见 [学习指南.md](学习指南.md) 第 11 节。

---

## 8. 文档

- **[学习指南.md](学习指南.md)**：完成任务所需的全部知识清单、规则速查、代码地图、三周学习计划、评分逐项自检和进阶功能建议。
- 项目原始要求：`../Peoject_XiangQi_bilingual.pdf`
- 课程评分表：`../project打分表（供参考）.pdf`

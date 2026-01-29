# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

jlatexmath-android 是一个将 LaTeX 数学公式渲染到 Android 平台的库,fork 自 [jlatexmath](https://github.com/opencollab/jlatexmath) 项目。核心思路是通过适配层模拟 `java.awt.*` 包,将原始的 Java AWT 渲染引擎移植到 Android Canvas。

当前维护者: [RikkaHub](https://github.com/re-ovo/rikkahub)

## 常用命令

### 构建项目
```bash
# 构建所有模块
./gradlew build

# 构建特定模块
./gradlew :jlatexmath:build
./gradlew :app:build

# 清理构建
./gradlew clean
```

### 运行示例应用
```bash
# 安装并运行演示应用
./gradlew :app:installDebug
adb shell am start -n ru.noties.jlatexmath.android.app/.MainActivity
```

### 发布到本地 Maven
```bash
# 发布核心库到本地 Maven 仓库
./gradlew :jlatexmath:publishToMavenLocal
./gradlew :jlatexmath-font-cyrillic:publishToMavenLocal
./gradlew :jlatexmath-font-greek:publishToMavenLocal
```

### 测试
```bash
# 运行单元测试 (主要在 jlatexmath 模块)
./gradlew :jlatexmath:test

# 运行特定测试类
./gradlew :jlatexmath:test --tests org.scilab.forge.jlatexmath.examples.basic.ExamplesTest
```

## 核心架构

### 三层设计

```
Android UI 层 (JLatexMathView / JLatexMathDrawable)
    ↓ 使用
AWT 适配层 (ru.noties.jlatexmath.awt.*)
    ↓ 桥接
LaTeX 渲染引擎 (org.scilab.forge.jlatexmath)
```

### 模块结构

- **jlatexmath**: 核心库,包含 AWT 适配层和 LaTeX 引擎
  - `ru.noties.jlatexmath.*`: Android 集成 API (JLatexMathDrawable, JLatexMathView)
  - `ru.noties.jlatexmath.awt.*`: AWT 到 Android Canvas 的适配层 (~23个类)
  - `org.scilab.forge.jlatexmath.*`: LaTeX 核心渲染引擎 (~149个文件)

- **jlatexmath-font-cyrillic**: 西里尔字体支持 (俄语等)
- **jlatexmath-font-greek**: 希腊字体支持
- **app**: 演示应用

### AWT 适配核心

项目通过重新实现 `java.awt.*` 接口将 Android Canvas 包装为 Graphics2D:

- **AndroidGraphics2D** (`awt/AndroidGraphics2D.java`): 核心适配器
  - 持有 `android.graphics.Canvas` 和 `Paint` 对象
  - 将 `fillRect()`, `drawChars()`, `drawLine()` 等 AWT 调用转换为 Canvas API
  - 管理变换矩阵、颜色、字体、笔画等绘图状态

- **Font** (`awt/Font.java`): 字体适配
  - 封装 `android.graphics.Typeface`
  - 从 assets 中的 TTF 文件加载 (通过 `JLatexMathAndroid.loadTypeface()`)

- **geom包** (`awt/geom/*`): 几何图形适配
  - `AffineTransform`: Canvas 矩阵变换
  - `Rectangle2D`, `Line2D`, `Point2D`: 基本几何类型

### 渲染流程

```
LaTeX 字符串 "\\frac{1}{2}"
    ↓ TeXFormula.parse()
Atom 逻辑树 (FractionAtom[CharAtom('1'), CharAtom('2')])
    ↓ createBox()
Box 布局树 (FractionBox[CharBox, CharBox])
    ↓ draw(Graphics2D)
AndroidGraphics2D
    ↓ Canvas API
屏幕渲染
```

### 字体系统

字体资源位于 `jlatexmath/src/main/assets/org/scilab/forge/jlatexmath/fonts/`:
- 35+ TTF 字体文件 (jlm_*.ttf)
- 每个字体对应一个 XML 配置文件 (字符映射、度量数据)
- 核心配置: `DefaultTeXFont.xml`, `TeXSymbols.xml`, `GlueSettings.xml`

外部字体包通过独立 assets 目录提供额外语言支持。

**加载机制**: `JLatexMathAndroid.init()` 在应用启动时通过 `JLatexMathInitProvider` (ContentProvider) 自动初始化,缓存 ApplicationContext 用于访问 assets。

### 主要 API

**JLatexMathDrawable** (`JLatexMathDrawable.java:38`):
```java
JLatexMathDrawable drawable = JLatexMathDrawable.builder(latex)
    .textSize(70)
    .color(0xFF000000)
    .padding(8)
    .align(JLatexMathDrawable.ALIGN_CENTER)
    .background(backgroundDrawable)
    .build();
```

**JLatexMathView** (`JLatexMathView.java:51`):
```xml
<ru.noties.jlatexmath.JLatexMathView
    android:id="@+id/latex_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:jlmv_textSize="16sp"
    app:jlmv_alignHorizontal="center"
    app:jlmv_alignVertical="center" />
```

## 开发注意事项

### 修改渲染逻辑
- 核心渲染在 `org.scilab.forge.jlatexmath` 包中,这是从上游继承的代码
- 如需修改绘图行为,优先在 `AndroidGraphics2D` 中调整适配逻辑
- 避免直接修改 LaTeX 引擎代码,除非是修复明确的 bug

### 字体相关修改
- 字体编辑器: https://kekee000.github.io/fonteditor/
- 添加新字体: 将 TTF 文件放入 assets/fonts 目录,并创建对应的 XML 映射文件
- 修改字体映射: 编辑 `DefaultTeXFont.xml` 和对应的字体 XML 文件

### 性能考虑
- LaTeX 解析和 Box 构建比较耗时,建议缓存 `JLatexMathDrawable` 实例
- 字体加载发生在首次渲染时,可能导致第一次绘制较慢
- 复杂公式建议在后台线程创建 Drawable

### 已知问题修复
- 最近修复了 `\Omega` 显示问题 (commit: 29bf3b1)
- 项目已清理过时代码并更新依赖 (commit: f2502e3)

## 构建配置

- **编译 SDK**: 36 (Android 16.0)
- **最低支持**: SDK 24 (Android 7.0)
- **Java 版本**: 11
- **Kotlin 版本**: 2.3.0
- **AGP 版本**: 8.11.2

使用 Gradle Version Catalog (`gradle/libs.versions.toml`) 管理依赖版本。

## 发布

库使用 JitPack 发布。配置在 `jlatexmath/build.gradle:34`:
```gradle
publishing {
    publications {
        release(MavenPublication) {
            groupId = 'me.rerere.jlatexmath'
            artifactId = 'jlatexmath'
            version = '1.0'
        }
    }
}
```

修改版本号后运行 `./gradlew publish` 即可发布到 JitPack。

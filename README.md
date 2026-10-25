# Z80Z Adaptive Soundscape

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)
![Forge](https://img.shields.io/badge/Forge-47.x-orange)

A client-side dynamic music mod for Minecraft 1.20.1 Forge. It picks background music based on the game
context - biome, day/night, nearby enemy count, locked-on targets and boss phases - with priority rules,
weighted random track pools, a 3-state explore/alert/combat machine, equal-power crossfades, a
drag-and-drop music library (OGG / MP3 / WAV / FLAC), two-layer volume calibration and an in-game hub UI.

一个面向 Minecraft 1.20.1 Forge 的纯客户端动态音乐模组：按生物群系、昼夜、敌人数量、锁定目标与 BOSS
阶段动态选曲；内置优先级规则、权重随机、三态状态机（探索/警戒/战斗）、等功率交叉淡化、拖拽导入的音乐库
（OGG/MP3/WAV/FLAC）、两级音量校准与游戏内配置中枢。默认按 `M` 打开配置，也可从 Forge 模组列表进入。

## Features / 功能

- **Context-aware rules / 情境规则** - stack all rules together, the highest priority wins; ties keep list order.
- **Weighted track pools / 权重曲目池** - one rule can hold many tracks drawn by weight, so music never repeats predictably.
- **3-state machine / 三态状态机** - explore / alert / combat with hysteresis; entering danger switches instantly, leaving fades back after a delay.
- **Boss phases / BOSS 阶段** - proximity, engagement and half-health phase two; detected by entity list or a health threshold.
- **Equal-power crossfade / 等功率交叉淡化** - 2-second dual-source crossfades without a loudness dip in the middle.
- **Music library / 音乐库** - drag `ogg / mp3 / wav / flac / aiff` files onto the game window at any time; MP3 and FLAC are decoded by embedded pure-Java decoders (JLayer + javasound-flac), no conversion needed.
- **Two-layer volume / 两级音量** - calibrate each track's overall volume once in the library; every rule follows it by default, and any rule can override it with an independent volume.
- **Hub UI / 配置中枢** - one window, left navigation (Overview / Rules / Library / Settings / JSON), live context dashboard, rule editor with drafts, JSON editor. Bilingual (English / 简体中文).

## Install / 安装

Drop the release jar (`z80z-adaptive-soundscape-x.y.z-all.jar`) into `.minecraft/mods/`.
Requires Minecraft 1.20.1 and Forge 47.x. Client side only.

把发布版 jar（`z80z-adaptive-soundscape-x.y.z-all.jar`）放入 `.minecraft/mods/` 即可，
需要 Minecraft 1.20.1 + Forge 47.x，纯客户端模组。

## Usage / 使用

- Press `M` in game to open the hub / 游戏内按 `M` 打开配置。
- Drag audio files onto the game window to import them / 把音频文件拖到游戏窗口即可导入。
- Music library lives in `.minecraft/config/z80z_adaptive_soundscape/custom_music/` /
  音乐库目录为 `.minecraft/config/z80z_adaptive_soundscape/custom_music/`。
- Full rule reference lives in the in-game JSON page / 完整规则字段说明见游戏内 JSON 页与下方文档。

## Build / 构建

```bash
gradle build
# output: build/libs/z80z-adaptive-soundscape-<version>-all.jar
```

## License / 许可

Code is released under the [MIT License](LICENSE). Bundled decoders: JLayer (LGPL) and
javasound-flac (LGPL), embedded via Forge jarJar. Only use music you have the rights to.

代码采用 [MIT 许可](LICENSE)；内嵌解码器 JLayer 与 javasound-flac 为 LGPL，经 jarJar 内嵌分发。
请使用你拥有授权的音乐。

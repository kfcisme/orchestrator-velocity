# Minecraft MLP Orchestrator (velocity版本)

---

## 專案依賴

本專案依賴於[Orchestrator-Core](https://github.com/kfcisme/orchestrator-core.git), 提供核心的MLP功能, 本專案在此基礎上實現了paper版本的完整MLP與LSTM功能。

---

## 專案簡介

Minecraft MLP Orchestrator 是一個專為 Minecraft MLP（Machine Learning Pipeline）的 velocity版本。
這個版本的 API 提供了完整的MLP與LSTM功能，並且專為 Minecraft Velocity 伺服器優化，確保在 Minecraft 環境中能夠高效運行。

---

## 使用方式

### 1. 版本支援

此插件目前支援 Minecraft 1.20.4 以上版本，確保你的伺服器運行在此版本以獲得最佳性能。

### 2. 安裝插件

將下載的**jar**檔案放入 Minecraft 伺服器的 `plugins` 資料夾中，然後重新啟動伺服器即可使用預設MLP與LSTM功能。

### 3. 設定config.yml

在 `plugins/MinecraftMLPOrchestrator/config.yml` 中，你可以根據需要調整 MLP 與 LSTM 的參數設定。 <\br>
同時可在設定中自訂每位玩家每個伺服器的權重等參數用以優化模型的表現。


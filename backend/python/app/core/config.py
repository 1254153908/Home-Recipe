import os
import yaml
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


# ---------- .env 敏感配置 ----------
class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    PROJECT_NAME: str = "HomeRecipe AI Service"
    PROJECT_VERSION: str = "1.0.0"
    LOG_LEVEL: str = "INFO"

    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 5000

    # LLM API (兼容 OpenAI 协议)
    OPENAI_API_KEY: str = ""
    OPENAI_BASE_URL: str = "https://api.openai.com/v1"
    OPENAI_MODEL: str = "gpt-4o-mini"
    OPENAI_VISION_MODEL: str = "qwen-vl-max"

    # AI 识别模式: mock / openai
    AI_MODE: str = "mock"

    # 页面抓取超时（秒）
    FETCH_TIMEOUT: int = 15

    # AI 图片识别总超时（秒）—— 多图并行 Vision 调用的截止时间
    RECOGNIZE_TIMEOUT: int = 30

settings = Settings()


# ---------- config.yaml 结构体 ----------
class ServerConfig:
    def __init__(self, cfg: dict):
        self.HOST = cfg.get("host", "0.0.0.0")
        self.PORT = cfg.get("port", 5000)

class AIConfig:
    def __init__(self, cfg: dict):
        self.PROVIDER = cfg.get("provider", "mock")
        self.MOCK_ENABLED = cfg.get("mock_enabled", True)

class LogConfig:
    def __init__(self, cfg: dict):
        self.LEVEL = cfg.get("level", "INFO")
        self.FORMAT = cfg.get("format", "%(asctime)s - %(name)s - %(levelname)s - %(message)s")


# ---------- 加载 config.yaml ----------
_yaml_path = Path(__file__).resolve().parent.parent.parent / "config.yaml"
with open(_yaml_path, "r", encoding="utf-8") as f:
    _yaml = yaml.safe_load(f)

server = ServerConfig(_yaml.get("server", {}))
ai = AIConfig(_yaml.get("ai", {}))
log = LogConfig(_yaml.get("logging", {}))

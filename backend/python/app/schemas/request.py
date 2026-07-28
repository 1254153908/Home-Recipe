from pydantic import BaseModel, ConfigDict, Field
from typing import Optional


class RecognizeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    source_type: str = Field(default="link", alias="sourceType", description="来源类型: link/image")
    content: str = Field(..., description="识别内容（URL 或 base64 图片）")
    url_hint: Optional[str] = Field(default=None, alias="urlHint", description="来源域名提示，用于选择解析策略")

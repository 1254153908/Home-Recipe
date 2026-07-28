from pydantic import BaseModel, ConfigDict, Field


class RecognizeRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    source_type: str = Field(default="link", alias="sourceType", description="来源类型: link/video/image")
    content: str = Field(..., description="识别内容（URL 或文本）")

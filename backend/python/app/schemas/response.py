from pydantic import BaseModel, Field
from typing import Optional


class StepItem(BaseModel):
    content: str = Field(default="", description="步骤内容")
    imageUrl: Optional[str] = Field(default="", description="步骤图片")


class IngredientItem(BaseModel):
    name: str = Field(default="", description="名称")
    quantity: str = Field(default="", description="数量")
    unit: str = Field(default="", description="单位")


class SeasoningItem(BaseModel):
    name: str = Field(default="", description="名称")
    quantity: str = Field(default="", description="数量")
    unit: str = Field(default="", description="单位")


class RecipeDraft(BaseModel):
    title: str = Field(default="", description="菜谱名称")
    imageUrl: Optional[str] = Field(default="", description="封面图")
    steps: list[StepItem] = Field(default_factory=list, description="步骤")
    ingredients: list[IngredientItem] = Field(default_factory=list, description="原料")
    seasonings: list[SeasoningItem] = Field(default_factory=list, description="调料")

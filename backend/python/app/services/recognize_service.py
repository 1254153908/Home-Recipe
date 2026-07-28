import json
import logging
import httpx
from openai import OpenAI
from bs4 import BeautifulSoup

from app.core.config import settings
from app.schemas.response import RecipeDraft, StepItem, IngredientItem, SeasoningItem

logger = logging.getLogger("homerecipe.service.recognize")

# ---------- Mock 菜谱库 ----------
MOCK_RECIPES: list[RecipeDraft] = [
    RecipeDraft(
        title="Tomato Egg Stir-fry",
        imageUrl="https://images.unsplash.com/photo-1582452919408-c5244e6dfe7b?w=600",
        steps=[
            StepItem(content="Beat 3 eggs with a pinch of salt", imageUrl=""),
            StepItem(content="Cut 2 tomatoes into wedges", imageUrl=""),
            StepItem(content="Heat oil in wok, scramble eggs until just set, remove", imageUrl=""),
            StepItem(content="Stir-fry tomatoes until soft, return eggs, season and serve", imageUrl=""),
        ],
        ingredients=[
            IngredientItem(name="Tomato", quantity="2", unit="pc"),
            IngredientItem(name="Egg", quantity="3", unit="pc"),
            IngredientItem(name="Scallion", quantity="2", unit="stalk"),
        ],
        seasonings=[
            SeasoningItem(name="Salt", quantity="5", unit="g"),
            SeasoningItem(name="Sugar", quantity="3", unit="g"),
            SeasoningItem(name="Cooking oil", quantity="15", unit="ml"),
        ],
    ),
    RecipeDraft(
        title="Mapo Tofu",
        imageUrl="https://images.unsplash.com/photo-1582452919408-c5244e6dfe7b?w=600",
        steps=[
            StepItem(content="Cut soft tofu into cubes, blanch in salted boiling water for 2 min", imageUrl=""),
            StepItem(content="Heat oil, stir-fry minced pork until browned", imageUrl=""),
            StepItem(content="Add doubanjiang and fermented black beans, fry until fragrant", imageUrl=""),
            StepItem(content="Add tofu, water, simmer 5 min, thicken with cornstarch slurry", imageUrl=""),
        ],
        ingredients=[
            IngredientItem(name="Soft tofu", quantity="1", unit="block"),
            IngredientItem(name="Minced pork", quantity="100", unit="g"),
            IngredientItem(name="Garlic", quantity="3", unit="clove"),
            IngredientItem(name="Scallion", quantity="2", unit="stalk"),
        ],
        seasonings=[
            SeasoningItem(name="Doubanjiang", quantity="2", unit="tbsp"),
            SeasoningItem(name="Soy sauce", quantity="1", unit="tbsp"),
            SeasoningItem(name="Sichuan peppercorn", quantity="1", unit="tsp"),
            SeasoningItem(name="Cornstarch", quantity="1", unit="tbsp"),
        ],
    ),
    RecipeDraft(
        title="Sweet and Sour Ribs",
        imageUrl="https://images.unsplash.com/photo-1582452919408-c5244e6dfe7b?w=600",
        steps=[
            StepItem(content="Blanch pork ribs in boiling water, drain and pat dry", imageUrl=""),
            StepItem(content="Heat oil, add rock sugar, caramelize until amber", imageUrl=""),
            StepItem(content="Add ribs, coat with caramel, add soy sauce, vinegar, water", imageUrl=""),
            StepItem(content="Simmer 40 min, reduce sauce until thick and glossy", imageUrl=""),
        ],
        ingredients=[
            IngredientItem(name="Pork ribs", quantity="500", unit="g"),
            IngredientItem(name="Ginger", quantity="3", unit="slice"),
        ],
        seasonings=[
            SeasoningItem(name="Rock sugar", quantity="30", unit="g"),
            SeasoningItem(name="Black vinegar", quantity="3", unit="tbsp"),
            SeasoningItem(name="Soy sauce", quantity="2", unit="tbsp"),
            SeasoningItem(name="Cooking wine", quantity="2", unit="tbsp"),
        ],
    ),
]

# ---------- 从 HTML 提取纯文本 ----------
def _extract_text(html: str, max_length: int = 8000) -> str:
    soup = BeautifulSoup(html, "html.parser")
    # 移除 script / style / nav / footer 等噪音标签
    for tag in soup(["script", "style", "nav", "footer", "header", "aside", "noscript"]):
        tag.decompose()
    text = soup.get_text(separator="\n", strip=True)
    # 去重空行
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    text = "\n".join(lines)
    if len(text) > max_length:
        text = text[:max_length] + "..."
    return text


# ---------- LLM 提取菜谱 ----------
EXTRACT_PROMPT = """你是一个菜谱提取助手。下面是一个网页的文本内容，请从中提取菜谱信息，返回纯 JSON（不要 markdown 代码块）。

要求：
1. title: 菜名
2. imageUrl: 封面图 URL（从页面中提取第一个菜谱相关的图片 URL，没有则填空字符串）
3. steps: 步骤数组，每项包含 content（步骤文字）和 imageUrl（该步骤配图 URL，没有则填空字符串）
4. ingredients: 原料数组，每项包含 name、quantity、unit
5. seasonings: 调料数组，每项包含 name、quantity、unit

如果没有找到任何菜谱信息，返回空字段。

JSON 格式：
{
  "title": "",
  "imageUrl": "",
  "steps": [{"content": "", "imageUrl": ""}],
  "ingredients": [{"name": "", "quantity": "", "unit": ""}],
  "seasonings": [{"name": "", "quantity": "", "unit": ""}]
}

网页内容：
"""


class RecognizeService:

    # ---------- Mock ----------
    def recognize_mock(self, source_type: str, content: str) -> RecipeDraft:
        logger.info("Mock recognizing: sourceType=%s, content=%s", source_type, content)
        content_lower = content.lower()

        keyword_map = {
            "tomato": 0, "egg": 0,
            "mapo": 1, "tofu": 1,
            "rib": 2, "sweet": 2, "sour": 2,
        }
        for keyword, idx in keyword_map.items():
            if keyword in content_lower:
                logger.info("Matched keyword '%s' → recipe #%d", keyword, idx)
                return MOCK_RECIPES[idx]

        logger.info("No keyword matched, returning default recipe")
        return MOCK_RECIPES[0]

    # ---------- OpenAI 通用提取 ----------
    async def recognize_openai(self, source_type: str, content: str) -> RecipeDraft:
        logger.info("Fetching URL: %s", content)

        # 1. 抓取页面 HTML
        async with httpx.AsyncClient(timeout=settings.FETCH_TIMEOUT, follow_redirects=True) as client:
            try:
                resp = await client.get(content, headers={
                    "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
                                  "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                })
                resp.raise_for_status()
                html = resp.text
            except Exception as e:
                logger.error("Failed to fetch URL: %s", e)
                return RecipeDraft()

        # 2. 提取纯文本
        text = _extract_text(html)
        if not text:
            logger.warning("No text extracted from page")
            return RecipeDraft()

        logger.info("Extracted %d chars from page", len(text))

        # 3. 调用 LLM
        client = OpenAI(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_BASE_URL,
        )

        try:
            response = client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {"role": "system", "content": "你是一个精准的菜谱提取助手，只返回 JSON，不返回其他内容。"},
                    {"role": "user", "content": EXTRACT_PROMPT + text},
                ],
                temperature=0.1,
                max_tokens=4096,
            )
            raw = response.choices[0].message.content.strip()
            logger.info("LLM response length: %d", len(raw))
        except Exception as e:
            logger.error("LLM call failed: %s", e)
            return RecipeDraft()

        # 4. 解析 JSON
        try:
            # 去除可能的 markdown 代码块标记
            if raw.startswith("```"):
                raw = raw.split("\n", 1)[1]
                if raw.endswith("```"):
                    raw = raw[:-3]
            data = json.loads(raw)
        except json.JSONDecodeError as e:
            logger.error("Failed to parse LLM response as JSON: %s", e)
            return RecipeDraft()

        # 5. 组装 RecipeDraft
        return RecipeDraft(
            title=data.get("title", ""),
            imageUrl=data.get("imageUrl", ""),
            steps=[StepItem(**s) for s in data.get("steps", []) if s.get("content")],
            ingredients=[IngredientItem(**i) for i in data.get("ingredients", []) if i.get("name")],
            seasonings=[SeasoningItem(**s) for s in data.get("seasonings", []) if s.get("name")],
        )

    # ---------- 统一入口 ----------
    async def recognize(self, source_type: str, content: str) -> RecipeDraft:
        if settings.AI_MODE == "openai" and settings.OPENAI_API_KEY:
            return await self.recognize_openai(source_type, content)
        return self.recognize_mock(source_type, content)


recognize_service = RecognizeService()

import asyncio
import json
import logging
import re
import base64
import httpx
from openai import OpenAI
from bs4 import BeautifulSoup
from urllib.parse import urlparse

from app.core.config import settings
from app.schemas.response import RecipeDraft, StepItem, IngredientItem, SeasoningItem

logger = logging.getLogger("homerecipe.service.recognize")
# ---------- JSON 解析辅助 ----------
def _parse_json(raw: str) -> dict:
    """解析 LLM 返回的 JSON，自动剥离 markdown 代码块"""
    raw = raw.strip()
    if raw.startswith("```"):
        lines = raw.split("\n")
        # 去掉第一行 ```json 或 ```
        if len(lines) > 1:
            raw = "\n".join(lines[1:])
        else:
            raw = ""
    if raw.endswith("```"):
        raw = raw[:-3].strip()
    return json.loads(raw)


def _build_recipe(data: dict) -> RecipeDraft:
    """从 dict 组装 RecipeDraft"""
    return RecipeDraft(
        title=data.get("title", ""),
        imageUrl=data.get("imageUrl", ""),
        steps=[StepItem(**s) for s in data.get("steps", []) if s.get("content")],
        ingredients=[IngredientItem(**i) for i in data.get("ingredients", []) if i.get("name")],
        seasonings=[SeasoningItem(**s) for s in data.get("seasonings", []) if s.get("name")],
    )


# ---------- 工具函数 ----------
def _is_xiachufang_url(url: str) -> bool:
    """判断是否为下厨房链接"""
    try:
        host = urlparse(url).hostname or ""
        return "xiachufang.com" in host
    except Exception:
        return False


# ---------- LLM 通用 Prompt ----------
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

# ---------- Vision 图片识别 Prompt ----------
VISION_PROMPT = """你是一个菜谱提取助手。这是一张包含菜谱信息的图片（可能来自小红书、抖音等社交平台截图），请从中提取菜谱信息，返回纯 JSON（不要 markdown 代码块）。

要求：
1. title: 菜名
2. imageUrl: 填空字符串
3. steps: 步骤数组，每项包含 content（步骤文字）和 imageUrl（填空字符串）
4. ingredients: 原料数组，每项包含 name、quantity、unit
5. seasonings: 调料数组，每项包含 name、quantity、unit

如果图片中没有菜谱信息，返回空字段。对于模糊或不确定的内容，尽力提取即可。

JSON 格式：
{
  "title": "",
  "imageUrl": "",
  "steps": [{"content": "", "imageUrl": ""}],
  "ingredients": [{"name": "", "quantity": "", "unit": ""}],
  "seasonings": [{"name": "", "quantity": "", "unit": ""}]
}"""


# ============================================================
# RecognizeService
# ============================================================
class RecognizeService:

    # ---------- 公共方法 ----------
    def _parse_ingredient_text(self, text: str) -> tuple[str, str, str]:
        """拆分原料字符串 → (quantity, unit, name)
        支持两种格式:
          - 数字在前: "500g 五花肉" → ("500", "g", "五花肉")
          - 名称在前: "盐少许"     → ("少许", "", "盐")
        """
        m = re.match(
            r"([\d.]+)\s*(克|勺|大勺|小勺|匙|汤匙|茶匙|大匙|小匙|只|个|根|把|块|片|条|碗|杯|少许|少量|适量|"
            r"毫升|升|斤|两|磅|g|kg|ml|l|tbsp|tsp|cup)?\s*(.+)",
            text, re.IGNORECASE
        )
        if m:
            return m.group(1), (m.group(2) or m.group(3) or ""), m.group(3)

        m = re.match(
            r"(.+?)([\d.]+)\s*(克|勺|大勺|小勺|匙|汤匙|茶匙|大匙|小匙|只|个|根|把|块|片|条|碗|杯|少许|少量|适量|"
            r"毫升|升|斤|两|磅|g|kg|ml|l|tbsp|tsp|cup)?$",
            text, re.IGNORECASE
        )
        if m:
            return m.group(2), (m.group(3) or ""), m.group(1).strip()

        return "", "", text

    def _is_seasoning(self, name: str) -> bool:
        """判断原料名是否为调料"""
        keywords = {
            "油", "盐", "糖", "生抽", "老抽", "酱油", "醋", "料酒", "蚝油",
            "淀粉", "生粉", "豆瓣酱", "甜面酱", "辣椒酱", "胡椒粉", "五香粉",
            "鸡精", "味精", "香油", "芝麻油", "花椒", "八角", "桂皮", "姜", "蒜", "葱",
        }
        return any(kw in name for kw in keywords)

    def _extract_img_url(self, img) -> str:
        """从 img 标签提取图片 URL，补全协议头"""
        src = img.get("src") or img.get("data-src") or ""
        if src.startswith("//"):
            src = "https:" + src
        return src


    # ================================================================
    # 策略1: 下厨房解析（优先 JSON-LD，回退 HTML 选择器）
    # ================================================================
    async def recognize_xiachufang(self, url: str) -> RecipeDraft:

        # ---------- 步骤1: 抓取页面 ----------
        # PC 版(www)有反爬验证，直接请求移动版(m)
        fetch_url = url.replace("www.xiachufang.com", "m.xiachufang.com")

        async with httpx.AsyncClient(timeout=settings.FETCH_TIMEOUT, follow_redirects=False) as client:
            try:
                resp = await client.get(fetch_url, headers={
                    "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
                                  "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                })
                resp.raise_for_status()
                html = resp.text
            except Exception as e:
                return RecipeDraft()

        # ---------- 步骤2: 优先从 JSON-LD 提取 ----------

        soup = BeautifulSoup(html, "html.parser")
        page_title = soup.title.get_text(strip=True) if soup.title else "无"

        ld_json = soup.find("script", type="application/ld+json")
        if ld_json and ld_json.string:
            try:
                ld_data = json.loads(ld_json.string)
                if isinstance(ld_data, list):
                    ld_data = ld_data[0]
                if ld_data.get("@type") == "Recipe":
                    return self._parse_ld_json(ld_data, soup)
            except json.JSONDecodeError:
                logger.warning("JSON-LD 解析失败, 回退 HTML 选择器")
        else:
            logger.info("未找到 JSON-LD, 回退 HTML 选择器")

        # ---------- 步骤3-7: HTML 选择器回退 ----------
        logger.info("[下厨房] 步骤3/7 - 提取菜名...")
        title = ""
        title_selectors = [
            "h1.title", "h1.recipe-title", ".recipe-title", "h1[itemprop='name']",
            "h1.page-title", "h1.name", ".recipe-info h1",
        ]
        for sel in title_selectors:
            title_tag = soup.select_one(sel)
            if title_tag:
                title = title_tag.get_text(strip=True)
                break
        if not title:
            h1 = soup.select_one("h1")
            if h1:
                title = h1.get_text(strip=True)

        # ---------- 步骤4: 提取封面图 ----------
        image_url = ""
        cover_selectors = [
            ".cover img", ".recipe-cover img", "img.cover-photo", "img[itemprop='image']",
            ".recipe-photo img", ".main-photo img",
        ]
        for sel in cover_selectors:
            cover_img = soup.select_one(sel)
            if cover_img:
                image_url = self._extract_img_url(cover_img)
                break
        if not image_url:
            logger.warning("[下厨房] 步骤4/7 - 未匹配到封面图, 尝试的选择器: %s", cover_selectors)

        # ---------- 步骤5: 提取用料 ----------
        logger.info("[下厨房] 步骤5/7 - 提取用料...")
        ingredients: list[IngredientItem] = []
        seasonings: list[SeasoningItem] = []

        ing_selectors = [
            ".ings tbody tr", ".ings tr", ".ingredients tbody tr", ".ingredients tr",
            ".ingredient-list li", "[itemprop='recipeIngredient']",
        ]
        ing_rows = []
        for sel in ing_selectors:
            rows = soup.select(sel)
            if rows:
                ing_rows = rows
                break
        if not ing_rows:
            logger.warning("未匹配到用料行, 尝试的选择器: %s", ing_selectors)

        for i, row in enumerate(ing_rows):
            name_el = row.select_one("td.name, .ingredient-name")
            if not name_el:
                name_el = row.select_one("a") or row
            qty_el = row.select_one("td.unit, .ingredient-quantity, .unit")

            if name_el:
                name = name_el.get_text(strip=True)
                quantity_text = qty_el.get_text(strip=True) if qty_el else ""
                quantity, unit, parsed_name = self._parse_ingredient_text(quantity_text)
                if parsed_name:
                    name = parsed_name

                if self._is_seasoning(name):
                    seasonings.append(SeasoningItem(name=name, quantity=quantity, unit=unit))
                else:
                    ingredients.append(IngredientItem(name=name, quantity=quantity, unit=unit))


        # ---------- 步骤6: 提取步骤 ----------
        steps: list[StepItem] = []
        step_selectors = [
            ".steps ol li", ".steps li", ".step-list li", ".cookstep li",
            "[itemprop='recipeInstructions'] li", ".step li",
        ]
        step_els = []
        for sel in step_selectors:
            els = soup.select(sel)
            els = [e for e in els if e.get_text(strip=True)]
            if els:
                step_els = els
                break

        if not step_els:
            logger.warning("[下厨房] 步骤6/7 - li 选择器未匹配, 降级尝试 p 标签...")
            step_texts = soup.select(".steps p.text, .steps p, .step-content p, .cookstep p, .step p")
            for p in step_texts:
                content = p.get_text(strip=True)
                if content and len(content) > 3:
                    steps.append(StepItem(content=content, imageUrl=""))
            logger.info("[下厨房] 步骤6/7 - 降级 p 标签提取到 %d 个步骤", len(steps))
        else:
            for i, step_el in enumerate(step_els):
                text_el = step_el.select_one("p.text, p")
                content = text_el.get_text(strip=True) if text_el else step_el.get_text(strip=True)
                content = re.sub(r"步骤\d+", "", content).strip()

                if content:
                    step_img = step_el.select_one("img")
                    step_image = self._extract_img_url(step_img) if step_img else ""
                    steps.append(StepItem(content=content, imageUrl=step_image))
                    logger.info("[下厨房] 步骤6/7 - 步骤[%d]: %.80s%s",
                               i + 1, content, " [有配图]" if step_image else "")
        logger.info("[下厨房] 步骤7/7 - 共提取 %d 个步骤", len(steps))

        # ---------- 汇总 ----------
        logger.info("[下厨房] 步骤7/7 - 汇总: title='%s' | 封面图=%s | 原料=%d | 调料=%d | 步骤=%d",
                    title, "有" if image_url else "无", len(ingredients), len(seasonings), len(steps))
        logger.info("=" * 60)

        return RecipeDraft(
            title=title,
            imageUrl=image_url,
            steps=steps,
            ingredients=ingredients,
            seasonings=seasonings,
        )

    # ---------- JSON-LD 解析辅助 ----------
    def _parse_ld_json(self, data: dict, soup: BeautifulSoup) -> RecipeDraft:
        """从 Schema.org Recipe JSON-LD + HTML 提取完整菜谱数据（含步骤图片）"""
        title = data.get("name", "")
        image_url = ""
        if isinstance(data.get("image"), list):
            image_url = data["image"][0] if data["image"] else ""
        elif isinstance(data.get("image"), str):
            image_url = data["image"]

        # 解析原料
        ingredients: list[IngredientItem] = []
        seasonings: list[SeasoningItem] = []

        raw_ings = data.get("recipeIngredient", [])
        if isinstance(raw_ings, str):
            raw_ings = [raw_ings]

        for ing_text in raw_ings:
            quantity, unit, name = self._parse_ingredient_text(ing_text)
            if self._is_seasoning(name):
                seasonings.append(SeasoningItem(name=name, quantity=quantity, unit=unit))
            else:
                ingredients.append(IngredientItem(name=name, quantity=quantity, unit=unit))


        # 解析步骤（文字从 JSON-LD，图片从 HTML 的 .step-cover img）
        steps: list[StepItem] = []
        instructions = data.get("recipeInstructions", "")
        if isinstance(instructions, list):
            # 可能是 [{@type: HowToStep, text: "..."}, ...]
            for item in instructions:
                if isinstance(item, dict):
                    step_text = item.get("text", "")
                else:
                    step_text = str(item)
                if step_text.strip():
                    steps.append(StepItem(content=step_text.strip(), imageUrl=""))
        elif isinstance(instructions, str):
            # 逗号分隔的步骤字符串，每步以 "0.", "1." 等数字开头
            step_parts = re.split(r",?\d+\.", instructions)
            for part in step_parts:
                part = part.strip()
                if part and len(part) > 2:
                    steps.append(StepItem(content=part, imageUrl=""))

        # 从 HTML 补充步骤图片：匹配 div.step > div.step-cover img
        step_covers = soup.select(".step .step-cover img, .steps .step-cover img")
        step_images = []
        for img in step_covers:
            src = img.get("src") or img.get("data-src") or ""
            if src.startswith("//"):
                src = "https:" + src
            step_images.append(src)

        # 将图片按顺序分配到步骤
        for i, img_url in enumerate(step_images):
            if i < len(steps):
                steps[i].imageUrl = img_url

        return RecipeDraft(
            title=title,
            imageUrl=image_url,
            steps=steps,
            ingredients=ingredients,
            seasonings=seasonings,
        )

    # ================================================================
    # 策略2: 通用网页 → 抓取 HTML → 提纯文本 → LLM 提取
    # ================================================================
    async def recognize_webpage(self, url: str) -> RecipeDraft:
        """通用网页：抓取 HTML 提取纯文本后交给 LLM 解析"""
        logger.info("[通用网页] LLM 提取: %s", url)

        # 1. 抓取页面
        async with httpx.AsyncClient(timeout=settings.FETCH_TIMEOUT, follow_redirects=True) as client:
            try:
                resp = await client.get(url, headers={
                    "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
                                  "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                })
                resp.raise_for_status()
                html = resp.text
            except Exception as e:
                logger.error("[通用网页] 抓取失败: %s", e)
                return RecipeDraft()

        # 2. 提取纯文本
        soup = BeautifulSoup(html, "html.parser")
        for tag in soup(["script", "style", "nav", "footer", "header", "aside", "noscript"]):
            tag.decompose()
        text = soup.get_text(separator="\n", strip=True)
        lines = [line.strip() for line in text.split("\n") if line.strip()]
        text = "\n".join(lines)
        if len(text) > 8000:
            text = text[:8000] + "..."

        if not text:
            logger.warning("[通用网页] 未提取到文本")
            return RecipeDraft()

        logger.info("[通用网页] 提取 %d 字符", len(text))

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
            logger.info("[通用网页] LLM 返回 %d 字符", len(raw))
        except Exception as e:
            logger.error("[通用网页] LLM 调用失败: %s", e)
            return RecipeDraft()

        # 4. 解析 JSON
        try:
            data = _parse_json(raw)
        except json.JSONDecodeError as e:
            logger.error("[通用网页] JSON 解析失败: %s", e)
            return RecipeDraft()

        return _build_recipe(data)

    # ================================================================
    # 策略3: 图片 → Vision LLM 直接识别
    # ================================================================
    def _recognize_image_sync(self, base64_data: str) -> RecipeDraft:
        """图片识别核心（同步），在线程池中运行以支持并发"""
        logger.info("[图片识别] Vision LLM 解析")

        # 兼容 data:image/xxx;base64, 前缀
        if "," in base64_data and base64_data.startswith("data:"):
            base64_data = base64_data.split(",", 1)[1]

        # 验证 base64
        try:
            base64.b64decode(base64_data)
        except Exception as e:
            logger.error("[图片识别] Base64 解码失败: %s", e)
            return RecipeDraft()

        client = OpenAI(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_BASE_URL,
        )
        try:
            response = client.chat.completions.create(
                model=settings.OPENAI_VISION_MODEL,
                messages=[
                    {"role": "system", "content": VISION_PROMPT},
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": "识别菜谱内容输出"},
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{base64_data}"
                                },
                            },
                        ],
                    },
                ],
                temperature=0.1,
                max_tokens=4096,
            )
            raw = response.choices[0].message.content.strip()
        except Exception as e:
            logger.error("[图片识别] LLM 调用失败: %s", e)
            return RecipeDraft()

        # 解析 JSON
        try:
            data = _parse_json(raw)
        except json.JSONDecodeError as e:
            logger.error("[图片识别] JSON 解析失败: %s", e)
            return RecipeDraft()

        return _build_recipe(data)

    async def recognize_image(self, base64_data: str) -> RecipeDraft:
        """异步包装，在线程池中运行避免阻塞事件循环"""
        return await asyncio.to_thread(self._recognize_image_sync, base64_data)

    # ================================================================
    # 统一入口：根据 source_type 和 url_hint 自动选择策略
    # ================================================================
    # ---------- 合并总结 Prompt（文本 LLM）----------
    CONSOLIDATE_PROMPT = """你是一个专业的菜谱编辑。以下是同一道菜的多张图片被分别识别后得到的原始结果。这些图片可能展示了同一菜谱的不同部分（食材清单截图、步骤截图、成品图等）。

请将这些结果整理成一份完整的菜谱 JSON，要求：
1. title: 最准确、最完整的菜名（不同版本选最好的那一个）
2. imageUrl: 填空字符串
3. steps: 按烹饪逻辑重新排序，合并相同或相似的步骤，去除重复，保持流程连贯
4. ingredients: 去重合并（同名原料只要有不同写法就统一名称），数量/单位取最合理的
5. seasonings: 同上，去重合并

只返回纯 JSON，不要 markdown 代码块。JSON 格式：
{
  "title": "",
  "imageUrl": "",
  "steps": [{"content": "", "imageUrl": ""}],
  "ingredients": [{"name": "", "quantity": "", "unit": ""}],
  "seasonings": [{"name": "", "quantity": "", "unit": ""}]
}

原始识别结果：
"""

    async def _consolidate_drafts(self, drafts: list[RecipeDraft]) -> RecipeDraft:
        """用文本 LLM 合并总结多张图片的识别结果"""
        if len(drafts) <= 1:
            return drafts[0] if drafts else RecipeDraft()

        # 序列化所有原始结果为 JSON
        raw_json = json.dumps(
            [d.model_dump() for d in drafts],
            ensure_ascii=False, indent=2
        )
        logger.info("[合并总结] 输入 %d 个原始结果, 共 %d 字符", len(drafts), len(raw_json))

        client = OpenAI(
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_BASE_URL,
        )
        try:
            response = client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {"role": "system", "content": "你是一个精准的菜谱提取助手，只返回 JSON，不返回其他内容。"},
                    {"role": "user", "content": self.CONSOLIDATE_PROMPT + raw_json},
                ],
                temperature=0.1,
                max_tokens=4096,
            )
            raw = response.choices[0].message.content.strip()
            logger.info("[合并总结] LLM 返回 %d 字符", len(raw))
        except Exception as e:
            logger.error("[合并总结] LLM 调用失败，回退机械合并: %s", e)
            return await self._merge_drafts(drafts)

        try:
            data = _parse_json(raw)
        except json.JSONDecodeError as e:
            logger.error("[合并总结] JSON 解析失败，回退机械合并: %s", e)
            return await self._merge_drafts(drafts)

        return _build_recipe(data)

    async def _merge_drafts(self, drafts: list[RecipeDraft]) -> RecipeDraft:
        """合并多张图片的识别结果 — 取第一张非空 title + 去重合并 steps/ingredients/seasonings"""
        merged = RecipeDraft()
        seen_ingredients = set()
        seen_seasonings = set()
        seen_steps = set()

        for d in drafts:
            if d.title and not merged.title:
                merged.title = d.title
                merged.imageUrl = d.imageUrl
            if not merged.title and d.title:
                merged.title = d.title
                merged.imageUrl = merged.imageUrl or d.imageUrl

            for ing in d.ingredients:
                key = (ing.name, ing.quantity, ing.unit)
                if key not in seen_ingredients:
                    seen_ingredients.add(key)
                    merged.ingredients.append(ing)

            for sea in d.seasonings:
                key = (sea.name, sea.quantity, sea.unit)
                if key not in seen_seasonings:
                    seen_seasonings.add(key)
                    merged.seasonings.append(sea)

            for step in d.steps:
                if step.content and step.content not in seen_steps:
                    seen_steps.add(step.content)
                    merged.steps.append(step)

        return merged

    async def recognize(self, source_type: str, content: str | list[str], url_hint: str | None = None) -> RecipeDraft:
        """统一识别入口，自动选择最优策略。
        content 可以是单个字符串（链接模式或单张图片）或字符串列表（多张图片）。
        """
        # Mock 模式
        if settings.AI_MODE != "openai" or not settings.OPENAI_API_KEY:
            return self.recognize_mock(source_type, content)

        # 图片模式 — 支持多图
        if source_type == "image":
            if isinstance(content, list):
                try:
                    drafts = await asyncio.wait_for(
                        asyncio.gather(*[self.recognize_image(img) for img in content]),
                        timeout=settings.RECOGNIZE_TIMEOUT
                    )
                except asyncio.TimeoutError:
                    logger.warning("[识别超时] 多图 Vision 调用超过 %ds，丢弃未完成结果", settings.RECOGNIZE_TIMEOUT)
                    return RecipeDraft()
                return await self._consolidate_drafts(drafts)
            else:
                return await self.recognize_image(content)

        # 链接模式 — content 是单个 URL 字符串
        if source_type == "link":
            url = content if isinstance(content, str) else content[0]
            # 下厨房 → 直接 HTML 解析
            if _is_xiachufang_url(url) or (url_hint and _is_xiachufang_url(url_hint)):
                return await self.recognize_xiachufang(url)
            # 其他网页 → LLM 提取
            return await self.recognize_webpage(url)

        # 兜底
        logger.warning("Unknown source_type=%s, fallback to webpage", source_type)
        url = content if isinstance(content, str) else content[0] if content else ""
        return await self.recognize_webpage(url)


recognize_service = RecognizeService()

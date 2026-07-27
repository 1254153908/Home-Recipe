"""
HomeRecipe AI 识别假数据服务（Flask）
用于演示：Java 端调用本服务，模拟“根据链接/视频/图片自动识别菜谱内容并填充”。

启动：
    pip install -r requirements.txt
    python ai_mock_service.py
默认监听 http://localhost:5000
"""
from flask import Flask, request, jsonify
import random

app = Flask(__name__)

# 模拟 AI 识别结果的知识库（假数据）
RECIPES = [
    {
        "title": "红烧肉",
        "imageUrl": "https://example.com/img/hongshao.jpg",
        "steps": [
            {"content": "五花肉切块焯水", "imageUrl": "https://example.com/img/hongshao-1.jpg"},
            {"content": "冰糖炒糖色", "imageUrl": None},
            {"content": "下肉上色加生抽老抽", "imageUrl": None},
            {"content": "加水炖40分钟收汁", "imageUrl": "https://example.com/img/hongshao-4.jpg"}
        ],
        "ingredients": [
            {"name": "五花肉", "quantity": "500", "unit": "g"},
            {"name": "生姜", "quantity": "1", "unit": "块"},
            {"name": "大蒜", "quantity": "3", "unit": "瓣"}
        ],
        "seasonings": [
            {"name": "生抽", "quantity": "15", "unit": "ml"},
            {"name": "老抽", "quantity": "5", "unit": "ml"},
            {"name": "糖", "quantity": "20", "unit": "g"},
            {"name": "盐", "quantity": "3", "unit": "g"}
        ]
    },
    {
        "title": "西红柿炒蛋",
        "imageUrl": "https://example.com/img/tomato-egg.jpg",
        "steps": [
            {"content": "鸡蛋打散", "imageUrl": None},
            {"content": "番茄切块", "imageUrl": "https://example.com/img/tomato-2.jpg"},
            {"content": "炒蛋盛出", "imageUrl": None},
            {"content": "炒番茄出汁后回锅翻炒均匀", "imageUrl": None}
        ],
        "ingredients": [
            {"name": "西红柿", "quantity": "2", "unit": "个"},
            {"name": "鸡蛋", "quantity": "3", "unit": "个"}
        ],
        "seasonings": [
            {"name": "盐", "quantity": "2", "unit": "g"},
            {"name": "糖", "quantity": "3", "unit": "g"},
            {"name": "食用油", "quantity": "15", "unit": "ml"}
        ]
    },
    {
        "title": "青椒土豆丝",
        "imageUrl": "https://example.com/img/potato.jpg",
        "steps": [
            {"content": "土豆切丝泡水", "imageUrl": None},
            {"content": "青椒切丝", "imageUrl": None},
            {"content": "热油爆香蒜", "imageUrl": None},
            {"content": "快炒加盐醋出锅", "imageUrl": "https://example.com/img/potato-4.jpg"}
        ],
        "ingredients": [
            {"name": "土豆", "quantity": "2", "unit": "个"},
            {"name": "青椒", "quantity": "1", "unit": "个"},
            {"name": "大蒜", "quantity": "2", "unit": "瓣"}
        ],
        "seasonings": [
            {"name": "盐", "quantity": "3", "unit": "g"},
            {"name": "醋", "quantity": "5", "unit": "ml"},
            {"name": "食用油", "quantity": "15", "unit": "ml"}
        ]
    }
]


@app.route("/recognize", methods=["POST"])
def recognize():
    """模拟 AI 识别：接收 {sourceType, content}，返回一条假菜谱草稿。"""
    data = request.get_json(silent=True) or {}
    source_type = data.get("sourceType", "image")
    content = data.get("content", "")

    draft = dict(random.choice(RECIPES))
    # 回写识别依据，方便前端展示“识别自哪类来源”
    draft["sourceType"] = "ai"
    draft["sourceUrl"] = content
    draft["recognizedFrom"] = source_type
    return jsonify(draft)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

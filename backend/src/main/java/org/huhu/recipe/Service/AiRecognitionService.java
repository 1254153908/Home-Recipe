package org.huhu.recipe.Service;

import org.huhu.recipe.dto.AiRecognizeRequest;
import org.huhu.recipe.dto.RecipeDraft;

public interface AiRecognitionService {

    /** 调用 Python 假数据服务识别链接/视频/图片，返回可自动填充的菜谱草稿 */
    RecipeDraft recognize(AiRecognizeRequest request);
}

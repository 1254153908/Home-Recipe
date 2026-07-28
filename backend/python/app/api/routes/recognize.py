from fastapi import APIRouter
from app.schemas.request import RecognizeRequest
from app.schemas.response import RecipeDraft
from app.services.recognize_service import recognize_service

router = APIRouter(prefix="/recognize", tags=["AI Recognize"])


@router.post("", response_model=RecipeDraft)
async def recognize(request: RecognizeRequest) -> RecipeDraft:
    return await recognize_service.recognize(
        source_type=request.source_type,
        content=request.content,
    )

from fastapi import APIRouter
from app.api.routes.recognize import router as recognize_router

router = APIRouter(prefix="/api")
router.include_router(recognize_router)

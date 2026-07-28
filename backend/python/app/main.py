import logging
from fastapi import FastAPI
from app.core.config import server, log
from app.api.main import router as api_router
from app.middleware.log_middleware import LogMiddleware
from app.exceptions.handlers import global_exception_handler

logging.basicConfig(level=getattr(logging, log.LEVEL, logging.INFO), format=log.FORMAT)
logger = logging.getLogger("homerecipe")

app = FastAPI(
    title="HomeRecipe AI Service",
    description="AI recognition service for HomeRecipe — auto-extract recipe data from links",
    version="1.0.0",
    servers=[{"url": f"http://{server.HOST}:{server.PORT}", "description": "Local"}],
)

app.add_middleware(LogMiddleware)
app.add_exception_handler(Exception, global_exception_handler)
app.include_router(api_router)


@app.on_event("startup")
async def startup():
    logger.info("HomeRecipe AI service starting on %s:%s", server.HOST, server.PORT)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "HomeRecipe AI"}


if __name__ == "__main__":
    import uvicorn
    from app.core.config import server
    logger.info("Starting HomeRecipe AI service on %s:%s", server.HOST, server.PORT)
    uvicorn.run("app.main:app", host=server.HOST, port=server.PORT, reload=True)

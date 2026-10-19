"""AI Service main application."""

import logging
from typing import Final

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api import disease_detection, doctor, fertilizer, health, version
from app.core.errors import KrishiAiError

logger: Final = logging.getLogger("krishi-ai")

app = FastAPI(
    title="Krishi AI Service",
    version="1.0.0",
    description="Krishi Platform AI microservice (Developer B). Stateless engine, no DB ownership.",
)


@app.exception_handler(KrishiAiError)
async def krishi_error_handler(_: Request, exc: KrishiAiError) -> JSONResponse:
    """Render the contract section-4 error envelope for all expected failures."""
    return JSONResponse(status_code=exc.status_code, content=exc.to_response())


@app.exception_handler(RequestValidationError)
async def validation_error_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    message = "; ".join(
        f"{'.'.join(str(part) for part in err['loc'])}: {err['msg']}" for err in exc.errors()
    )
    return JSONResponse(
        status_code=400,
        content=KrishiAiError(message=message, error_code="VALIDATION_ERROR").to_response(),
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled error")
    return JSONResponse(
        status_code=500,
        content=KrishiAiError(message="Unexpected internal error.", error_code="INTERNAL_ERROR").to_response(),
    )


app.include_router(health.router)
app.include_router(version.router)
app.include_router(fertilizer.router)
app.include_router(doctor.router)
app.include_router(disease_detection.router)

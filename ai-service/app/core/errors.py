"""Contract-conforming error format (Contract section 4)."""


class KrishiAiError(Exception):
    """Raised for any failure that must render the contract error envelope."""

    def __init__(self, message: str, error_code: str = "INTERNAL_ERROR", status_code: int = 400) -> None:
        super().__init__(message)
        self.message = message
        self.error_code = error_code
        self.status_code = status_code

    def to_response(self) -> dict:
        return {"success": False, "errorCode": self.error_code, "message": self.message}

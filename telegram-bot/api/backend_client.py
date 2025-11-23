import requests
from typing import Optional, Dict, List, Any
import logging

logger = logging.getLogger(__name__)

class BackendClient:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url.rstrip('/')
        self.timeout = 10

    def check_health(self) -> bool:
        try:
            response = requests.get(
                f"{self.base_url}/actuator/health",
                timeout=self.timeout
            )
            return response.status_code == 200
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return False

    def get_user(self, telegram_id: int) -> Optional[Dict[str, Any]]:
        try:
            response = requests.get(
                f"{self.base_url}/api/v1/users/me",
                params={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 200:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Get user failed: {e}")
            return None

    def register_client(self, telegram_id: int) -> Optional[Dict[str, Any]]:
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/users/register",
                json={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 201:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Register client failed: {e}")
            return None

    def register_appraiser(self, telegram_id: int) -> Optional[Dict[str, Any]]:
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/users/register-appraiser",
                json={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 201:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Register appraiser failed: {e}")
            return None

    def create_order(self, telegram_id: int, car_ad_url: str, car_location: str, car_price: float) -> Optional[Dict[str, Any]]:
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/orders",
                params={"telegramId": telegram_id},
                json={
                    "carAdUrl": car_ad_url,
                    "carLocation": car_location,
                    "carPrice": car_price
                },
                timeout=self.timeout
            )
            if response.status_code == 201:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Create order failed: {e}")
            return None

    def get_orders(self, telegram_id: int) -> List[Dict[str, Any]]:
        try:
            response = requests.get(
                f"{self.base_url}/api/v1/orders",
                params={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 200:
                return response.json()
            return []
        except Exception as e:
            logger.error(f"Get orders failed: {e}")
            return []

    def get_order(self, order_id: int, telegram_id: int) -> Optional[Dict[str, Any]]:
        try:
            response = requests.get(
                f"{self.base_url}/api/v1/orders/{order_id}",
                params={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 200:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Get order failed: {e}")
            return None

    def assign_order(self, order_id: int, telegram_id: int) -> Optional[Dict[str, Any]]:
        try:
            response = requests.post(
                f"{self.base_url}/api/v1/orders/{order_id}/assign",
                params={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 200:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Assign order failed: {e}")
            return None

    def change_order_status(self, order_id: int, telegram_id: int, status: str) -> Optional[Dict[str, Any]]:
        try:
            response = requests.put(
                f"{self.base_url}/api/v1/orders/{order_id}/status",
                params={"telegramId": telegram_id},
                json={"status": status},
                timeout=self.timeout
            )
            if response.status_code == 200:
                return response.json()
            return None
        except Exception as e:
            logger.error(f"Change order status failed: {e}")
            return None

    def cancel_order(self, order_id: int, telegram_id: int) -> Optional[Dict[str, Any]]:
        return self.change_order_status(order_id, telegram_id, "COMPLETION_FAILURE")

    def upload_report(self, order_id: int, telegram_id: int, pdf_file: bytes) -> bool:
        try:
            if not isinstance(pdf_file, bytes):
                logger.error(f"PDF file must be bytes, got {type(pdf_file)}")
                return False
            
            if len(pdf_file) == 0:
                logger.error("PDF file is empty")
                return False
            
            files = {
                'file': ('report.pdf', pdf_file, 'application/pdf')
            }
            
            response = requests.post(
                f"{self.base_url}/api/v1/reports/orders/{order_id}",
                params={"telegramId": telegram_id},
                files=files,
                timeout=30
            )
            
            if response.status_code != 201:
                logger.error(f"Upload failed with status {response.status_code}: {response.text}")
            
            return response.status_code == 201
        except Exception as e:
            logger.error(f"Upload report failed: {e}")
            import traceback
            logger.error(traceback.format_exc())
            return False

    def get_report(self, order_id: int, telegram_id: int) -> Optional[bytes]:
        try:
            response = requests.get(
                f"{self.base_url}/api/v1/reports/orders/{order_id}",
                params={"telegramId": telegram_id},
                timeout=self.timeout
            )
            if response.status_code == 200:
                return response.content
            return None
        except Exception as e:
            logger.error(f"Get report failed: {e}")
            return None


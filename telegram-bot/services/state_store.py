from typing import Dict, Any, Optional
from enum import Enum

class ConversationState(Enum):
    IDLE = 0
    REGISTRATION_SELECT = 1
    ORDER_MAKE = 2
    ORDER_MODEL = 3
    ORDER_YEAR = 4
    ORDER_ADDRESS = 5
    REPORT_PHOTOS = 6
    REPORT_TEXT = 7

class StateStore:
    def __init__(self):
        self.user_states: Dict[int, ConversationState] = {}
        self.user_data: Dict[int, Dict[str, Any]] = {}

    def set_state(self, user_id: int, state: ConversationState):
        self.user_states[user_id] = state

    def get_state(self, user_id: int) -> ConversationState:
        return self.user_states.get(user_id, ConversationState.IDLE)

    def clear_state(self, user_id: int):
        self.user_states.pop(user_id, None)
        self.user_data.pop(user_id, None)

    def set_data(self, user_id: int, key: str, value: Any):
        if user_id not in self.user_data:
            self.user_data[user_id] = {}
        self.user_data[user_id][key] = value

    def get_data(self, user_id: int, key: str, default: Any = None) -> Any:
        return self.user_data.get(user_id, {}).get(key, default)

    def get_all_data(self, user_id: int) -> Dict[str, Any]:
        return self.user_data.get(user_id, {}).copy()

    def clear_data(self, user_id: int):
        self.user_data.pop(user_id, None)


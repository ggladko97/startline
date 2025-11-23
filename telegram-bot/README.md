# Telegram Bot for Appraise Service

Production-ready Python3 Telegram bot using python-telegram-bot SDK v21+.

## Features

- User registration (Client/Appraiser)
- Order creation wizard
- Order listing with pagination
- Order details and management
- Report submission with PDF generation
- Real-time notifications

## Setup

### Prerequisites

- Python 3.8+
- Backend service running on `http://localhost:8080`
- Telegram Bot Token

### Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Set environment variables:
```bash
export TELEGRAM_BOT_TOKEN="your_bot_token_here"
export BACKEND_URL="http://localhost:8080"
```

3. Run the bot:
```bash
python main.py
```

## Project Structure

```
telegram-bot/
├── main.py                 # Bot entry point
├── api/
│   └── backend_client.py   # Backend API client
├── flows/
│   ├── registration.py     # Registration flow
│   ├── order_creation.py  # Order creation wizard
│   ├── order_listing.py   # Order listing and details
│   └── report_flow.py      # Report submission flow
├── services/
│   ├── pdf_generator.py   # PDF report generation
│   └── state_store.py     # FSM state management
├── assets/                # Static assets
└── requirements.txt       # Python dependencies
```

## Flows

### FLOW 1 - Startup
- `/start` command checks backend health
- Verifies user registration
- Routes to registration or main menu

### FLOW 2 - Registration
- User selects role (Client/Appraiser)
- Registers via backend API
- Redirects to main menu

### FLOW 3 - Main Menu
- Place Order (clients only)
- My Orders
- Help

### FLOW 4 - Order Creation
- Wizard collects: make, model, year, address
- Creates order via backend
- Notifies appraisers

### FLOW 5 - Appraiser Notifications
- Backend triggers notifications
- Appraisers receive order details
- Accept button assigns order

### FLOW 7 - Order Listing
- Paginated list of user orders
- Click to view details

### FLOW 8 - Order Details
- Client view: status, report, cancel option
- Appraiser view: submit report, cancel, postpone

### FLOW 9 - Report Submission
- Photo upload (multiple)
- Text description
- PDF generation with photos
- Upload to backend
- Client notification

## API Integration

The bot communicates with the backend service at `/api/v1/`:

- `GET /actuator/health` - Health check
- `GET /users/me?telegramId={id}` - Get user
- `POST /users/register` - Register client
- `POST /users/register-appraiser` - Register appraiser
- `POST /orders?telegramId={id}` - Create order
- `GET /orders?telegramId={id}` - List orders
- `GET /orders/{id}?telegramId={id}` - Get order
- `POST /orders/{id}/assign?telegramId={id}` - Assign order
- `PUT /orders/{id}/status?telegramId={id}` - Cancel order
- `POST /reports/orders/{id}?telegramId={id}` - Upload report
- `GET /reports/orders/{id}?telegramId={id}` - Get report

## State Management

Uses FSM (Finite State Machine) with ConversationHandler:
- `IDLE` - No active conversation
- `REGISTRATION_SELECT` - Registration role selection
- `ORDER_MAKE` - Order creation: make
- `ORDER_MODEL` - Order creation: model
- `ORDER_YEAR` - Order creation: year
- `ORDER_ADDRESS` - Order creation: address
- `REPORT_PHOTOS` - Report submission: photos
- `REPORT_TEXT` - Report submission: text

## Error Handling

- Backend unavailability: Shows user-friendly message
- Invalid input: Prompts for correction
- Network errors: Logged and handled gracefully

## PDF Generation

Reports include:
- Order information
- Car details (make, model, year, location)
- Appraiser information
- Text description
- Photo evidence (4 per page, 2x2 grid)

## Notes

- Bot uses polling (not webhooks)
- State is stored in memory (not persistent)
- Photos are stored in memory during report creation
- Backend must be accessible for bot to function
- Appraiser notifications require known appraiser telegram IDs

## Development

To extend the bot:
1. Add new flows in `flows/` directory
2. Register handlers in `main.py`
3. Update state store if needed
4. Add API methods in `backend_client.py`


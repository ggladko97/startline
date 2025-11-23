from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ContextTypes, ConversationHandler
from api.backend_client import BackendClient
from services.state_store import ConversationState, StateStore

async def start_order_creation(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    if query:
        await query.answer()
    
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    state_store.set_state(telegram_id, ConversationState.ORDER_MAKE)
    state_store.clear_data(telegram_id)
    
    if query:
        await query.edit_message_text("Provide car make.")
    else:
        await update.message.reply_text("Provide car make.")
    
    return ConversationState.ORDER_MAKE.value

async def handle_order_make(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    make = update.message.text.strip()
    state_store.set_data(telegram_id, 'make', make)
    state_store.set_state(telegram_id, ConversationState.ORDER_MODEL)
    
    await update.message.reply_text("Provide car model.")
    return ConversationState.ORDER_MODEL.value

async def handle_order_model(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    model = update.message.text.strip()
    state_store.set_data(telegram_id, 'model', model)
    state_store.set_state(telegram_id, ConversationState.ORDER_YEAR)
    
    await update.message.reply_text("Provide car year.")
    return ConversationState.ORDER_YEAR.value

async def handle_order_year(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    try:
        year = int(update.message.text.strip())
        state_store.set_data(telegram_id, 'year', year)
        state_store.set_state(telegram_id, ConversationState.ORDER_ADDRESS)
        
        await update.message.reply_text("Provide address or location URL.")
        return ConversationState.ORDER_ADDRESS.value
    except ValueError:
        await update.message.reply_text("Please provide a valid year (number).")
        return ConversationState.ORDER_YEAR.value

async def handle_order_address(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    backend = context.bot_data.get('backend')
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await update.message.reply_text("Backend is not available. Try again later.")
        state_store.clear_state(telegram_id)
        return ConversationHandler.END
    
    address_url = update.message.text.strip()
    data = state_store.get_all_data(telegram_id)
    
    make = data.get('make', '')
    model = data.get('model', '')
    year = data.get('year', '')
    
    car_ad_url = f"{make} {model} {year}"
    car_location = address_url
    car_price = 100
    
    order = backend.create_order(telegram_id, car_ad_url, car_location, car_price)
    
    if order:
        state_store.clear_state(telegram_id)
        await update.message.reply_text("Order created. Appraisers will be notified.")
        
        notification_func = context.bot_data.get('send_notification_to_appraisers')
        if notification_func:
            try:
                notification_func(order.get('id'), order)
            except Exception:
                pass
        
        keyboard = [[InlineKeyboardButton("Back to Menu", callback_data="main_menu")]]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await update.message.reply_text("Main Menu", reply_markup=reply_markup)
    else:
        await update.message.reply_text("Failed to create order. Backend is not available.")
        state_store.clear_state(telegram_id)
    
    return ConversationHandler.END

async def cancel_order_creation(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    state_store.clear_state(telegram_id)
    
    await update.message.reply_text("Order creation cancelled.")
    return ConversationHandler.END


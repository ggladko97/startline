from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ContextTypes, ConversationHandler
from api.backend_client import BackendClient
from services.state_store import ConversationState, StateStore

async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    backend = context.bot_data.get('backend')
    state_store = context.bot_data.get('state_store')
    
    if not backend.check_health():
        await update.message.reply_text("Backend is not available. Try again later.")
        return ConversationHandler.END
    
    telegram_id = update.effective_user.id
    user = backend.get_user(telegram_id)
    
    if user is None:
        keyboard = [
            [InlineKeyboardButton("Register as Client", callback_data="register_client")],
            [InlineKeyboardButton("Register as Appraiser", callback_data="register_appraiser")]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await update.message.reply_text(
            "How would you like to register?",
            reply_markup=reply_markup
        )
        state_store.set_state(telegram_id, ConversationState.REGISTRATION_SELECT)
        return ConversationState.REGISTRATION_SELECT.value
    else:
        return await show_main_menu(update, context)

async def registration_callback(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    backend = context.bot_data.get('backend')
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    if query.data == "register_client":
        user = backend.register_client(telegram_id)
        if user:
            state_store.clear_state(telegram_id)
            await query.edit_message_text("Registration successful! Welcome.")
            return await show_main_menu_from_query(update, context)
        else:
            await query.edit_message_text("Registration failed. Backend is not available.")
            return ConversationHandler.END
    elif query.data == "register_appraiser":
        user = backend.register_appraiser(telegram_id)
        if user:
            state_store.clear_state(telegram_id)
            await query.edit_message_text("You are registered as Appraiser")
            return await show_main_menu_from_query(update, context)
        else:
            await query.edit_message_text("Registration failed. Backend is not available.")
            return ConversationHandler.END
    
    return ConversationHandler.END

async def show_main_menu(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    user = backend.get_user(telegram_id)
    
    if not user:
        return ConversationHandler.END
    
    role = user.get('role', 'CLIENT')
    keyboard = []
    
    if role == 'CLIENT':
        keyboard.append([InlineKeyboardButton("Place Order", callback_data="place_order")])
    
    keyboard.append([InlineKeyboardButton("My Orders", callback_data="my_orders")])
    keyboard.append([InlineKeyboardButton("Help", callback_data="help")])
    
    reply_markup = InlineKeyboardMarkup(keyboard)
    await update.message.reply_text(
        "Main Menu",
        reply_markup=reply_markup
    )
    return ConversationHandler.END

async def show_main_menu_from_query(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    if query:
        await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    user = backend.get_user(telegram_id)
    
    if not user:
        return ConversationHandler.END
    
    role = user.get('role', 'CLIENT')
    keyboard = []
    
    if role == 'CLIENT':
        keyboard.append([InlineKeyboardButton("Place Order", callback_data="place_order")])
    
    keyboard.append([InlineKeyboardButton("My Orders", callback_data="my_orders")])
    keyboard.append([InlineKeyboardButton("Help", callback_data="help")])
    
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    if query:
        await query.message.reply_text(
            "Main Menu",
            reply_markup=reply_markup
        )
    else:
        await update.message.reply_text(
            "Main Menu",
            reply_markup=reply_markup
        )
    
    return ConversationHandler.END

async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    if query:
        await query.answer()
        await query.edit_message_text(
            "Help:\n\n"
            "• Place Order: Create a new appraisal order\n"
            "• My Orders: View your orders\n"
            "• Help: Show this help message\n\n"
            "Use /start to return to the main menu."
        )
    else:
        await update.message.reply_text(
            "Help:\n\n"
            "• Place Order: Create a new appraisal order\n"
            "• My Orders: View your orders\n"
            "• Help: Show this help message\n\n"
            "Use /start to return to the main menu."
        )
    
    return await show_main_menu(update, context) if not query else ConversationHandler.END


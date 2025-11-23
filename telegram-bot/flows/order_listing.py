import os
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ContextTypes, ConversationHandler
from api.backend_client import BackendClient
from services.state_store import StateStore

ITEMS_PER_PAGE = 5

async def show_orders(update: Update, context: ContextTypes.DEFAULT_TYPE, page: int = 0) -> int:
    query = update.callback_query
    if query:
        await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        message = "Backend is not available. Try again later."
        if query:
            await query.edit_message_text(message)
        else:
            await update.message.reply_text(message)
        return ConversationHandler.END
    
    orders = backend.get_orders(telegram_id)
    
    if not orders:
        message = "You have no orders."
        if query:
            await query.edit_message_text(message)
        else:
            await update.message.reply_text(message)
        return ConversationHandler.END
    
    total_pages = (len(orders) + ITEMS_PER_PAGE - 1) // ITEMS_PER_PAGE
    start_idx = page * ITEMS_PER_PAGE
    end_idx = min(start_idx + ITEMS_PER_PAGE, len(orders))
    page_orders = orders[start_idx:end_idx]
    
    text = "Your Orders:\n\n"
    for i, order in enumerate(page_orders, start=start_idx + 1):
        status = order.get('status', 'UNKNOWN')
        order_id = order.get('id', 'N/A')
        text += f"{i}. Order #{order_id} - {status}\n"
    
    keyboard = []
    
    if total_pages > 1:
        page_buttons = []
        for p in range(total_pages):
            if p == page:
                page_buttons.append(InlineKeyboardButton(f"[{p+1}]", callback_data=f"page_{p}"))
            else:
                page_buttons.append(InlineKeyboardButton(str(p+1), callback_data=f"page_{p}"))
        keyboard.append(page_buttons)
    
    order_buttons = []
    for order in page_orders:
        order_id = order.get('id')
        order_buttons.append([InlineKeyboardButton(
            f"Order #{order_id}",
            callback_data=f"order_detail_{order_id}"
        )])
    keyboard.extend(order_buttons)
    
    keyboard.append([InlineKeyboardButton("Back to Menu", callback_data="main_menu")])
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    if query:
        await query.edit_message_text(text, reply_markup=reply_markup)
    else:
        await update.message.reply_text(text, reply_markup=reply_markup)
    
    return ConversationHandler.END

async def handle_page_callback(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    try:
        page = int(query.data.split('_')[1])
    except (ValueError, IndexError):
        page = 0
    
    return await show_orders(update, context, page=page)

async def show_order_details(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await query.edit_message_text("Backend is not available. Try again later.")
        return ConversationHandler.END
    
    order_id = int(query.data.split('_')[-1])
    order = backend.get_order(order_id, telegram_id)
    
    if not order:
        await query.edit_message_text("Order not found.")
        return ConversationHandler.END
    
    user = backend.get_user(telegram_id)
    role = user.get('role', 'CLIENT') if user else 'CLIENT'
    
    status = order.get('status', 'UNKNOWN')
    car_ad_url = order.get('carAdUrl', 'N/A')
    car_location = order.get('carLocation', 'N/A')
    appraiser_id = order.get('appraiserId')
    report_id = order.get('reportId')
    
    text = f"Order #{order_id}\n\n"
    text += f"Status: {status}\n"
    text += f"Car: {car_ad_url}\n"
    text += f"Location: {car_location}\n"
    
    if appraiser_id:
        text += f"Appraiser ID: {appraiser_id}\n"
    
    if report_id:
        text += f"Report ID: {report_id}\n"
    
    keyboard = []
    
    if role == 'CLIENT':
        if status in ['CREATED', 'APPRAISOR_SEARCH']:
            keyboard.append([InlineKeyboardButton("Cancel Order", callback_data=f"cancel_order_{order_id}")])
        
        if report_id:
            keyboard.append([InlineKeyboardButton("View Report", callback_data=f"view_report_{order_id}")])
    
    elif role == 'APPRAISER':
        if status == 'APPRAISOR_SEARCH' and appraiser_id:
            keyboard.append([InlineKeyboardButton("Start Appraisal", callback_data=f"start_appraisal_{order_id}")])
        elif status in ['ASSIGNED', 'IN_PROGRESS']:
            keyboard.append([InlineKeyboardButton("Submit Report", callback_data=f"submit_report_{order_id}")])
        keyboard.append([InlineKeyboardButton("Cancel Order", callback_data=f"cancel_order_{order_id}")])
        keyboard.append([InlineKeyboardButton("Postpone Order", callback_data=f"postpone_order_{order_id}")])
    
    keyboard.append([InlineKeyboardButton("Back to Orders", callback_data="my_orders")])
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    await query.edit_message_text(text, reply_markup=reply_markup)
    return ConversationHandler.END

async def handle_cancel_order(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await query.edit_message_text("Backend is not available. Try again later.")
        return ConversationHandler.END
    
    order_id = int(query.data.split('_')[-1])
    result = backend.cancel_order(order_id, telegram_id)
    
    if result:
        await query.edit_message_text(f"Order #{order_id} has been cancelled.")
    else:
        await query.edit_message_text("Failed to cancel order. Backend is not available.")
    
    return ConversationHandler.END

async def handle_assign_order(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await query.edit_message_text("Backend is not available. Try again later.")
        return ConversationHandler.END
    
    order_id = int(query.data.split('_')[-1])
    order = backend.assign_order(order_id, telegram_id)
    
    if order:
        await query.edit_message_text(f"Order assigned to you.")
        
        appraiser_telegram_id = telegram_id
        appraiser_user = backend.get_user(telegram_id)
        if appraiser_user:
            appraiser_telegram_id = appraiser_user.get('telegramId', telegram_id)
        
        client_id = order.get('clientId')
        if client_id:
            try:
                appraiser_whitelist = os.getenv('APPRAISER_WHITELIST', '123456789,987654321')
                test_telegram_ids = [int(tid.strip()) for tid in appraiser_whitelist.split(',') if tid.strip().isdigit()]
                
                for test_telegram_id in test_telegram_ids:
                    test_user = backend.get_user(test_telegram_id)
                    if test_user and test_user.get('id') == client_id:
                        await context.bot.send_message(
                            chat_id=test_telegram_id,
                            text=f"Your order was picked up by appraiser {appraiser_telegram_id}."
                        )
                        break
            except Exception:
                pass
    else:
        await query.edit_message_text("Failed to assign order. Backend is not available.")
    
    return ConversationHandler.END

async def handle_start_appraisal(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await query.edit_message_text("Backend is not available. Try again later.")
        return ConversationHandler.END
    
    order_id = int(query.data.split('_')[-1])
    order = backend.get_order(order_id, telegram_id)
    
    if not order:
        await query.edit_message_text("Order not found.")
        return ConversationHandler.END
    
    current_status = order.get('status')
    if current_status == 'APPRAISOR_SEARCH':
        result = backend.change_order_status(order_id, telegram_id, 'ASSIGNED')
        if result:
            updated_order = backend.get_order(order_id, telegram_id)
            if updated_order:
                user = backend.get_user(telegram_id)
                role = user.get('role', 'APPRAISER') if user else 'APPRAISER'
                
                status = updated_order.get('status', 'UNKNOWN')
                car_ad_url = updated_order.get('carAdUrl', 'N/A')
                car_location = updated_order.get('carLocation', 'N/A')
                appraiser_id = updated_order.get('appraiserId')
                report_id = updated_order.get('reportId')
                
                text = f"Order #{order_id}\n\n"
                text += f"Status: {status}\n"
                text += f"Car: {car_ad_url}\n"
                text += f"Location: {car_location}\n"
                
                if appraiser_id:
                    text += f"Appraiser ID: {appraiser_id}\n"
                
                if report_id:
                    text += f"Report ID: {report_id}\n"
                
                keyboard = []
                if status in ['ASSIGNED', 'IN_PROGRESS']:
                    keyboard.append([InlineKeyboardButton("Submit Report", callback_data=f"submit_report_{order_id}")])
                keyboard.append([InlineKeyboardButton("Cancel Order", callback_data=f"cancel_order_{order_id}")])
                keyboard.append([InlineKeyboardButton("Postpone Order", callback_data=f"postpone_order_{order_id}")])
                keyboard.append([InlineKeyboardButton("Back to Orders", callback_data="my_orders")])
                reply_markup = InlineKeyboardMarkup(keyboard)
                
                await query.edit_message_text(text, reply_markup=reply_markup)
            else:
                await query.edit_message_text(f"Appraisal started for order #{order_id}. You can now submit the report.")
        else:
            await query.edit_message_text("Failed to start appraisal. Backend is not available.")
    elif current_status in ['ASSIGNED', 'IN_PROGRESS']:
        await query.edit_message_text(f"Appraisal already started for order #{order_id}.")
    else:
        await query.edit_message_text(f"Cannot start appraisal. Order status is {current_status}.")
    
    return ConversationHandler.END

async def handle_postpone_order(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    order_id = int(query.data.split('_')[-1])
    await query.edit_message_text(f"Postpone functionality not implemented yet for order #{order_id}.")
    
    return ConversationHandler.END

async def handle_view_report(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    await query.answer()
    
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await query.edit_message_text("Backend is not available. Try again later.")
        return ConversationHandler.END
    
    order_id = int(query.data.split('_')[-1])
    order = backend.get_order(order_id, telegram_id)
    
    if not order:
        await query.edit_message_text("Order not found.")
        return ConversationHandler.END
    
    report_id = order.get('reportId')
    if not report_id:
        await query.edit_message_text("Report not available yet.")
        return ConversationHandler.END
    
    pdf_data = backend.get_report(order_id, telegram_id)
    
    if pdf_data:
        await context.bot.send_document(
            chat_id=telegram_id,
            document=pdf_data,
            filename=f"report_{order_id}.pdf"
        )
        await query.edit_message_text("Report sent.")
    else:
        await query.edit_message_text("Failed to retrieve report. Backend is not available.")
    
    return ConversationHandler.END

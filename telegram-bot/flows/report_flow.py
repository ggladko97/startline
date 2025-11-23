import os
from telegram import Update
from telegram.ext import ContextTypes, ConversationHandler
from api.backend_client import BackendClient
from services.state_store import ConversationState, StateStore
from services.pdf_generator import generate_report_pdf
from io import BytesIO

async def start_report_submission(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    query = update.callback_query
    if query:
        await query.answer()
    
    state_store = context.bot_data.get('state_store')
    backend = context.bot_data.get('backend')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        message = "Backend is not available. Try again later."
        if query:
            await query.edit_message_text(message)
        else:
            await update.message.reply_text(message)
        return ConversationHandler.END
    
    order_id = int(query.data.split('_')[-1])
    order = backend.get_order(order_id, telegram_id)
    
    if not order:
        message = "Order not found."
        if query:
            await query.edit_message_text(message)
        else:
            await update.message.reply_text(message)
        return ConversationHandler.END
    
    state_store.set_state(telegram_id, ConversationState.REPORT_PHOTOS)
    state_store.set_data(telegram_id, 'order_id', order_id)
    state_store.set_data(telegram_id, 'photos', [])
    
    message = "Provide photo evidence. Send multiple photos. Type /done when finished."
    if query:
        await query.edit_message_text(message)
    else:
        await update.message.reply_text(message)
    
    return ConversationState.REPORT_PHOTOS.value

async def handle_report_photo(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    if update.message.photo:
        photo = update.message.photo[-1]
        photo_file = await context.bot.get_file(photo.file_id)
        photo_data = await photo_file.download_as_bytearray()
        
        photos = state_store.get_data(telegram_id, 'photos', [])
        photos.append(photo_data)
        state_store.set_data(telegram_id, 'photos', photos)
        
        await update.message.reply_text(f"Photo received ({len(photos)} total). Send more or type /done to continue.")
        return ConversationState.REPORT_PHOTOS.value
    else:
        await update.message.reply_text("Please send a photo.")
        return ConversationState.REPORT_PHOTOS.value

async def handle_report_photos_done(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    state_store.set_state(telegram_id, ConversationState.REPORT_TEXT)
    await update.message.reply_text("Provide text about the car's state.")
    
    return ConversationState.REPORT_TEXT.value

async def handle_report_text(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    backend = context.bot_data.get('backend')
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    
    if not backend.check_health():
        await update.message.reply_text("Backend is not available. Try again later.")
        state_store.clear_state(telegram_id)
        return ConversationHandler.END
    
    text_description = update.message.text.strip()
    order_id = state_store.get_data(telegram_id, 'order_id')
    photos = state_store.get_data(telegram_id, 'photos', [])
    
    order = backend.get_order(order_id, telegram_id)
    if not order:
        await update.message.reply_text("Order not found.")
        state_store.clear_state(telegram_id)
        return ConversationHandler.END
    
    user = backend.get_user(telegram_id)
    appraiser_name = f"Appraiser {telegram_id}"
    if user:
        appraiser_name = f"Appraiser {user.get('telegramId', telegram_id)}"
    
    car_ad_url = order.get('carAdUrl', 'N/A')
    car_location = order.get('carLocation', 'N/A')
    
    parts = car_ad_url.split()
    car_make = parts[0] if len(parts) > 0 else 'N/A'
    car_model = ' '.join(parts[1:-1]) if len(parts) > 2 else 'N/A'
    car_year = parts[-1] if len(parts) > 1 else 'N/A'
    
    await update.message.reply_text("Generating report PDF...")
    
    try:
        pdf_data = generate_report_pdf(
            order_title=f"Order #{order_id}",
            car_make=car_make,
            car_model=car_model,
            car_year=car_year,
            address_url=car_location,
            appraiser_name=appraiser_name,
            text_description=text_description,
            photos=photos
        )
        
        success = backend.upload_report(order_id, telegram_id, pdf_data)
        
        if success:
            state_store.clear_state(telegram_id)
            await update.message.reply_text(f"Report ready for order {order_id}.")
            
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
                                text=f"Report ready for order {order_id}."
                            )
                            break
                except Exception:
                    pass
        else:
            await update.message.reply_text("Failed to upload report. Backend is not available.")
            state_store.clear_state(telegram_id)
    except Exception as e:
        await update.message.reply_text(f"Failed to generate report: {str(e)}")
        state_store.clear_state(telegram_id)
    
    return ConversationHandler.END

async def cancel_report_submission(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    state_store = context.bot_data.get('state_store')
    telegram_id = update.effective_user.id
    state_store.clear_state(telegram_id)
    
    await update.message.reply_text("Report submission cancelled.")
    return ConversationHandler.END


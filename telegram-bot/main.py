import os
import logging
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import (
    Application,
    CommandHandler,
    CallbackQueryHandler,
    MessageHandler,
    ConversationHandler,
    filters
)
from api.backend_client import BackendClient
from services.state_store import StateStore, ConversationState
from flows.registration import (
    start_command,
    registration_callback,
    show_main_menu,
    show_main_menu_from_query,
    help_command
)
from flows.order_creation import (
    start_order_creation,
    handle_order_make,
    handle_order_model,
    handle_order_year,
    handle_order_address,
    cancel_order_creation
)
from flows.order_listing import (
    show_orders,
    handle_page_callback,
    show_order_details,
    handle_cancel_order,
    handle_assign_order,
    handle_start_appraisal,
    handle_postpone_order,
    handle_view_report
)
from flows.report_flow import (
    start_report_submission,
    handle_report_photo,
    handle_report_photos_done,
    handle_report_text,
    cancel_report_submission
)

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

def setup_handlers(application: Application):
    backend = BackendClient(base_url=os.getenv('BACKEND_URL', 'http://localhost:8080'))
    state_store = StateStore()
    
    application.bot_data['backend'] = backend
    application.bot_data['state_store'] = state_store
    
    registration_handler = ConversationHandler(
        entry_points=[CommandHandler('start', start_command)],
        states={
            ConversationState.REGISTRATION_SELECT.value: [
                CallbackQueryHandler(registration_callback, pattern='^register_(client|appraiser)$')
            ],
        },
        fallbacks=[CommandHandler('start', start_command)],
        name='registration'
    )
    
    order_creation_handler = ConversationHandler(
        entry_points=[CallbackQueryHandler(start_order_creation, pattern='^place_order$')],
        states={
            ConversationState.ORDER_MAKE.value: [
                MessageHandler(filters.TEXT & ~filters.COMMAND, handle_order_make)
            ],
            ConversationState.ORDER_MODEL.value: [
                MessageHandler(filters.TEXT & ~filters.COMMAND, handle_order_model)
            ],
            ConversationState.ORDER_YEAR.value: [
                MessageHandler(filters.TEXT & ~filters.COMMAND, handle_order_year)
            ],
            ConversationState.ORDER_ADDRESS.value: [
                MessageHandler(filters.TEXT & ~filters.COMMAND, handle_order_address)
            ],
        },
        fallbacks=[CommandHandler('cancel', cancel_order_creation)],
        name='order_creation'
    )
    
    report_submission_handler = ConversationHandler(
        entry_points=[CallbackQueryHandler(start_report_submission, pattern='^submit_report_')],
        states={
            ConversationState.REPORT_PHOTOS.value: [
                MessageHandler(filters.PHOTO, handle_report_photo),
                CommandHandler('done', handle_report_photos_done)
            ],
            ConversationState.REPORT_TEXT.value: [
                MessageHandler(filters.TEXT & ~filters.COMMAND, handle_report_text)
            ],
        },
        fallbacks=[CommandHandler('cancel', cancel_report_submission)],
        name='report_submission'
    )
    
    application.add_handler(registration_handler)
    application.add_handler(order_creation_handler)
    application.add_handler(report_submission_handler)
    
    application.add_handler(CallbackQueryHandler(show_main_menu_from_query, pattern='^main_menu$'))
    application.add_handler(CallbackQueryHandler(help_command, pattern='^help$'))
    application.add_handler(CallbackQueryHandler(show_orders, pattern='^my_orders$'))
    application.add_handler(CallbackQueryHandler(handle_page_callback, pattern='^page_'))
    application.add_handler(CallbackQueryHandler(show_order_details, pattern='^order_detail_'))
    application.add_handler(CallbackQueryHandler(handle_cancel_order, pattern='^cancel_order_'))
    application.add_handler(CallbackQueryHandler(handle_assign_order, pattern='^accept_order_'))
    application.add_handler(CallbackQueryHandler(handle_start_appraisal, pattern='^start_appraisal_'))
    application.add_handler(CallbackQueryHandler(handle_postpone_order, pattern='^postpone_order_'))
    application.add_handler(CallbackQueryHandler(handle_view_report, pattern='^view_report_'))
    
    application.add_handler(CommandHandler('help', help_command))

def send_notification_to_appraisers(application: Application, order_id: int, order_data: dict):
    backend = application.bot_data.get('backend')
    
    if not backend.check_health():
        logger.error("Backend unavailable for notifications")
        return
    
    appraiser_telegram_ids = []
    
    appraiser_whitelist = os.getenv('APPRAISER_WHITELIST', '123456789,987654321')
    test_telegram_ids = [int(tid.strip()) for tid in appraiser_whitelist.split(',') if tid.strip().isdigit()]
    
    for telegram_id in test_telegram_ids:
        try:
            user = backend.get_user(telegram_id)
            if user and user.get('role') == 'APPRAISER':
                appraiser_telegram_ids.append(telegram_id)
        except Exception:
            continue
    
    if not appraiser_telegram_ids:
        logger.warning("No appraisers found for notification")
        return
    
    car_ad_url = order_data.get('carAdUrl', 'N/A')
    car_location = order_data.get('carLocation', 'N/A')
    
    message = (
        f"New Order #{order_id}\n\n"
        f"Car: {car_ad_url}\n"
        f"Location: {car_location}"
    )
    
    keyboard = [[InlineKeyboardButton("Accept Order", callback_data=f"accept_order_{order_id}")]]
    reply_markup = InlineKeyboardMarkup(keyboard)
    
    for appraiser_id in appraiser_telegram_ids:
        try:
            application.bot.send_message(
                chat_id=appraiser_id,
                text=message,
                reply_markup=reply_markup
            )
        except Exception as e:
            logger.error(f"Failed to send notification to appraiser {appraiser_id}: {e}")

def main():
    token = os.getenv('TELEGRAM_BOT_TOKEN')
    if not token:
        raise ValueError("TELEGRAM_BOT_TOKEN environment variable is required")
    
    application = Application.builder().token(token).build()
    
    setup_handlers(application)
    
    application.bot_data['send_notification_to_appraisers'] = lambda order_id, order_data: send_notification_to_appraisers(application, order_id, order_data)
    
    logger.info("Bot starting...")
    application.run_polling(allowed_updates=Update.ALL_TYPES)

if __name__ == '__main__':
    main()


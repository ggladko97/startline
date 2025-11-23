from reportlab.lib.pagesizes import A4
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Image, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib import colors
from io import BytesIO
from PIL import Image as PILImage

def generate_report_pdf(
    order_title: str,
    car_make: str,
    car_model: str,
    car_year: str,
    address_url: str,
    appraiser_name: str,
    text_description: str,
    photos: list
) -> bytes:
    buffer = BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=A4)
    story = []
    styles = getSampleStyleSheet()

    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Heading1'],
        fontSize=18,
        textColor=colors.HexColor('#1a1a1a'),
        spaceAfter=30,
        alignment=TA_CENTER
    )

    heading_style = ParagraphStyle(
        'CustomHeading',
        parent=styles['Heading2'],
        fontSize=14,
        textColor=colors.HexColor('#2c3e50'),
        spaceAfter=12,
        spaceBefore=12
    )

    normal_style = ParagraphStyle(
        'CustomNormal',
        parent=styles['Normal'],
        fontSize=11,
        textColor=colors.HexColor('#34495e'),
        spaceAfter=10,
        alignment=TA_LEFT
    )

    story.append(Paragraph(order_title, title_style))
    story.append(Spacer(1, 0.3*inch))

    story.append(Paragraph("<b>Car Information</b>", heading_style))
    story.append(Paragraph(f"<b>Make:</b> {car_make}", normal_style))
    story.append(Paragraph(f"<b>Model:</b> {car_model}", normal_style))
    story.append(Paragraph(f"<b>Year:</b> {car_year}", normal_style))
    story.append(Paragraph(f"<b>Location:</b> {address_url}", normal_style))
    story.append(Spacer(1, 0.2*inch))

    story.append(Paragraph("<b>Appraiser</b>", heading_style))
    story.append(Paragraph(f"{appraiser_name}", normal_style))
    story.append(Spacer(1, 0.2*inch))

    story.append(Paragraph("<b>Description</b>", heading_style))
    story.append(Paragraph(text_description, normal_style))
    story.append(Spacer(1, 0.3*inch))

    if photos:
        story.append(Paragraph("<b>Photo Evidence</b>", heading_style))
        story.append(Spacer(1, 0.1*inch))

        photos_per_page = 4
        photos_per_row = 2
        img_size = 2.5 * inch

        for i in range(0, len(photos), photos_per_page):
            page_photos = photos[i:i + photos_per_page]
            
            for row_start in range(0, len(page_photos), photos_per_row):
                row_photos = page_photos[row_start:row_start + photos_per_row]
                
                row_elements = []
                for photo_data in row_photos:
                    try:
                        img = PILImage.open(BytesIO(photo_data))
                        img.thumbnail((int(img_size), int(img_size)), PILImage.Resampling.LANCZOS)
                        img_buffer = BytesIO()
                        img.save(img_buffer, format='PNG')
                        img_buffer.seek(0)
                        
                        reportlab_img = Image(img_buffer, width=img_size, height=img_size)
                        row_elements.append(reportlab_img)
                    except Exception:
                        continue
                
                if row_elements:
                    table_data = [[img for img in row_elements]]
                    table = Table(table_data, colWidths=[img_size] * len(row_elements))
                    table.setStyle(TableStyle([
                        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
                    ]))
                    story.append(table)
                    story.append(Spacer(1, 0.1*inch))

            if i + photos_per_page < len(photos):
                story.append(PageBreak())

    doc.build(story)
    buffer.seek(0)
    return buffer.getvalue()


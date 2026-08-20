package com.udc.collection.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.domain.model.Patient
import com.udc.collection.domain.model.PaymentStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class PdfResult {
    data class Success(val file: File) : PdfResult()
    data class Error(val message: String) : PdfResult()
}

@Singleton
class PdfReceiptGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // A4 at 72 dpi
    private val PAGE_WIDTH = 595
    private val PAGE_HEIGHT = 842
    private val MARGIN = 40f
    private val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

    fun generate(patient: Patient, agentName: String): PdfResult {
        return runCatching {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            draw(page.canvas, patient, agentName)
            document.finishPage(page)

            val dir = File(context.getExternalFilesDir(null), "UDC_Receipts").apply { mkdirs() }
            val file = File(dir, "Receipt_${patient.receiptNumber.replace("-", "_")}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            PdfResult.Success(file)
        }.getOrElse { e -> PdfResult.Error(e.localizedMessage ?: "PDF generation failed") }
    }

    private fun draw(canvas: Canvas, patient: Patient, agentName: String) {
        val dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
        var y = MARGIN

        // ── Header ──────────────────────────────────────────────────────────
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("URBAN DIAGNOSTIC CENTRE", MARGIN, y + 20f, headerPaint)

        val subPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        canvas.drawText("Professional Blood Collection Services", MARGIN, y + 38f, subPaint)

        // Horizontal rule
        y += 52f
        val rulePaint = Paint().apply { color = Color.parseColor("#1565C0"); strokeWidth = 2f }
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, rulePaint)

        // Receipt number + date on right
        val rightPaint = Paint().apply {
            color = Color.DKGRAY; textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Receipt: ${patient.receiptNumber}", MARGIN + CONTENT_WIDTH, y - 20f, rightPaint)
        canvas.drawText(patient.date.format(dateFormat), MARGIN + CONTENT_WIDTH, y - 8f, rightPaint)

        // ── Patient Info ────────────────────────────────────────────────────
        y += 18f
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#1565C0"); textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PATIENT INFORMATION", MARGIN, y, sectionPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, rulePaint.apply { strokeWidth = 0.5f })
        y += 14f

        val labelPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        val valuePaint = Paint().apply {
            color = Color.BLACK; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        y = drawInfoRow(canvas, y, "Patient No.", patient.patientNumber, labelPaint, valuePaint)
        y = drawInfoRow(canvas, y, "Patient Name", patient.name, labelPaint, valuePaint)
        if (patient.age.isNotBlank()) y = drawInfoRow(canvas, y, "Age / Gender",
            "${patient.age}${if (patient.gender.isNotBlank()) " / ${patient.gender}" else ""}", labelPaint, valuePaint)
        if (patient.phone.isNotBlank()) y = drawInfoRow(canvas, y, "Phone", patient.phone, labelPaint, valuePaint)
        if (patient.address.isNotBlank()) y = drawInfoRow(canvas, y, "Address", patient.address, labelPaint, valuePaint)
        if (patient.referringDoctor.isNotBlank()) y = drawInfoRow(canvas, y, "Referred By", patient.referringDoctor, labelPaint, valuePaint)
        if (patient.remarks.isNotBlank()) y = drawInfoRow(canvas, y, "Remarks", patient.remarks, labelPaint, valuePaint)

        // ── Tests Table ─────────────────────────────────────────────────────
        y += 8f
        canvas.drawText("TESTS / INVESTIGATIONS", MARGIN, y, sectionPaint)
        y += 6f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, rulePaint.apply { strokeWidth = 0.5f })
        y += 4f

        // Table header
        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableRowBg = Paint().apply { color = Color.parseColor("#1565C0") }
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 18f, tableRowBg)
        canvas.drawText("Test / Investigation", MARGIN + 6f, y + 12f, tableHeaderPaint)
        val priceHeaderPaint = tableHeaderPaint.apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Price (₹)", MARGIN + CONTENT_WIDTH - 6f, y + 12f, priceHeaderPaint)
        y += 18f

        val rowPaint = Paint().apply { color = Color.parseColor("#F5F5F5") }
        val rowTextPaint = Paint().apply { color = Color.BLACK; textSize = 10f }
        val rowPricePaint = Paint().apply {
            color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.RIGHT
        }

        patient.selectedTests.forEachIndexed { index, test ->
            if (index % 2 == 0) {
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 16f, rowPaint)
            }
            val displayName = if (test.isPackage) "★ ${test.testName}" else test.testName
            canvas.drawText(displayName, MARGIN + 6f, y + 11f, rowTextPaint)
            canvas.drawText(test.price.formatCurrency(), MARGIN + CONTENT_WIDTH - 6f, y + 11f, rowPricePaint)
            y += 16f
        }

        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, rulePaint.apply { strokeWidth = 0.5f })

        // ── Billing Summary ─────────────────────────────────────────────────
        y += 16f
        val billingX = MARGIN + CONTENT_WIDTH * 0.55f
        val billingWidth = CONTENT_WIDTH * 0.45f

        val summaryLabelPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        val summaryValuePaint = Paint().apply {
            color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.RIGHT
        }

        y = drawBillingRow(canvas, y, billingX, "Subtotal", patient.subtotal.formatCurrency(), summaryLabelPaint, summaryValuePaint, billingWidth)

        if (patient.discountType != DiscountType.NONE && patient.discountValue > 0) {
            val discLabel = when (patient.discountType) {
                DiscountType.PERCENTAGE -> "Discount (${patient.discountValue.toInt()}%)"
                DiscountType.FLAT -> "Discount (Flat)"
                DiscountType.NONE -> ""
            }
            val discAmount = when (patient.discountType) {
                DiscountType.PERCENTAGE -> patient.subtotal * patient.discountValue / 100.0
                DiscountType.FLAT -> patient.discountValue
                DiscountType.NONE -> 0.0
            }
            val discValuePaint = Paint().apply {
                color = Color.RED; textSize = 10f; textAlign = Paint.Align.RIGHT
            }
            y = drawBillingRow(canvas, y, billingX, discLabel, "- ${discAmount.formatCurrency()}", summaryLabelPaint, discValuePaint, billingWidth)
        }

        // Grand total bold
        val grandTotalBg = Paint().apply { color = Color.parseColor("#E3F2FD") }
        canvas.drawRect(billingX - 6f, y - 2f, billingX + billingWidth + 6f, y + 18f, grandTotalBg)
        val grandLabelPaint = Paint().apply {
            color = Color.parseColor("#1565C0"); textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val grandValuePaint = Paint().apply {
            color = Color.parseColor("#1565C0"); textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("GRAND TOTAL", billingX, y + 12f, grandLabelPaint)
        canvas.drawText(patient.grandTotal.formatCurrency(), billingX + billingWidth, y + 12f, grandValuePaint)
        y += 26f

        // ── Payment Status ──────────────────────────────────────────────────
        y += 4f
        canvas.drawText("PAYMENT DETAILS", MARGIN, y, sectionPaint.apply { color = Color.parseColor("#1565C0") })
        y += 6f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, rulePaint.apply { strokeWidth = 0.5f })
        y += 14f

        y = drawInfoRow(canvas, y, "Payment Status", patient.paymentStatus.label, labelPaint, valuePaint.apply {
            color = when (patient.paymentStatus) {
                PaymentStatus.PAID -> Color.parseColor("#2E7D32")
                PaymentStatus.UNPAID -> Color.RED
                PaymentStatus.PARTIAL -> Color.parseColor("#E65100")
            }
        })
        valuePaint.color = Color.BLACK

        y = drawInfoRow(canvas, y, "Payment Method", patient.paymentMethod.label, labelPaint, valuePaint)

        if (patient.paymentStatus == PaymentStatus.PARTIAL) {
            y = drawInfoRow(canvas, y, "Amount Received", patient.amountReceived.formatCurrency(), labelPaint, valuePaint)
            val balance = (patient.grandTotal - patient.amountReceived).coerceAtLeast(0.0)
            y = drawInfoRow(canvas, y, "Balance Due", balance.formatCurrency(), labelPaint,
                valuePaint.apply { color = Color.RED })
            valuePaint.color = Color.BLACK
        }

        // ── Footer ──────────────────────────────────────────────────────────
        val footerY = PAGE_HEIGHT - MARGIN - 50f
        canvas.drawLine(MARGIN, footerY, MARGIN + CONTENT_WIDTH, footerY, rulePaint.apply { strokeWidth = 0.5f })

        val footerPaint = Paint().apply { color = Color.DKGRAY; textSize = 9f }
        canvas.drawText("Collection Agent: $agentName", MARGIN, footerY + 14f, footerPaint)
        canvas.drawText("This is a computer-generated receipt.", MARGIN, footerY + 26f, footerPaint)
        canvas.drawText("Urban Diagnostic Centre — All data stored securely.", MARGIN, footerY + 38f, footerPaint)

        val footerRightPaint = footerPaint.apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Patient: ${patient.patientNumber}", MARGIN + CONTENT_WIDTH, footerY + 14f, footerRightPaint)
    }

    private fun drawInfoRow(
        canvas: Canvas, y: Float, label: String, value: String,
        labelPaint: Paint, valuePaint: Paint
    ): Float {
        canvas.drawText("$label:", MARGIN, y, labelPaint)
        canvas.drawText(value, MARGIN + 130f, y, valuePaint)
        return y + 16f
    }

    private fun drawBillingRow(
        canvas: Canvas, y: Float, x: Float,
        label: String, value: String,
        labelPaint: Paint, valuePaint: Paint, width: Float
    ): Float {
        canvas.drawText(label, x, y + 12f, labelPaint)
        canvas.drawText(value, x + width, y + 12f, valuePaint)
        return y + 16f
    }
}

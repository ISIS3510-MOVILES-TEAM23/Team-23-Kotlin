package com.example.team_23_kotlin.presentation.purchases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.team_23_kotlin.R
import com.example.team_23_kotlin.data.purchases.PurchaseEntity
import com.example.team_23_kotlin.domain.repository.PurchasesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PurchasesViewModel @Inject constructor(
    private val repository: PurchasesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PurchasesState())
    val state = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo_goat)


    init {
        loadPurchases()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onEvent(event: PurchasesEvent) {
        when (event) {
            is PurchasesEvent.OnRefresh -> loadPurchases()
            is PurchasesEvent.OnTabChange -> {
                _state.update { it.copy(currentTab = event.tab) }
                applyFilter()
            }
            is PurchasesEvent.OnDownloadReceipt -> {
                if (event.purchase.isDownloaded) {
                    // 👇 Ya estaba descargado → solo abrir
                    openExistingReceipt(event.purchase)
                } else {
                    // 👇 No estaba descargado → generar PDF nuevo
                    generatePdfReceipt(event.purchase)
                }
            }
        }
    }


    private fun loadPurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val purchases = repository.getPurchases()

                // Integrar si ya están descargados
                val enriched = purchases.map { p ->
                    val downloaded = repository.isReceiptGenerated(p.id)
                    p.copy(isDownloaded = downloaded)
                }

                _state.update {
                    it.copy(
                        purchases = enriched,
                        isLoading = false,
                        error = null
                    )
                }

                computeStats()
                applyFilter()

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun computeStats() {
        val purchases = _state.value.purchases
        _state.update {
            it.copy(
                totalPurchased = purchases.size,
                totalCompleted = purchases.count { p -> p.status == "completed" },
                totalPending = purchases.count { p -> p.status == "pending" }
            )
        }
    }

    private fun applyFilter() {
        val st = _state.value

        val filtered = when (st.currentTab) {
            PurchasesTab.ALL -> st.purchases
            PurchasesTab.PENDING -> st.purchases.filter { it.status == "pending" }
            PurchasesTab.COMPLETED -> st.purchases.filter { it.status == "completed" }
        }

        _state.update { it.copy(filteredPurchases = filtered) }
    }

    private suspend fun loadImageBitmap(url: String): Bitmap? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()

            val result = loader.execute(request) as SuccessResult
            result.drawable.toBitmap()
        } catch (e: Exception) {
            null
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun generatePdfReceipt(purchase: PurchaseEntity) {
        viewModelScope.launch(Dispatchers.IO) {

            _uiEvent.emit("Generando recibo en segundo plano...")

            try {
                val pdf = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(500, 700, 1).create()
                val page = pdf.startPage(pageInfo)

                val canvas = page.canvas
                val paint = android.graphics.Paint()

// ----- Título -----
                paint.textSize = 24f
                paint.isFakeBoldText = true
                canvas.drawText("Recibo de Compra", 30f, 60f, paint)

// ----- Logo de la app -----
                logoBitmap?.let {
                    val scaledLogo = Bitmap.createScaledBitmap(it, 90, 90, true)
                    canvas.drawBitmap(scaledLogo, 360f, 20f, null)
                }

// ----- Línea separadora -----
                paint.strokeWidth = 2f
                canvas.drawLine(20f, 90f, 520f, 90f, paint)

// ----- Texto principal -----
                paint.textSize = 16f
                paint.isFakeBoldText = false
                var y = 130f

                canvas.drawText("Producto: ${purchase.title}", 30f, y, paint); y += 35
                canvas.drawText("Vendedor: ${purchase.sellerName}", 30f, y, paint); y += 35
                canvas.drawText("Precio: $${purchase.price}", 30f, y, paint); y += 35
                canvas.drawText("Fecha: ${purchase.createdAt}", 30f, y, paint); y += 35
                canvas.drawText("ID de compra: ${purchase.id}", 30f, y, paint)
                y += 60

// ----- Imagen del producto -----
                val productBitmap = purchase.postImages.firstOrNull()?.let { loadImageBitmap(it) }
                productBitmap?.let {
                    val scaledProduct = Bitmap.createScaledBitmap(it, 300, 300, true)
                    canvas.drawBitmap(scaledProduct, 100f, y, null)
                    y += 330
                }

// ----- Línea final -----
                canvas.drawLine(20f, y, 520f, y, paint)


                pdf.finishPage(page)

                val resolver = context.contentResolver
                val fileName = "receipt_${purchase.id}.pdf"

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/")
                }

                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw Exception("No se pudo crear archivo en Downloads")

                resolver.openOutputStream(uri)?.use { output ->
                    pdf.writeTo(output)
                }

                pdf.close()

                _uiEvent.emit("Recibo guardado en Descargas")

                // Guardar en Room
                repository.markReceiptGenerated(purchase.id)

                // Actualizar UI
                withContext(Dispatchers.Main) {
                    val updated = _state.value.purchases.map {
                        if (it.id == purchase.id) it.copy(isDownloaded = true)
                        else it
                    }

                    _state.update { it.copy(purchases = updated) }
                    applyFilter()
                }

                openPdf(uri)

            } catch (e: Exception) {
                _uiEvent.emit("Error generando PDF: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openExistingReceipt(purchase: PurchaseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val fileName = "receipt_${purchase.id}.pdf"

                val projection = arrayOf(
                    android.provider.MediaStore.Downloads._ID,
                    android.provider.MediaStore.Downloads.DISPLAY_NAME
                )
                val selection = "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(fileName)

                val cursor = resolver.query(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Downloads._ID)
                        val id = it.getLong(idColumn)

                        val uri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            id
                        )

                        _uiEvent.emit("Abriendo recibo desde Descargas")
                        openPdf(uri)
                        return@launch
                    }
                }

                // ❗ Si no se encuentra el archivo (lo borraron, etc.)
                _uiEvent.emit("No encontré el recibo en Descargas, generando uno nuevo…")
                generatePdfReceipt(purchase)

            } catch (e: Exception) {
                _uiEvent.emit("Error al abrir el recibo, generando uno nuevo…")
                generatePdfReceipt(purchase)
            }
        }
    }


    private suspend fun openPdf(uri: android.net.Uri) {
        withContext(Dispatchers.Main) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }

                context.startActivity(intent)
            } catch (e: Exception) {
                _uiEvent.emit("No se pudo abrir el PDF")
            }
        }
    }
}

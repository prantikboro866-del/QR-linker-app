package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.UserProfile
import com.example.data.UserRepository
import com.example.ui.AppColorTheme
import com.example.ui.AppFontTheme
import com.example.ui.theme.MyApplicationTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val repository = UserRepository(db.userDao())
        val viewModelFactory = ProfileViewModelFactory(repository)

        enableEdgeToEdge()
        setContent {
            val viewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
            val userProfile by viewModel.userProfile.collectAsState(initial = null)
            val isDarkMode = userProfile?.isDarkMode ?: false
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProfileEditor(viewModel, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ThemeCustomization(
    selectedColor: AppColorTheme,
    onColorChange: (AppColorTheme) -> Unit,
    selectedFont: AppFontTheme,
    onFontChange: (AppFontTheme) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Theme Customization", style = MaterialTheme.typography.titleMedium)
        
        Text("Color Palette", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppColorTheme.entries.forEach { theme ->
                FilterChip(
                    selected = theme == selectedColor,
                    onClick = { onColorChange(theme) },
                    label = { Text(theme.displayName) }
                )
            }
        }
        
        Text("Font Style", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFontTheme.entries.forEach { font ->
                FilterChip(
                    selected = font == selectedFont,
                    onClick = { onFontChange(font) },
                    label = { Text(font.displayName) }
                )
            }
        }
    }
}

@Composable
fun LivePreview(
    name: String, 
    links: List<Triple<String, String, Int>>, 
    backgroundImageUrl: String, 
    gradientTheme: String, 
    onLinkClick: (Int) -> Unit
) {
    val brush = when (gradientTheme) {
        "Sunset" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFF9966), androidx.compose.ui.graphics.Color(0xFFFF5E62)))
        "Ocean" -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF2E3192), androidx.compose.ui.graphics.Color(0xFF1BFFFF)))
        else -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Text("Live Preview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(name.ifEmpty { "Your Name" }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            links.forEachIndexed { index, link ->
                if (link.second.isNotEmpty()) {
                    TextButton(onClick = { onLinkClick(index) }) {
                        Text("${link.first}: ${link.second}")
                    }
                }
            }
        }
    }
}

@Composable
fun QRGeneration(qrContent: String, logo: Bitmap? = null, foregroundColor: Int = android.graphics.Color.BLACK, backgroundColor: Int = android.graphics.Color.WHITE, modifier: Modifier = Modifier) {
    val bitmap = generateQRCode(qrContent, logo, foregroundColor, backgroundColor)
    if (bitmap != null) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = modifier.padding(16.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(160.dp)
            )
        }
    }
}

fun getSocialIconForUrl(url: String): ImageVector {
    val lowerUrl = url.lowercase()
    return when {
        lowerUrl.contains("github") -> Icons.Default.AccountBox
        lowerUrl.contains("twitter") -> Icons.Default.Share
        lowerUrl.contains("linkedin") -> Icons.Default.Person
        else -> Icons.Default.List
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditor(viewModel: ProfileViewModel, modifier: Modifier = Modifier) {
    val userProfile by viewModel.userProfile.collectAsState(initial = null)
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var avatar by remember { mutableStateOf(userProfile?.avatarUrl ?: "") }
    var background by remember { mutableStateOf(userProfile?.backgroundImageUrl ?: "") }
    var gradient by remember { mutableStateOf(userProfile?.gradientTheme ?: "None") }
    var isDarkMode by remember { mutableStateOf(userProfile?.isDarkMode ?: false) }
    var qrForegroundColor by remember { mutableStateOf(userProfile?.qrForegroundColor ?: "BLACK") }
    var qrBackgroundColor by remember { mutableStateOf(userProfile?.qrBackgroundColor ?: "WHITE") }
    var qrStyle by remember { mutableStateOf(userProfile?.qrStyle ?: "SQUARE") }
    
    var themeColor by remember { 
        mutableStateOf(try { AppColorTheme.valueOf(userProfile?.themeColor ?: "DEFAULT") } catch (e: Exception) { AppColorTheme.DEFAULT }) 
    }
    var themeFont by remember { 
        mutableStateOf(try { AppFontTheme.valueOf(userProfile?.themeFont ?: "NORMAL") } catch (e: Exception) { AppFontTheme.NORMAL }) 
    }
    
    var links by remember { 
        mutableStateOf(
            userProfile?.linksJson?.split(",")
                ?.map { it.split("|") }
                ?.filter { it.size >= 2 }
                ?.map { Triple(it[0], it[1], if (it.size >= 3) it[2].toIntOrNull() ?: 0 else 0) }
                ?: listOf(Triple("Instagram", "", 0))
        ) 
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile Editor") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Bio Preview", style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                links.forEachIndexed { index, link ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getSocialIconForUrl(link.second), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.incrementLinkClick(index) }) {
                            Text(link.first)
                        }
                    }
                }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(64.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("JD", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(name.ifEmpty { "Jane Doe" }, style = MaterialTheme.typography.titleLarge)
                            Text(bio.ifEmpty { "User Bio" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                ThemeCustomization(
                    selectedColor = themeColor,
                    onColorChange = { themeColor = it },
                    selectedFont = themeFont,
                    onFontChange = { themeFont = it }
                )
            }

            item {
                Text("Links", style = MaterialTheme.typography.titleLarge)
                links.forEachIndexed { index, link ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(getSocialIconForUrl(link.second), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = link.first,
                            onValueChange = { newVal -> links = links.toMutableList().apply { set(index, Triple(newVal, link.second, link.third)) } },
                            label = { Text("Platform") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = link.second,
                            onValueChange = { newVal -> links = links.toMutableList().apply { set(index, Triple(link.first, newVal, link.third)) } },
                            label = { Text("URL") },
                            modifier = Modifier.weight(1f)
                        )
                        Text("${link.third} clicks", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { links = links.toMutableList().apply { removeAt(index) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
                Button(onClick = { links = links + Triple("Platform", "", 0) }) { Text("Add Link") }
            }

            item {
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = avatar, onValueChange = { avatar = it }, label = { Text("Avatar URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = background, onValueChange = { background = it }, label = { Text("Background URL") }, modifier = Modifier.fillMaxWidth())
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                }
                Text("QR Customization", style = MaterialTheme.typography.titleLarge)
                Text("FG Color")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BLACK", "BLUE", "RED").forEach { c -> FilterChip(label = { Text(c) }, selected = qrForegroundColor == c, onClick = { qrForegroundColor = c }) }
                }
                Text("BG Color")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WHITE", "LIGHT_GRAY").forEach { c -> FilterChip(label = { Text(c) }, selected = qrBackgroundColor == c, onClick = { qrBackgroundColor = c }) }
                }
            }

            item {
                Button(
                    onClick = { 
                        val linksJson = links.joinToString(",") { "${it.first}|${it.second}|${it.third}" }
                        viewModel.saveProfile(UserProfile(0, name, bio, avatar, linksJson, 0, themeColor.name, themeFont.name, background, gradient, isDarkMode, qrForegroundColor, qrBackgroundColor, qrStyle)) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Save Profile")
                }
            }
        }
    }
}

fun saveBitmapAsPdf(context: android.content.Context, bitmap: Bitmap, fileName: String): File? {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    document.finishPage(page)
    
    val file = File(context.cacheDir, "$fileName.pdf")
    try {
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        document.close()
        fos.close()
        return file
    } catch (e: Exception) {
        e.printStackTrace()
        document.close()
        return null
    }
}

fun generateQRCode(content: String, logo: Bitmap? = null, foregroundColor: Int = android.graphics.Color.BLACK, backgroundColor: Int = android.graphics.Color.WHITE): Bitmap? {
    val size = 512
    val writer = QRCodeWriter()
    return try {
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        // Draw QR
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) foregroundColor else backgroundColor)
            }
        }
        
        // Draw Logo
        logo?.let {
            val logoSize = size / 5 // Logo is 1/5 of the QR size
            val left = (size - logoSize) / 2
            val top = (size - logoSize) / 2
            val scaledLogo = Bitmap.createScaledBitmap(it, logoSize, logoSize, true)
            canvas.drawBitmap(scaledLogo, left.toFloat(), top.toFloat(), paint)
        }
        
        bitmap
    } catch (e: Exception) {
        null
    }
}

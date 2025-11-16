package com.example.chatlumen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chatlumen.ui.theme.ChatLumenTheme
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

// Chat message model with timestamp
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    // 🔑 Replace with your Gemini API key
    private val geminiApiKey = "YOUR_API_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatLumenTheme {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 🔹 Background image
                        Image(
                            painter = painterResource(id = R.drawable.chat_background),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 🔹 Chat UI goes on top
                        ChatScreen(apiKey = geminiApiKey)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(apiKey: String) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showHelpDialog by remember { mutableStateOf(false) }
    var showPurposeBox by remember { mutableStateOf(false) }
    var showCustomPurposeDialog by remember { mutableStateOf(false) }

    val purposes = remember { mutableStateListOf("General Chat", "Coding Help", "Education", "Health Advice", "Motivation") }
    var expanded by remember { mutableStateOf(false) }
    var selectedPurpose by remember { mutableStateOf(purposes[0]) }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )
    }

    // Auto scroll when new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.Transparent,   // ✅ allow image to show through
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                .height(80.dp) // 🔹 makes the blue box taller
                    .background(Color(0xFF00008B).copy(alpha = 0.9f)) // ✅ semi-transparent instead of solid
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 1.dp), // 🔹 added top padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Select Purpose",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Chat Lumen",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showHelpDialog = true }) {
                    Text(
                        text = "?",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    purposes.forEach { purpose ->
                        DropdownMenuItem(
                            text = { Text(purpose, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedPurpose = purpose
                                expanded = false
                                showPurposeBox = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("➕ Custom ", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                        onClick = {
                            expanded = false
                            showCustomPurposeDialog = true
                        }
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                    placeholder = { Text("Ask Anything…") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.Black,       // ✅ input text black
                        unfocusedTextColor = Color.Black,     // ✅ input text black
                        cursorColor = Color.Black,            // ✅ black cursor
                        focusedContainerColor = Color(0xFFD3D3D3),   // ✅ grey background
                        unfocusedContainerColor = Color(0xFFD3D3D3), // ✅ grey background
                        disabledContainerColor = Color(0xFFD3D3D3),
                        focusedIndicatorColor = Color(0xFFD3D3D3),
                        unfocusedIndicatorColor = Color(0xFFD3D3D3),
                        disabledIndicatorColor = Color(0xFFD3D3D3)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (userInput.text.isNotBlank()) {
                            val message = userInput.text
                            messages.add(ChatMessage(message, true))
                            coroutineScope.launch {
                                try {
                                    val prompt = """
                                        You are now acting as a highly knowledgeable and helpful $selectedPurpose assistant. 
                                        Your responses should be:
                                        - Clear, concise, and easy to understand
                                        - Polite, professional, and friendly
                                        - Relevant to the selected purpose ($selectedPurpose)
                                        - Include examples or explanations if it helps understanding
                                        
                                        Answer the following user question accordingly:
                                        
                                        "$message"
                                        
                                        Make sure to stay in the role of a $selectedPurpose assistant and provide accurate, useful, and context-aware information.
                                    """.trimIndent()

                                    val response = generativeModel.generateContent(prompt)
                                    val reply = response.text ?: "No response"
                                    messages.add(ChatMessage(reply, false))
                                } catch (e: Exception) {
                                    messages.add(ChatMessage("Error: ${e.message}", false))
                                }
                            }
                            userInput = TextFieldValue("")
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00008B),
                        contentColor = Color.White
                    )
                ) {
                    Text("Send")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            if (showPurposeBox) {
                PurposeInfoBox(
                    purpose = selectedPurpose,
                    onDismiss = { showPurposeBox = false }
                )
            }

            if (showHelpDialog) {
                HelpDialog(onDismiss = { showHelpDialog = false })
            }

            if (showCustomPurposeDialog) {
                CustomPurposeDialog(
                    onConfirm = { customText ->
                        if (!purposes.contains(customText)) purposes.add(customText)
                        selectedPurpose = customText
                        showPurposeBox = true
                        showCustomPurposeDialog = false
                    },
                    onDismiss = { showCustomPurposeDialog = false }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = Color(0xFFE0E0E0),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (message.isUser) {
                        AnnotatedString(message.text)
                    } else {
                        parseMarkdownBold(message.text)
                    },
                    color = Color.Black
                )

                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

fun parseMarkdownBold(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val regex = Regex("\\*\\*(.*?)\\*\\*")
    var lastIndex = 0
    for (match in regex.findAll(text)) {
        val start = match.range.first
        val end = match.range.last + 1
        builder.append(text.substring(lastIndex, start))
        val boldText = match.groupValues[1]
        val startIndex = builder.length
        builder.append(boldText)
        builder.addStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
            start = startIndex,
            end = builder.length
        )
        lastIndex = end
    }
    builder.append(text.substring(lastIndex))
    return builder.toAnnotatedString()
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun PurposeInfoBox(purpose: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF00008B))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Purpose Selected", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(purpose, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color.Black),
                    shape = RoundedCornerShape(4.dp)
                ) { Text("OK") }
            }
        }
    }
}

@Composable
fun CustomPurposeDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var customPurpose by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Column(
                modifier = Modifier.background(Color.White).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Enter Custom Purpose", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                TextField(value = customPurpose, onValueChange = { customPurpose = it }, placeholder = { Text("Type your purpose…") })
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { customPurpose = ""; onDismiss() }) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (customPurpose.isNotBlank()) {
                            val normalized = customPurpose.trim().replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }
                            onConfirm(normalized)
                            customPurpose = ""
                        }
                    }) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.95f).padding(24.dp)
        ) {
            Column(
                modifier = Modifier.background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("How to Use", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    """
                        ▪️ Tap the ☰ menu to select a purpose (General Chat, Coding Help, etc.)
                        ▪️ Add your own purpose using ➕ Custom
                        ▪️ Type your question in the box below and press Send
                        ▪️ The assistant will reply according to the chosen purpose
                    """.trimIndent(),
                    color = Color.Black,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("Developer Info", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))

                val annotatedText = buildAnnotatedString {
                    append("Developed by: ")
                    pushStringAnnotation(tag = "LINKEDIN", annotation = "https://www.linkedin.com/in/techarjun")
                    withStyle(SpanStyle(color = Color.DarkGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)) {
                        append("Arjun")
                    }
                    pop()
                    append("\nVersion: 1.0.0")
                }

                val context = LocalContext.current
                ClickableText(
                    text = annotatedText,
                    style = LocalTextStyle.current.copy(color = Color.DarkGray),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations("LINKEDIN", offset, offset).firstOrNull()?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00008B), contentColor = Color.White)
                ) { Text("Close") }
            }
        }
    }
}

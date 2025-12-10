package com.sebahatselcuk.kampustakip
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            CampusApp()
        }
    }
}
enum class Screen { LOGIN, REGISTER, FORGOT_PASS, FEED }
@Composable
fun CampusApp() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = Color(0xFF121212),
            onBackground = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                Screen.LOGIN -> LoginScreen(
                    onLogin = { _, _ ->
                        // 1. Gün için sadece geçiş simülasyonu
                        Toast.makeText(context, "Giriş Başarılı (Test)", Toast.LENGTH_SHORT).show()
                        currentScreen = Screen.FEED
                    },
                    onRegisterClick = { currentScreen = Screen.REGISTER },
                    onForgotClick = { currentScreen = Screen.FORGOT_PASS }
                )
                Screen.REGISTER -> RegisterScreen(
                    onRegister = { _, _, _, _ -> currentScreen = Screen.LOGIN },
                    onLoginClick = { currentScreen = Screen.LOGIN }
                )
                Screen.FORGOT_PASS -> ForgotPasswordScreen(onBack = { currentScreen = Screen.LOGIN })
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ana Akış Ekranı (2. Gün Eklenecek)", color = Color.White)
                    Button(onClick = { currentScreen = Screen.LOGIN }, Modifier.align(Alignment.BottomCenter)) { Text("Çıkış") }
                }
            }
        }
    }
}
@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onRegisterClick: () -> Unit, onForgotClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.login_bg), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Kampüs Takip Sistemi", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha=0.9f), unfocusedContainerColor = Color.White.copy(alpha=0.8f)))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Şifre") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha=0.9f), unfocusedContainerColor = Color.White.copy(alpha=0.8f)))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onLogin(email, pass) }, modifier = Modifier.fillMaxWidth()) { Text("Giriş Yap") }
            TextButton(onClick = onForgotClick) { Text("Şifremi Unuttum?", color = Color.White) }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRegisterClick) { Text("Hesap Oluştur", color = Color.White) }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegister: (String, String, String, String) -> Unit, onLoginClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Kayıt Ol", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name=it }, label = { Text("İsim") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email=it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = { pass=it }, label = { Text("Şifre") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = dept, onValueChange = { dept=it }, label = { Text("Birim") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onRegister(name, email, pass, dept) }, modifier = Modifier.fillMaxWidth()) { Text("Kaydol") }
        TextButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) { Text("Giriş'e Dön") }
    }
}
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Şifre Sıfırlama", color=Color.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email=it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Gönder") }
        TextButton(onClick = onBack) { Text("Geri") }
    }
}
package com.sebahatselcuk.kampustakip
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
// --- DATA MODELS ---
data class User(val id: String="", val name: String="", val email: String="", val role: String="USER", val department: String="", val followedIncidents: List<String> = emptyList())
data class Incident(val id: String="", val typeName: String="TECH", val title: String="", val description: String="", val statusName: String="OPEN", val date: Long=0, val authorId: String="") {
    fun getTypeEnum(): IncidentType = try { IncidentType.valueOf(typeName) } catch(e:Exception) { IncidentType.TECH }
    fun getStatusEnum(): IncidentStatus = try { IncidentStatus.valueOf(statusName) } catch(e:Exception) { IncidentStatus.OPEN }
}
enum class IncidentType(val label: String, val icon: ImageVector, val color: Color) {
    HEALTH("Sağlık", Icons.Filled.Favorite, Color(0xFFEF4444)),
    SECURITY("Güvenlik", Icons.Filled.Lock, Color(0xFF2563EB)),
    ENVIRONMENT("Çevre", Icons.Filled.Place, Color(0xFF10B981)),
    LOST_FOUND("Kayıp/Buluntu", Icons.Filled.Search, Color(0xFF8B5CF6)),
    TECH("Teknik Arıza", Icons.Filled.Build, Color(0xFFF59E0B))
}
enum class IncidentStatus(val label: String, val color: Color) {
    OPEN("Açık", Color(0xFFEF4444)),
    IN_PROGRESS("İnceleniyor", Color(0xFFF59E0B)),
    RESOLVED("Çözüldü", Color(0xFF10B981))
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { CampusApp() }
    }
}
enum class Screen { LOGIN, REGISTER, FORGOT_PASS, FEED, MAP, CREATE, PROFILE, ADMIN }
@Composable
fun CampusApp() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    val context = LocalContext.current
    // Sahte Veriler (2. Gün için)
    val dummyIncidents = listOf(
        Incident("1", "TECH", "Projeksiyon Arızası", "B1-201 nolu sınıfta.", "OPEN", 0),
        Incident("2", "HEALTH", "Merdiven Kaygan", "Giriş kapısı önü buzlanmış.", "IN_PROGRESS", 0),
        Incident("3", "LOST_FOUND", "Mavi Cüzdan Bulundu", "Kütüphane girişinde.", "RESOLVED", 0)
    )
    val dummyUser = User("1", "Test Öğrenci", "test@ogr.com")
    MaterialTheme(colorScheme = lightColorScheme(background = Color(0xFF121212), surface = Color(0xFF1E1E1E), onSurface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when(currentScreen) {
                Screen.LOGIN -> LoginScreen(
                    onLogin = { _, _ ->
                        Toast.makeText(context, "Giriş Yapıldı (Simülasyon)", Toast.LENGTH_SHORT).show()
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
                else -> {
                    // İÇERİK EKRANLARI (Feed + Navbar)
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(selected = currentScreen == Screen.FEED, onClick = { currentScreen = Screen.FEED }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Akış") })
                                NavigationBarItem(selected = currentScreen == Screen.MAP, onClick = { currentScreen = Screen.MAP }, icon = { Icon(Icons.Filled.LocationOn, null) }, label = { Text("Harita") })
                                NavigationBarItem(selected = currentScreen == Screen.CREATE, onClick = { currentScreen = Screen.CREATE }, icon = { Icon(Icons.Filled.AddCircle, null) }, label = { Text("Ekle") })
                                NavigationBarItem(selected = currentScreen == Screen.PROFILE, onClick = { currentScreen = Screen.PROFILE }, icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Profil") })
                            }
                        }
                    ) { p ->
                        Box(Modifier.padding(p)) {
                            if(currentScreen == Screen.FEED) {
                                FeedScreen(dummyIncidents, dummyUser)
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Bu ekran 3. gün eklenecek", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- EKRAN BİLEŞENLERİ ---
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
            OutlinedTextField(value = email, onValueChange = { email=it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha=0.9f), unfocusedContainerColor = Color.White.copy(alpha=0.8f)))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = pass, onValueChange = { pass=it }, label = { Text("Şifre") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha=0.9f), unfocusedContainerColor = Color.White.copy(alpha=0.8f)))
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
        OutlinedTextField(value = email, onValueChange = { email=it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Gönder") }
        TextButton(onClick = onBack) { Text("Geri") }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(incidents: List<Incident>, user: User) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(incidents) { inc ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(inc.getTypeEnum().icon, null, tint = inc.getTypeEnum().color)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(inc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(inc.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Surface(color = inc.getStatusEnum().color, shape = RoundedCornerShape(4.dp)) {
                        Text(inc.getStatusEnum().label, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
package com.sebahatselcuk.kampustakip
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
enum class Screen { LOGIN, REGISTER, FORGOT_PASS, FEED, MAP, CREATE, DETAIL, PROFILE, ADMIN, SPLASH }
@Composable
fun CampusApp() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var currentUser by remember { mutableStateOf<User?>(null) }
    var incidents by remember { mutableStateOf<List<Incident>>(emptyList()) }
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var selectedIncident by remember { mutableStateOf<Incident?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // AUTH LISTENER
    LaunchedEffect(Unit) {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentUser = doc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
                        currentScreen = Screen.FEED
                    } else {
                        auth.signOut()
                        currentScreen = Screen.LOGIN
                    }
                }
                .addOnFailureListener { currentScreen = Screen.LOGIN }
        } else {
            currentScreen = Screen.LOGIN
        }
    }
    // DATA LISTENER
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            db.collection("incidents").orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        incidents = snapshot.documents.mapNotNull { doc -> doc.toObject(Incident::class.java)?.copy(id = doc.id) }
                    }
                }
        }
    }
    // --- ACTIONS ---
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) return
        isLoading = true
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener { res ->
            val uid = res.user?.uid
            if (uid != null) {
                db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                    isLoading = false
                    currentUser = doc.toObject(User::class.java)?.copy(id = uid)
                    currentScreen = Screen.FEED
                }
            }
        }.addOnFailureListener { isLoading = false; errorMessage = it.localizedMessage }
    }
    fun register(name: String, email: String, pass: String, dept: String) {
        if (email.isBlank() || pass.isBlank()) return
        isLoading = true
        auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { res ->
            val uid = res.user?.uid
            if (uid != null) {
                val role = if (email.lowercase().contains("admin")) "ADMIN" else "USER"
                val newUser = User(id = uid, name = name, email = email, role = role, department = dept)
                db.collection("users").document(uid).set(newUser).addOnSuccessListener {
                    isLoading = false; currentUser = newUser; currentScreen = Screen.FEED
                }
            }
        }.addOnFailureListener { isLoading = false; errorMessage = it.localizedMessage }
    }
    fun logout() { auth.signOut(); currentUser = null; currentScreen = Screen.LOGIN }

    fun addIncident(type: IncidentType, title: String, desc: String) {
        val u = currentUser ?: return
        val newInc = Incident(typeName = type.name, title = title, description = desc, statusName = "OPEN", date = System.currentTimeMillis(), authorId = u.id)
        db.collection("incidents").add(newInc)
    }
    fun toggleFollow(incId: String) {
        val u = currentUser ?: return
        val newFollows = if (u.followedIncidents.contains(incId)) u.followedIncidents - incId else u.followedIncidents + incId
        currentUser = u.copy(followedIncidents = newFollows)
        db.collection("users").document(u.id).update("followedIncidents", newFollows)
    }
    MaterialTheme(colorScheme = lightColorScheme(background = Color(0xFF121212), surface = Color(0xFF1E1E1E), onSurface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (currentScreen == Screen.SPLASH || isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (currentUser == null) {
                    when (currentScreen) {
                        Screen.REGISTER -> RegisterScreen(onRegister = { n, e, p, d -> register(n, e, p, d) }, onLoginClick = { currentScreen = Screen.LOGIN })
                        Screen.FORGOT_PASS -> ForgotPasswordScreen(onBack = { currentScreen = Screen.LOGIN })
                        else -> LoginScreen(onLogin = { e, p -> login(e, p) }, onRegisterClick = { currentScreen = Screen.REGISTER }, onForgotClick = { currentScreen = Screen.FORGOT_PASS })
                    }
                } else {
                    Scaffold(
                        bottomBar = {
                            if (currentScreen != Screen.DETAIL) {
                                NavigationBar {
                                    NavigationBarItem(selected = currentScreen == Screen.FEED, onClick = { currentScreen = Screen.FEED }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Akış") })
                                    NavigationBarItem(selected = currentScreen == Screen.MAP, onClick = { currentScreen = Screen.MAP }, icon = { Icon(Icons.Filled.LocationOn, null) }, label = { Text("Harita") })
                                    NavigationBarItem(selected = currentScreen == Screen.CREATE, onClick = { currentScreen = Screen.CREATE }, icon = { Icon(Icons.Filled.AddCircle, null) }, label = { Text("Ekle") })
                                    if(currentUser?.role == "ADMIN") NavigationBarItem(selected = currentScreen == Screen.ADMIN, onClick = { currentScreen = Screen.ADMIN }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Ynt") })
                                    NavigationBarItem(selected = currentScreen == Screen.PROFILE, onClick = { currentScreen = Screen.PROFILE }, icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Profil") })
                                }
                            }
                        }
                    ) { p ->
                        Box(Modifier.padding(p)) {
                            when (currentScreen) {
                                Screen.FEED -> FeedScreen(incidents, currentUser!!) { selectedIncident = it; currentScreen = Screen.DETAIL }
                                Screen.MAP -> MapScreen(incidents)
                                Screen.CREATE -> CreateScreen { t, ti, d -> addIncident(t, ti, d); currentScreen = Screen.FEED }
                                Screen.PROFILE -> ProfileScreen(currentUser!!, incidents) { logout() }
                                Screen.DETAIL -> DetailScreen(selectedIncident, currentUser!!) { currentScreen = Screen.FEED; toggleFollow(it) }
                                Screen.ADMIN -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Yönetici Paneli (Son Gün Eklenecek)") }
                                else -> Text("")
                            }
                        }
                    }
                }
                if (errorMessage != null) {
                    AlertDialog(onDismissRequest = { errorMessage = null }, confirmButton = { Button(onClick = { errorMessage = null }) { Text("OK") } }, title = { Text("Hata") }, text = { Text(errorMessage!!) })
                }
            }
        }
    }
}
// --- FULL SCREENS (Feed with Filters, Map with Dates) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(incidents: List<Incident>, user: User, onDetail: (Incident) -> Unit) {
    var filterType by remember { mutableStateOf<IncidentType?>(null) }
    var filterStatus by remember { mutableStateOf<IncidentStatus?>(null) }
    var showOnlyFollowed by remember { mutableStateOf(false) }
    val filtered = incidents.filter { inc ->
        val typeMatch = filterType == null || inc.getTypeEnum() == filterType
        val statusMatch = filterStatus == null || inc.getStatusEnum() == filterStatus
        val followMatch = if (showOnlyFollowed) user.followedIncidents.contains(inc.id) else true
        typeMatch && statusMatch && followMatch
    }
    Column {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = showOnlyFollowed, onClick = { showOnlyFollowed = !showOnlyFollowed }, label = { Text("Takip Ettiklerim") }, leadingIcon = if (showOnlyFollowed) {{ Icon(Icons.Filled.Check, null) }} else null)
                Spacer(Modifier.width(8.dp))
                IncidentStatus.values().forEach { s ->
                    FilterChip(selected = filterStatus == s, onClick = { filterStatus = if(filterStatus==s) null else s }, label = { Text(s.label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = s.color.copy(alpha=0.2f)))
                    Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = filterType==null, onClick = { filterType=null }, label = { Text("Tüm Tipler") })
                IncidentType.values().forEach { t ->
                    Spacer(Modifier.width(4.dp))
                    FilterChip(selected = filterType==t, onClick = { filterType = if(filterType==t) null else t }, label = { Text(t.label) })
                }
            }
        }
        if (incidents.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Veri yok veya yükleniyor...", color = Color.Gray) }
        else if (filtered.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Filtrelere uygun sonuç yok.", color = Color.Gray) }
        else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { inc ->
                    Card(onClick = { onDetail(inc) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(inc.getTypeEnum().icon, null, tint = inc.getTypeEnum().color)
                                Spacer(Modifier.width(8.dp))
                                Text(inc.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Badge(containerColor = inc.getStatusEnum().color) { Text(inc.getStatusEnum().label, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp)) }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(inc.description, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MapScreen(incidents: List<Incident>) {
    val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
    val htmlContent = """
        <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1.0"><link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" /><script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script><style>body{margin:0;width:100vw;height:100vh;}#map{width:100%;height:100%;}</style></head>
        <body><div id="map"></div><script>
            var map = L.map('map').setView([39.9255, 32.8662], 15);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
            var places = [${incidents.joinToString(",") {
        val dateStr = dateFormat.format(java.util.Date(it.date))
        "{lat: 39.9255+${Math.random()*0.005}, lng: 32.8662+${Math.random()*0.005}, t:'${it.title}', d:'$dateStr'}"
    }}];
            places.forEach(p => L.marker([p.lat, p.lng]).addTo(map).bindPopup("<b>" + p.t + "</b><br><small>" + p.d + "</small>"));
        </script></body></html>
    """.trimIndent()
    AndroidView({ ctx -> WebView(ctx).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled=true; settings.domStorageEnabled=true
        loadDataWithBaseURL("https://example.com", htmlContent, "text/html", "UTF-8", null)
    } }, Modifier.fillMaxSize())
}
@Composable
fun DetailScreen(incident: Incident?, user: User, onAction: (String) -> Unit) {
    if (incident == null) return
    val type = incident.getTypeEnum()
    val status = incident.getStatusEnum()
    val isFollowing = user.followedIncidents.contains(incident.id)
    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(type.label, color = type.color)
        Text(incident.title, style = MaterialTheme.typography.headlineMedium)
        Text(status.label, color = status.color, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(incident.description)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onAction(incident.id) }, modifier = Modifier.fillMaxWidth()) { Text(if(isFollowing) "Takibi Bırak" else "Takip Et") }
        TextButton(onClick = { onAction("") }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Geri") }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(onSubmit: (IncidentType, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(IncidentType.TECH) }
    Column(Modifier.padding(16.dp)) {
        Text("Yeni Bildirim", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            IncidentType.values().forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                Spacer(Modifier.width(8.dp))
            }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = { onSubmit(type, title, desc) }, modifier = Modifier.fillMaxWidth()) { Text("KAYDET") }
    }
}
// ... (LoginScreen, RegisterScreen, ForgotPasswordScreen, ProfileScreen - Önceki günlerden aynı) ...
// (Lütfen bu fonksiyonları 3. günden veya 5. gün kodundan kopyalayın, ama yer kaplamaması için buraya tekrar yazmıyorum,
//  ancak SİYAH EKRAN OLMAMASI İÇİN `LoginScreen` vb. fonksiyonların kodda yer aldığından emin olun.)
@Composable fun LoginScreen(onLogin: (String, String) -> Unit, onRegisterClick: () -> Unit, onForgotClick: () -> Unit) { /* ... 3. Gün Kodunun Aynısı ... */
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
@Composable fun RegisterScreen(onRegister: (String, String, String, String) -> Unit, onLoginClick: () -> Unit) {
    // ... Simplified Register code for brevity, ensure full code is used in real file ...
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Kayıt Ol", style = MaterialTheme.typography.headlineMedium, color = Color.White); Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name=it }, label = { Text("İsim") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email=it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = { pass=it }, label = { Text("Şifre") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = dept, onValueChange = { dept=it }, label = { Text("Birim") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp)); Button(onClick = { onRegister(name, email, pass, dept) }, modifier = Modifier.fillMaxWidth()) { Text("Kaydol") }; TextButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) { Text("Giriş'e Dön") }
    }
}
@Composable fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Şifre Sıfırlama", color=Color.White); OutlinedTextField(value = email, onValueChange = { email=it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Gönder") }; TextButton(onClick = onBack) { Text("Geri") }
    }
}
@Composable fun ProfileScreen(user: User, incidents: List<Incident>, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Person, null, modifier = Modifier.size(80.dp), tint = Color.White)
        Text(user.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(user.department, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Çıkış Yap") }
    }
}
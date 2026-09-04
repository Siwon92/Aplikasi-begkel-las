package com.bengkel.karyatunasmuda

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val nomorHp: String,
    val alamat: String,
    val catatan: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomorOrder: String,
    val customerId: Long,
    val jenisPekerjaan: String,
    val deskripsi: String,
    val ukuran: String,
    val lokasi: String,
    val tanggalOrder: String,
    val deadline: String,
    val status: String,
    val totalHarga: Double,
    val totalDibayar: Double,
    val catatan: String
)

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val kategori: String,
    val ukuran: String,
    val satuan: String,
    val harga: Double,
    val stok: Int,
    val stokMinimum: Int
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggal: String,
    val kategori: String,
    val keterangan: String,
    val jumlah: Double
)

@Dao
interface BengkelDao {
    @Query("SELECT * FROM customers")
    fun getAllCustomers(): LiveData<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM orders")
    fun getAllOrders(): LiveData<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM materials")
    fun getAllMaterials(): LiveData<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity)

    @Query("SELECT * FROM expenses")
    fun getAllExpenses(): LiveData<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)
}

@Database(entities = [CustomerEntity::class, OrderEntity::class, MaterialEntity::class, ExpenseEntity::class], version = 1, exportSchema = false)
abstract class BengkelDatabase : RoomDatabase() {
    abstract fun bengkelDao(): BengkelDao

    companion object {
        @Volatile
        private var INSTANCE: BengkelDatabase? = null

        fun getDatabase(context: Context): BengkelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BengkelDatabase::class.java,
                    "bengkel_karya_tunas_muda.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class BengkelRepository(private val dao: BengkelDao) {
    val allCustomers: LiveData<List<CustomerEntity>> = dao.getAllCustomers()
    val allOrders: LiveData<List<OrderEntity>> = dao.getAllOrders()
    val allMaterials: LiveData<List<MaterialEntity>> = dao.getAllMaterials()
    val allExpenses: LiveData<List<ExpenseEntity>> = dao.getAllExpenses()

    suspend fun insertCustomer(customer: CustomerEntity) = dao.insertCustomer(customer)
    suspend fun insertOrder(order: OrderEntity) = dao.insertOrder(order)
    suspend fun insertMaterial(material: MaterialEntity) = dao.insertMaterial(material)
    suspend fun insertExpense(expense: ExpenseEntity) = dao.insertExpense(expense)
}

class BengkelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BengkelRepository
    val allCustomers: LiveData<List<CustomerEntity>>
    val allOrders: LiveData<List<OrderEntity>>
    val allMaterials: LiveData<List<MaterialEntity>>
    val allExpenses: LiveData<List<ExpenseEntity>>

    init {
        val db = BengkelDatabase.getDatabase(application)
        repository = BengkelRepository(db.bengkelDao())
        allCustomers = repository.allCustomers
        allOrders = repository.allOrders
        allMaterials = repository.allMaterials
        allExpenses = repository.allExpenses
    }

    fun addCustomer(nama: String, hp: String, alamat: String, catatan: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertCustomer(CustomerEntity(nama = nama, nomorHp = hp, alamat = alamat, catatan = catatan))
    }

    fun addOrder(order: OrderEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertOrder(order)
    }

    fun addMaterial(material: MaterialEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertMaterial(material)
    }

    fun addExpense(expense: ExpenseEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertExpense(expense)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BengkelAppMainScreen()
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Pelanggan : Screen("pelanggan", "Pelanggan", Icons.Default.Person)
    object Order : Screen("order", "Order", Icons.Default.List)
    object Kalkulator : Screen("kalkulator", "Kalkulator", Icons.Default.Build)
    object Stok : Screen("stok", "Stok", Icons.Default.ShoppingCart)
    object Keuangan : Screen("keuangan", "Keuangan", Icons.Default.CheckCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BengkelAppMainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = remember { BengkelViewModel(context.applicationContext as Application) }
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val customers by viewModel.allCustomers.observeAsState(emptyList())
    val orders by viewModel.allOrders.observeAsState(emptyList())
    val materials by viewModel.allMaterials.observeAsState(emptyList())
    val expenses by viewModel.allExpenses.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bengkel Karya Tunas Muda", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Screen.Dashboard,
                    Screen.Pelanggan,
                    Screen.Order,
                    Screen.Kalkulator,
                    Screen.Stok,
                    Screen.Keuangan
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                is Screen.Dashboard -> DashboardScreen(orders, expenses)
                is Screen.Pelanggan -> PelangganScreen(customers, viewModel)
                is Screen.Order -> OrderScreen(orders, customers, viewModel)
                is Screen.Kalkulator -> KalkulatorScreen(materials)
                is Screen.Stok -> StokScreen(materials, viewModel)
                is Screen.Keuangan -> KeuanganScreen(expenses, viewModel)
            }
        }
    }
}

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}

@Composable
fun DashboardScreen(orders: List<OrderEntity>, expenses: List<ExpenseEntity>) {
    val totalOmzet = orders.sumOf { it.totalDibayar }
    val totalPengeluaran = expenses.sumOf { it.jumlah }
    val estimasiKeuntungan = totalOmzet - totalPengeluaran

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Ringkasan Bengkel", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                DashboardCard("Total Order", "${orders.size}", Modifier.weight(1f))
                DashboardCard("Belum Lunas", "${orders.count { it.status != "Lunas" }}", Modifier.weight(1f))
            }
        }
        item { DashboardCard("Total Omzet", formatRupiah(totalOmzet), Modifier.fillMaxWidth()) }
        item { DashboardCard("Total Pengeluaran", formatRupiah(totalPengeluaran), Modifier.fillMaxWidth()) }
        item { DashboardCard("Estimasi Keuntungan", formatRupiah(estimasiKeuntungan), Modifier.fillMaxWidth()) }
    }
}

@Composable
fun DashboardCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PelangganScreen(customers: List<CustomerEntity>, viewModel: BengkelViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var nama by remember { mutableStateOf("") }
    var nomorHp by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Daftar Pelanggan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { showDialog = true }) { Text("+ Tambah") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(customers) { customer ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(customer.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("HP: ${customer.nomorHp}", fontSize = 14.sp)
                        Text("Alamat: ${customer.alamat}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Tambah Pelanggan Baru") },
                text = {
                    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Pelanggan") })
                        OutlinedTextField(value = nomorHp, onValueChange = { nomorHp = it }, label = { Text("Nomor HP") })
                        OutlinedTextField(value = alamat, onValueChange = { alamat = it }, label = { Text("Alamat") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (nama.isNotBlank()) {
                            viewModel.addCustomer(nama, nomorHp, alamat, "")
                            nama = ""; nomorHp = ""; alamat = ""; showDialog = false
                        }
                    }) { Text("Simpan") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
            )
        }
    }
}

@Composable
fun OrderScreen(orders: List<OrderEntity>, customers: List<CustomerEntity>, viewModel: BengkelViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Daftar Order Bengkel", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(orders) { order ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("No: ${order.nomorOrder}", fontWeight = FontWeight.Bold)
                    Text("Pekerjaan: ${order.jenisPekerjaan}")
                    Text("Status: ${order.status}", color = MaterialTheme.colorScheme.primary)
                    Text("Total: ${formatRupiah(order.totalHarga)}")
                }
            }
        }
    }
}

@Composable
fun KalkulatorScreen(materials: List<MaterialEntity>) {
    var selectedMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var qty by remember { mutableStateOf("1") }
    val quantity = qty.toIntOrNull() ?: 0
    val totalHarga = (selectedMaterial?.harga ?: 0.0) * quantity

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kalkulator Material & Biaya", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Pilih Bahan dari Master Material:", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(materials) { mat ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(mat.nama, fontWeight = FontWeight.Bold)
                            Text("Harga: ${formatRupiah(mat.harga)}", fontSize = 12.sp)
                        }
                        Button(onClick = { selectedMaterial = mat }) { Text("Pilih") }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (selectedMaterial != null) {
            Text("Bahan Terpilih: ${selectedMaterial?.nama}")
            OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity (Qty)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total Harga Material: ${formatRupiah(totalHarga)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StokScreen(materials: List<MaterialEntity>, viewModel: BengkelViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var nama by remember { mutableStateOf("") }
    var harga by remember { mutableStateOf("") }
    var stok by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Manajemen Stok Material", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { showDialog = true }) { Text("+ Bahan") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(materials) { mat ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(mat.nama, fontWeight = FontWeight.Bold)
                            Text("Stok: ${mat.stok} ${mat.satuan}", fontSize = 12.sp)
                        }
                        Text(formatRupiah(mat.harga), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Tambah Material Baru") },
                text = {
                    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Material (Cth: Hollow 4x4)") })
                        OutlinedTextField(value = harga, onValueChange = { harga = it }, label = { Text("Harga Satuan") })
                        OutlinedTextField(value = stok, onValueChange = { stok = it }, label = { Text("Jumlah Stok") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val h = harga.toDoubleOrNull() ?: 0.0
                        val s = stok.toIntOrNull() ?: 0
                        if (nama.isNotBlank()) {
                            viewModel.addMaterial(MaterialEntity(nama = nama, kategori = "Besi", ukuran = "Standard", satuan = "Batang", harga = h, stok = s, stokMinimum = 2))
                            nama = ""; harga = ""; stok = ""; showDialog = false
                        }
                    }) { Text("Simpan") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
            )
        }
    }
}

@Composable
fun KeuanganScreen(expenses: List<ExpenseEntity>, viewModel: BengkelViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var ket by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pencatatan Keuangan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { showDialog = true }) { Text("+ Pengeluaran") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(expenses) { exp ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(exp.keterangan, fontWeight = FontWeight.Bold)
                            Text(exp.tanggal, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text("- ${formatRupiah(exp.jumlah)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Catat Pengeluaran Baru") },
                text = {
                    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ket, onValueChange = { ket = it }, label = { Text("Keterangan (Cth: Beli Listrik)") })
                        OutlinedTextField(value = jumlah, onValueChange = { jumlah = it }, label = { Text("Jumlah Biaya (Rp)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val jml = jumlah.toDoubleOrNull() ?: 0.0
                        if (ket.isNotBlank() && jml > 0) {
                            viewModel.addExpense(ExpenseEntity(tanggal = "2026-09-04", kategori = "Operasional", keterangan = ket, jumlah = jml))
                            ket = ""; jumlah = ""; showDialog = false
                        }
                    }) { Text("Simpan") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
            )
        }
    }
}

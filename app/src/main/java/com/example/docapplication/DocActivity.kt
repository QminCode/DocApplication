package com.example.docapplication

/**
 * @author: playboi_YzY
 * @date: 2025/5/7 16:07
 * @description:
 * @version:
 */
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.navArgument
import com.example.docapplication.DocumentRepository.getMockDocuments
import com.example.docapplication.ui.theme.DocApplicationTheme
import com.example.docapplication.viewModel.DocumentListViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import androidx.navigation.compose.navigation
import java.io.File

import java.util.Locale
import kotlin.io.path.exists

@AndroidEntryPoint
class DocActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainApp()
                }
            }
        }
    }

    @Preview
    @Composable
    fun MainAppPreview() {
        MainApp()
    }

    @Composable
    fun MainApp() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = Screen.DocumentFlow.route) {
            // 创建上层路由，使得ViewModel可被复用
            navigation(
                startDestination = Screen.DocumentList.route, // DocumentList 是路由的起点
                route = Screen.DocumentFlow.route //使用父图路由
            ) {
                composable(Screen.DocumentList.route) { backStackEntry ->
                    // Get the parent NavGraph's NavBackStackEntry (which is the DocumentFlow graph)
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.DocumentFlow.route)
                    }
                    // Get the ViewModel scoped to the parent graph (shared ViewModel)
                    val viewModel: DocumentListViewModel = hiltViewModel(parentEntry)

                    DocumentListScreen(
                        navController = navController,
                        viewModel = viewModel // Pass the shared ViewModel
                    )
                }
                composable(
                    Screen.WebViewContainer.route,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    // Get the parent NavGraph's NavBackStackEntry using the parent graph's route
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.DocumentFlow.route)
                    }
                    // Get the ViewModel scoped to the parent graph
                    val viewModel: DocumentListViewModel = hiltViewModel(parentEntry)

                    val documentId = backStackEntry.arguments?.getString("documentId")
                    if (documentId != null) {
                        WebViewContainerScreen(
                            navController = navController,
                            documentId = documentId,
                            viewModel = viewModel // Pass the shared ViewModel
                        )
                    } else {
                        Text("Error: Document ID not provided")
                    }
                }

                composable(
                    Screen.DocumentDetail.route,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    // Get the parent NavGraph's NavBackStackEntry using the parent graph's route
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Screen.DocumentFlow.route)
                    }
                    // Get the ViewModel scoped to the parent graph
                    val viewModel: DocumentListViewModel = hiltViewModel(parentEntry)

                    val documentId = backStackEntry.arguments?.getString("documentId")
                    if (documentId != null) {
                        DocumentDetailScreen(
                            navController = navController,
                            documentId = documentId,
                            viewModel = viewModel // Pass the shared ViewModel
                        )
                    } else {
                        Text("Error: Document ID not provided")
                    }
                }
            }
        }
    }

    @Composable
    fun DocumentScreen(
        documentId: String?,
        navController: NavController,
        content: @Composable (Document) -> Unit) {
        val document = getMockDocuments().find { it.id == documentId }
        if (document != null) {
            content(document)
        } else {
            Text("Document not found")
            // Consider: show a error dialog or navigating to a specific error screen.
        }
    }

    @Preview
    @Composable
    fun WebViewContainerPreview() {
        val url = "https://www.example.com"
        WebViewContainer(url = url)
    }

    // WebView Container
    @Composable
    fun WebViewContainer(url: String, modifier: Modifier = Modifier) {
        var webViewLoading by remember { mutableStateOf(true) }
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        if (0 != (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
                            WebView.setWebContentsDebuggingEnabled(true)
                        }

                        //——缓存和Cookie管理——
                        val webSettings = this.settings

                        // 1. 配置HTTP缓存
                        //    - WebSettings.LOAD_DEFAULT: 默认缓存策略。
                        //    - WebSettings.LOAD_CACHE_ELSE_NETWORK: 即使过期也要使用缓存。
                        //    - WebSettings.LOAD_NO_CACHE:不要使用缓存。
                        //    - WebSettings.LOAD_CACHE_ONLY: 只从缓存加载。
                        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

                        // 设置HTTP缓存路径。
                        // 这对于启用持久磁盘缓存至关重要。
                        val cacheDirPath = File(ctx.cacheDir, "http_cache")
                        if (!cacheDirPath.exists()) {
                            cacheDirPath.mkdirs()
                        }
                        webSettings.databasePath = ctx.getDir("databases", Context.MODE_PRIVATE).path
                        webSettings.setGeolocationDatabasePath(ctx.getDir("geolocation", Context.MODE_PRIVATE).path)


                        // 2. 启用应用程序缓存(AppCache) -可选，因为它已被弃用
                        // webSettings.setAppCacheEnabled(true)
                        // webSettings.setAppCacheMaxSize(10 * 1024 * 1024)

                        // 3. 启用DOM存储
                        webSettings.domStorageEnabled = true // You likely had this from previous X5 setup

                        // 4. Enable Database Storage (WebSQL - deprecated, but some sites might still use it)
                        webSettings.databaseEnabled = true // You likely had this

                        // 5. Enable JavaScript (Prerequisite for modern caching like Service Workers, IndexedDB)
                        webSettings.javaScriptEnabled = true // You likely had this

                        // Other potentially useful settings for caching/performance
                        webSettings.allowFileAccess = true
                        webSettings.loadsImagesAutomatically = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        // --- WebViewClient ---
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                webViewLoading = true
                                Log.d("WebViewStandard", "Page started: $url")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                webViewLoading = false
                                Log.d("WebViewStandard", "Page finished: $url")
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                webViewLoading = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Log.e("WebViewStandard", "Error: ${error?.description} on URL: ${request?.url}")
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    Log.e("WebViewStandard", "Error: $description on URL: $failingUrl (Error Code: $errorCode)")
                                }
                                webViewLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestedUrl = request?.url?.toString()
                                Log.d("WebViewStandard", "shouldOverrideUrlLoading: $requestedUrl")
                                // Implement your shouldUseExternalBrowser logic if needed
                                // if (shouldUseExternalBrowser(requestedUrl)) {
                                //     requestedUrl?.let {
                                //         try {
                                //             val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                //             ctx.startActivity(intent)
                                //             return true
                                //         } catch (e: Exception) { /* ... */ }
                                //     }
                                // }
                                return super.shouldOverrideUrlLoading(view, request) // Let WebView handle by default
                            }
                        }

                        // --- WebChromeClient ---
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress == 100) {
                                    webViewLoading = false
                                } else if (!webViewLoading && newProgress > 0) {
                                    webViewLoading = true
                                }
                                Log.d("WebViewStandard", "Progress: $newProgress")
                            }

                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    Log.d("WebViewStandard_Console", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }

                            // For AppCache quota (if AppCache is enabled)
                            /*
                            override fun onReachedMaxAppCacheSize(
                                requiredStorage: Long,
                                quota: Long,
                                quotaUpdater: WebStorage.QuotaUpdater
                            ) {
                                quotaUpdater.updateQuota(requiredStorage * 2)
                            }
                            */

                            // For WebSQL quota (if databaseEnabled is true)
                            /*
                            override fun onExceededDatabaseQuota(
                                url: String?,
                                databaseIdentifier: String?,
                                currentQuota: Long,
                                estimatedSize: Long,
                                totalUsedQuota: Long,
                                quotaUpdater: WebStorage.QuotaUpdater
                            ) {
                                quotaUpdater.updateQuota(estimatedSize * 2)
                            }
                            */
                        }

                        Log.d("WebViewStandard", "Initial loadUrl with caching: $url")
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    // Ensure cache settings persist if necessary, though factory settings are usually sufficient.
                    // webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

                    val currentLoadedUrl = webView.url
                    if (url != currentLoadedUrl && url.isNotBlank()) {
                        // if (shouldUseExternalBrowser(url)) { /* ... */ } else {
                        webViewLoading = true
                        Log.d("WebViewStandard", "Update loadUrl with caching: $url")
                        webView.loadUrl(url)
                        // }
                    }
                },
                modifier = modifier.fillMaxSize()
            )

            if (webViewLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    @Preview
    @Composable
    fun DocumentListScreenPreview() {
        val navController = rememberNavController()
        val viewModel = DocumentListViewModel()
        DocumentListScreen(navController = navController, viewModel = viewModel)
    }

    // 文档列表
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DocumentListScreen(
        navController: NavController,
        viewModel: DocumentListViewModel
    ) {
        val context = LocalContext.current
        // 使用Flow
        val documents by viewModel.documents.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
        var documentToDelete by remember { mutableStateOf<Document?>(null) }
        var showAddMenu by remember { mutableStateOf(false) }

        val openFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri != null) {
                    // 处理文件URI，向列表中添加一个新的Document
                    val newDocument = createDocumentFromUri(context, uri)
                    if (newDocument != null) {
                        // TODO: 添加新文档的方法放在ViewModel中
                        // viewModel.addDocument(newDocument) // Example of calling ViewModel method
                        Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导入失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("文档列表") },
                    actions = {
                        IconButton(onClick = { showAddMenu = !showAddMenu }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("从本地导入") },
                                onClick = {
                                    showAddMenu = false
                                    openFileLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("扫描导入") },
                                onClick = {
                                    showAddMenu = false
                                    // Handle scan import
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建空白文档") },
                                onClick = {
                                    showAddMenu = false
                                }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(documents, key = { it.id }) { document ->
                    var showMenu by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(document.name) },
                        supportingContent = { Text(document.url) },
                        trailingContent = {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_document)) },
                                    onClick = {
                                        showMenu = false
                                        coroutineScope.launch {
                                            viewModel.shareDocument(context)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_document)) },
                                    onClick = {
                                        showMenu = false
                                        documentToDelete = document
                                        showDeleteConfirmationDialog = true
                                    }
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.WebViewContainer.createRoute(document.id))
                        }
                    )
                }
            }
            if (showDeleteConfirmationDialog && documentToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmationDialog = false
                        documentToDelete = null
                    },
                    title = { Text("确认删除") },
                    text = { Text("您确定要删除此文档吗？") },
                    confirmButton = {
                        Button(onClick = {
//                            documents = documents.filter { it.id != documentToDelete!!.id }
                            documentToDelete?.let { doc ->
                                viewModel.deleteDocumentFromList(doc.id) // Add this hypothetical method to ViewModel
                            }
                            showDeleteConfirmationDialog = false
                            documentToDelete = null
                        }) {
                            Text("确认")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showDeleteConfirmationDialog = false
                            documentToDelete = null
                        }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
    // 文件选择器选择文档
    fun createDocumentFromUri(context: Context, uri: Uri): Document? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1 && sizeIndex != -1) {
                    val fileName = cursor.getString(nameIndex)
                    val fileSize = cursor.getLong(sizeIndex)
                    val documentId = DocumentUtils.generateDocumentId()
                    return Document(
                        id = documentId,
                        name = fileName,
                        url = uri.toString(),
                        owner = MockData.USER_ALICE,
                        members = emptyList(),
                        shareLink = ""
                    )
                }
            }
        }
        return null
    }

    @Preview
    @Composable
    fun WebViewContainerScreenPreview() {
        val navController = rememberNavController()
        val viewModel = DocumentListViewModel() // Assuming a default constructor
        WebViewContainerScreen(
            navController = navController,
            documentId = "mockDocumentId", // Provide a mock document ID
            viewModel = viewModel
        )
    }
    // WebView容器
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun WebViewContainerScreen(
        navController: NavController,
        documentId: String,
        viewModel: DocumentListViewModel
    ) {
        // 收集当前文档作为状态
        val document by viewModel.currentDocument.collectAsState()
        LaunchedEffect(documentId) {
            // 只在文档尚未加载时加载
            if (viewModel.currentDocument.value?.id != documentId) {
                viewModel.loadDocument(documentId)
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(document?.name ?: "Loading...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = { // 添加菜单按钮的操作块
                        document?.let {
                            IconButton(onClick = {
                                document?.id?.let { docId ->
                                    navController.navigate(Screen.DocumentDetail.createRoute(docId))
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert, // Use the MoreVert icon
                                    contentDescription = "Menu" // Content description for accessibility
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier
                .padding(padding)
                .fillMaxSize(), contentAlignment = Alignment.Center) {
                //只有当文档被加载时才显示WebView和水印
                document?.let { loadedDocument ->
                    WebViewWithWatermark(
                        url = loadedDocument.url,
                        isWatermarkVisible = loadedDocument.canShowWatermark,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    //当文档正在加载时显示一个加载指示器
                    CircularProgressIndicator()
                }
            }
        }
    }

    @Composable
    fun WebViewWithWatermark(url: String,isWatermarkVisible: Boolean, modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            // WebView
            WebViewContainer(url = url)

            // Watermark
            if (isWatermarkVisible) {
                WatermarkOverlay()
            }
        }
    }

    // 水印
    @Composable
    fun WatermarkOverlay() {
        var currentTime by remember { mutableStateOf(getCurrentTime()) }

        LaunchedEffect(key1 = true) {
            while (true) {
                delay(60000)
                currentTime = getCurrentTime()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = currentTime,
                color = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(-45f)
                    .padding(16.dp),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    @Preview
    @Composable
    fun DocumentDetailScreenPreview() {
        val navController = rememberNavController()
        val viewModel = DocumentListViewModel()
        DocumentDetailScreen(navController = navController, documentId = "mockId",
            viewModel = viewModel)
    }


    // 文档详情页
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DocumentDetailScreen(
        navController: NavController,
        documentId: String,
        viewModel: DocumentListViewModel
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        // 收集文档列表作为state
        val document by viewModel.currentDocument.collectAsState()

        //当compose对象第一次启动或documentId发生变化时加载文档。
        //这确保ViewModel在屏幕出现时加载了正确的文档。
        LaunchedEffect(documentId) {
            if (viewModel.currentDocument.value?.id != documentId) {
                viewModel.loadDocument(documentId)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            // 如果找不到对应文档，还有个BOx套着
            if (document != null) {
                val shareDocument: () -> Unit = {
                    coroutineScope.launch {
                        viewModel.shareDocument(context)
                    }
                }

                val deleteDocument: () -> Unit = {
                    coroutineScope.launch {
                        viewModel.deleteDocument(context) {
                            navController.navigate(Screen.DocumentList.route) {
                                Screen.DocumentList.popUpToInclusive(inclusive = true).invoke(this)
                                // 如果你想要弹出列表，但将列表保留在堆栈中：
                                // Screen.DocumentList.popUpToInclusive(inclusive = false).invoke(this)

                                // 避免同一目标在堆栈上的多个副本
                                launchSingleTop = true
                            }
                        }
                    }
                }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showBottomSheet by remember { mutableStateOf(false) }
                //Dialog state
                var showTransferDialog by remember { mutableStateOf(false) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("文档信息") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding)) {
                        // Owner info
                        Text(
                            "Owner: ${document?.owner?.name ?: "Loading..."}",
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        // Member List
                        Text(
                            "Members (${document?.members?.size ?: 0})",
                            modifier = Modifier.padding(start = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            val membersToShow = document?.members?.take(5) ?: emptyList()
                            items(membersToShow) { member ->
                                MemberListItem(
                                    member = member,
                                    documentOwner = document?.owner ?: MockData.USER_ALICE,
                                    currentDocument = document?: MockData.DOCUMENT_1,
                                    onPermissionChange = { newMember, newPermissionType ->
                                        // TODO: 权限变更方法定义
//                                        viewModel.updateMemberPermission(newMember, newPermissionType)
                                    },
                                    onTransferOwnership = { member ->
                                        showTransferDialog = true
                                    })
                            }
                            if ((document?.members?.size ?: 0) > 5) {
                                item {
                                    Text(
                                        "...",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showBottomSheet = true
                                            }
                                            .padding(8.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            if (document != null && DocumentRepository.getCurrentUsers().id == document!!.owner.id) {
                                item {
                                    // 控制是否显示水印
                                    ListItem(
                                        headlineContent = { Text("显示水印") },
                                        trailingContent = {
                                            Switch(
                                                checked = document?.canShowWatermark ?: false,
                                                onCheckedChange = { newCanShowWatermark ->
                                                    viewModel.updateDocumentWatermarkStatus(document!!.id, newCanShowWatermark)
                                                    Log.d("documentCanShow", "Document state: ${document!!.canShowWatermark}")
                                                }
                                            )
                                        }
                                    )
                                }
                            }

                            item {
                                ListItem(
                                    modifier = Modifier.clickable(onClick = shareDocument),
                                    headlineContent = { Text(stringResource(R.string.share_document)) }
                                )
                            }
                            item {  // 点击删除后删除文档并跳转到文档列表页
                                ListItem(
                                    modifier = Modifier.clickable(onClick = deleteDocument),
                                    headlineContent = {
                                        Text(
                                            stringResource(R.string.delete_document),
                                            color = Color.Red
                                        )
                                    }
                                )
                            }

                        }
                    }
                }
                if (showTransferDialog) {
                    document?.let { doc ->
                        TransferOwnershipDialog(
                            onDismissRequest = { showTransferDialog = false },
                            document = doc,
                            onConfirmTransfer = { memberId ->
                                viewModel.transferOwnership(memberId)
                                showTransferDialog = false
                            }
                        )
                    }
                }
                if (showBottomSheet) {
                    document?.let { doc ->
                        ModalBottomSheet(
                            onDismissRequest = { showBottomSheet = false },
                            sheetState = sheetState,
                        ) {
                            BottomSheetContent(doc) { showBottomSheet = false }
                        }
                    }
                }
            } else {
                // 文档未找到或仍在加载
                Text(
                    "找不到文档",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    @Composable
    fun TransferOwnershipDialog(
        onDismissRequest: () -> Unit,
        document: Document, // 这个文档来自ViewModel
        onConfirmTransfer: (String) -> Unit // 使用所选成员ID回调
    ) {
        var selectedMember by remember { mutableStateOf<Member?>(null) }
        val currentUser = DocumentRepository.getCurrentUsers()
        val isCurrentUserOwner = document.owner.id == currentUser.id

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("转移文档所有权") },
            text = {
                Column {
                    if (isCurrentUserOwner) {
                        Text("请选择要转移所有权的成员：")
                        LazyColumn {
                            items(document.members) { member ->
                                if (member.permissionType != PermissionType.OWNER) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMember = member }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedMember == member,
                                            onClick = { selectedMember = member }
                                        )
                                        Text(
                                            member.user.name,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }else {
                                    Text("无操作权限")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isCurrentUserOwner) {
                    Button(
                        onClick = {
                            // Call the callback with the selected member ID
                            selectedMember?.user?.id?.let { memberId ->
                                onConfirmTransfer(memberId)
                            }
                        },
                        enabled = selectedMember != null
                    ) {
                        Text("确定")
                    }
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text("取消")
                }
            }
        )
    }
    @Composable
    fun BottomSheetContent(document: Document, onClose: () -> Unit) {
        // Get screen height
        val configuration = LocalConfiguration.current
        val screenHeight = SystemUIUtils.getScreenHeight(LocalContext.current)
        val statusBarHeight = SystemUIUtils.getStatusBarHeight(LocalContext.current)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight.dp - 56.dp - statusBarHeight.dp)
        ) {
            items(document.members) { member ->
                MemberListItem(
                    member = member,
                    documentOwner = document.owner,
                    currentDocument = document, // Ideally this also comes from the ViewModel
                    onPermissionChange = { newMember, newPermissionType ->
                        // Handle permission change (ideally in ViewModel)
                    },
                    onTransferOwnership = { member ->
                        // Handle transfer ownership (ideally in ViewModel)
                    }
                )
            }
        }
    }

    @Preview
    @Composable
    fun BottomSheetContentPreview() {
        val document = getMockDocuments().first()
        BottomSheetContent(document = document) {}
    }

    @Composable
    fun PermissionButton(permissionType: PermissionType, onClick: () -> Unit) {
        val backgroundColor = when (permissionType) {
            PermissionType.READ -> Color.LightGray
            PermissionType.EDIT -> Color.Cyan
            PermissionType.OWNER -> Color.Green
        }
        Button(
            onClick = onClick,
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
            enabled = permissionType != PermissionType.OWNER
        ) {
            Text(
                text = when (permissionType) {
                    PermissionType.READ -> "READ"
                    PermissionType.EDIT -> "EDIT"
                    PermissionType.OWNER -> "OWNER"
                },
                color = if (permissionType == PermissionType.OWNER) Color.White else Color.Black
            )
        }
    }
    @Composable
    fun MemberListItem(
        member: Member,
        documentOwner: User,
        currentDocument: Document,
        onPermissionChange: (Member, PermissionType) -> Unit,
        onTransferOwnership: (Member) -> Unit
    ) {
        //val userViewModel: UserViewModel = hiltViewModel()
        //val userState by userViewModel.userState.collectAsState()
        val currentUser = DocumentRepository.getCurrentUsers()
        val isCurrentUserOwner = documentOwner.id == currentUser.id

        // Dialog state
        var showPermissionChangeDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(member.user.avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(member.user.name)
                }
            },
            trailingContent = {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("权限修改") },
                        onClick = {
                            expanded = false
                            if (isCurrentUserOwner) {
                                showPermissionChangeDialog = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "无操作权限",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = isCurrentUserOwner
                    )
                    DropdownMenuItem(
                        text = { Text("转移所有权") },
                        onClick = {
                            expanded = false
                            if (isCurrentUserOwner) {
                                onTransferOwnership(member)
                            } else {
                                Toast.makeText(
                                    context,
                                    "无操作权限",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = isCurrentUserOwner
                    )
                }
            }
        )

        if (showPermissionChangeDialog) {
            ChangePermissionDialog(
                onDismissRequest = { showPermissionChangeDialog = false },
                currentMember = member,
                onPermissionChange = onPermissionChange
            )
        }
    }
    @Composable
    fun ChangePermissionDialog(
        onDismissRequest: () -> Unit,
        currentMember: Member,
        onPermissionChange: (Member, PermissionType) -> Unit
    ) {
        var selectedPermission by remember { mutableStateOf(currentMember.permissionType) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Change Permission") },
            text = {
                Column {
                    PermissionType.values().forEach { permissionType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPermission == permissionType,
                                onClick = { selectedPermission = permissionType }
                            )
                            Text(
                                text = when (permissionType) {
                                    PermissionType.READ -> "Read"
                                    PermissionType.EDIT -> "Edit"
                                    PermissionType.OWNER -> "Owner"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Update the permission and dismiss the dialog
                        onPermissionChange(currentMember, selectedPermission)
                        onDismissRequest()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        )
    }
}

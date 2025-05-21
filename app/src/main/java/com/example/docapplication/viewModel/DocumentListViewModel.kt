package com.example.docapplication.viewModel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.docapplication.Document
import com.example.docapplication.DocumentRepository
import com.example.docapplication.PermissionType
import kotlinx.coroutines.delay
import com.example.docapplication.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @author: playboi_YzY
 * @date: 2025/5/7 0:16
 * @description:
 * @version:
 */
class DocumentListViewModel : ViewModel() {
    // TODO: 这些只是为Mock数据准备，以后要移除
    private val _documents = MutableStateFlow<List<Document>>(DocumentRepository.getMockDocuments())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()
    private val _currentDocument = MutableStateFlow<Document?>(null)
    // 以state形式获取当前文档，
    // 在需要根据文档状态更新组件的都应该在这里获取document对象
    val currentDocument: StateFlow<Document?> = _currentDocument.asStateFlow()

    fun loadDocument(documentId: String) {
        val document = _documents.value.find { it.id == documentId }
        _currentDocument.value = document
    }
    suspend fun shareDocument(context: Context) {
        _currentDocument.value?.shareLink?.let { shareLink ->
            delay(1000)
            copyToClipboard(context, shareLink)
        } ?: Toast.makeText(context, "Share link not available", Toast.LENGTH_SHORT).show()
    }


    suspend fun deleteDocument(context: Context, onComplete: () -> Unit) {
        _currentDocument.value?.let { currentDocument ->
            // 显示确认对话框（在UI中实现）
            val result = true
            if (result) {
                delay(1000)
                _documents.value = _documents.value.filter { it.id != currentDocument.id }
                // 清除当前文档状态
                _currentDocument.value = null
                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                onComplete() // Navigate back or update UI
            } else {
                Toast.makeText(context, "删除取消", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(context, "没有可删除的文档", Toast.LENGTH_SHORT).show()
    }

    // 删除文档
    fun deleteDocumentFromList(documentId: String) {
        _documents.value = _documents.value.filter { it.id != documentId }
        if (_currentDocument.value?.id == documentId) {
            _currentDocument.value = null
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Share Link", text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(
            context,
            context.getString(R.string.link_copied_to_clipboard),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 更新文档的水印可见性状态，此时只针对Mock数据进行更新
     *
     * @param documentId 要更新的文档的ID。
     * @param canShowWatermark 新的水印可见性状态.
     */
    fun updateDocumentWatermarkStatus(documentId: String, canShowWatermark: Boolean) {
        val updatedDocuments = _documents.value.map { document ->
            if (document.id == documentId) {
                document.copy(canShowWatermark = canShowWatermark)
            } else {
                document
            }
        }
        _documents.value = updatedDocuments
        if (_currentDocument.value?.id == documentId) {
            _currentDocument.value = _currentDocument.value?.copy(canShowWatermark = canShowWatermark)
        }
    }

    /**
     * 转移文档所有权
     * 仅针对Mock数据进行更新
     * @param selectedMemberId 要转移所有权的用户的ID
     */
    fun transferOwnership(selectedMemberId: String) {
        _currentDocument.value?.let { currentDocument ->
            val updatedMembers = currentDocument.members.map { member ->
                when {
                    member.user.id == selectedMemberId -> member.copy(permissionType = PermissionType.OWNER)
                    member.permissionType == PermissionType.OWNER -> member.copy(permissionType = PermissionType.EDIT) // Demote current owner
                    else -> member
                }
            }
            val updatedDocument = currentDocument.copy(members = updatedMembers)
            _currentDocument.value = updatedDocument
            // 如果需要，还可以在主要文档列表中进行更新
            _documents.value = _documents.value.map { doc ->
                if (doc.id == updatedDocument.id) updatedDocument else doc
            }
            // 存储库中的更新
        }
    }
}
package com.example.docapplication


/**
 * @author: playboi_YzY
 * @date: 2025/5/6 23:58
 * @description:
 * @version:
 */

// Define Permission Types
enum class PermissionType {
    READ,       // Read-only access
    EDIT,       // Editable access
    OWNER       // Full control
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String // Add this field
)

// Data Classes
data class Document(
    val id: String,
    val name: String, // Add name field
    val url: String,
    val owner: User, //who is the owner of the document
    val members: List<Member>,
    val shareLink: String,
    val canShowWatermark: Boolean = true
)

data class Member(
    val user: User,
    var permissionType: PermissionType
)

// Constants for Mock Data (Improve readability and maintainability)
object MockData {
    val USER_ALICE = User(
        "1",
        "Alice",
        "alice@example.com",
        "https://pic1.zhimg.com/v2-c2a88aafd157095e153287fc813dec08_b.jpg"
    )
    val USER_BOB = User(
        "2",
        "Bob",
        "bob@example.com",
        "https://th.bing.com/th/id/OIP.AS2Fb0QaouQpkfJIb_XN9gHaFs?rs=1&pid=ImgDetMain"
    )
    val USER_CHARLIE = User(
        "3",
        "Charlie",
        "charlie@example.com",
        "https://th.bing.com/th/id/OIP.zWErXp3wZHVHT66urRu9dAHaGk?rs=1&pid=ImgDetMain"
    )
    val USER_DAVID = User(
        "4",
        "David",
        "david@example.com",
        "https://c-ssl.dtstatic.com/uploads/blog/202008/01/20200801204406_sbzsd.thumb.400_0.jpg"
    )
    val USER_EMILY = User(
        "5",
        "Emily",
        "emily@example.com",
        "https://c-ssl.dtstatic.com/uploads/blog/202008/01/20200801204406_sbzsd.thumb.400_0.jpg"
    )
    val USER_FRANK = User(
        "6",
        "Frank",
        "frank@example.com",
        "https://c-ssl.dtstatic.com/uploads/blog/202008/01/20200801204406_sbzsd.thumb.400_0.jpg"
    )
    val USER_GRACE = User(
        "7",
        "Grace",
        "grace@example.com",
        "https://example.com/avatars/grace.png"
    )
    val USER_HENRY = User(
        "8",
        "Henry",
        "henry@example.com",
        "https://example.com/avatars/henry.png"
    )
    val USER_ISABELLA = User(
        "9",
        "Isabella",
        "isabella@example.com",
        "https://example.com/avatars/isabella.png"
    )
    val USER_JACK = User(
        "10",
        "Jack",
        "jack@example.com",
        "https://example.com/avatars/jack.png"
    )
    val USER_KATE = User(
        "11",
        "Kate",
        "kate@example.com",
        "https://example.com/avatars/kate.png"
    )
    val USER_LIAM = User(
        "12",
        "Liam",
        "liam@example.com",
        "https://example.com/avatars/liam.png"
    )

    val allUsers = listOf(
        USER_ALICE,
        USER_BOB,
        USER_CHARLIE,
        USER_DAVID,
        USER_EMILY,
        USER_FRANK,
        USER_GRACE,
        USER_HENRY,
        USER_ISABELLA,
        USER_JACK,
        USER_KATE,
        USER_LIAM
    )

    val DOCUMENT_1 = Document(
        "doc1",
        "document1", // Add document name
        "https://cryptpad.fr/pad/#/2/pad/edit/iDPLePb8rkL34NkTg5iRBo8A/",
        USER_ALICE,
        listOf(
            Member(USER_ALICE, PermissionType.OWNER),
            Member(USER_BOB, PermissionType.EDIT),
            Member(USER_CHARLIE, PermissionType.READ)
        ),
        "https://cryptpad.fr/pad/#/2/pad/edit/iDPLePb8rkL34NkTg5iRBo8A/",
        true
    )

    val DOCUMENT_2 = Document(
        "doc2",
        "document2", // Add document name
        "https://cryptpad.fr/doc/",
        USER_BOB,
        listOf(
            Member(USER_BOB, PermissionType.OWNER),
            Member(USER_CHARLIE, PermissionType.EDIT)
        ),
        "https://cryptpad.fr/doc/",
        true
    )

    val DOCUMENT_3 = Document(
        "doc3",
        "文档3", // Add document name
        "https://cryptpad.fr/sheet/#/2/sheet/edit/bsGd2rsD2MyIDnVvb4quQTT9/",
        USER_DAVID,
        listOf(
            Member(USER_DAVID, PermissionType.OWNER),
            Member(USER_EMILY, PermissionType.EDIT),
            Member(USER_FRANK, PermissionType.READ),
            Member(USER_GRACE, PermissionType.READ)
        ),
        "https://cryptpad.fr/sheet/#/2/sheet/edit/bsGd2rsD2MyIDnVvb4quQTT9/",
        true
    )

    val DOCUMENT_4 = Document(
        "doc4",
        "文档4", // Add document name
        "https://cryptpad.fr/pad/#/2/pad/edit/J1lDO0Fnyt84iEEEmhWpdGik/",
        USER_HENRY,
        listOf(
            Member(USER_HENRY, PermissionType.OWNER),
            Member(USER_ISABELLA, PermissionType.EDIT),
            Member(USER_JACK, PermissionType.READ),
            Member(USER_KATE, PermissionType.EDIT),
            Member(USER_LIAM, PermissionType.READ)
        ),
        "https://cryptpad.fr/pad/#/2/pad/edit/J1lDO0Fnyt84iEEEmhWpdGik/",
        true
    )

    //All the documents
    val allDocuments = listOf(DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4)
}

// Mock Data Provider (Clearer purpose)
object DocumentRepository {
    fun getMockDocuments(): List<Document> {
        return MockData.allDocuments
    }

    fun getMockUsers(): List<User> {
        return MockData.allUsers
    }

    // 获取当前用户
    fun getCurrentUsers(): User {
        return MockData.USER_ALICE
    }
}

fun main() {
    //Example how to get the Documents
    val documents = DocumentRepository.getMockDocuments()
    println(documents)
    val users = DocumentRepository.getMockUsers()
    println(users)
}
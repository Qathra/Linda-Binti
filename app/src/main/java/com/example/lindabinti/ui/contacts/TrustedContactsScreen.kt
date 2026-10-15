package com.example.lindabinti.ui.contacts

import android.Manifest
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.example.lindabinti.ui.common.LindaProtectedScaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.R
import com.example.lindabinti.data.repository.ContactRepository
import com.example.lindabinti.ui.common.LindaSectionCard
import com.example.lindabinti.ui.common.ProtectedTopBar
import com.example.lindabinti.ui.locale.localizedContactMessage
import com.example.lindabinti.util.PermissionHelper
import kotlinx.coroutines.launch

@Composable
fun TrustedContactsScreen(
    onBack: () -> Unit,
    onQuickHide: () -> Unit
) {
    val mod = androidx.compose.ui.Modifier
    val context = LocalContext.current
    val app = LindaBintiApp.instance
    val contacts by app.contactRepository.observeContacts()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    fun applyPickedContact(uri: Uri?) {
        if (uri == null) return
        val resolver = context.contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            val contactId = cursor.getString(idIndex)
            val displayName = cursor.getString(nameIndex) ?: ""
            val hasPhone = cursor.getString(hasPhoneIndex)?.toIntOrNull() ?: 0
            var number = ""
            if (hasPhone > 0) {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                    arrayOf(contactId),
                    null
                )?.use { phones ->
                    if (phones.moveToFirst()) {
                        val numIdx = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        number = phones.getString(numIdx) ?: ""
                    }
                }
            }
            name = displayName
            phone = number.filter { it.isDigit() || it == '+' }
            showAdd = true
            error = null
        }
    }

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri -> applyPickedContact(uri) }

    val requestContactsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickContact.launch(null)
        else error = context.getString(R.string.contacts_permission_needed)
    }

    LindaProtectedScaffold(
        topBar = {
            ProtectedTopBar(
                title = stringResource(R.string.contacts_title),
                onBack = onBack,
                onQuickHide = onQuickHide
            )
        },
    ) { padding ->
        Column(
            modifier = mod
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(
                    R.string.contacts_count,
                    contacts.size,
                    ContactRepository.MAX_CONTACTS
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = mod.weight(1f, fill = true)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    LindaSectionCard {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = mod.fillMaxWidth()
                        ) {
                            Column(modifier = mod.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.SemiBold)
                                Text(contact.phoneE164)
                                if (contact.relationship.isNotBlank()) {
                                    Text(
                                        contact.relationship,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        if (contacts.size <= ContactRepository.MIN_CONTACTS) {
                                            error = context.getString(R.string.contacts_keep_one)
                                        } else {
                                            app.contactRepository.deleteContact(contact.id)
                                            error = null
                                        }
                                    }
                                }
                            ) { Text(stringResource(R.string.action_delete)) }
                        }
                    }
                }
            }

            if (showAdd && contacts.size < ContactRepository.MAX_CONTACTS) {
                LindaSectionCard {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.contacts_name)) },
                        modifier = mod.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.contacts_phone)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = mod.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text(stringResource(R.string.contacts_relationship)) },
                        modifier = mod.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val result = app.contactRepository.addContact(name, phone, relationship)
                                if (result.isSuccess) {
                                    name = ""
                                    phone = ""
                                    relationship = ""
                                    showAdd = false
                                    error = null
                                } else {
                                    error = result.exceptionOrNull()?.localizedContactMessage(context)
                                }
                            }
                        },
                        modifier = mod.fillMaxWidth()
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }

            if (contacts.size < ContactRepository.MAX_CONTACTS) {
                OutlinedButton(
                    onClick = {
                        if (PermissionHelper.contactsGranted(context)) {
                            pickContact.launch(null)
                        } else {
                            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    modifier = mod.fillMaxWidth()
                ) { Text(stringResource(R.string.contacts_pick)) }

                OutlinedButton(
                    onClick = { showAdd = !showAdd },
                    modifier = mod.fillMaxWidth()
                ) {
                    Text(
                        if (showAdd) stringResource(R.string.action_cancel)
                        else stringResource(R.string.contacts_add)
                    )
                }
            }
        }
    }
}

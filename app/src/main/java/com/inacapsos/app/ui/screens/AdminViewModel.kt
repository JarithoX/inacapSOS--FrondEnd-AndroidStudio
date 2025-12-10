package com.inacapsos.app.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.inacapsos.app.data.model.Guardia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _listaGuardias = MutableStateFlow<List<Guardia>>(emptyList())
    val listaGuardias = _listaGuardias.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        obtenerGuardias()
    }

    private fun obtenerGuardias() {
        // Estás leyendo de la colección "usuario"
        db.collection("usuario")
            .whereEqualTo("rol", "guardia")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (value != null) {
                    val nuevosGuardias = value.documents.map { doc ->
                        Guardia(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "Sin nombre",
                            email = doc.getString("email") ?: "Sin email"
                        )
                    }
                    _listaGuardias.value = nuevosGuardias
                    _isLoading.value = false
                }
            }
    }

    fun eliminarGuardia(guardiaId: String) {
        // CORRECCIÓN: Cambié "users" por "usuario" para que coincida con tu base de datos
        db.collection("usuario").document(guardiaId)
            .delete()
            .addOnSuccessListener {
                println("Guardia eliminado correctamente")
            }
            .addOnFailureListener { e ->
                println("Error al eliminar: ${e.message}")
            }
    }
}
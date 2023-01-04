package com.example.nilopartner

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nilopartner.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange.Type.ADDED
import com.google.firebase.firestore.DocumentChange.Type.MODIFIED
import com.google.firebase.firestore.DocumentChange.Type.REMOVED
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ProductAdapter
    private lateinit var firestoreListener: ListenerRegistration
    private var productSelected: Product? = null

    private var registerActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val response = IdpResponse.fromResultIntent(it.data)

        if (it.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Toast.makeText(this, "Welcome", Toast.LENGTH_LONG).show()
            }
        } else {
            if (response == null) {
                Toast.makeText(this, "See you around", Toast.LENGTH_LONG).show()
                finish()
            } else {
                response.error?.let { error ->
                    if (error.errorCode == ErrorCodes.NO_NETWORK) {
                        Toast.makeText(this, "You need internet", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error code ${error.errorCode}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configAuth()
        configRecyclerView()
        // configFirestore()
        configButtons()
    }

    private fun configButtons() {
        binding.efab.setOnClickListener {
            productSelected = null
            AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
        }
    }

    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(
                this@MainActivity,
                3,
                GridLayoutManager.HORIZONTAL,
                false
            )
            adapter = this@MainActivity.adapter
        }

       /* (1..20).forEach {
            val product = Product(
                it.toString(),
                "Producto $it",
                "Este producto es el $it",
                "",
                it,
                it * 1.1
            )
            adapter.add(product)
        }*/
    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                supportActionBar?.title = auth.currentUser?.displayName
                binding.nsvProducts.visibility = View.VISIBLE
                binding.llProgress.visibility = View.GONE
                binding.efab.show()
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )
                registerActivity.launch(
                    AuthUI
                        .getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build()
                )
            }
        }
    }

    private fun configFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PRODUCTS)
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot) {
                    val product = document.toObject(Product::class.java)
                    product.id = document.id
                    adapter.add(product)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_LONG).show()
            }
    }

    private fun configFirestoreRealTime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)
        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges) {
                val product = snapshot.document.toObject(Product::class.java)
                product.id = snapshot.document.id
                when (snapshot.type) {
                    ADDED -> adapter.add(product)
                    MODIFIED -> adapter.update(product)
                    REMOVED -> adapter.delete(product)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configFirestoreRealTime()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onPause() {
        super.onPause()
        firestoreListener.remove()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session finalized", Toast.LENGTH_LONG).show()
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.llProgress.visibility = View.VISIBLE
                            binding.efab.hide()
                        } else {
                            Toast.makeText(this, "There was an error", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(product: Product) {
        productSelected = product
        AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
    }

    override fun onLongClick(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)
        product.id?.let { id ->
            productRef.document(id)
                .delete()
                .addOnFailureListener {
                    Toast.makeText(this, "There was an error", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun getProductSelected(): Product? = productSelected
}

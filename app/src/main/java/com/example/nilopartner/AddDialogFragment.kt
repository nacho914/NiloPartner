package com.example.nilopartner

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.nilopartner.databinding.FragmentDialogAddBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener {

    private var binding: FragmentDialogAddBinding? = null

    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var product: Product? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))
            binding?.let {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("Agregar producto")
                    .setPositiveButton("Agregar", null)
                    .setNegativeButton("Cancelar", null)
                    .setView(it.root)

                val dialog = builder.create()
                dialog.setOnShowListener(this)

                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        initProduct()
        val dialog = dialog as? AlertDialog
        dialog?.let { it ->
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {
                binding?.let { binding ->
                    if (product == null) {
                        val product = Product(
                            name = binding.etName.text.toString().trim(),
                            description = binding.etDescription.text.toString().trim(),
                            quantity = binding.etQuantity.text.toString().toInt(),
                            price = binding.etPrice.text.toString().toDouble()
                        )
                        save(product)
                    } else {
                        product?.apply {
                            name = binding.etName.text.toString().trim()
                            description = binding.etDescription.text.toString().trim()
                            quantity = binding.etQuantity.text.toString().toInt()
                            price = binding.etPrice.text.toString().toDouble()

                            update(this)
                        }
                    }
                }
            }

            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun update(product: Product) {
        val db = FirebaseFirestore.getInstance()
        product.id?.let { id ->
            db.collection(Constants.COLL_PRODUCTS).document(id)
                .set(product)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Producto Actualizado", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error al actualizar", Toast.LENGTH_LONG).show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                    dismiss()
                }
        }
    }

    private fun initProduct() {
        product = (activity as? MainAux)?.getProductSelected()
        product?.let { product ->
            binding?.let {
                it.etName.setText(product.name)
                it.etDescription.setText(product.description)
                it.etPrice.setText(product.price.toString())
                it.etQuantity.setText(product.quantity.toString())
            }
        }
    }

    private fun save(product: Product) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_PRODUCTS)
            .add(product)
            .addOnSuccessListener {
                Toast.makeText(activity, "Producto añadido", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Error al insertar", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                enableUI(true)
                dismiss()
            }
    }

    private fun enableUI(enable: Boolean) {
        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable
        binding?.let {
            with(it) {
                etName.isEnabled = enable
                etDescription.isEnabled = enable
                etQuantity.isEnabled = enable
                etPrice.isEnabled = enable
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
